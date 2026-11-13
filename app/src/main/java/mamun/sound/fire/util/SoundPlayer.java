package mamun.sound.fire.util;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.StringRes;

import mamun.sound.fire.R;
import mamun.sound.fire.data.SoundRepository;
import mamun.sound.fire.model.Sound;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Plays one sound at a time. Short sounds (up to 5 s) use SoundPool for low latency, longer ones use MediaPlayer.
 * Audio is downloaded on the first tap and cached on disk, so it plays offline afterwards.
 *
 * MAIN THREAD ONLY: call every public method from the UI thread.
 *
 * On purpose it does NOT request audio focus, so a phone call or game audio is not paused or ducked.
 * Sound goes out on the media stream, so the phone's media volume matters.
 */
public final class SoundPlayer {

    public enum Result { OK, PREMIUM_REQUIRED }

    public enum PlayerError {
        UNAVAILABLE(R.string.err_audio_unavailable),
        NO_INTERNET(R.string.err_audio_no_internet),
        DOWNLOAD_FAILED(R.string.err_audio_download),
        PLAY_FAILED(R.string.err_audio_play);
        public final @StringRes int messageRes;
        PlayerError(int r) { messageRes = r; }
    }

    /** Errors come back only to the screen that asked to play (not broadcast). */
    public interface ErrorCallback { void onError(PlayerError error); }

    /** State goes to every listening screen so all grids and the mini player stay in sync. */
    public interface Listener { void onStateChanged(long playingId, long loadingId); }

    private static final long SHORT_MS = 5000;
    private static final long MAX_BYTES = 10L * 1024 * 1024;        // one sound file
    private static final long CACHE_LIMIT = 100L * 1024 * 1024;     // whole audio cache
    private static final int POOL_MAX = 24;                         // sounds kept loaded in SoundPool

    private static SoundPlayer instance;
    public static synchronized SoundPlayer get(Context c) {
        if (instance == null) instance = new SoundPlayer(c.getApplicationContext());
        return instance;
    }

    private static final class Pending {
        final Sound sound; final long durationMs; final int seq; final ErrorCallback err;
        Pending(Sound s, long d, int q, ErrorCallback e) { sound = s; durationMs = d; seq = q; err = e; }
    }

    private final Context app;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final AudioAttributes attrs = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build();

    private final SoundPool pool;
    private final Map<String, Integer> poolIds = new LinkedHashMap<>();   // cache file name -> sample id (oldest first)
    private final Set<Integer> loaded = new HashSet<>();
    private final Map<Integer, Pending> pending = new HashMap<>();
    private final Map<String, Long> durations = new HashMap<>();

    private MediaPlayer mp;
    private int streamId;
    private Runnable endTask;
    private int seq;                       // every new request/stop bumps it; stale async results are ignored
    private long playingId = -1, loadingId = -1;
    private String playingName = "";
    private float volume = 1f;

    private SoundPlayer(Context app) {
        this.app = app;
        pool = new SoundPool.Builder().setMaxStreams(1).setAudioAttributes(attrs).build();
        pool.setOnLoadCompleteListener((p, sampleId, status) -> {
            Pending pd = pending.remove(sampleId);
            if (status == 0) loaded.add(sampleId);
            if (pd == null || pd.seq != seq) return;
            if (status != 0) { setState(-1, -1); pd.err.onError(PlayerError.PLAY_FAILED); return; }
            fire(sampleId, pd.sound, pd.durationMs, pd.seq, pd.err);
        });
    }

    // ---------- public API ----------

    public void addListener(Listener l) { listeners.add(l); }
    public void removeListener(Listener l) { listeners.remove(l); }
    public long getPlayingId() { return playingId; }
    public long getLoadingId() { return loadingId; }
    public String getPlayingName() { return playingName; }

    /** 0.0 to 1.0, applies immediately to the sound that is playing. */
    public void setVolume(float v) {
        volume = Math.max(0f, Math.min(1f, v));
        if (mp != null) { try { mp.setVolume(volume, volume); } catch (IllegalStateException ignored) {} }
        if (streamId != 0) pool.setVolume(streamId, volume, volume);
    }

