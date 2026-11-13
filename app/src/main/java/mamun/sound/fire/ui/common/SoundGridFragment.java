package mamun.sound.fire.ui.common;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
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
import mamun.sound.fire.util.AppPrefs;
import mamun.sound.fire.util.Nav;
import mamun.sound.fire.util.Session;
import mamun.sound.fire.util.SoundPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Base for every screen that shows the sound grid (Home, Category, Favorites).
 * It owns the SoundCard adapter, loading / empty / error states, paging, the offline banner,
 * play, favorite and long-press menu. A subclass only says WHAT to load.
 */
public abstract class SoundGridFragment extends Fragment
        implements SoundAdapter.Listener, SoundPlayer.Listener, FavoritesStore.Listener {

    private static final int LOAD_MORE_AT = 6;     // items from the end when the next page starts loading

    protected SoundAdapter adapter;
    protected SoundRepository repo;
    private SoundPlayer player;
    private FavoritesStore favorites;

    private RecyclerView grid;
    private GridLayoutManager layoutManager;
    private View loadingView, stateGroup, offlineBanner;
    private TextView stateText;
    private Button stateButton;

    private final List<Sound> items = new ArrayList<>();
    private int generation;                         // bumped on reload: late answers from old requests are ignored
    private boolean hasMore, loadingPage;

    // ---------- what subclasses provide ----------
    /** Load one page. Use a cache key only when offset == 0. */
    protected abstract void loadPage(int offset, SoundRepository.Callback<SoundsData> cb);
    protected abstract @StringRes int emptyTextRes();
    /** Optional view above the grid (search box, chips). */
    protected View createHeader(LayoutInflater inflater, ViewGroup parent) { return null; }
    /** Return false if the subclass must prepare something first and then call reload() itself. */
    protected boolean autoLoad() { return true; }
    /** Retry button. */
    protected void onRetry() { reload(); }

    // ---------- lifecycle ----------
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle b) {
        View v = inflater.inflate(R.layout.fragment_sound_grid, container, false);
        repo = SoundRepository.get(requireContext());
        player = SoundPlayer.get(requireContext());
        favorites = FavoritesStore.get(requireContext());

        grid = v.findViewById(R.id.grid);
        loadingView = v.findViewById(R.id.loading);
        stateGroup = v.findViewById(R.id.stateGroup);
        offlineBanner = v.findViewById(R.id.offlineBanner);
        stateText = v.findViewById(R.id.stateText);
        stateButton = v.findViewById(R.id.stateButton);
        stateButton.setOnClickListener(x -> onRetry());

        adapter = new SoundAdapter(this);
        layoutManager = new GridLayoutManager(requireContext(), AppPrefs.gridColumns(requireContext()));
        grid.setLayoutManager(layoutManager);
        grid.setAdapter(adapter);
        // State refreshes (playing glow, hearts) should not cross-fade the whole card
        ((SimpleItemAnimator) grid.getItemAnimator()).setSupportsChangeAnimations(false);
        grid.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy <= 0 || !hasMore || loadingPage) return;
                if (layoutManager.findLastVisibleItemPosition() >= items.size() - LOAD_MORE_AT) request(items.size());
            }
        });

        FrameLayout headerHolder = v.findViewById(R.id.header);
        View header = createHeader(inflater, headerHolder);
        if (header != null) { headerHolder.addView(header); headerHolder.setVisibility(View.VISIBLE); }

        if (autoLoad()) reload();
        return v;
    }

    @Override public void onStart() {
        super.onStart();
        player.addListener(this);
        favorites.addListener(this);
        layoutManager.setSpanCount(AppPrefs.gridColumns(requireContext()));   // user may have changed it in Settings
        adapter.setUserPremium(Session.isPremium());
        adapter.setFavoriteIds(favorites.ids());
        adapter.setPlayingId(player.getPlayingId());
        adapter.setLoadingId(player.getLoadingId());
    }

    @Override public void onStop() {
        player.removeListener(this);
        favorites.removeListener(this);
        super.onStop();
    }

    @Override public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden || adapter == null) return;
        adapter.setUserPremium(Session.isPremium());                              // may have changed
        layoutManager.setSpanCount(AppPrefs.gridColumns(requireContext()));      // Settings may have changed it
    }

    // ---------- loading ----------
    public void reload() {
        if (!isAdded()) return;
        generation++;
        items.clear();
        adapter.submitList(Collections.emptyList());
        hasMore = false;
        showLoading();
        request(0);
    }

    private void request(int offset) {
        final int gen = generation;
        loadingPage = true;
        loadPage(offset, new SoundRepository.Callback<SoundsData>() {
            @Override public void onData(SoundsData d, boolean fromCache) {
                if (!isAdded() || gen != generation) return;
                loadingPage = false;
                if (d.sounds != null) items.addAll(d.sounds);
                hasMore = d.hasMore && !fromCache;
                offlineBanner.setVisibility(fromCache ? View.VISIBLE : View.GONE);
                render();
            }
            @Override public void onError(ApiError e) {
                if (!isAdded() || gen != generation) return;
                loadingPage = false;
                if (Nav.handleCommonError(requireActivity(), e)) return;
                if (offset == 0) showError(e);          // later pages fail quietly; scrolling again retries
            }
        });
    }

    /** For subclasses that change the list themselves (Favorites removing a sound). */
    protected void setItems(List<Sound> list) {
        items.clear();
        items.addAll(list);
        render();
    }
    protected List<Sound> getItems() { return new ArrayList<>(items); }

    private void render() {
        adapter.submitList(new ArrayList<>(items));
        if (items.isEmpty()) showMessage(emptyTextRes(), false);
        else { grid.setVisibility(View.VISIBLE); loadingView.setVisibility(View.GONE); stateGroup.setVisibility(View.GONE); }
    }

    protected void showLoading() {
        grid.setVisibility(View.GONE);
        stateGroup.setVisibility(View.GONE);
        loadingView.setVisibility(View.VISIBLE);
    }

    protected void showError(ApiError e) {
        showMessage(e.isNetwork() ? R.string.error_no_internet : R.string.error_generic, true);
    }

    private void showMessage(@StringRes int textRes, boolean withRetry) {
        grid.setVisibility(View.GONE);
        loadingView.setVisibility(View.GONE);
        stateText.setText(textRes);
        stateButton.setVisibility(withRetry ? View.VISIBLE : View.GONE);
        stateGroup.setVisibility(View.VISIBLE);
    }

    // ---------- SoundAdapter.Listener ----------
    @Override public void onPlayClick(Sound sound) {
        SoundPlayer.Result r = player.play(sound, Session.isPremium(), err -> {
            if (isAdded()) Toast.makeText(requireContext(), err.messageRes, Toast.LENGTH_SHORT).show();
        });
        if (r == SoundPlayer.Result.PREMIUM_REQUIRED) Nav.openPremium(requireActivity());
    }

    @Override public void onFavoriteClick(Sound sound) {
        favorites.toggle(sound.id, () -> {
            if (isAdded()) Toast.makeText(requireContext(), R.string.err_favorite, Toast.LENGTH_SHORT).show();
        });
    }

    /** Long press: Favorite and Share. (Timer and Combo are V2.) */
    @Override public void onMenuClick(Sound sound, View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add(0, 1, 0, favorites.contains(sound.id) ? R.string.menu_unfavorite : R.string.menu_favorite);
        menu.getMenu().add(0, 2, 1, R.string.menu_share);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) { onFavoriteClick(sound); return true; }
            Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, getString(R.string.share_sound_text, sound.name));
            startActivity(Intent.createChooser(send, getString(R.string.menu_share)));
            return true;
        });
        menu.show();
    }

    // ---------- SoundPlayer.Listener / FavoritesStore.Listener ----------
    @Override public void onStateChanged(long playingId, long loadingId) {
        if (adapter == null) return;
        adapter.setPlayingId(playingId);
        adapter.setLoadingId(loadingId);
    }

    @Override public void onFavoritesChanged(Set<Long> ids) {
        if (adapter != null) adapter.setFavoriteIds(ids);
    }
}
