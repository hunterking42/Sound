package mamun.sound.fire.model.admin;

import com.google.gson.annotations.SerializedName;

/** admin/settings_manage.php. The server sends every value as text. */
public class AdminSettingsData {
    @SerializedName("telegram_channel") public String telegramChannel;
    @SerializedName("telegram_group")   public String telegramGroup;
    @SerializedName("telegram_admin")   public String telegramAdmin;
    @SerializedName("share_text")       public String shareText;
    @SerializedName("maintenance_mode") public String maintenanceMode;     // "0" or "1"
    @SerializedName("maintenance_msg")  public String maintenanceMsg;
    @SerializedName("privacy_url")      public String privacyUrl;
    @SerializedName("terms_url")        public String termsUrl;

    public boolean isMaintenance() { return "1".equals(maintenanceMode); }
}
