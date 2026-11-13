package mamun.sound.fire.ui.admin;

import mamun.sound.fire.ui.common.AppDialog;

import androidx.annotation.NonNull;

import mamun.sound.fire.R;
import mamun.sound.fire.model.admin.AdminUpdateData;
import mamun.sound.fire.network.Api;
import mamun.sound.fire.network.ApiClient;
import mamun.sound.fire.network.ApiError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.RequestBody;

/**
 * Publish a new version: version code and name, minimum version, what's new, the APK (upload it and the server
 * works out the SHA-256, or paste a link and the hash), and the force-update switch.
 * The new APK must be signed with the SAME keystore or Android will refuse to install it.
 */
public class AdminUpdateFormFragment extends AdminFormFragment {

    private static final long MAX_APK = 200L * 1024 * 1024;     // same limit as the server
    private AdminUpdateData cur;

    @Override protected int titleRes() { return R.string.admin_update; }

    @Override protected void prepare(@NonNull Runnable ready) {
        Api.call(ApiClient.admin().updateInfo(), new Api.Result<AdminUpdateData>() {
            @Override public void onSuccess(AdminUpdateData d) { if (isAdded()) { cur = d; ready.run(); } }
            @Override public void onError(ApiError e) { failPrepare(e); }
        });
    }

    @Override protected List<Field> createFields() {
        List<Field> f = new ArrayList<>();
        f.add(Field.number("code", getString(R.string.f_ver_code), cur.latestVersionCode).hint("2"));
        f.add(Field.text("name", getString(R.string.f_ver_name), cur.latestVersionName, true).hint("1.1.0"));
        f.add(Field.number("min", getString(R.string.f_min_code), cur.minVersionCode));
        f.add(Field.multiline("log", getString(R.string.f_changelog), cur.changelog, false));
        f.add(Field.file("apk", getString(R.string.f_apk), "*/*", MAX_APK, false, false).hint(getString(R.string.upload_hint)));
        f.add(Field.url("apk_url", getString(R.string.f_apk_url), cur.apkUrl, false).hint("https://"));
        f.add(Field.text("apk_sha", getString(R.string.f_apk_sha), cur.apkSha256, false));
        f.add(Field.sw("force", getString(R.string.f_force), cur.isForced()));
        return f;
    }

    private static int toInt(String s, int def) {
        try { return Integer.parseInt(s == null ? "" : s.trim()); } catch (NumberFormatException e) { return def; }
    }

    /** Forcing an update, or raising the minimum version, blocks old apps: confirm first. */
    @Override protected boolean confirmBeforeSave(Values v, Runnable proceed) {
        boolean raisingMin = v.number("min", 1) > toInt(cur.minVersionCode, 1);
        if (!v.flag("force") && !raisingMin) return false;
        new AppDialog.Spec(R.drawable.ic_update, getString(R.string.dlg_update_title), getString(R.string.dlg_update_msg))
                .primary(getString(R.string.btn_continue), true, t -> proceed.run())
                .show(requireActivity());
        return true;
    }

    @Override protected void save(Values v, Done done) {
        Map<String, RequestBody> parts = new HashMap<>();
        parts.put("latest_version_code", text(String.valueOf(v.number("code", toInt(cur.latestVersionCode, 1)))));
        parts.put("latest_version_name", text(v.str("name")));
        parts.put("min_version_code", text(String.valueOf(v.number("min", toInt(cur.minVersionCode, 1)))));
        parts.put("changelog", text(v.str("log")));
        parts.put("force_update", text(v.flag("force") ? "1" : "0"));
        // A picked APK wins: the server saves it and computes the hash. Otherwise send the link and hash as typed.
        if (!v.hasFile("apk")) {
            parts.put("apk_url", text(v.str("apk_url")));
            parts.put("apk_sha256", text(v.str("apk_sha")));
        }
        Api.call(ApiClient.admin().updateSave(parts, v.part("apk", "apk")), this.<AdminUpdateData>result(done));
    }
}
