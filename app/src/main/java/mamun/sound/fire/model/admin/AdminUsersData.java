package mamun.sound.fire.model.admin;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AdminUsersData {
    public List<AdminUser> users;
    @SerializedName("has_more") public boolean hasMore;
}
