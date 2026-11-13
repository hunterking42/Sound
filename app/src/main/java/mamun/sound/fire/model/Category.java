package mamun.sound.fire.model;

import com.google.gson.annotations.SerializedName;

public class Category {
    public long id;
    public String name;
    @SerializedName("icon_url")   public String iconUrl;
    @SerializedName("sort_order") public int sortOrder;
}
