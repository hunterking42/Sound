package mamun.sound.fire.model;

import com.google.gson.annotations.SerializedName;

public class User {
    public long id;
    public String username;
    public String role;
    public String status;
    @SerializedName("is_premium")     public int isPremium;
    @SerializedName("premium_expiry") public String premiumExpiry;
    @SerializedName("ban_reason")     public String banReason;
    @SerializedName("lock_until")     public String lockUntil;

    public boolean isAdmin() { return "admin".equals(role); }     // UI only; the server re-checks
    public boolean isPremium() { return isPremium != 0; }

    /** Premium AND not expired. The server enforces it too; this keeps the UI right without a network call. */
    public boolean isPremiumNow() {
        if (!isPremium()) return false;
        if (premiumExpiry == null || premiumExpiry.isEmpty()) return true;      // no expiry = lifetime
        long exp = mamun.sound.fire.util.TimeUtil.parseUtcMillis(premiumExpiry);
        return exp < 0 || exp > System.currentTimeMillis();
    }
}
