package mamun.sound.fire.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import mamun.sound.fire.model.ApiResponse;
import mamun.sound.fire.model.AppSettings;
import mamun.sound.fire.model.BannersData;
import mamun.sound.fire.model.NoticesData;
import mamun.sound.fire.model.CategoriesData;
import mamun.sound.fire.model.EmptyData;
import mamun.sound.fire.model.FavoriteIds;
import mamun.sound.fire.model.FavoriteRequest;
import mamun.sound.fire.model.FavoriteResult;
import mamun.sound.fire.model.SoundRef;
import mamun.sound.fire.model.SoundsData;
import mamun.sound.fire.network.Api;
import mamun.sound.fire.network.ApiClient;
import mamun.sound.fire.network.ApiError;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;

/**
 * One place for sound data. Network first; when the phone is offline the last good response
 * is read from disk, so grids still show cached sounds (images come from Glide's own disk cache).
 * Callbacks run on the main thread. Check isAdded() in fragments before touching views.
 */
public final class SoundRepository {

    public interface Callback<T> {
        void onData(T data, boolean fromCache);
        void onError(ApiError error);
    }

    private static SoundRepository instance;
    public static synchronized SoundRepository get(Context c) {
        if (instance == null) instance = new SoundRepository(c.getApplicationContext());
        return instance;
    }

    private final Context app;
    private final Gson gson = new Gson();
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private SoundRepository(Context app) { this.app = app; }

    // ---- ready-made calls ----

    /** cacheKey = e.g. "home_new" or "cat_3". Pass null for pages after the first (never cached). */
    public void sounds(String cacheKey, Long categoryId, String sort, int limit, int offset, Callback<SoundsData> cb) {
        load(cacheKey, ApiClient.get().sounds(categoryId, null, sort, limit, offset), SoundsData.class, cb);
    }

    /** Search is never cached. */
    public void search(String query, Callback<SoundsData> cb) {
        load(null, ApiClient.get().sounds(null, query, null, 60, 0), SoundsData.class, cb);
    }

    public void categories(Callback<CategoriesData> cb) {
        load("categories", ApiClient.get().categories(), CategoriesData.class, cb);
    }

    public void favoriteSounds(Callback<SoundsData> cb) {
        load("fav_sounds", ApiClient.get().favoriteSounds(), SoundsData.class, cb);
    }

    public void favoriteIds(Callback<FavoriteIds> cb) {
        load("fav_ids", ApiClient.get().favoriteIds(), FavoriteIds.class, cb);
    }

    public void setFavorite(long soundId, boolean add, Api.Result<FavoriteResult> cb) {
        Api.call(ApiClient.get().setFavorite(new FavoriteRequest(add ? "add" : "remove", soundId)), cb);
    }

    /** Telegram links etc. Cached, so the links still work offline. */
    public void appSettings(Callback<AppSettings> cb) {
        load("app_settings", ApiClient.get().appSettings(), AppSettings.class, cb);
    }

    /** Admin messages. Cached, so the list still opens offline. */
    public void notices(Callback<NoticesData> cb) {
        load("notices", ApiClient.get().notices(), NoticesData.class, cb);
    }

    /** Home slider. Cached. */
    public void banners(Callback<BannersData> cb) {
        load("banners", ApiClient.get().banners(), BannersData.class, cb);
    }

    /** Fire and forget. Offline or failed counts are simply lost. */
    public void reportPlay(long soundId) {
        Api.call(ApiClient.get().playCount(new SoundRef(soundId)), new Api.Result<EmptyData>() {
            @Override public void onSuccess(EmptyData d) {}
            @Override public void onError(ApiError e) {}
        });
    }

    /** On logout: remove cached lists so the next user never sees them. */
    public void wipe() {
        io.execute(() -> {
            File[] files = dir().listFiles();
            if (files != null) for (File f : files) f.delete();
        });
    }

    // ---- network first, disk fallback ----

    public <D> void load(String key, Call<ApiResponse<D>> call, Class<D> cls, Callback<D> cb) {
        Api.call(call, new Api.Result<D>() {
            @Override public void onSuccess(D data) {
                cb.onData(data, false);
                if (key != null) io.execute(() -> write(key, gson.toJson(data)));
            }
            @Override public void onError(ApiError e) {
                if (!e.isNetwork() || key == null) { cb.onError(e); return; }
                io.execute(() -> {
                    D cached = read(key, cls);
                    main.post(() -> { if (cached != null) cb.onData(cached, true); else cb.onError(e); });
                });
            }
        });
    }

    private File dir() {
        File d = new File(app.getFilesDir(), "lists");
        if (!d.exists()) d.mkdirs();
        return d;
    }
    private File file(String key) { return new File(dir(), key.replaceAll("[^a-zA-Z0-9_]", "_") + ".json"); }

    private void write(String key, String json) {
        try (FileWriter w = new FileWriter(file(key))) { w.write(json); } catch (Exception ignored) { /* cache is best effort */ }
    }

    private <D> D read(String key, Class<D> cls) {
        File f = file(key);
        if (!f.exists()) return null;
        try (Reader r = new FileReader(f)) { return gson.fromJson(r, cls); } catch (Exception e) { return null; }
    }
}
