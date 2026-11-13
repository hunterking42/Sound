package mamun.sound.fire.model.admin;

import com.google.gson.annotations.SerializedName;

public class AdminCategory {
    public long id;
    public String name;
    @SerializedName("icon_url")   public String iconUrl;
    @SerializedName("sort_order") public int sortOrder;
    @SerializedName("is_active")  public int isActive;
}
