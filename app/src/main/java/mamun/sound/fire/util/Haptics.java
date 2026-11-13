package mamun.sound.fire.util;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

/** One short buzz when a sound starts, if the user left Vibration on in Settings. */
public final class Haptics {
    private Haptics() {}

    @SuppressWarnings("deprecation")
    public static void tick(Context c) {
        if (!AppPrefs.vibration(c)) return;
        Vibrator v = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
        if (v == null || !v.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE));
        else v.vibrate(25);                                   // Android 7: no VibrationEffect
    }
}
