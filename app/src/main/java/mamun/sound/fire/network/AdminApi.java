package mamun.sound.fire.network;

import mamun.sound.fire.model.ApiResponse;
import mamun.sound.fire.model.admin.AdminBannersData;
import mamun.sound.fire.model.admin.AdminNoticesData;
import mamun.sound.fire.model.admin.AdminSettingsData;
import mamun.sound.fire.model.admin.AdminUpdateData;
import mamun.sound.fire.model.admin.BroadcastResult;
import mamun.sound.fire.model.admin.AdminCategoriesData;
import mamun.sound.fire.model.admin.AdminLogsData;
import mamun.sound.fire.model.admin.AdminSoundsData;
import mamun.sound.fire.model.admin.ManageResult;
import mamun.sound.fire.model.admin.AdminStats;
import mamun.sound.fire.model.admin.AdminUserDetail;
import mamun.sound.fire.model.admin.AdminUsersData;
import mamun.sound.fire.model.admin.UserActionRequest;
import mamun.sound.fire.model.admin.UserActionResult;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.POST;
import retrofit2.http.Query;

/** Admin endpoints. The server checks role = admin on EVERY call; this interface being here proves nothing. */
public interface AdminApi {
    @GET("admin/stats.php") Call<ApiResponse<AdminStats>> stats();

    @GET("admin/users.php") Call<ApiResponse<AdminUsersData>> users(
            @Query("q") String query, @Query("filter") String filter,
            @Query("limit") Integer limit, @Query("offset") Integer offset);
    @GET("admin/users.php") Call<ApiResponse<AdminUserDetail>> userDetail(@Query("id") long id);
    @POST("admin/action.php") Call<ApiResponse<UserActionResult>> userAction(@Body UserActionRequest r);

    @GET("admin/logs.php") Call<ApiResponse<AdminLogsData>> logs(@Query("limit") Integer limit, @Query("offset") Integer offset);

    // ---- sounds ----
    @GET("admin/sounds_manage.php") Call<ApiResponse<AdminSoundsData>> sounds(
            @Query("q") String query, @Query("category_id") Long categoryId,
            @Query("limit") Integer limit, @Query("offset") Integer offset);

    /** Add or edit with optional image and audio files. "action" (add/edit) and the other fields go in the part map. */
    @Multipart @POST("admin/sounds_manage.php") Call<ApiResponse<ManageResult>> soundSave(
            @PartMap Map<String, RequestBody> fields,
            @Part MultipartBody.Part image, @Part MultipartBody.Part audio);      // null parts are skipped

    @POST("admin/sounds_manage.php") Call<ApiResponse<ManageResult>> soundAction(@Body Map<String, Object> body);

    // ---- categories ----
    @GET("admin/categories_manage.php") Call<ApiResponse<AdminCategoriesData>> categories();
    @POST("admin/categories_manage.php") Call<ApiResponse<ManageResult>> categoryAction(@Body Map<String, Object> body);

    // ---- banners ----
    @GET("admin/banners_manage.php") Call<ApiResponse<AdminBannersData>> banners();
    @POST("admin/banners_manage.php") Call<ApiResponse<ManageResult>> bannerAction(@Body Map<String, Object> body);

    // ---- app settings ----
    @GET("admin/settings_manage.php") Call<ApiResponse<AdminSettingsData>> settings();
    @POST("admin/settings_manage.php") Call<ApiResponse<AdminSettingsData>> saveSettings(@Body Map<String, Object> body);

    // ---- update manager (the APK is optional; the server computes its SHA-256) ----
    @GET("admin/update_manager.php") Call<ApiResponse<AdminUpdateData>> updateInfo();
    @Multipart @POST("admin/update_manager.php") Call<ApiResponse<AdminUpdateData>> updateSave(
            @PartMap Map<String, RequestBody> fields, @Part MultipartBody.Part apk);

    // ---- broadcast ----
    @GET("admin/broadcast.php") Call<ApiResponse<AdminNoticesData>> notices();
    @POST("admin/broadcast.php") Call<ApiResponse<BroadcastResult>> broadcastAction(@Body Map<String, Object> body);
}
