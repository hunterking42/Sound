package mamun.sound.fire.model.admin;

import com.google.gson.annotations.SerializedName;

/** A sound as admins see it (includes hidden sounds). */
public class AdminSound {
    public long id;
    public String name;
    @SerializedName("image_url")   public String imageUrl;
    @SerializedName("audio_url")   public String audioUrl;
    @SerializedName("category_id") public long categoryId;
    @SerializedName("is_premium")  public int isPremium;
    @SerializedName("is_active")   public int isActive;
    @SerializedName("play_count")  public int playCount;
    @SerializedName("sort_order")  public int sortOrder;
    @SerializedName("created_at")  public String createdAt;
}
