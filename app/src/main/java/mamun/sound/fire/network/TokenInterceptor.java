package mamun.sound.fire.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import mamun.sound.fire.BuildConfig;
import mamun.sound.fire.util.DeviceUtil;
import mamun.sound.fire.util.SecurePrefs;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Runs on every request:
 *  - adds version_code and device_id (the server re-checks both every time)
 *  - adds the auth token header when we have one
 *  - on HTTP 426 (app too old) tells the app to open the force-update screen
 */
public class TokenInterceptor implements Interceptor {
    private final Context ctx;
    private final Handler main = new Handler(Looper.getMainLooper());

    public TokenInterceptor(Context ctx) { this.ctx = ctx.getApplicationContext(); }

    @NonNull @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request orig = chain.request();
        HttpUrl url = orig.url().newBuilder()
                .addQueryParameter("version_code", String.valueOf(BuildConfig.VERSION_CODE))
                .addQueryParameter("device_id", DeviceUtil.getDeviceId(ctx))
                .build();
        Request.Builder b = orig.newBuilder().url(url);
        String token = SecurePrefs.getToken();
        if (token != null) b.header("Authorization", "Bearer " + token);

        Response resp = chain.proceed(b.build());
        if (resp.code() == 426) {
            Runnable r = ApiClient.forceUpdateListener;
            if (r != null) main.post(r);
        }
        return resp;
    }
}
