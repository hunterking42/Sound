package mamun.sound.fire.util;

import android.app.Activity;
import android.content.Intent;

import mamun.sound.fire.config.AppConfig;
import mamun.sound.fire.ui.auth.LoginActivity;
import mamun.sound.fire.network.ApiError;
import mamun.sound.fire.ui.blocked.BlockedActivity;
import mamun.sound.fire.ui.main.MainActivity;
import mamun.sound.fire.ui.maintenance.MaintenanceActivity;

/** Shared screen routing used by Splash and the auth screens. */
public final class Nav {
    private Nav() {}

    public static void openMain(Activity a) {
        Intent i = new Intent(a, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        a.startActivity(i);
    }

    public static void openBlocked(Activity a, ApiError e) {
        Intent i = new Intent(a, BlockedActivity.class);
        i.putExtra(AppConfig.EXTRA_STATUS, e.status);
        i.putExtra(AppConfig.EXTRA_REASON, e.reason);
        i.putExtra(AppConfig.EXTRA_LOCK_UNTIL, e.lockUntil);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        a.startActivity(i);
    }

    public static void openMaintenance(Activity a, String message) {
        Intent i = new Intent(a, MaintenanceActivity.class);
        i.putExtra(AppConfig.EXTRA_MESSAGE, message);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        a.startActivity(i);
    }

    /**
     * Common reactions to a server error on any screen. Returns true if it changed screens,
     * so the caller should stop. 426 is handled globally by ApiClient.
     */
    public static boolean handleCommonError(Activity a, ApiError e) {
        if (e.httpCode == 426) return true;
        if (e.isBlocked())     { openBlocked(a, e); return true; }
        if (e.isMaintenance()) { openMaintenance(a, e.message); return true; }
        if (e.isAuthInvalid()) {                                   // token dead or other phone
            Session.clear();
            Intent i = new Intent(a, LoginActivity.class);
            i.putExtra(AppConfig.EXTRA_MESSAGE, e.message);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            a.startActivity(i);
            return true;
        }
        return false;
    }

    public static void openPremium(Activity a) {
        a.startActivity(new Intent(a, mamun.sound.fire.ui.premium.PremiumActivity.class));
    }
}
