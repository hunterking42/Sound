package mamun.sound.fire.network;

import mamun.sound.fire.model.ApiResponse;
import mamun.sound.fire.model.AppSettings;
import mamun.sound.fire.model.BannersData;
import mamun.sound.fire.model.NoticesData;
import mamun.sound.fire.model.ChangePasswordRequest;
import mamun.sound.fire.model.TokenData;
import mamun.sound.fire.model.AuthData;
import mamun.sound.fire.model.CategoriesData;
import mamun.sound.fire.model.Credentials;
import mamun.sound.fire.model.EmptyData;
import mamun.sound.fire.model.FavoriteIds;
import mamun.sound.fire.model.FavoriteRequest;
import mamun.sound.fire.model.FavoriteResult;
import mamun.sound.fire.model.MeData;
import mamun.sound.fire.model.SoundRef;
import mamun.sound.fire.model.SoundsData;
import mamun.sound.fire.model.VersionInfo;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

/** version_code and device_id are added to every request by TokenInterceptor. Null query values are skipped. */
public interface ApiService {
    @GET("version_check.php") Call<ApiResponse<VersionInfo>> versionCheck();
    @GET("me.php")            Call<ApiResponse<MeData>> me();
    @POST("login.php")        Call<ApiResponse<AuthData>> login(@Body Credentials c);
    @POST("signup.php")       Call<ApiResponse<AuthData>> signup(@Body Credentials c);

    @GET("categories.php")    Call<ApiResponse<CategoriesData>> categories();
    @GET("sounds.php")        Call<ApiResponse<SoundsData>> sounds(
            @Query("category_id") Long categoryId, @Query("q") String query, @Query("sort") String sort,
            @Query("limit") Integer limit, @Query("offset") Integer offset);

    @GET("favorites.php?action=list") Call<ApiResponse<SoundsData>> favoriteSounds();
    @GET("favorites.php?action=ids")  Call<ApiResponse<FavoriteIds>> favoriteIds();
    @POST("favorites.php")            Call<ApiResponse<FavoriteResult>> setFavorite(@Body FavoriteRequest r);

    @POST("play_count.php")   Call<ApiResponse<EmptyData>> playCount(@Body SoundRef r);

    @GET("settings.php")        Call<ApiResponse<AppSettings>> appSettings();
    @POST("change_password.php") Call<ApiResponse<TokenData>> changePassword(@Body ChangePasswordRequest r);

    /** The token is passed explicitly: the app clears its own copy right after calling this. */
    @POST("logout.php")         Call<ApiResponse<EmptyData>> logout(@Header("Authorization") String bearer);

    @GET("notices.php") Call<ApiResponse<NoticesData>> notices();
    @GET("banners.php") Call<ApiResponse<BannersData>> banners();
}
