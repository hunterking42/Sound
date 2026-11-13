package mamun.sound.fire.ui.admin;

import mamun.sound.fire.ui.common.AppDialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import mamun.sound.fire.R;
import mamun.sound.fire.adapter.AdminRow;
import mamun.sound.fire.adapter.RowAdapter;
import mamun.sound.fire.model.admin.AdminNotice;
import mamun.sound.fire.model.admin.AdminNoticesData;
import mamun.sound.fire.model.admin.BroadcastResult;
import mamun.sound.fire.network.Api;
import mamun.sound.fire.network.ApiClient;
import mamun.sound.fire.network.ApiError;
import mamun.sound.fire.util.Nav;
import mamun.sound.fire.util.TimeUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Notices already sent. Tap one to read it in full or delete it; the button sends a new one. */
public class AdminBroadcastFragment extends AdminListFragment<AdminRow> {

    private final Map<Long, AdminNotice> byId = new HashMap<>();
    private boolean resumedOnce;

    @Override protected int titleRes() { return R.string.admin_broadcast; }
    @Override protected int emptyTextRes() { return R.string.admin_no_notices; }

    @Override protected ListAdapter<AdminRow, ? extends RecyclerView.ViewHolder> createAdapter() {
        return new RowAdapter(row -> {
            AdminNotice n = byId.get(row.id);
            if (n == null) return;
            new AppDialog.Spec(R.drawable.ic_send, n.title, n.message)
                    .primary(getString(R.string.btn_delete), true, t -> deleteNotice(n.id))
                    .show(requireActivity());
        });
    }

    @Override protected View createHeader(LayoutInflater inflater, ViewGroup parent) {
        View h = inflater.inflate(R.layout.header_admin_add, parent, false);
        Button add = h.findViewById(R.id.btnAdd);
        add.setText(R.string.add_notice);
        add.setOnClickListener(v -> ((AdminActivity) requireActivity()).push(new AdminBroadcastFormFragment()));
        return h;
    }

    @Override protected void loadPage(int offset, PageCallback<AdminRow> cb) {
        Api.call(ApiClient.admin().notices(), new Api.Result<AdminNoticesData>() {
            @Override public void onSuccess(AdminNoticesData d) {
                if (!isAdded()) return;
                byId.clear();
                List<AdminRow> rows = new ArrayList<>();
                if (d.notices != null) for (AdminNotice n : d.notices) {
                    byId.put(n.id, n);
                    rows.add(new AdminRow(n.id, n.title, TimeUtil.utcToLocal(n.createdAt),
                            n.showPopup != 0 ? getString(R.string.row_popup) : null, R.color.volt, false, null));
                }
                cb.onPage(rows, false);
            }
            @Override public void onError(ApiError e) { cb.onError(e); }
        });
    }

    private void deleteNotice(long id) {
        Map<String, Object> body = new HashMap<>();
        body.put("action", "delete");
        body.put("id", id);
        Api.call(ApiClient.admin().broadcastAction(body), new Api.Result<BroadcastResult>() {
            @Override public void onSuccess(BroadcastResult r) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), R.string.item_deleted, Toast.LENGTH_SHORT).show();
                reload();
            }
            @Override public void onError(ApiError e) {
                if (!isAdded() || Nav.handleCommonError(requireActivity(), e)) return;
                Toast.makeText(requireContext(), e.message != null ? e.message : getString(R.string.error_generic), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override public void onResume() {
        super.onResume();
        if (resumedOnce) reload();
        resumedOnce = true;
    }
}
