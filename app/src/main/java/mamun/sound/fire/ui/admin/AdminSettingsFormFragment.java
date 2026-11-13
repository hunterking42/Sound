package mamun.sound.fire.ui.admin;

import mamun.sound.fire.ui.common.AppDialog;

import androidx.annotation.NonNull;

import mamun.sound.fire.R;
import mamun.sound.fire.model.admin.AdminSettingsData;
import mamun.sound.fire.network.Api;
import mamun.sound.fire.network.ApiClient;
import mamun.sound.fire.network.ApiError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Telegram links, share text, policy links and maintenance mode. These take effect for everyone
 * without an app update. The server validates every value again (https only, Telegram formats).
 */
public class AdminSettingsFormFragment extends AdminFormFragment {

    private AdminSettingsData cur;

    @Override protected int titleRes() { return R.string.admin_settings; }

    @Override protected void prepare(@NonNull Runnable ready) {
        Api.call(ApiClient.admin().settings(), new Api.Result<AdminSettingsData>() {
            @Override public void onSuccess(AdminSettingsData d) { if (isAdded()) { cur = d; ready.run(); } }
            @Override public void onError(ApiError e) { failPrepare(e); }
        });
    }

    @Override protected List<Field> createFields() {
        List<Field> f = new ArrayList<>();
        f.add(Field.url("tg_channel", getString(R.string.f_tg_channel), cur.telegramChannel, false).hint("https://t.me/name"));
        f.add(Field.url("tg_group", getString(R.string.f_tg_group), cur.telegramGroup, false).hint("https://t.me/name"));
        f.add(Field.url("tg_admin", getString(R.string.f_tg_admin), cur.telegramAdmin, false).hint("@name"));
        f.add(Field.multiline("share", getString(R.string.f_share_text), cur.shareText, false));
        f.add(Field.url("privacy", getString(R.string.f_privacy), cur.privacyUrl, false).hint("https://"));
        f.add(Field.url("terms", getString(R.string.f_terms), cur.termsUrl, false).hint("https://"));
        f.add(Field.sw("maint", getString(R.string.f_maint), cur.isMaintenance()));
        f.add(Field.multiline("maint_msg", getString(R.string.f_maint_msg), cur.maintenanceMsg, false));
        return f;
    }

    /** Turning maintenance mode ON locks every normal user out: confirm first. */
    @Override protected boolean confirmBeforeSave(Values v, Runnable proceed) {
        if (!v.flag("maint") || cur.isMaintenance()) return false;
        new AppDialog.Spec(R.drawable.ic_maintenance, getString(R.string.dlg_maint_title), getString(R.string.dlg_maint_msg))
                .primary(getString(R.string.btn_continue), true, t -> proceed.run())
                .show(requireActivity());
        return true;
    }

    @Override protected void save(Values v, Done done) {
        Map<String, Object> m = new HashMap<>();
        m.put("telegram_channel", v.str("tg_channel"));
        m.put("telegram_group", v.str("tg_group"));
        m.put("telegram_admin", v.str("tg_admin"));
        m.put("share_text", v.str("share"));
        m.put("privacy_url", v.str("privacy"));
        m.put("terms_url", v.str("terms"));
        m.put("maintenance_mode", v.flag("maint") ? 1 : 0);
        m.put("maintenance_msg", v.str("maint_msg"));
        Api.call(ApiClient.admin().saveSettings(m), this.<AdminSettingsData>result(done));
    }
}
