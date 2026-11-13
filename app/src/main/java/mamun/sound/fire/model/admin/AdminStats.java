package mamun.sound.fire.model.admin;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AdminStats {
    @SerializedName("users_total")   public int usersTotal;
    public int premium;
    public int banned;
    public int locked;
    @SerializedName("active_today")  public int activeToday;
    @SerializedName("new_7_days")    public int newWeek;
    @SerializedName("sounds_total")  public int soundsTotal;
    @SerializedName("sounds_active") public int soundsActive;
    @SerializedName("plays_total")   public long playsTotal;
    @SerializedName("top_sounds")    public List<TopSound> topSounds;

    public static class TopSound {
        public long id;
        public String name;
        @SerializedName("play_count") public int playCount;
    }
}
