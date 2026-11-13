package mamun.sound.fire.model.admin;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AdminLogsData {
    public List<AdminLog> logs;
    @SerializedName("has_more") public boolean hasMore;
}
