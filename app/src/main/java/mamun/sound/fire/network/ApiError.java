package mamun.sound.fire.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Normalised failure: network problem, or a JSON error from the server. */
public class ApiError {
    public int httpCode;            // 0 = no response (network)
    public String message;
    public String status;           // "banned" / "locked" when the account is blocked
    public String reason;
    public String lockUntil;
    public boolean maintenance;

    public boolean isNetwork()     { return httpCode == 0; }
    public boolean isBlocked()     { return "banned".equals(status) || "locked".equals(status); }
    public boolean isMaintenance() { return httpCode == 503 && maintenance; }
    /** Token invalid, or the account belongs to another phone: go back to Login. */
    public boolean isAuthInvalid() { return httpCode == 401 || (httpCode == 403 && !isBlocked()); }

    static ApiError network() {
        ApiError e = new ApiError();
        e.message = null;
        return e;
    }

    /** Parses the server's JSON error body; never throws. */
    static ApiError fromBody(int code, String json) {
        ApiError e = new ApiError();
        e.httpCode = code;
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            e.message = str(root, "message");
            if (root.has("data") && root.get("data").isJsonObject()) {
                JsonObject d = root.getAsJsonObject("data");
                e.status = str(d, "status");
                e.reason = str(d, "reason");
                e.lockUntil = str(d, "lock_until");
                e.maintenance = d.has("maintenance") && d.get("maintenance").getAsBoolean();
            }
        } catch (Exception ignored) { /* not JSON: keep defaults */ }
        return e;
    }

    private static String str(JsonObject o, String k) {
        JsonElement v = o.get(k);
        return (v == null || v.isJsonNull()) ? null : v.getAsString();
    }
}
