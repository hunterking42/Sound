package mamun.sound.fire.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import mamun.sound.fire.R;
import mamun.sound.fire.config.AppConfig;
import mamun.sound.fire.model.ApiResponse;
import mamun.sound.fire.model.AuthData;
import mamun.sound.fire.model.Credentials;
import mamun.sound.fire.network.Api;
import mamun.sound.fire.network.ApiError;
import mamun.sound.fire.util.FormUtil;
import mamun.sound.fire.util.Nav;
import mamun.sound.fire.util.SecurePrefs;
import mamun.sound.fire.util.Session;

import java.util.regex.Pattern;

import retrofit2.Call;

/**
 * Login and Signup share one layout and one flow. Subclasses only supply the texts,
 * the API call, and the screen to switch to.
 */
public abstract class AuthBaseActivity extends AppCompatActivity {

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{3,20}$");   // same rule as the server
    private static final int PASSWORD_MIN = 6;

    protected abstract @StringRes int titleRes();
    protected abstract @StringRes int subtitleRes();
    protected abstract @StringRes int buttonRes();
    protected abstract @StringRes int switchRes();
    protected abstract Class<?> switchTarget();
    protected abstract Call<ApiResponse<AuthData>> request(Credentials c);

    private View boxUser, boxPass;
    private EditText inUser, inPass;
    private TextView errorMsg;
    private Button btnSubmit;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_auth);

        ((TextView) findViewById(R.id.title)).setText(titleRes());
        ((TextView) findViewById(R.id.subtitle)).setText(subtitleRes());
        btnSubmit = findViewById(R.id.btnSubmit);
        btnSubmit.setText(buttonRes());
        TextView sw = findViewById(R.id.btnSwitch);
        sw.setText(switchRes());
        sw.setOnClickListener(v -> { startActivity(new Intent(this, switchTarget())); finish(); });

        boxUser = findViewById(R.id.boxUsername);
        boxPass = findViewById(R.id.boxPassword);
        inUser = findViewById(R.id.inputUsername);
        inPass = findViewById(R.id.inputPassword);
        errorMsg = findViewById(R.id.errorMsg);
        FormUtil.bindInput(boxUser, inUser);
        FormUtil.bindInput(boxPass, inPass);
        FormUtil.bindPasswordToggle(inPass, findViewById(R.id.btnEye));
        btnSubmit.setOnClickListener(v -> submit());

        // Message passed from Splash (e.g. "linked to another phone")
        String msg = getIntent().getStringExtra(AppConfig.EXTRA_MESSAGE);
        if (msg != null && !msg.isEmpty()) showError(msg);
    }

    private void submit() {
        String user = inUser.getText().toString().trim();
        String pass = inPass.getText().toString();
        hideError();

        // Quick local checks first, the server checks again
        if (!USERNAME.matcher(user).matches()) {
            FormUtil.setError(boxUser, true);
            showError(getString(R.string.err_username_format));
            return;
        }
        if (pass.length() < PASSWORD_MIN) {
            FormUtil.setError(boxPass, true);
            showError(getString(R.string.err_password_short));
            return;
        }

        setBusy(true);
        Api.call(request(new Credentials(user, pass)), new Api.Result<AuthData>() {
            @Override public void onSuccess(AuthData d) {
                SecurePrefs.saveSession(d.token, d.user.role);     // token only, never the password
                Session.set(d.user);
                Nav.openMain(AuthBaseActivity.this);
                finish();
            }
            @Override public void onError(ApiError e) {
                setBusy(false);
                if (e.httpCode == 426) return;                      // global listener opens ForceUpdate
                if (e.isBlocked()) { Nav.openBlocked(AuthBaseActivity.this, e); finish(); return; }
                if (e.isMaintenance()) { Nav.openMaintenance(AuthBaseActivity.this, e.message); finish(); return; }
                if (e.isNetwork()) { showError(getString(R.string.error_no_internet)); return; }
                if (e.httpCode == 409 || e.httpCode == 422) FormUtil.setError(boxUser, true);
                if (e.httpCode == 401) FormUtil.setError(boxPass, true);
                showError(e.message != null ? e.message : getString(R.string.error_generic));
            }
        });
    }

    private void setBusy(boolean busy) {
        btnSubmit.setEnabled(!busy);
        btnSubmit.setText(busy ? R.string.btn_please_wait : buttonRes());
    }

    private void showError(String m) { errorMsg.setText(m); errorMsg.setVisibility(View.VISIBLE); }
    private void hideError() { errorMsg.setVisibility(View.GONE); }
}
