package mamun.sound.fire.ui.admin;

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

import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import mamun.sound.fire.R;
import mamun.sound.fire.adapter.UserAdapter;
import mamun.sound.fire.model.admin.AdminUser;
import mamun.sound.fire.model.admin.AdminUsersData;
import mamun.sound.fire.network.Api;
import mamun.sound.fire.network.ApiClient;
import mamun.sound.fire.network.ApiError;
import mamun.sound.fire.util.ChipRow;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Search, filter (active / banned / locked / premium / admins) and open a user. */
public class AdminUsersFragment extends AdminListFragment<AdminUser> {

    private static final String[] FILTERS = {null, "active", "banned", "locked", "premium", "admin"};
    private static final long DEBOUNCE_MS = 400;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable doReload = this::reload;
    private String query = "";
    private String filter = null;

    @Override protected int titleRes() { return R.string.admin_users; }
    @Override protected int emptyTextRes() { return R.string.admin_no_users; }

    @Override protected ListAdapter<AdminUser, ? extends RecyclerView.ViewHolder> createAdapter() {
        return new UserAdapter(u -> ((AdminActivity) requireActivity()).push(AdminUserDetailFragment.newInstance(u.id)));
    }

    @Override protected View createHeader(LayoutInflater inflater, ViewGroup parent) {
        View h = inflater.inflate(R.layout.header_admin_users, parent, false);
        EditText search = h.findViewById(R.id.inputSearch);
        View clear = h.findViewById(R.id.btnClear);

        List<String> labels = Arrays.asList(getString(R.string.filter_all), getString(R.string.filter_active),
                getString(R.string.filter_banned), getString(R.string.filter_locked),
                getString(R.string.filter_premium), getString(R.string.filter_admin));
        ChipRow.bind((LinearLayout) h.findViewById(R.id.filterRow), labels, 0, i -> { filter = FILTERS[i]; reload(); });

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                query = s.toString().trim();
                clear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                handler.removeCallbacks(doReload);
                handler.postDelayed(doReload, DEBOUNCE_MS);          // wait until typing pauses
            }
        });
        search.setOnEditorActionListener((v, actionId, e) -> {
            if (actionId != EditorInfo.IME_ACTION_SEARCH) return false;
            ((InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(search.getWindowToken(), 0);
            handler.removeCallbacks(doReload);
            reload();
            return true;
        });
        clear.setOnClickListener(v -> search.setText(""));
        return h;
    }

    @Override protected void loadPage(int offset, PageCallback<AdminUser> cb) {
        Api.call(ApiClient.admin().users(query.isEmpty() ? null : query, filter, 50, offset), new Api.Result<AdminUsersData>() {
            @Override public void onSuccess(AdminUsersData d) { cb.onPage(d.users != null ? d.users : Collections.<AdminUser>emptyList(), d.hasMore); }
            @Override public void onError(ApiError e) { cb.onError(e); }
        });
    }

    /** Coming back from a user's detail: the status may have changed. */
    @Override public void onResume() {
        super.onResume();
        if (resumedOnce) reload();
        resumedOnce = true;
    }
    private boolean resumedOnce;

    @Override public void onDestroyView() {
        handler.removeCallbacks(doReload);
        super.onDestroyView();
    }
}
