package mamun.sound.fire.network;

import android.content.Context;

import mamun.sound.fire.config.AppConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Single Retrofit instance. Call init() once from the Application class. */
public final class ApiClient {
    private ApiClient() {}

    private static ApiService service;
    private static AdminApi adminService;
    /** Set by the Application class: opens the force-update screen when any call returns 426. */
    static volatile Runnable forceUpdateListener;

    public static void init(Context ctx) {
        OkHttpClient http = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(new TokenInterceptor(ctx))
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.BASE_URL)
                .client(http)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        service = retrofit.create(ApiService.class);
        adminService = retrofit.create(AdminApi.class);
    }

    public static ApiService get() {
        if (service == null) throw new IllegalStateException("ApiClient.init() was not called");
        return service;
    }

    public static AdminApi admin() {
        if (adminService == null) throw new IllegalStateException("ApiClient.init() was not called");
        return adminService;
    }

    public static void setForceUpdateListener(Runnable r) { forceUpdateListener = r; }
}
