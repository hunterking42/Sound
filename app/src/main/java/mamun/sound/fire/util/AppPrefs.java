package mamun.sound.fire.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import mamun.sound.fire.R;

import java.util.ArrayList;
import java.util.List;

/** Plain (non-secret) settings: grid columns, volume, recent searches. Nothing sensitive goes here. */
public final class AppPrefs {
    private AppPrefs() {}

    private static SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences("soundbomb_prefs", Context.MODE_PRIVATE);
    }

    /** 2, 3 or 4 on phones (user choice); default 3 on phones and 5 on tablets. */
    public static int gridColumns(Context c) {
        return p(c).getInt("grid_columns", c.getResources().getInteger(R.integer.grid_default_columns));
    }
    public static void setGridColumns(Context c, int n) { p(c).edit().putInt("grid_columns", n).apply(); }

    public static float volume(Context c) { return p(c).getFloat("volume", 1f); }
    public static void setVolume(Context c, float v) { p(c).edit().putFloat("volume", v).apply(); }

    public static List<String> recentSearches(Context c) {
        List<String> out = new ArrayList<>();
        for (String s : p(c).getString("recent", "").split("\n")) if (!s.isEmpty()) out.add(s);
        return out;
    }

    /** Newest first, no duplicates, at most 5. */
    public static void addRecentSearch(Context c, String q) {
        List<String> list = recentSearches(c);
        for (int i = list.size() - 1; i >= 0; i--) if (list.get(i).equalsIgnoreCase(q)) list.remove(i);
        list.add(0, q);
        while (list.size() > 5) list.remove(list.size() - 1);
        p(c).edit().putString("recent", TextUtils.join("\n", list)).apply();
    }

    public static void clearRecentSearches(Context c) { p(c).edit().remove("recent").apply(); }

    // ---- floating bubble ----
    public static boolean overlayEnabled(Context c) { return p(c).getBoolean("overlay_enabled", false); }
    public static void setOverlayEnabled(Context c, boolean on) { p(c).edit().putBoolean("overlay_enabled", on).apply(); }

    /** Last bubble position in pixels, or null if it was never moved. */
    public static int[] overlayPos(Context c) {
        SharedPreferences s = p(c);
        return s.contains("overlay_x") ? new int[]{s.getInt("overlay_x", 0), s.getInt("overlay_y", 0)} : null;
    }
    public static void setOverlayPos(Context c, int x, int y) {
        p(c).edit().putInt("overlay_x", x).putInt("overlay_y", y).apply();
    }

    // ---- admin notices ----
    /** Last notice we looked at for pop-ups. -1 = never checked: the first check only sets a baseline (no old pop-ups). */
    public static long noticePopupSeen(Context c) { return p(c).getLong("notice_popup_seen", -1); }
    public static void setNoticePopupSeen(Context c, long id) { p(c).edit().putLong("notice_popup_seen", id).apply(); }
    /** Newest notice the user has opened or been shown. */
    public static long noticeRead(Context c) { return p(c).getLong("notice_read", 0); }
    public static void setNoticeRead(Context c, long id) { p(c).edit().putLong("notice_read", id).apply(); }
    /** Newest notice that exists (from the last check). Unread dot = latest > read. */
    public static long noticeLatest(Context c) { return p(c).getLong("notice_latest", 0); }
    public static void setNoticeLatest(Context c, long id) { p(c).edit().putLong("notice_latest", id).apply(); }
    public static void clearNoticeState(Context c) { p(c).edit().remove("notice_popup_seen").remove("notice_read").remove("notice_latest").apply(); }

    // ---- appearance and feedback ----
    public static boolean vibration(Context c) { return p(c).getBoolean("vibration", true); }
    public static void setVibration(Context c, boolean on) { p(c).edit().putBoolean("vibration", on).apply(); }

    /** "dark" (default: the look of the app), "light" or "system". */
    public static String themeMode(Context c) { return p(c).getString("theme_mode", "dark"); }
    public static void setThemeMode(Context c, String mode) { p(c).edit().putString("theme_mode", mode).apply(); }

    /** The theme choice as an AppCompatDelegate night mode. */
    public static int nightMode(Context c) {
        switch (themeMode(c)) {
            case "light": return androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
            case "system": return androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            default: return androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;
        }
    }
}
