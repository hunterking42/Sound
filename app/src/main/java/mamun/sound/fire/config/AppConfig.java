package mamun.sound.fire.config;

/** All app-wide constants in ONE place (never hardcode the API URL elsewhere). */
public final class AppConfig {
    private AppConfig() {}

    /** HTTPS only. Must end with a slash. Replace with your server's public folder URL. */
    public static final String BASE_URL = "https://example.com/soundbomb/";

    // Intent extra keys used when routing between screens
    public static final String EXTRA_MESSAGE = "extra_message";
    public static final String EXTRA_STATUS = "extra_status";          // banned / locked
    public static final String EXTRA_REASON = "extra_reason";
    public static final String EXTRA_LOCK_UNTIL = "extra_lock_until";
}
