package mamun.sound.fire.ui.home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import mamun.sound.fire.R;
import mamun.sound.fire.adapter.BannerAdapter;
import mamun.sound.fire.data.SoundRepository;
import mamun.sound.fire.model.Banner;
import mamun.sound.fire.model.BannersData;
import mamun.sound.fire.model.SoundsData;
import mamun.sound.fire.network.ApiError;
import mamun.sound.fire.ui.common.NoticeChecker;
import mamun.sound.fire.ui.common.SoundGridFragment;
import mamun.sound.fire.ui.notices.NoticesActivity;
import mamun.sound.fire.util.AppPrefs;
import mamun.sound.fire.util.ChipRow;
import mamun.sound.fire.util.Links;

import java.util.Arrays;
import java.util.List;

/**
 * Home: search box (with recent searches), notifications bell, banner slider, All / Trending / New,
 * and the sound grid.
 */
public class HomeFragment extends SoundGridFragment implements NoticeChecker.Listener {

    private static final String[] SORTS = {null, "trending", "new"};
    private static final long DEBOUNCE_MS = 400;
    private static final long BANNER_MS = 5000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable doReload = this::reload;
    private final Runnable nextBanner = this::advanceBanner;

    private String query = "";
    private String sort = null;
    private boolean hasBanners;

    private EditText search;
    private View btnClear, sortScroll, recentsScroll, bellDot;
    private LinearLayout recentsRow;
    private ViewPager2 bannerPager;

    @Override protected View createHeader(LayoutInflater inflater, ViewGroup parent) {
        View h = inflater.inflate(R.layout.header_home, parent, false);
        search = h.findViewById(R.id.inputSearch);
        btnClear = h.findViewById(R.id.btnClear);
        sortScroll = h.findViewById(R.id.sortScroll);
        recentsScroll = h.findViewById(R.id.recentsScroll);
        recentsRow = h.findViewById(R.id.recentsRow);
        bannerPager = h.findViewById(R.id.bannerPager);
        bellDot = h.findViewById(R.id.bellDot);

        h.findViewById(R.id.bellBox).setOnClickListener(v -> startActivity(new Intent(requireContext(), NoticesActivity.class)));

        List<String> labels = Arrays.asList(getString(R.string.chip_all), getString(R.string.chip_trending), getString(R.string.chip_new));
        ChipRow.bind(h.findViewById(R.id.sortRow), labels, 0, index -> { sort = SORTS[index]; reload(); });

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                query = s.toString().trim();
                btnClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                sortScroll.setVisibility(query.isEmpty() ? View.VISIBLE : View.GONE);   // sort does not apply to search
                updateBannerVisibility();                                               // banners also step aside while searching
                updateRecents();
                handler.removeCallbacks(doReload);
                handler.postDelayed(doReload, query.isEmpty() ? 0 : DEBOUNCE_MS);       // wait until typing pauses
            }
        });
        search.setOnFocusChangeListener((v, focused) -> updateRecents());
        search.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEARCH) return false;
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(search.getWindowToken(), 0);
            if (query.length() >= 2) AppPrefs.addRecentSearch(requireContext(), query);
            handler.removeCallbacks(doReload);
            reload();
            return true;
        });
        btnClear.setOnClickListener(v -> search.setText(""));

        // Auto-advance, but not while the user is dragging
        bannerPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageScrollStateChanged(int state) {
                if (state == ViewPager2.SCROLL_STATE_IDLE) scheduleBanner();
                else handler.removeCallbacks(nextBanner);
            }
        });
        loadBanners();
        updateBell();
        return h;
    }

    // ---------- banners ----------
    private void loadBanners() {
        repo.banners(new SoundRepository.Callback<BannersData>() {
            @Override public void onData(BannersData d, boolean fromCache) {
                if (!isAdded()) return;
                List<Banner> list = d.banners;
                hasBanners = list != null && !list.isEmpty();
                if (hasBanners) bannerPager.setAdapter(new BannerAdapter(list, banner -> openBanner(banner)));
                updateBannerVisibility();
                scheduleBanner();
            }
            @Override public void onError(ApiError e) { /* banners are optional: stay hidden */ }
        });
    }

    private void openBanner(Banner b) {
        if (b.link == null || b.link.trim().isEmpty()) return;
        if (!Links.openWeb(requireActivity(), b.link))      // https only
            Toast.makeText(requireContext(), R.string.err_cannot_open, Toast.LENGTH_SHORT).show();
    }

    private void updateBannerVisibility() {
        if (bannerPager == null) return;
        bannerPager.setVisibility(hasBanners && query.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void scheduleBanner() {
        handler.removeCallbacks(nextBanner);
        if (hasBanners && query.isEmpty()) handler.postDelayed(nextBanner, BANNER_MS);
    }

    private void advanceBanner() {
        RecyclerView.Adapter<?> a = bannerPager == null ? null : bannerPager.getAdapter();
        if (a != null && a.getItemCount() > 1 && isResumed())
            bannerPager.setCurrentItem((bannerPager.getCurrentItem() + 1) % a.getItemCount(), true);
        scheduleBanner();
    }

    // ---------- bell ----------
    private void updateBell() {
        if (bellDot != null && isAdded())
            bellDot.setVisibility(NoticeChecker.hasUnread(requireContext()) ? View.VISIBLE : View.GONE);
    }

    @Override public void onNoticesChanged() { updateBell(); }

    @Override public void onStart() {
        super.onStart();
        NoticeChecker.addListener(this);
        updateBell();
        scheduleBanner();
    }

    @Override public void onStop() {
        NoticeChecker.removeListener(this);
        handler.removeCallbacks(nextBanner);
        super.onStop();
    }

    // ---------- search ----------
    /** Recent searches show only while the box is focused and empty. */
    private void updateRecents() {
        if (!isAdded()) return;
        List<String> recents = AppPrefs.recentSearches(requireContext());
        boolean show = search.hasFocus() && query.isEmpty() && !recents.isEmpty();
        recentsScroll.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) ChipRow.bind(recentsRow, recents, -1, i -> search.setText(recents.get(i)));
    }

    @Override protected void loadPage(int offset, SoundRepository.Callback<SoundsData> cb) {
        if (!query.isEmpty()) {
            repo.search(query, new SoundRepository.Callback<SoundsData>() {
                @Override public void onData(SoundsData d, boolean fromCache) { d.hasMore = false; cb.onData(d, fromCache); }  // search is one page
                @Override public void onError(ApiError e) { cb.onError(e); }
            });
            return;
        }
        String key = offset == 0 ? "home_" + (sort == null ? "all" : sort) : null;   // cache the first page only
        repo.sounds(key, null, sort, 30, offset, cb);
    }

    @Override protected int emptyTextRes() {
        return query.isEmpty() ? R.string.state_empty_sounds : R.string.state_no_results;
    }

    @Override public void onDestroyView() {
        handler.removeCallbacks(doReload);
        handler.removeCallbacks(nextBanner);
        super.onDestroyView();
    }
}
