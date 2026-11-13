package mamun.sound.fire.ui.main;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import mamun.sound.fire.R;
import mamun.sound.fire.data.FavoritesStore;
import mamun.sound.fire.ui.category.CategoryFragment;
import mamun.sound.fire.ui.common.NoticeChecker;
import mamun.sound.fire.ui.favorites.FavoritesFragment;
import mamun.sound.fire.ui.home.HomeFragment;
import mamun.sound.fire.util.AppPrefs;
import mamun.sound.fire.util.SoundPlayer;

/**
 * Bottom navigation: Home, Categories, Favorites, Settings.
 * All four fragments stay alive (show/hide), so scroll position and playing state survive tab changes.
 * A mini player above the bar shows the playing sound with a stop button (panic stop).
 */
public class MainActivity extends AppCompatActivity {

    private static final String[] TAGS = {"home", "categories", "favorites", "settings"};
    private static final int[] IDS = {R.id.nav_home, R.id.nav_categories, R.id.nav_favorites, R.id.nav_settings};

    private BottomNavigationView nav;
    private SoundPlayer player;
    private View miniPlayer;
    private TextView miniName;
    private final SoundPlayer.Listener playerListener = (playing, loading) -> updateMini(playing);

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        nav = findViewById(R.id.bottomNav);
        miniPlayer = findViewById(R.id.miniPlayer);
        miniName = findViewById(R.id.miniName);

        player = SoundPlayer.get(this);
        player.setVolume(AppPrefs.volume(this));
        findViewById(R.id.btnMiniStop).setOnClickListener(v -> player.stop());
        FavoritesStore.get(this).refresh();                    // hearts on every grid
        mamun.sound.fire.util.OverlayController.syncOnLaunch(this);

        // First start only. After rotation the FragmentManager restores the fragments and their hidden state.
        if (b == null) {
            FragmentTransaction t = getSupportFragmentManager().beginTransaction();
            for (int i = 0; i < TAGS.length; i++) {
                Fragment f = createTab(i);
                t.add(R.id.container, f, TAGS[i]);
                if (i != 0) t.hide(f);
            }
            t.commitNow();
        }

        nav.setOnItemSelectedListener(item -> { showTab(item.getItemId()); return true; });

        // Back from another tab returns to Home; Back on Home leaves the app
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (nav.getSelectedItemId() != R.id.nav_home) {
                    nav.setSelectedItemId(R.id.nav_home);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    @Override protected void onStart() {
        super.onStart();
        player.addListener(playerListener);
        updateMini(player.getPlayingId());
        NoticeChecker.check(this);                              // new admin notice? (at most every 10 minutes)
    }

    @Override protected void onStop() {
        player.removeListener(playerListener);
        super.onStop();
    }

    private Fragment createTab(int i) {
        switch (i) {
            case 0:  return new HomeFragment();
            case 1:  return new CategoryFragment();
            case 2:  return new FavoritesFragment();
            default: return new mamun.sound.fire.ui.settings.SettingsFragment();
        }
    }

    private void updateMini(long playingId) {
        if (playingId == -1) { miniPlayer.setVisibility(View.GONE); return; }
        miniName.setText(getString(R.string.mini_playing, player.getPlayingName()));
        miniPlayer.setVisibility(View.VISIBLE);
    }

    private void showTab(int itemId) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction t = fm.beginTransaction();
        for (int i = 0; i < TAGS.length; i++) {
            Fragment f = fm.findFragmentByTag(TAGS[i]);
            if (f == null) continue;
            if (IDS[i] == itemId) t.show(f); else t.hide(f);
        }
        t.commit();
    }
}