    /**
     * Tap on a card. Same sound again = stop. A free user on a premium sound gets PREMIUM_REQUIRED
     * (the screen then opens the Premium screen). Pass Session.isPremium() as userPremium.
     */
    public Result play(Sound s, boolean userPremium, ErrorCallback err) {
        if (s.id == playingId || s.id == loadingId) { stop(); return Result.OK; }
        if (s.isPremium() && !userPremium) return Result.PREMIUM_REQUIRED;     // checked on EVERY play, cached or not
        if (s.audioUrl == null || s.audioUrl.isEmpty()) { err.onError(PlayerError.UNAVAILABLE); return Result.OK; }

        Haptics.tick(app);
        releaseCurrent();
        playingName = s.name;
        final int mySeq = ++seq;
        File f = cacheFile(s);

        if (f.exists() && f.length() > 0) {
            f.setLastModified(System.currentTimeMillis());       // marks it as recently used
            start(s, f, mySeq, err);
            return Result.OK;
        }
        if (!s.audioUrl.startsWith("https://")) { err.onError(PlayerError.UNAVAILABLE); return Result.OK; }

        setState(-1, s.id);                                       // card shows a spinner
        io.execute(() -> {
            PlayerError e = download(s, f);
            main.post(() -> {
                if (mySeq != seq) return;                         // user tapped something else meanwhile
                if (e == null) start(s, f, mySeq, err);
                else { setState(-1, -1); err.onError(e); }
            });
        });
        return Result.OK;
    }

    /** Panic stop: silence everything now. */
    public void stop() {
        seq++;
        releaseCurrent();
        setState(-1, -1);
    }

    /** On logout: stop and delete every downloaded sound. */
    public void wipe() {
        stop();
        for (int id : poolIds.values()) pool.unload(id);
        poolIds.clear(); loaded.clear(); pending.clear(); durations.clear();
        io.execute(() -> {
            File[] files = audioDir().listFiles();
            if (files != null) for (File f : files) f.delete();
        });
    }

    // ---------- playback ----------

    private void start(Sound s, File f, int mySeq, ErrorCallback err) {
        long dur = durationMs(f);
        if (dur > 0 && dur <= SHORT_MS) playShort(s, f, dur, mySeq, err);
        else playLong(s, f, mySeq, err);
    }

    private void playShort(Sound s, File f, long dur, int mySeq, ErrorCallback err) {
        Integer sample = poolIds.get(f.getName());
        if (sample != null && loaded.contains(sample)) { fire(sample, s, dur, mySeq, err); return; }
        if (sample == null) {
            evictOldest();
            sample = pool.load(f.getAbsolutePath(), 1);
            poolIds.put(f.getName(), sample);
        }
        pending.put(sample, new Pending(s, dur, mySeq, err));    // plays when onLoadComplete fires
        setState(-1, s.id);
    }

    private void fire(int sampleId, Sound s, long dur, int mySeq, ErrorCallback err) {
        streamId = pool.play(sampleId, volume, volume, 1, 0, 1f);
        if (streamId == 0) { setState(-1, -1); err.onError(PlayerError.PLAY_FAILED); return; }
        onStarted(s);
        // SoundPool has no "finished" callback, so end the playing state after the known duration
        endTask = () -> { if (mySeq == seq) finishPlayback(); };
        main.postDelayed(endTask, dur + 150);
    }

    private void playLong(Sound s, File f, int mySeq, ErrorCallback err) {
        try {
            MediaPlayer m = new MediaPlayer();
            m.setAudioAttributes(attrs);
            m.setDataSource(f.getAbsolutePath());
            m.setVolume(volume, volume);
            m.setOnPreparedListener(p -> {
                if (mySeq != seq) { p.release(); return; }
                p.start();
                onStarted(s);
            });
            m.setOnCompletionListener(p -> { if (mySeq == seq) finishPlayback(); });
            m.setOnErrorListener((p, what, extra) -> {
                if (mySeq == seq) { releaseCurrent(); setState(-1, -1); err.onError(PlayerError.PLAY_FAILED); }
                return true;
            });
            mp = m;
            setState(-1, s.id);
            m.prepareAsync();
        } catch (Exception e) {
            releaseCurrent();
            setState(-1, -1);
            err.onError(PlayerError.PLAY_FAILED);
        }
    }

