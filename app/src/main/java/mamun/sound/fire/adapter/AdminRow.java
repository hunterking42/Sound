package mamun.sound.fire.adapter;

import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;

/** One line in an admin list (sounds, categories, banners): optional thumbnail, title, subtitle, small badge. */
public class AdminRow {
    public final long id;
    public final String title, subtitle;
    @Nullable public final String badge;
    public final @ColorRes int badgeColor;
    public final boolean showThumb;
    @Nullable public final String imageUrl;

    public AdminRow(long id, String title, String subtitle, @Nullable String badge, @ColorRes int badgeColor,
                    boolean showThumb, @Nullable String imageUrl) {
        this.id = id; this.title = title; this.subtitle = subtitle; this.badge = badge;
        this.badgeColor = badgeColor; this.showThumb = showThumb; this.imageUrl = imageUrl;
    }

    String signature() { return title + "|" + subtitle + "|" + badge + "|" + imageUrl; }
}
