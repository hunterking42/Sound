package mamun.sound.fire.model.admin;

import com.google.gson.annotations.SerializedName;

public class AdminBanner {
    public long id;
    @SerializedName("image_url")  public String imageUrl;
    public String link;
    @SerializedName("sort_order") public int sortOrder;
    @SerializedName("is_active")  public int isActive;
}
