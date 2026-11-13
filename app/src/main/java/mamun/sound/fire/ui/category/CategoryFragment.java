package mamun.sound.fire.ui.category;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import mamun.sound.fire.R;
import mamun.sound.fire.data.SoundRepository;
import mamun.sound.fire.model.CategoriesData;
import mamun.sound.fire.model.Category;
import mamun.sound.fire.model.SoundsData;
import mamun.sound.fire.network.ApiError;
import mamun.sound.fire.ui.common.SoundGridFragment;
import mamun.sound.fire.util.ChipRow;
import mamun.sound.fire.util.Nav;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Category tabs on top, the shared sound grid below. The first category is selected by default. */
public class CategoryFragment extends SoundGridFragment {

    private final List<Category> categories = new ArrayList<>();
    private int selected = 0;
    private LinearLayout chipRow;

    @Override protected boolean autoLoad() { return false; }       // sounds load after the categories arrive

    @Override protected View createHeader(LayoutInflater inflater, ViewGroup parent) {
        View h = inflater.inflate(R.layout.header_chips, parent, false);
        chipRow = h.findViewById(R.id.chipRow);
        loadCategories();
        return h;
    }

    private void loadCategories() {
        showLoading();
        repo.categories(new SoundRepository.Callback<CategoriesData>() {
            @Override public void onData(CategoriesData d, boolean fromCache) {
                if (!isAdded()) return;
                categories.clear();
                if (d.categories != null) categories.addAll(d.categories);
                List<String> names = new ArrayList<>();
                for (Category c : categories) names.add(c.name);
                selected = Math.min(selected, Math.max(0, categories.size() - 1));
                ChipRow.bind(chipRow, names, selected, index -> { selected = index; reload(); });
                if (categories.isEmpty()) setItems(Collections.emptyList());
                else reload();
            }
            @Override public void onError(ApiError e) {
                if (!isAdded()) return;
                if (Nav.handleCommonError(requireActivity(), e)) return;
                showError(e);
            }
        });
    }

    @Override protected void onRetry() {
        if (categories.isEmpty()) loadCategories(); else reload();
    }

    @Override protected void loadPage(int offset, SoundRepository.Callback<SoundsData> cb) {
        long id = categories.get(selected).id;
        repo.sounds(offset == 0 ? "cat_" + id : null, id, null, 30, offset, cb);
    }

    @Override protected int emptyTextRes() { return R.string.state_empty_category; }
}
