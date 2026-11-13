package mamun.sound.fire.model;

import com.google.gson.annotations.SerializedName;

public class ChangePasswordRequest {
    @SerializedName("current_password") public final String currentPassword;
    @SerializedName("new_password")     public final String newPassword;
    public ChangePasswordRequest(String cur, String neu) { currentPassword = cur; newPassword = neu; }
}
