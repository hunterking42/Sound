package mamun.sound.fire.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import mamun.sound.fire.R;
import mamun.sound.fire.adapter.AdminRow;
import mamun.sound.fire.adapter.RowAdapter;
import mamun.sound.fire.model.admin.AdminCategoriesData;
import mamun.sound.fire.model.admin.AdminCategory;
import mamun.sound.fire.network.Api;
import mamun.sound.fire.network.ApiClient;
import mamun.sound.fire.network.ApiError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminCategoriesFragment extends AdminListFragment<AdminRow> {

    private final Map<Long, AdminCategory> byId = new HashMap<>();
    private boolean resumedOnce;

    @Override protected int titleRes() { return R.string.admin_categories; }
    @Override protected int emptyTextRes() { return R.string.admin_no_categories; }

    @Override protected ListAdapter<AdminRow, ? extends RecyclerView.ViewHolder> createAdapter() {
        return new RowAdapter(row -> {
            AdminCategory c = byId.get(row.id);
            if (c != null) ((AdminActivity) requireActivity()).push(AdminCategoryFormFragment.newInstance(c));
        });
    }

    @Override protected View createHeader(LayoutInflater inflater, ViewGroup parent) {
        View h = inflater.inflate(R.layout.header_admin_add, parent, false);
        Button add = h.findViewById(R.id.btnAdd);
        add.setText(R.string.add_category);
        add.setOnClickListener(v -> ((AdminActivity) requireActivity()).push(AdminCategoryFormFragment.newInstance(null)));
        return h;
    }

    @Override protected void loadPage(int offset, PageCallback<AdminRow> cb) {
        Api.call(ApiClient.admin().categories(), new Api.Result<AdminCategoriesData>() {
            @Override public void onSuccess(AdminCategoriesData d) {
                if (!isAdded()) return;
                byId.clear();
                List<AdminRow> rows = new ArrayList<>();
                if (d.categories != null) for (AdminCategory c : d.categories) {
                    byId.put(c.id, c);
                    rows.add(new AdminRow(c.id, c.name, getString(R.string.row_order, c.sortOrder),
                            c.isActive == 0 ? getString(R.string.row_hidden) : null, R.color.danger, false, null));
                }
                cb.onPage(rows, false);            // one page
            }
            @Override public void onError(ApiError e) { cb.onError(e); }
        });
    }

    @Override public void onResume() {
        super.onResume();
        if (resumedOnce) reload();
        resumedOnce = true;
    }
}
