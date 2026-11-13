package mamun.sound.fire.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import mamun.sound.fire.R;
import mamun.sound.fire.network.ApiError;
import mamun.sound.fire.util.Nav;

import java.util.ArrayList;
import java.util.List;

/** Base for admin lists (users, logs): loading / empty / error states and paging. Subclasses say what to load. */
public abstract class AdminListFragment<T> extends Fragment {

    public interface PageCallback<T> {
        void onPage(List<T> items, boolean hasMore);
        void onError(ApiError error);
    }

    private static final int LOAD_MORE_AT = 5;

    protected abstract ListAdapter<T, ? extends RecyclerView.ViewHolder> createAdapter();
    protected abstract void loadPage(int offset, PageCallback<T> cb);
    protected abstract @StringRes int titleRes();
    protected abstract @StringRes int emptyTextRes();
    protected View createHeader(LayoutInflater inflater, ViewGroup parent) { return null; }

    private ListAdapter<T, ? extends RecyclerView.ViewHolder> adapter;
    private RecyclerView list;
    private View loadingView, stateGroup;
    private TextView stateText;
    private Button stateButton;
    private LinearLayoutManager layoutManager;

    private final List<T> items = new ArrayList<>();
    private int generation;                 // bumped on reload so late answers from old requests are ignored
    private boolean hasMore, loadingPage;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle b) {
        View v = inflater.inflate(R.layout.fragment_admin_list, container, false);
        list = v.findViewById(R.id.list);
        loadingView = v.findViewById(R.id.loading);
        stateGroup = v.findViewById(R.id.stateGroup);
        stateText = v.findViewById(R.id.stateText);
        stateButton = v.findViewById(R.id.stateButton);
        stateButton.setOnClickListener(x -> reload());

        adapter = createAdapter();
        layoutManager = new LinearLayoutManager(requireContext());
        list.setLayoutManager(layoutManager);
        list.setAdapter(adapter);
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 0 && hasMore && !loadingPage && layoutManager.findLastVisibleItemPosition() >= items.size() - LOAD_MORE_AT)
                    request(items.size());
            }
        });

        FrameLayout headerHolder = v.findViewById(R.id.header);
        View header = createHeader(inflater, headerHolder);
        if (header != null) { headerHolder.addView(header); headerHolder.setVisibility(View.VISIBLE); }
        reload();
        return v;
    }

    @Override public void onResume() {
        super.onResume();
        ((AdminActivity) requireActivity()).setScreenTitle(titleRes());
    }

    public void reload() {
        if (!isAdded()) return;
        generation++;
        items.clear();
        adapter.submitList(new ArrayList<>());
        hasMore = false;
        list.setVisibility(View.GONE);
        stateGroup.setVisibility(View.GONE);
        loadingView.setVisibility(View.VISIBLE);
        request(0);
    }

    private void request(int offset) {
        final int gen = generation;
        loadingPage = true;
        loadPage(offset, new PageCallback<T>() {
            @Override public void onPage(List<T> page, boolean more) {
                if (!isAdded() || gen != generation) return;
                loadingPage = false;
                items.addAll(page);
                hasMore = more;
                adapter.submitList(new ArrayList<>(items));
                loadingView.setVisibility(View.GONE);
                if (items.isEmpty()) showMessage(emptyTextRes(), false);
                else { stateGroup.setVisibility(View.GONE); list.setVisibility(View.VISIBLE); }
            }
            @Override public void onError(ApiError e) {
                if (!isAdded() || gen != generation) return;
                loadingPage = false;
                if (Nav.handleCommonError(requireActivity(), e)) return;
                if (offset == 0) showMessage(e.isNetwork() ? R.string.error_no_internet : R.string.error_generic, true);
            }
        });
    }

    private void showMessage(@StringRes int text, boolean retry) {
        list.setVisibility(View.GONE);
        loadingView.setVisibility(View.GONE);
        stateText.setText(text);
        stateButton.setVisibility(retry ? View.VISIBLE : View.GONE);
        stateGroup.setVisibility(View.VISIBLE);
    }
}
