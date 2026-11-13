package mamun.sound.fire.network;

import mamun.sound.fire.model.ApiResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Wraps Retrofit calls so screens only handle onSuccess(data) / onError(error). Callbacks run on the main thread. */
public final class Api {
    private Api() {}

    public interface Result<T> {
        void onSuccess(T data);
        void onError(ApiError error);
    }

    public static <T> void call(Call<ApiResponse<T>> call, Result<T> cb) {
        call.enqueue(new Callback<ApiResponse<T>>() {
            @Override public void onResponse(Call<ApiResponse<T>> c, Response<ApiResponse<T>> r) {
                ApiResponse<T> body = r.body();
                if (r.isSuccessful() && body != null && body.success) {
                    cb.onSuccess(body.data);
                    return;
                }
                String json = "";
                try { if (r.errorBody() != null) json = r.errorBody().string(); } catch (Exception ignored) {}
                cb.onError(ApiError.fromBody(r.code(), json));
            }
            @Override public void onFailure(Call<ApiResponse<T>> c, Throwable t) {
                if (!c.isCanceled()) cb.onError(ApiError.network());
            }
        });
    }
}
