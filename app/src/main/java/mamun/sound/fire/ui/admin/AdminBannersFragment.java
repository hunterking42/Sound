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
import mamun.sound.fire.model.admin.AdminBanner;
import mamun.sound.fire.model.admin.AdminBannersData;
import mamun.sound.fire.network.Api;
import mamun.sound.fire.network.ApiClient;
import mamun.sound.fire.network.ApiError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminBannersFragment extends AdminListFragment<AdminRow> {

    private final Map<Long, AdminBanner> byId = new HashMap<>();
    private boolean resumedOnce;

    @Override protected int titleRes() { return R.string.admin_banners; }
    @Override protected int emptyTextRes() { return R.string.admin_no_banners; }

    @Override protected ListAdapter<AdminRow, ? extends RecyclerView.ViewHolder> createAdapter() {
        return new RowAdapter(row -> {
            AdminBanner b = byId.get(row.id);
            if (b != null) ((AdminActivity) requireActivity()).push(AdminBannerFormFragment.newInstance(b));
        });
    }

    @Override protected View createHeader(LayoutInflater inflater, ViewGroup parent) {
        View h = inflater.inflate(R.layout.header_admin_add, parent, false);
        Button add = h.findViewById(R.id.btnAdd);
        add.setText(R.string.add_banner);
        add.setOnClickListener(v -> ((AdminActivity) requireActivity()).push(AdminBannerFormFragment.newInstance(null)));
        return h;
    }

    @Override protected void loadPage(int offset, PageCallback<AdminRow> cb) {
        Api.call(ApiClient.admin().banners(), new Api.Result<AdminBannersData>() {
            @Override public void onSuccess(AdminBannersData d) {
                if (!isAdded()) return;
                byId.clear();
                List<AdminRow> rows = new ArrayList<>();
                if (d.banners != null) for (AdminBanner b : d.banners) {
                    byId.put(b.id, b);
                    rows.add(new AdminRow(b.id, getString(R.string.row_banner, b.id),
                            b.link == null || b.link.isEmpty() ? b.imageUrl : b.link,
                            b.isActive == 0 ? getString(R.string.row_hidden) : null, R.color.danger, true, b.imageUrl));
                }
                cb.onPage(rows, false);
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
