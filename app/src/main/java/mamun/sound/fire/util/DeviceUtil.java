package mamun.sound.fire.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;

import java.util.UUID;

/** Device id for the "one phone, one account" rule. */
public final class DeviceUtil {
    private DeviceUtil() {}

    // Known bad value returned by some old/broken devices
    private static final String BAD_ID = "9774d56d682e549c";
    private static String cached;

    /**
     * ANDROID_ID is per app-signing-key + user + device. Debug and release builds signed with
     * different keys get DIFFERENT ids, so test account rules with the release keystore.
     */
    @SuppressLint("HardwareIds")
    public static synchronized String getDeviceId(Context ctx) {
        if (cached != null) return cached;
        String id = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (id == null || id.length() < 8 || BAD_ID.equals(id)) {
            // Rare fallback: random id saved once so it stays stable
            id = SecurePrefs.getFallbackId();
            if (id == null) { id = UUID.randomUUID().toString(); SecurePrefs.setFallbackId(id); }
        }
        cached = id;
        return id;
    }
}
