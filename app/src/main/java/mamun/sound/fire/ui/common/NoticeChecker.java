package mamun.sound.fire.ui.common;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import mamun.sound.fire.R;
import mamun.sound.fire.data.SoundRepository;
import mamun.sound.fire.model.Notice;
import mamun.sound.fire.model.NoticesData;
import mamun.sound.fire.network.ApiError;
import mamun.sound.fire.ui.notices.NoticesActivity;
import mamun.sound.fire.util.AppPrefs;
import mamun.sound.fire.util.Nav;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Checks for new admin notices when the app comes to the front (at most every 10 minutes), shows a pop-up for
 * a new notice that asks for one, and keeps the unread dot on the Home bell right. Main thread only.
 *
 * The first check on a new install or account only sets a baseline, so nobody is greeted with old messages.
 */
public final class NoticeChecker {
    private NoticeChecker() {}

    public interface Listener { void onNoticesChanged(); }

    private static final long MIN_GAP_MS = 10 * 60 * 1000;
    private static final CopyOnWriteArrayList<Listener> LISTENERS = new CopyOnWriteArrayList<>();
    private static long lastCheck;

    public static void addListener(Listener l) { LISTENERS.add(l); }
    public static void removeListener(Listener l) { LISTENERS.remove(l); }
    public static void notifyChanged() { for (Listener l : LISTENERS) l.onNoticesChanged(); }

    public static boolean hasUnread(Context c) { return AppPrefs.noticeLatest(c) > AppPrefs.noticeRead(c); }

    public static void check(Activity a) {
        long now = SystemClock.elapsedRealtime();
        if (lastCheck != 0 && now - lastCheck < MIN_GAP_MS) return;
        lastCheck = now;
        Context app = a.getApplicationContext();

        SoundRepository.get(app).notices(new SoundRepository.Callback<NoticesData>() {
            @Override public void onData(NoticesData d, boolean fromCache) {
                if (fromCache) { lastCheck = 0; return; }          // offline: look again next time
                handle(a, app, d);
            }
            @Override public void onError(ApiError e) {
                lastCheck = 0;
                if (!a.isFinishing() && !a.isDestroyed()) Nav.handleCommonError(a, e);
            }
        });
    }

    private static void handle(Activity a, Context app, NoticesData d) {
        List<Notice> list = d.notices == null ? Collections.<Notice>emptyList() : d.notices;
        long latest = 0;
        for (Notice n : list) latest = Math.max(latest, n.id);
        AppPrefs.setNoticeLatest(app, latest);

        long seen = AppPrefs.noticePopupSeen(app);
        if (seen < 0) {                                  // first check: baseline only
            AppPrefs.setNoticePopupSeen(app, latest);
            AppPrefs.setNoticeRead(app, latest);
            notifyChanged();
            return;
        }

        Notice popup = null;                             // the newest unseen notice that wants a pop-up
        for (Notice n : list) if (n.id > seen && n.showsPopup() && (popup == null || n.id > popup.id)) popup = n;
        AppPrefs.setNoticePopupSeen(app, latest);        // older ones are skipped, not queued

        if (popup != null && !a.isFinishing() && !a.isDestroyed()) {
            AppPrefs.setNoticeRead(app, Math.max(AppPrefs.noticeRead(app), popup.id));
            new AppDialog.Spec(R.drawable.ic_bell, popup.title, popup.message)
                    .primary(a.getString(R.string.notice_open), false, t -> a.startActivity(new Intent(a, NoticesActivity.class)))
                    .cancel(a.getString(R.string.btn_close))
                    .show(a);
        }
        notifyChanged();
    }
}
