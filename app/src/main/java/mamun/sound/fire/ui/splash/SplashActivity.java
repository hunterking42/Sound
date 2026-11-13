package mamun.sound.fire.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import mamun.sound.fire.R;
import mamun.sound.fire.config.AppConfig;
import mamun.sound.fire.model.MeData;
import mamun.sound.fire.model.VersionInfo;
import mamun.sound.fire.network.Api;
import mamun.sound.fire.network.ApiClient;
import mamun.sound.fire.network.ApiError;
import mamun.sound.fire.ui.auth.LoginActivity;
import mamun.sound.fire.ui.blocked.BlockedActivity;
import mamun.sound.fire.ui.main.MainActivity;
import mamun.sound.fire.ui.maintenance.MaintenanceActivity;
import mamun.sound.fire.ui.onboarding.OnboardingActivity;
import mamun.sound.fire.ui.update.ForceUpdateActivity;
import mamun.sound.fire.util.SecurePrefs;
import mamun.sound.fire.util.Session;

/**
 * Routing:
 *   version_check -> update required?  -> ForceUpdate
 *   has token?    -> me.php            -> Main / Blocked / Maintenance / Login
 *   no token?     -> maintenance? Maintenance : first launch? Onboarding : Login
 * Offline with a saved token opens Main so cached sounds still work.
 */
public class SplashActivity extends AppCompatActivity {

    private View loading, errorGroup;
    private TextView errorText;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_splash);
        loading = findViewById(R.id.loading);
        errorGroup = findViewById(R.id.errorGroup);
        errorText = findViewById(R.id.errorText);
        findViewById(R.id.btnRetry).setOnClickListener(v -> start());
        start();
    }

    private void start() {
        loading.setVisibility(View.VISIBLE);
        errorGroup.setVisibility(View.GONE);

        Api.call(ApiClient.get().versionCheck(), new Api.Result<VersionInfo>() {
            @Override public void onSuccess(VersionInfo v) {
                if (v.updateRequired) { go(ForceUpdateActivity.class, null); return; }
                if (SecurePrefs.hasToken()) checkSession();
                else routeLoggedOut(v);
            }
            @Override public void onError(ApiError e) {
                if (e.isNetwork() && SecurePrefs.hasToken()) { go(MainActivity.class, null); return; }  // offline mode
                showError(e.isNetwork() ? getString(R.string.error_no_internet) : getString(R.string.error_generic));
            }
        });
    }

    private void routeLoggedOut(VersionInfo v) {
        if (v.maintenance) {
            Intent i = new Intent(this, MaintenanceActivity.class);
            i.putExtra(AppConfig.EXTRA_MESSAGE, v.maintenanceMsg);
            open(i);
        } else if (!SecurePrefs.isOnboardingDone()) {
            go(OnboardingActivity.class, null);
        } else {
            go(LoginActivity.class, null);
        }
    }

    /** Auto login: me.php re-checks token, device, status, maintenance and version. */
    private void checkSession() {
        Api.call(ApiClient.get().me(), new Api.Result<MeData>() {
            @Override public void onSuccess(MeData me) {
                SecurePrefs.saveRole(me.user.role);
                Session.set(me.user);
                go(MainActivity.class, null);
            }
            @Override public void onError(ApiError e) {
                if (e.httpCode == 426) return;                       // the global listener opens ForceUpdate
                if (e.isBlocked()) {
                    Intent i = new Intent(SplashActivity.this, BlockedActivity.class);
                    i.putExtra(AppConfig.EXTRA_STATUS, e.status);
                    i.putExtra(AppConfig.EXTRA_REASON, e.reason);
                    i.putExtra(AppConfig.EXTRA_LOCK_UNTIL, e.lockUntil);
                    open(i);
                } else if (e.isMaintenance()) {
                    Intent i = new Intent(SplashActivity.this, MaintenanceActivity.class);
                    i.putExtra(AppConfig.EXTRA_MESSAGE, e.message);
                    open(i);
                } else if (e.isAuthInvalid()) {
                    SecurePrefs.clearSession();                       // token dead or other phone: sign in again
                    Intent i = new Intent(SplashActivity.this, LoginActivity.class);
                    i.putExtra(AppConfig.EXTRA_MESSAGE, e.message);
                    open(i);
                } else if (e.isNetwork()) {
                    go(MainActivity.class, null);                     // offline: cached sounds
                } else {
                    showError(getString(R.string.error_generic));
                }
            }
        });
    }

    private void go(Class<?> cls, Bundle extras) {
        Intent i = new Intent(this, cls);
        if (extras != null) i.putExtras(extras);
        open(i);
    }

    private void open(Intent i) {
        startActivity(i);
        finish();
    }

    private void showError(String msg) {
        loading.setVisibility(View.GONE);
        errorText.setText(msg);
        errorGroup.setVisibility(View.VISIBLE);
    }
}
