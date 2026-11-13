package mamun.sound.fire.util;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;

/**
 * Client for downloading files from any host (sound audio). It has NO interceptor on purpose:
 * ApiClient adds the auth token and device id, which must never be sent to a file/CDN host.
 */
public final class PlainHttp {
    private PlainHttp() {}
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    public static OkHttpClient client() { return CLIENT; }
}
