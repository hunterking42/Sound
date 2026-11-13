package mamun.sound.fire.ui.update;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import mamun.sound.fire.R;
import mamun.sound.fire.model.VersionInfo;
import mamun.sound.fire.network.Api;
import mamun.sound.fire.network.ApiClient;
import mamun.sound.fire.network.ApiError;
import mamun.sound.fire.ui.splash.SplashActivity;
import mamun.sound.fire.util.UpdateManager;

import java.io.File;

/**
 * Non-cancelable update screen (it is a full screen, so there is no "tap outside").
 * Back and Exit both close the app. It always loads fresh info from version_check.php,
 * so it works the same when opened by Splash or by a 426 from any API call.
 */
public class ForceUpdateActivity extends AppCompatActivity {

    private enum State { LOADING, READY, DOWNLOADING, PERMISSION, ERROR_LOAD, ERROR_DL }

    private View loading, changelogCard, progressGroup;
    private TextView version, changelog, message, progressText;
    private ProgressBar progress;
    private Button btnPrimary;

    private State state = State.LOADING;
    private VersionInfo info;
    private File apk;                       // verified APK, ready to install
    private UpdateManager.Task task;
    private Runnable primaryAction;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_force_update);
        loading = findViewById(R.id.loading);
        changelogCard = findViewById(R.id.changelogCard);
        progressGroup = findViewById(R.id.progressGroup);
        version = findViewById(R.id.version);
        changelog = findViewById(R.id.changelog);
        message = findViewById(R.id.message);
        progressText = findViewById(R.id.progressText);
        progress = findViewById(R.id.progress);
        btnPrimary = findViewById(R.id.btnPrimary);

        btnPrimary.setOnClickListener(v -> { if (primaryAction != null) primaryAction.run(); });
        findViewById(R.id.btnExit).setOnClickListener(v -> finishAffinity());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { finishAffinity(); }
        });
        load();
    }

    /** After the user returns from the "Install unknown apps" settings page. */
    @Override protected void onResume() {
        super.onResume();
        if (state == State.PERMISSION && apk != null && UpdateManager.canInstall(this)) installNow();
    }

    @Override protected void onDestroy() {
        if (task != null) task.cancel();       // the .part file stays, so the next try resumes
        super.onDestroy();
    }

    // ---- loading update info ----
    private void load() {
        show(State.LOADING, 0);
        Api.call(ApiClient.get().versionCheck(), new Api.Result<VersionInfo>() {
            @Override public void onSuccess(VersionInfo v) {
                if (!v.updateRequired) {                      // already up to date: back to normal start
                    startActivity(new Intent(ForceUpdateActivity.this, SplashActivity.class));
                    finish();
                    return;
                }
                info = v;
                version.setText(getString(R.string.update_version, v.latestVersionName));
                boolean hasLog = v.changelog != null && !v.changelog.trim().isEmpty();
                changelog.setText(hasLog ? v.changelog : "");
                changelogCard.setTag(hasLog);                 // read in show()
                show(State.READY, 0);
            }
            @Override public void onError(ApiError e) {
                show(State.ERROR_LOAD, e.isNetwork() ? R.string.error_no_internet : R.string.error_generic);
            }
        });
    }

    // ---- download ----
    private void startDownload() {
        show(State.DOWNLOADING, 0);
        progress.setIndeterminate(true);
        progressText.setText(R.string.update_downloading_start);

        task = UpdateManager.download(this, info.apkUrl, info.apkSha256, info.latestVersionCode,
                new UpdateManager.Callback() {
                    @Override public void onProgress(int percent) {
                        progress.setIndeterminate(false);
                        progress.setProgress(percent);
                        progressText.setText(getString(R.string.update_downloading, percent));
                    }
                    @Override public void onDone(File f) { apk = f; tryInstall(); }
                    @Override public void onError(UpdateManager.Error e) { show(State.ERROR_DL, errorText(e)); }
                });
    }

    private @StringRes int errorText(UpdateManager.Error e) {
        switch (e) {
            case UNAVAILABLE: return R.string.update_err_unavailable;
            case NETWORK:     return R.string.update_err_network;
            case CHECKSUM:    return R.string.update_err_checksum;
            case DISK:        return R.string.update_err_disk;
            default:          return R.string.update_err_http;
        }
    }

    // ---- install ----
    private void tryInstall() {
        if (UpdateManager.canInstall(this)) installNow();
        else show(State.PERMISSION, R.string.update_permission_msg);   // guide, then open settings
    }

    private void installNow() {
        if (UpdateManager.install(this, apk)) {
            show(State.READY, 0);      // if the user cancels the installer, "Update now" reuses the verified file
        } else {
            show(State.ERROR_DL, R.string.update_err_install);
        }
    }

    // ---- one place that decides what is visible ----
    private void show(State s, @StringRes int msgRes) {
        state = s;
        boolean hasLog = Boolean.TRUE.equals(changelogCard.getTag());

        loading.setVisibility(s == State.LOADING ? View.VISIBLE : View.GONE);
        boolean showInfo = s == State.READY || s == State.DOWNLOADING || s == State.PERMISSION || s == State.ERROR_DL;
        version.setVisibility(showInfo ? View.VISIBLE : View.GONE);
        changelogCard.setVisibility(showInfo && hasLog ? View.VISIBLE : View.GONE);
        progressGroup.setVisibility(s == State.DOWNLOADING ? View.VISIBLE : View.GONE);

        boolean showMsg = msgRes != 0;
        message.setVisibility(showMsg ? View.VISIBLE : View.GONE);
        if (showMsg) {
            message.setText(msgRes);
            message.setTextColor(getColor(s == State.PERMISSION ? R.color.mist : R.color.danger));
        }

        boolean showBtn = s != State.LOADING && s != State.DOWNLOADING;
        btnPrimary.setVisibility(showBtn ? View.VISIBLE : View.GONE);
        switch (s) {
            case READY:      btnPrimary.setText(R.string.btn_update_now);    primaryAction = this::startDownload; break;
            case PERMISSION: btnPrimary.setText(R.string.btn_open_settings); primaryAction = () -> UpdateManager.openInstallSettings(this); break;
            case ERROR_DL:   btnPrimary.setText(R.string.btn_try_again);     primaryAction = this::startDownload; break;
            case ERROR_LOAD: btnPrimary.setText(R.string.btn_try_again);     primaryAction = this::load; break;
            default:         primaryAction = null;
        }
    }
}
