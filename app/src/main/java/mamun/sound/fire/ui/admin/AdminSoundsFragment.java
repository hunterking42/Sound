package mamun.sound.fire.ui.admin;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import mamun.sound.fire.R;
import mamun.sound.fire.adapter.AdminRow;
import mamun.sound.fire.adapter.RowAdapter;
import mamun.sound.fire.model.admin.AdminSound;
import mamun.sound.fire.model.admin.AdminSoundsData;
import mamun.sound.fire.network.Api;
import mamun.sound.fire.network.ApiClient;
import mamun.sound.fire.network.ApiError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** All sounds, including hidden ones. Tap one to edit; the button adds a new one. */
public class AdminSoundsFragment extends AdminListFragment<AdminRow> {

    private static final long DEBOUNCE_MS = 400;
    private final Map<Long, AdminSound> byId = new HashMap<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable doReload = this::reload;
    private String query = "";
    private boolean resumedOnce;

    @Override protected int titleRes() { return R.string.admin_sounds; }
    @Override protected int emptyTextRes() { return R.string.admin_no_sounds; }

    @Override protected ListAdapter<AdminRow, ? extends RecyclerView.ViewHolder> createAdapter() {
        return new RowAdapter(row -> {
            AdminSound s = byId.get(row.id);
            if (s != null) ((AdminActivity) requireActivity()).push(AdminSoundFormFragment.newInstance(s));
        });
    }

    @Override protected View createHeader(LayoutInflater inflater, ViewGroup parent) {
        View h = inflater.inflate(R.layout.header_admin_add, parent, false);
        h.findViewById(R.id.searchBox).setVisibility(View.VISIBLE);
        Button add = h.findViewById(R.id.btnAdd);
        add.setText(R.string.add_sound);
        add.setOnClickListener(v -> ((AdminActivity) requireActivity()).push(AdminSoundFormFragment.newInstance(null)));
        ((EditText) h.findViewById(R.id.inputSearch)).addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                query = s.toString().trim();
                handler.removeCallbacks(doReload);
                handler.postDelayed(doReload, DEBOUNCE_MS);
            }
        });
        return h;
    }

    @Override protected void loadPage(int offset, PageCallback<AdminRow> cb) {
        Api.call(ApiClient.admin().sounds(query.isEmpty() ? null : query, null, 50, offset), new Api.Result<AdminSoundsData>() {
            @Override public void onSuccess(AdminSoundsData d) {
                if (!isAdded()) return;
                if (offset == 0) byId.clear();
                List<AdminRow> rows = new ArrayList<>();
                if (d.sounds != null) for (AdminSound s : d.sounds) {
                    byId.put(s.id, s);
                    String kind = getString(s.isPremium != 0 ? R.string.row_premium : R.string.row_free);
                    rows.add(new AdminRow(s.id, s.name, kind + "  •  " + getString(R.string.row_plays, s.playCount),
                            s.isActive == 0 ? getString(R.string.row_hidden) : null, R.color.danger, true, s.imageUrl));
                }
                cb.onPage(rows, d.hasMore);
            }
            @Override public void onError(ApiError e) { cb.onError(e); }
        });
    }

    /** Back from the form: the list may have changed. */
    @Override public void onResume() {
        super.onResume();
        if (resumedOnce) reload();
        resumedOnce = true;
    }

    @Override public void onDestroyView() { handler.removeCallbacks(doReload); super.onDestroyView(); }
}
