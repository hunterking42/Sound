package mamun.sound.fire.model;

import com.google.gson.annotations.SerializedName;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** One sound from sounds.php. Flags are ints (0/1) because PHP/SQLite sends numbers, not booleans. */
public class Sound {
    private static final int HOT_PLAYS = 100;                      // plays needed for the HOT badge
    private static final long NEW_MS = 7L * 24 * 60 * 60 * 1000;   // NEW for 7 days

    public long id;
    public String name;
    @SerializedName("image_url")   public String imageUrl;
    @SerializedName("audio_url")   public String audioUrl;
    @SerializedName("category_id") public long categoryId;
    @SerializedName("is_premium")  public int isPremium;
    @SerializedName("play_count")  public int playCount;
    @SerializedName("created_at")  public String createdAt;        // "yyyy-MM-dd HH:mm:ss" UTC

    public boolean isPremium() { return isPremium != 0; }
    public boolean isHot() { return playCount >= HOT_PLAYS; }

    public boolean isNew() {
        if (createdAt == null) return false;
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            f.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d = f.parse(createdAt);
            return d != null && System.currentTimeMillis() - d.getTime() < NEW_MS;
        } catch (Exception e) {
            return false;   // bad date = no badge, never crash the grid
        }
    }
}
