package mamun.sound.fire.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import mamun.sound.fire.R;

/**
 * Opens links that come from the server. Only https links are ever opened, so a bad value
 * in app_settings cannot launch another app or a plain-HTTP page.
 */
public final class Links {
    private Links() {}

    private static final String TELEGRAM_PACKAGE = "org.telegram.messenger";

    /** Accepts "https://t.me/x", "t.me/x", "@x" or "x". Anything else (http://, javascript:, ...) returns null. */
    public static String normalizeTelegram(String link) {
        if (link == null) return null;
        link = link.trim();
        if (link.isEmpty()) return null;
        if (link.startsWith("https://")) return link;
        if (link.startsWith("t.me/")) return "https://" + link;
        if (link.startsWith("@")) link = link.substring(1);
        return link.matches("[A-Za-z0-9_]{4,}") ? "https://t.me/" + link : null;
    }

    /** Telegram app first, browser as the fallback when Telegram is not installed. */
    public static boolean openTelegram(Activity a, String link) {
        String url = normalizeTelegram(link);
        if (url == null) return false;
        Uri uri = Uri.parse(url);
        try {
            a.startActivity(new Intent(Intent.ACTION_VIEW, uri).setPackage(TELEGRAM_PACKAGE));
            return true;
        } catch (ActivityNotFoundException ignored) { /* not installed: use the browser */ }
        return openWeb(a, url);
    }

    /** Privacy policy, terms and other pages. https only. */
    public static boolean openWeb(Activity a, String url) {
        if (url == null || !url.trim().startsWith("https://")) return false;
        try {
            a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url.trim())));
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    /**
     * Opens the admin chat with the message prefilled. Not every Telegram client fills the text from a link,
     * so the message is also copied to the clipboard as a fallback.
     */
    public static boolean buyPremium(Activity a, String adminLink, String username) {
        String base = normalizeTelegram(adminLink);
        if (base == null) return false;
        String msg = a.getString(R.string.premium_message, username);
        ClipboardManager cm = (ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("SoundBomb", msg));
            Toast.makeText(a, R.string.premium_message_copied, Toast.LENGTH_LONG).show();
        }
        return openTelegram(a, base + (base.contains("?") ? "&" : "?") + "text=" + Uri.encode(msg));
    }
}
