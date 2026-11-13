package mamun.sound.fire.model.admin;

import com.google.gson.annotations.SerializedName;

/** Result of admin/action.php. The user is missing after a delete. */
public class UserActionResult {
    public AdminUser user;
    @SerializedName("temporary_password") public String temporaryPassword;   // only after reset_password
    public Long deleted;
}
