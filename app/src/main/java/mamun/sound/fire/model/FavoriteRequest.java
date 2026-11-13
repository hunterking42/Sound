package mamun.sound.fire.model;

import com.google.gson.annotations.SerializedName;

public class FavoriteRequest {
    public final String action;                       // "add" or "remove"
    @SerializedName("sound_id") public final long soundId;
    public FavoriteRequest(String action, long soundId) { this.action = action; this.soundId = soundId; }
}
