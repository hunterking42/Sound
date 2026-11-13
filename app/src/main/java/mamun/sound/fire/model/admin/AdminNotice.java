package mamun.sound.fire.model.admin;

import com.google.gson.annotations.SerializedName;

public class AdminNotice {
    public long id;
    public String title;
    public String message;
    @SerializedName("created_at") public String createdAt;
    @SerializedName("show_popup") public int showPopup;
}
