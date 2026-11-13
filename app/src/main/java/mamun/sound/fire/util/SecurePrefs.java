package mamun.sound.fire.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

/**
 * Stores ONLY the auth token plus small UI flags. The password is never saved (Golden Rule 2).
 * The token is never stored in plain SharedPreferences.
 */
public final class SecurePrefs {
    private SecurePrefs() {}

    private static final String FILE = "soundbomb_secure";
    private static final String K_TOKEN = "token";
    private static final String K_ROLE = "role";                  // UI hint only
    private static final String K_ONBOARDED = "onboarded";
    private static final String K_FALLBACK_ID = "fallback_device_id";
    private static final String K_USER = "user_json";

    private static SharedPreferences prefs;

    public static synchronized void init(Context c) {
        Context app = c.getApplicationContext();
        try {
            prefs = open(app);
        } catch (Exception first) {
            // Keystore/file corruption happens on some phones. Wipe and retry once: the user just signs in again.
            app.deleteSharedPreferences(FILE);
            try { prefs = open(app); }
            catch (Exception second) { throw new IllegalStateException("Secure storage unavailable", second); }
        }
    }

    private static SharedPreferences open(Context app) throws Exception {
        MasterKey key = new MasterKey.Builder(app).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
        return EncryptedSharedPreferences.create(app, FILE, key,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
    }

    public static String getToken()          { return prefs.getString(K_TOKEN, null); }
    public static boolean hasToken()         { return getToken() != null; }
    public static String getRole()           { return prefs.getString(K_ROLE, "user"); }
    public static boolean isOnboardingDone() { return prefs.getBoolean(K_ONBOARDED, false); }
    public static void setOnboardingDone()   { prefs.edit().putBoolean(K_ONBOARDED, true).apply(); }

    public static void saveSession(String token, String role) {
        prefs.edit().putString(K_TOKEN, token).putString(K_ROLE, role).apply();
    }
    public static void saveRole(String role) { prefs.edit().putString(K_ROLE, role).apply(); }

    /** Logout or invalid token. Keeps onboarding flag and the device fallback id. */
    public static void clearSession() { prefs.edit().remove(K_TOKEN).remove(K_ROLE).remove(K_USER).apply(); }

    public static String getUserJson()          { return prefs.getString(K_USER, null); }
    public static void saveUserJson(String j)   { prefs.edit().putString(K_USER, j).apply(); }

    static String getFallbackId()          { return prefs.getString(K_FALLBACK_ID, null); }
    static void setFallbackId(String id)   { prefs.edit().putString(K_FALLBACK_ID, id).apply(); }
}
