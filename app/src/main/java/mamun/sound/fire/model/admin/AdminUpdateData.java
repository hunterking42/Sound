package mamun.sound.fire.model.admin;

import com.google.gson.annotations.SerializedName;

/** admin/update_manager.php. Version codes arrive as text. */
public class AdminUpdateData {
    @SerializedName("latest_version_code") public String latestVersionCode;
    @SerializedName("latest_version_name") public String latestVersionName;
    @SerializedName("min_version_code")    public String minVersionCode;
    @SerializedName("apk_url")             public String apkUrl;
    @SerializedName("apk_sha256")          public String apkSha256;
    public String changelog;
    @SerializedName("force_update")        public String forceUpdate;      // "0" or "1"

    public boolean isForced() { return "1".equals(forceUpdate); }
}
