package mamun.sound.fire.model.admin;

import com.google.gson.annotations.SerializedName;

/** One admin action. In a user's detail, "target" is not sent (it is that user). */
public class AdminLog {
    public long id;
    public String action;
    public String details;
    public String admin;                                    // username of the admin
    public String target;                                   // username of the user acted on, if any
    @SerializedName("created_at") public String createdAt;
}
