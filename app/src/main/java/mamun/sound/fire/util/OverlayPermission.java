package mamun.sound.fire.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

/** "Display over other apps" (SYSTEM_ALERT_WINDOW). Reused by Onboarding, Settings and OverlayService. */
public final class OverlayPermission {
    private OverlayPermission() {}

    public static boolean isGranted(Context c) { return Settings.canDrawOverlays(c); }

    public static void openSettings(Activity a) {
        a.startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + a.getPackageName())));
    }
}
