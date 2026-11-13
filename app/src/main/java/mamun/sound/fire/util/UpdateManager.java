package mamun.sound.fire.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Downloads the new APK (with resume), verifies SHA-256, and opens the system installer.
 * The installer only accepts an APK signed with the SAME keystore as the installed app.
 */
public final class UpdateManager {
    private UpdateManager() {}

    public enum Error { UNAVAILABLE, NETWORK, HTTP, CHECKSUM, DISK }

    public interface Callback {
        void onProgress(int percent);
        void onDone(File apk);
        void onError(Error error);
    }

    public static final class Task {
        private volatile boolean cancelled;
        public void cancel() { cancelled = true; }
    }

    /**
     * Separate, plain client on purpose: ApiClient's interceptor adds the auth token and device id,
     * and those must never be sent to whatever host serves the APK.
     */
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static Task download(Context ctx, String url, String sha256, int versionCode, Callback cb) {
        Task task = new Task();
        Context app = ctx.getApplicationContext();
        IO.execute(() -> run(app, url, sha256, versionCode, task, cb));
        return task;
    }

    private static void run(Context app, String url, String sha, int code, Task t, Callback cb) {
        // HTTPS only, and a SHA-256 is required: never install an unverified file
        if (url == null || !url.startsWith("https://") || sha == null || sha.trim().length() != 64) {
            fail(cb, Error.UNAVAILABLE);
            return;
        }
        sha = sha.trim();
        File dir = new File(app.getFilesDir(), "updates");
        if (!dir.exists() && !dir.mkdirs()) { fail(cb, Error.DISK); return; }
        File apk = new File(dir, "soundbomb-" + code + ".apk");
        File part = new File(dir, "soundbomb-" + code + ".apk.part");

        try {
            deleteOthers(dir, apk, part);
            // Already downloaded and valid (user cancelled the installer last time)
            if (apk.exists()) {
                if (sha256Of(apk).equalsIgnoreCase(sha)) { ok(cb, apk); return; }
                apk.delete();
            }

            for (int attempt = 0; attempt < 2; attempt++) {
                long have = part.exists() ? part.length() : 0;
                Request.Builder rb = new Request.Builder().url(url);
                if (have > 0) rb.header("Range", "bytes=" + have + "-");   // resume

                try (Response r = CLIENT.newCall(rb.build()).execute()) {
                    if (r.code() == 416) { part.delete(); continue; }       // bad partial file: start over
                    if (!r.isSuccessful() || r.body() == null) { fail(cb, Error.HTTP); return; }

                    boolean resumed = r.code() == 206;
                    if (!resumed) have = 0;                                  // server ignored Range: full file
                    long len = r.body().contentLength();
                    long total = len > 0 ? len + have : -1;

                    try (InputStream in = r.body().byteStream();
                         FileOutputStream out = new FileOutputStream(part, resumed)) {
                        byte[] buf = new byte[16 * 1024];
                        int n; long received = have; int last = -1;
                        while ((n = in.read(buf)) != -1) {
                            if (t.cancelled) return;
                            out.write(buf, 0, n);
                            received += n;
                            if (total > 0) {
                                int p = (int) (received * 100 / total);
                                if (p != last) { last = p; progress(cb, p); }
                            }
                        }
                    }
                }

                // Verify BEFORE the file gets its final name or reaches the installer
                if (!sha256Of(part).equalsIgnoreCase(sha)) { part.delete(); fail(cb, Error.CHECKSUM); return; }
                if (!part.renameTo(apk)) { fail(cb, Error.DISK); return; }
                ok(cb, apk);
                return;
            }
            fail(cb, Error.HTTP);
        } catch (IOException e) {
            if (!t.cancelled) fail(cb, Error.NETWORK);
        }
    }

    /** Remove APKs from older update attempts. */
    private static void deleteOthers(File dir, File keepApk, File keepPart) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) if (!f.equals(keepApk) && !f.equals(keepPart)) f.delete();
    }

    static String sha256Of(File f) throws IOException {
        try (FileInputStream in = new FileInputStream(f)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[16 * 1024];
            int n;
            while ((n = in.read(buf)) != -1) md.update(buf, 0, n);
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);   // SHA-256 always exists on Android
        }
    }

    private static void progress(Callback cb, int p) { MAIN.post(() -> cb.onProgress(p)); }
    private static void ok(Callback cb, File f)      { MAIN.post(() -> cb.onDone(f)); }
    private static void fail(Callback cb, Error e)   { MAIN.post(() -> cb.onError(e)); }

    // ---- install ----

    /** Android 8+ needs the per-app "Install unknown apps" switch. */
    public static boolean canInstall(Context c) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || c.getPackageManager().canRequestPackageInstalls();
    }

    public static void openInstallSettings(Activity a) {
        Intent i = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + a.getPackageName()));
        a.startActivity(i);
    }

    /** Returns false if no installer could be opened. */
    public static boolean install(Activity a, File apk) {
        Uri uri = FileProvider.getUriForFile(a, a.getPackageName() + ".fileprovider", apk);
        Intent i = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            a.startActivity(i);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }
}
