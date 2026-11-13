package mamun.sound.fire.model;

import com.google.gson.annotations.SerializedName;

/** settings.php: values the admin can change without an app update. Any of them may be empty. */
public class AppSettings {
    @SerializedName("telegram_channel") public String telegramChannel;
    @SerializedName("telegram_group")   public String telegramGroup;
    @SerializedName("telegram_admin")   public String telegramAdmin;
    @SerializedName("share_text")       public String shareText;
    @SerializedName("privacy_url")      public String privacyUrl;
    @SerializedName("terms_url")        public String termsUrl;
}
