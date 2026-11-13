package mamun.sound.fire.ui.settings;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import mamun.sound.fire.R;
import mamun.sound.fire.model.ChangePasswordRequest;
import mamun.sound.fire.model.TokenData;
import mamun.sound.fire.model.User;
import mamun.sound.fire.network.Api;
import mamun.sound.fire.network.ApiClient;
import mamun.sound.fire.network.ApiError;
import mamun.sound.fire.util.FormUtil;
import mamun.sound.fire.util.Nav;
import mamun.sound.fire.util.SecurePrefs;
import mamun.sound.fire.util.Session;

/** Plum dialog, 28dp radius. On success the server returns a new token (other sessions are signed out). */
public final class ChangePasswordDialog {
    private ChangePasswordDialog() {}

    public static void show(Activity a) {
        Dialog d = new Dialog(a);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(R.layout.dialog_change_password);
        d.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog);

        View boxCur = d.findViewById(R.id.boxCurrent), boxNew = d.findViewById(R.id.boxNew);
        EditText inCur = d.findViewById(R.id.inputCurrent), inNew = d.findViewById(R.id.inputNew);
        TextView error = d.findViewById(R.id.errorMsg);
        Button save = d.findViewById(R.id.btnSave);

        FormUtil.bindInput(boxCur, inCur);
        FormUtil.bindInput(boxNew, inNew);
        FormUtil.bindPasswordToggle(inCur, d.findViewById(R.id.eyeCurrent));
        FormUtil.bindPasswordToggle(inNew, d.findViewById(R.id.eyeNew));
        d.findViewById(R.id.btnCancel).setOnClickListener(v -> d.dismiss());

        save.setOnClickListener(v -> {
            String cur = inCur.getText().toString();
            String neu = inNew.getText().toString();
            error.setVisibility(View.GONE);

            if (cur.isEmpty()) { FormUtil.setError(boxCur, true); show(error, a.getString(R.string.err_password_short)); return; }
            if (neu.length() < 6) { FormUtil.setError(boxNew, true); show(error, a.getString(R.string.err_password_short)); return; }

            save.setEnabled(false);
            save.setText(R.string.btn_please_wait);
            Api.call(ApiClient.get().changePassword(new ChangePasswordRequest(cur, neu)), new Api.Result<TokenData>() {
                @Override public void onSuccess(TokenData t) {
                    User u = Session.get();
                    SecurePrefs.saveSession(t.token, u != null ? u.role : "user");   // the old token no longer works
                    d.dismiss();
                    Toast.makeText(a, R.string.pw_done, Toast.LENGTH_SHORT).show();
                }
                @Override public void onError(ApiError e) {
                    save.setEnabled(true);
                    save.setText(R.string.pw_save);
                    if (Nav.handleCommonError(a, e)) { d.dismiss(); return; }
                    if (e.httpCode == 422) FormUtil.setError(boxCur, true);
                    show(error, e.isNetwork() ? a.getString(R.string.error_no_internet)
                            : (e.message != null ? e.message : a.getString(R.string.error_generic)));
                }
            });
        });

        d.show();
        // 90% of the screen width (a Dialog is narrow by default)
        d.getWindow().setLayout((int) (a.getResources().getDisplayMetrics().widthPixels * 0.9f), ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private static void show(TextView t, String msg) { t.setText(msg); t.setVisibility(View.VISIBLE); }
}
