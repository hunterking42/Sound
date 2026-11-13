package mamun.sound.fire.model.admin;

import com.google.gson.annotations.SerializedName;

/** A user as admins see it (never includes password or token hashes). */
public class AdminUser {
    public long id;
    public String username;
    public String role;
    public String status;                                   // active / banned / locked
    @SerializedName("is_premium")     public int isPremium;
    @SerializedName("premium_expiry") public String premiumExpiry;
    @SerializedName("ban_reason")     public String banReason;
    @SerializedName("lock_until")     public String lockUntil;
    @SerializedName("device_id")      public String deviceId;
    @SerializedName("created_at")     public String createdAt;
    @SerializedName("last_login")     public String lastLogin;

    public boolean isAdmin() { return "admin".equals(role); }
    public boolean isPremium() { return isPremium != 0; }
}
