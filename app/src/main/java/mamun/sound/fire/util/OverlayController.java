package mamun.sound.fire.util;

import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

import mamun.sound.fire.service.OverlayService;

/** Start/stop the floating bubble and keep the saved on/off choice in step with reality. */
public final class OverlayController {
    private OverlayController() {}

    public static void enable(Context c) {
        AppPrefs.setOverlayEnabled(c, true);
        start(c);
    }

    public static void disable(Context c) {
        AppPrefs.setOverlayEnabled(c, false);
        c.stopService(new Intent(c, OverlayService.class));
    }

    public static void start(Context c) {
        if (OverlayService.running) return;
        ContextCompat.startForegroundService(c, new Intent(c, OverlayService.class));
    }

    /**
     * Called when the app opens. If the bubble should be on (the process may have been killed), bring it back.
     * If its permission was taken away or the user signed out, switch the setting off.
     */
    public static void syncOnLaunch(Context c) {
        if (!AppPrefs.overlayEnabled(c)) return;
        if (!OverlayPermission.isGranted(c) || !SecurePrefs.hasToken()) { AppPrefs.setOverlayEnabled(c, false); return; }
        start(c);
    }
}
