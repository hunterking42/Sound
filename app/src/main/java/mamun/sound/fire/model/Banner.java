package mamun.sound.fire.model;

import com.google.gson.annotations.SerializedName;

public class Banner {
    public long id;
    @SerializedName("image_url") public String imageUrl;
    public String link;                       // https, may be empty
    @SerializedName("sort_order") public int sortOrder;
}
