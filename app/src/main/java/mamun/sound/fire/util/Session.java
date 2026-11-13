package mamun.sound.fire.util;

import com.google.gson.Gson;
import mamun.sound.fire.model.User;

/**
 * The signed-in user, kept in memory and in secure storage so premium checks also work offline.
 * Filled by Splash (me.php) and by Login/Signup. On logout call Session.clear().
 */
public final class Session {
    private Session() {}

    private static final Gson GSON = new Gson();
    private static User user;

    public static synchronized void set(User u) {
        user = u;
        SecurePrefs.saveUserJson(GSON.toJson(u));
    }

    public static synchronized User get() {
        if (user == null) {
            String j = SecurePrefs.getUserJson();
            if (j != null) {
                try { user = GSON.fromJson(j, User.class); } catch (Exception ignored) { /* bad json: treat as signed out */ }
            }
        }
        return user;
    }

    public static boolean isPremium() { User u = get(); return u != null && u.isPremiumNow(); }
    public static boolean isAdmin()   { User u = get(); return u != null && u.isAdmin(); }

    public static synchronized void clear() {
        user = null;
        SecurePrefs.clearSession();
    }
}
