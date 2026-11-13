package mamun.sound.fire.model.admin;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AdminSoundsData {
    public List<AdminSound> sounds;
    @SerializedName("has_more") public boolean hasMore;
}
