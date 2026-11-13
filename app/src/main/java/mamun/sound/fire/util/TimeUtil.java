package mamun.sound.fire.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class TimeUtil {
    private TimeUtil() {}

    /** Server times are UTC "yyyy-MM-dd HH:mm:ss". Returns a local, readable string (or the input if it can't parse). */
    public static String utcToLocal(String utc) {
        if (utc == null) return null;
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            in.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d = in.parse(utc);
            return d == null ? utc : DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(d);
        } catch (Exception e) {
            return utc;
        }
    }

    /** UTC "yyyy-MM-dd HH:mm:ss" to epoch millis, or -1 if it cannot be parsed. */
    public static long parseUtcMillis(String utc) {
        if (utc == null) return -1;
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            in.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d = in.parse(utc);
            return d == null ? -1 : d.getTime();
        } catch (Exception e) {
            return -1;
        }
    }
}