    private void onStarted(Sound s) {
        setState(s.id, -1);
        SoundRepository.get(app).reportPlay(s.id);               // trending counter, best effort
    }

    private void finishPlayback() { releaseCurrent(); setState(-1, -1); }

    /** Stops audio and clears ids silently (callers decide when to notify). */
    private void releaseCurrent() {
        if (endTask != null) { main.removeCallbacks(endTask); endTask = null; }
        if (streamId != 0) { pool.stop(streamId); streamId = 0; }
        if (mp != null) {
            try { if (mp.isPlaying()) mp.stop(); } catch (IllegalStateException ignored) {}
            mp.release();
            mp = null;
        }
        pending.clear();
        playingId = -1;
        loadingId = -1;
    }

    private void setState(long playing, long loading) {
        playingId = playing;
        loadingId = loading;
        for (Listener l : listeners) l.onStateChanged(playing, loading);
    }

    private void evictOldest() {
        if (poolIds.size() < POOL_MAX) return;
        Iterator<Map.Entry<String, Integer>> it = poolIds.entrySet().iterator();
        if (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            pool.unload(e.getValue());
            loaded.remove(e.getValue());
            it.remove();
        }
    }

    // ---------- files ----------

    private File audioDir() {
        File d = new File(app.getFilesDir(), "audio");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    /** Name includes a hash of the URL: when the admin replaces the audio, the new file is downloaded. */
    private File cacheFile(Sound s) {
        return new File(audioDir(), s.id + "_" + Integer.toHexString(s.audioUrl.hashCode()) + ".snd");
    }

    private long durationMs(File f) {
        Long cached = durations.get(f.getName());
        if (cached != null) return cached;
        long dur = -1;
        MediaMetadataRetriever r = new MediaMetadataRetriever();
        try {
            r.setDataSource(f.getAbsolutePath());
            String d = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (d != null) dur = Long.parseLong(d);
        } catch (Exception ignored) {
            // unknown duration: fall back to MediaPlayer
        } finally {
            try { r.release(); } catch (Exception ignored) {}
        }
        durations.put(f.getName(), dur);
        return dur;
    }

    /** Runs on the background thread. Returns null on success. */
    private PlayerError download(Sound s, File dest) {
        File part = new File(dest.getParentFile(), dest.getName() + ".part");
        try (Response r = PlainHttp.client().newCall(new Request.Builder().url(s.audioUrl).build()).execute()) {
            if (!r.isSuccessful() || r.body() == null) return PlayerError.DOWNLOAD_FAILED;
            if (r.body().contentLength() > MAX_BYTES) return PlayerError.DOWNLOAD_FAILED;

            long total = 0;
            try (InputStream in = r.body().byteStream(); FileOutputStream out = new FileOutputStream(part)) {
                byte[] buf = new byte[16 * 1024];
                int n;
                while ((n = in.read(buf)) != -1) {
                    total += n;
                    if (total > MAX_BYTES) { part.delete(); return PlayerError.DOWNLOAD_FAILED; }   // size guard even without Content-Length
                    out.write(buf, 0, n);
                }
            }
            if (total == 0 || !part.renameTo(dest)) { part.delete(); return PlayerError.DOWNLOAD_FAILED; }
            deleteOldVersions(dest.getParentFile(), s.id, dest);
            trimCache(dest.getParentFile());
            return null;
        } catch (IOException e) {
            part.delete();
            boolean offline = e instanceof UnknownHostException || e instanceof ConnectException || e instanceof SocketTimeoutException;
            return offline ? PlayerError.NO_INTERNET : PlayerError.DOWNLOAD_FAILED;
        }
    }

    private void deleteOldVersions(File dir, long id, File keep) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) if (f.getName().startsWith(id + "_") && !f.equals(keep) && !f.getName().endsWith(".part")) f.delete();
    }

    /** Keep the cache under CACHE_LIMIT by deleting the least recently played files first. */
    private void trimCache(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        long total = 0;
        for (File f : files) total += f.length();
        if (total <= CACHE_LIMIT) return;
        List<File> sorted = new ArrayList<>(Arrays.asList(files));
        Collections.sort(sorted, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
        for (File f : sorted) {
            if (total <= CACHE_LIMIT) break;
            total -= f.length();
            f.delete();
        }
    }
}
