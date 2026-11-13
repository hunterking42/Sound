package mamun.sound.fire.model;

import com.google.gson.annotations.SerializedName;

/** version_check.php result */
public class VersionInfo {
    @SerializedName("update_required")      public boolean updateRequired;
    @SerializedName("update_available")     public boolean updateAvailable;
    @SerializedName("latest_version_code")  public int latestVersionCode;
    @SerializedName("latest_version_name")  public String latestVersionName;
    public String changelog;
    @SerializedName("apk_url")              public String apkUrl;
    @SerializedName("apk_sha256")           public String apkSha256;
    public boolean maintenance;
    @SerializedName("maintenance_msg")      public String maintenanceMsg;
}
