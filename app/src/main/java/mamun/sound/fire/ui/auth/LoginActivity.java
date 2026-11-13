package mamun.sound.fire.ui.auth;

import mamun.sound.fire.R;
import mamun.sound.fire.model.ApiResponse;
import mamun.sound.fire.model.AuthData;
import mamun.sound.fire.model.Credentials;
import mamun.sound.fire.network.ApiClient;

import retrofit2.Call;

public class LoginActivity extends AuthBaseActivity {
    @Override protected int titleRes()    { return R.string.auth_login_title; }
    @Override protected int subtitleRes() { return R.string.auth_login_subtitle; }
    @Override protected int buttonRes()   { return R.string.auth_login_button; }
    @Override protected int switchRes()   { return R.string.auth_login_switch; }
    @Override protected Class<?> switchTarget() { return SignupActivity.class; }
    @Override protected Call<ApiResponse<AuthData>> request(Credentials c) { return ApiClient.get().login(c); }
}
