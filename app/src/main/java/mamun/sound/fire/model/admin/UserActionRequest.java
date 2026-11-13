package mamun.sound.fire.model.admin;

import com.google.gson.annotations.SerializedName;

/** Body for admin/action.php. Fields left null are not sent (Gson skips nulls). */
public class UserActionRequest {
    public String action;
    @SerializedName("user_id") public long userId;
    public String reason;
    public Integer minutes;
    public String mode;
    public Integer days;
    public Boolean lifetime;

    private UserActionRequest(String action, long userId) { this.action = action; this.userId = userId; }

    public static UserActionRequest ban(long id, String reason) { UserActionRequest r = new UserActionRequest("ban", id); r.reason = reason; return r; }
    public static UserActionRequest unban(long id) { return new UserActionRequest("unban", id); }
    public static UserActionRequest lock(long id, Integer minutes) { UserActionRequest r = new UserActionRequest("lock", id); r.minutes = minutes; return r; }
    public static UserActionRequest unlock(long id) { return new UserActionRequest("unlock", id); }
    public static UserActionRequest givePremium(long id, Integer days, boolean lifetime) {
        UserActionRequest r = new UserActionRequest("premium", id);
        r.mode = "give";
        if (lifetime) r.lifetime = true; else r.days = days;
        return r;
    }
    public static UserActionRequest removePremium(long id) { UserActionRequest r = new UserActionRequest("premium", id); r.mode = "remove"; return r; }
    public static UserActionRequest resetDevice(long id) { return new UserActionRequest("reset_device", id); }
    public static UserActionRequest resetPassword(long id) { return new UserActionRequest("reset_password", id); }
    public static UserActionRequest delete(long id) { return new UserActionRequest("delete", id); }
}
