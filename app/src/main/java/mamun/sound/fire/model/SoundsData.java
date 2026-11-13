package mamun.sound.fire.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** sounds.php and favorites.php?action=list */
public class SoundsData {
    public List<Sound> sounds;
    @SerializedName("has_more") public boolean hasMore;
}
