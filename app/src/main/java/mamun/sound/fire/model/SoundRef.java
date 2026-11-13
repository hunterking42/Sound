package mamun.sound.fire.model;

import com.google.gson.annotations.SerializedName;

public class SoundRef {
    @SerializedName("sound_id") public final long soundId;
    public SoundRef(long soundId) { this.soundId = soundId; }
}
