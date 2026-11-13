package mamun.sound.fire.model;

import com.google.gson.annotations.SerializedName;

/** A message from the admin (broadcast). */
public class Notice {
    public long id;
    public String title;
    public String message;
    @SerializedName("created_at") public String createdAt;
    @SerializedName("show_popup") public int showPopup;

    public boolean showsPopup() { return showPopup != 0; }
}
