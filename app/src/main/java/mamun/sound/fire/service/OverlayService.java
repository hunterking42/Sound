package mamun.sound.fire.service;

import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import mamun.sound.fire.R;
import mamun.sound.fire.adapter.SoundAdapter;
import mamun.sound.fire.data.FavoritesStore;
import mamun.sound.fire.data.SoundRepository;
import mamun.sound.fire.model.Sound;
import mamun.sound.fire.model.SoundsData;
import mamun.sound.fire.network.ApiError;
import mamun.sound.fire.ui.premium.PremiumActivity;
import mamun.sound.fire.ui.splash.SplashActivity;
import mamun.sound.fire.util.AppPrefs;
import mamun.sound.fire.util.OverlayPermission;
import mamun.sound.fire.util.SecurePrefs;
import mamun.sound.fire.util.Session;
import mamun.sound.fire.util.SoundPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Floating bubble over any app. Foreground service with a persistent notification
 * (actions: Stop sound, Turn off). Tap the bubble to open a quick panel with the shared SoundCard grid
 * (favorites first). Drag the bubble anywhere; it snaps to the nearest side.
 *
 * Everything here runs on the main thread.
 */
public class OverlayService extends Service
        implements SoundPlayer.Listener, FavoritesStore.Listener, SoundAdapter.Listener {

    public static final String ACTION_STOP_SOUND = "mamun.sound.fire.STOP_SOUND";
    public static final String ACTION_TURN_OFF = "mamun.sound.fire.TURN_OFF";
    private static final String CHANNEL_ID = "overlay";
    private static final int NOTIFICATION_ID = 1001;
    private static final int PANEL_MAX_SOUNDS = 60;

    /** True while the service is alive (read by OverlayController). */
    public static volatile boolean running;

    private Context themed;                 // a Service has no app theme: SoundCard needs one to inflate
    private WindowManager wm;
    private SoundPlayer player;
    private FavoritesStore favorites;
    private SoundRepository repo;

    private ImageView bubble;
    private WindowManager.LayoutParams bubbleLp;
    private int bubbleSize, touchSlop;

    private View panel;
    private SoundAdapter adapter;
    private View panelLoading;
    private TextView panelStatus;

    private boolean destroyed;

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }

    @Override public void onCreate() {
        super.onCreate();
        running = true;
        createChannel();
        // Must be called quickly after startForegroundService, even if we stop right after
        int type = Build.VERSION.SDK_INT >= 34 ? ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE : 0;
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type);

        if (!OverlayPermission.isGranted(this) || !SecurePrefs.hasToken()) { turnOff(); return; }

        themed = new ContextThemeWrapper(this, R.style.Theme_SoundBomb);
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        repo = SoundRepository.get(this);
        player = SoundPlayer.get(this);
        favorites = FavoritesStore.get(this);
        player.addListener(this);
        favorites.addListener(this);
        addBubble();
    }

    /** Notification buttons arrive here. */
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (ACTION_STOP_SOUND.equals(action) && player != null) player.stop();     // panic stop
        else if (ACTION_TURN_OFF.equals(action)) turnOff();
        return START_NOT_STICKY;      // the app restarts it on launch if the setting is still on
    }

    @Override public void onDestroy() {
        destroyed = true;
        running = false;
        if (player != null) player.removeListener(this);
        if (favorites != null) favorites.removeListener(this);
        closePanel();
        if (bubble != null) {
            try { wm.removeView(bubble); } catch (IllegalArgumentException ignored) { /* already gone */ }
            bubble = null;
        }
        super.onDestroy();
    }

    @Override public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        if (bubble == null) return;
        closePanel();                       // sizes depend on the screen: reopen to get new ones
        clampBubble();
        wm.updateViewLayout(bubble, bubbleLp);
    }

    private void turnOff() {
        AppPrefs.setOverlayEnabled(this, false);
        stopSelf();
    }

    // ---------- notification ----------
    private void createChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, getString(R.string.overlay_channel_name),
                NotificationManager.IMPORTANCE_LOW);          // silent, no pop-up
        getSystemService(NotificationManager.class).createNotificationChannel(ch);
    }

    private Notification buildNotification() {
        int imm = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, SplashActivity.class), imm);
        PendingIntent stopSound = PendingIntent.getService(this, 1,
                new Intent(this, OverlayService.class).setAction(ACTION_STOP_SOUND), imm);
        PendingIntent off = PendingIntent.getService(this, 2,
                new Intent(this, OverlayService.class).setAction(ACTION_TURN_OFF), imm);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_wave)
                .setContentTitle(getString(R.string.overlay_notif_title))
                .setContentText(getString(R.string.overlay_notif_text))
                .setContentIntent(open)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .addAction(R.drawable.ic_stop, getString(R.string.overlay_action_stop_sound), stopSound)
                .addAction(R.drawable.ic_close, getString(R.string.overlay_action_off), off)
                .build();
    }

    // ---------- bubble ----------
    @SuppressWarnings("deprecation")
    private int overlayType() {
        return Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                                           : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private void addBubble() {
        bubbleSize = dp(56);
        bubble = new ImageView(themed);
        bubble.setImageResource(R.drawable.ic_wave);
        bubble.setColorFilter(getColor(R.color.on_fuse));
        bubble.setBackgroundResource(R.drawable.bg_bubble);
        bubble.setPadding(dp(14), dp(14), dp(14), dp(14));
        bubble.setContentDescription(getString(R.string.overlay_cd_bubble));
        bubble.setActivated(player.getPlayingId() != -1);

        bubbleLp = new WindowManager.LayoutParams(bubbleSize, bubbleSize, overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        bubbleLp.gravity = Gravity.TOP | Gravity.START;
        int[] saved = AppPrefs.overlayPos(this);
        bubbleLp.x = saved != null ? saved[0] : 0;
        bubbleLp.y = saved != null ? saved[1] : screen().heightPixels / 3;
        clampBubble();

        bubble.setOnTouchListener(new View.OnTouchListener() {
            int startX, startY; float downX, downY; boolean moved;
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = bubbleLp.x; startY = bubbleLp.y;
                        downX = e.getRawX(); downY = e.getRawY();
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getRawX() - downX, dy = e.getRawY() - downY;
                        if (!moved && Math.hypot(dx, dy) > touchSlop) moved = true;     // small wobble is still a tap
                        if (moved) {
                            bubbleLp.x = startX + (int) dx;
                            bubbleLp.y = startY + (int) dy;
                            clampBubble();
                            wm.updateViewLayout(bubble, bubbleLp);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (moved) snapToEdge();
                        else { v.performClick(); togglePanel(); }
                        return true;
                    default:
                        return false;
                }
            }
        });
        bubble.setOnClickListener(v -> {});     // keeps performClick() accessible
        wm.addView(bubble, bubbleLp);
    }

    private DisplayMetrics screen() { return getResources().getDisplayMetrics(); }

    private void clampBubble() {
        DisplayMetrics dm = screen();
        bubbleLp.x = Math.max(0, Math.min(bubbleLp.x, dm.widthPixels - bubbleSize));
        bubbleLp.y = Math.max(0, Math.min(bubbleLp.y, dm.heightPixels - bubbleSize));
    }

    /** After a drag, glide to the nearest side and remember the spot. */
    private void snapToEdge() {
        int maxX = screen().widthPixels - bubbleSize;
        int target = (bubbleLp.x + bubbleSize / 2 < screen().widthPixels / 2) ? 0 : maxX;
        ValueAnimator a = ValueAnimator.ofInt(bubbleLp.x, target).setDuration(150);
        a.addUpdateListener(anim -> {
            if (bubble == null) return;
            bubbleLp.x = (int) anim.getAnimatedValue();
            wm.updateViewLayout(bubble, bubbleLp);
        });
        a.start();
        AppPrefs.setOverlayPos(this, target, bubbleLp.y);
    }

    /** A panel added later is drawn above the bubble: put the bubble back on top. */
    private void raiseBubble() {
        if (bubble == null || destroyed) return;
        try { wm.removeView(bubble); wm.addView(bubble, bubbleLp); } catch (RuntimeException ignored) {}
    }

    // ---------- panel ----------
    private void togglePanel() {
        if (panel != null) closePanel(); else openPanel();
    }

    private void openPanel() {
        if (!SecurePrefs.hasToken()) { Toast.makeText(this, R.string.overlay_signed_out, Toast.LENGTH_LONG).show(); turnOff(); return; }

        panel = LayoutInflater.from(themed).inflate(R.layout.overlay_panel, null);
        panelLoading = panel.findViewById(R.id.panelLoading);
        panelStatus = panel.findViewById(R.id.panelStatus);

        adapter = new SoundAdapter(this);          // the same SoundCard adapter as every screen
        adapter.setUserPremium(Session.isPremium());
        adapter.setFavoriteIds(favorites.ids());
        adapter.setPlayingId(player.getPlayingId());
        adapter.setLoadingId(player.getLoadingId());

        RecyclerView grid = panel.findViewById(R.id.panelGrid);
        grid.setLayoutManager(new GridLayoutManager(themed, Math.min(3, AppPrefs.gridColumns(this))));
        grid.setAdapter(adapter);
        ((SimpleItemAnimator) grid.getItemAnimator()).setSupportsChangeAnimations(false);

        panel.findViewById(R.id.btnPanelStop).setOnClickListener(v -> player.stop());
        panel.findViewById(R.id.btnPanelClose).setOnClickListener(v -> closePanel());
        // A tap outside the panel closes it
        panel.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_OUTSIDE) { closePanel(); return true; }
            return false;
        });

        DisplayMetrics dm = screen();
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                Math.min(dm.widthPixels - dp(24), dp(380)),
                Math.min((int) (dm.heightPixels * 0.55f), dp(440)),
                overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp.y = dp(16);
        wm.addView(panel, lp);
        bubble.post(this::raiseBubble);
        loadPanelSounds();
    }

    private void closePanel() {
        if (panel == null) return;
        try { wm.removeView(panel); } catch (IllegalArgumentException ignored) {}
        panel = null;
        adapter = null;
    }

    /** Favorites first, then trending. Offline, the saved lists are used. */
    private void loadPanelSounds() {
        panelLoading.setVisibility(View.VISIBLE);
        panelStatus.setVisibility(View.GONE);
        repo.favoriteSounds(new SoundRepository.Callback<SoundsData>() {
            @Override public void onData(SoundsData fav, boolean fromCache) {
                if (!panelAlive()) return;
                loadTrending(fav.sounds != null ? fav.sounds : Collections.<Sound>emptyList());
            }
            @Override public void onError(ApiError e) {
                if (!panelAlive() || stopForError(e)) return;
                loadTrending(Collections.<Sound>emptyList());
            }
        });
    }

    private void loadTrending(List<Sound> favs) {
        repo.sounds("home_trending", null, "trending", 30, 0, new SoundRepository.Callback<SoundsData>() {
            @Override public void onData(SoundsData d, boolean fromCache) {
                if (!panelAlive()) return;
                showSounds(merge(favs, d.sounds));
            }
            @Override public void onError(ApiError e) {
                if (!panelAlive() || stopForError(e)) return;
                showSounds(merge(favs, null));       // at least the favorites
            }
        });
    }

    private static List<Sound> merge(List<Sound> first, @Nullable List<Sound> second) {
        Map<Long, Sound> out = new LinkedHashMap<>();       // keeps order, drops duplicates
        for (Sound s : first) out.put(s.id, s);
        if (second != null) for (Sound s : second) if (!out.containsKey(s.id)) out.put(s.id, s);
        List<Sound> list = new ArrayList<>(out.values());
        return list.size() > PANEL_MAX_SOUNDS ? list.subList(0, PANEL_MAX_SOUNDS) : list;
    }

    private void showSounds(List<Sound> list) {
        panelLoading.setVisibility(View.GONE);
        adapter.submitList(new ArrayList<>(list));
        panelStatus.setText(R.string.overlay_panel_empty);
        panelStatus.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private boolean panelAlive() { return !destroyed && panel != null && adapter != null; }

    /** Signed out, banned, or maintenance: the bubble cannot do anything useful, so turn it off. */
    private boolean stopForError(ApiError e) {
        if (e.httpCode == 426) return true;                  // ApiClient opens the update screen
        if (e.isBlocked() || e.isAuthInvalid() || e.isMaintenance()) {
            Toast.makeText(this, R.string.overlay_signed_out, Toast.LENGTH_LONG).show();
            turnOff();
            return true;
        }
        return false;
    }

    // ---------- SoundAdapter.Listener ----------
    @Override public void onPlayClick(Sound sound) {
        SoundPlayer.Result r = player.play(sound, Session.isPremium(), err ->
                Toast.makeText(this, err.messageRes, Toast.LENGTH_SHORT).show());
        if (r == SoundPlayer.Result.PREMIUM_REQUIRED) {
            closePanel();
            startActivity(new Intent(this, PremiumActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    @Override public void onFavoriteClick(Sound sound) {
        favorites.toggle(sound.id, () -> Toast.makeText(this, R.string.err_favorite, Toast.LENGTH_SHORT).show());
    }

    /** No long-press menu here: a popup needs a focusable window, which would steal keys from the game. */
    @Override public void onMenuClick(Sound sound, View anchor) {}

    // ---------- SoundPlayer.Listener / FavoritesStore.Listener ----------
    @Override public void onStateChanged(long playingId, long loadingId) {
        if (bubble != null) bubble.setActivated(playingId != -1);     // glow ring on the bubble
        if (adapter != null) { adapter.setPlayingId(playingId); adapter.setLoadingId(loadingId); }
    }

    @Override public void onFavoritesChanged(Set<Long> ids) {
        if (adapter != null) adapter.setFavoriteIds(ids);
    }
}
