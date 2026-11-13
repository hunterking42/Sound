package mamun.sound.fire.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import mamun.sound.fire.R;
import mamun.sound.fire.model.admin.AdminLog;
import mamun.sound.fire.model.admin.AdminLogsData;
import mamun.sound.fire.network.Api;
import mamun.sound.fire.network.ApiClient;
import mamun.sound.fire.network.ApiError;
import mamun.sound.fire.util.TimeUtil;

import java.util.Collections;

/** Every admin action: who did what to whom, and when. */
public class AdminLogsFragment extends AdminListFragment<AdminLog> {

    @Override protected int titleRes() { return R.string.admin_logs; }
    @Override protected int emptyTextRes() { return R.string.admin_no_logs; }

    @Override protected ListAdapter<AdminLog, ? extends RecyclerView.ViewHolder> createAdapter() { return new LogAdapter(); }

    @Override protected void loadPage(int offset, PageCallback<AdminLog> cb) {
        Api.call(ApiClient.admin().logs(50, offset), new Api.Result<AdminLogsData>() {
            @Override public void onSuccess(AdminLogsData d) { cb.onPage(d.logs != null ? d.logs : Collections.<AdminLog>emptyList(), d.hasMore); }
            @Override public void onError(ApiError e) { cb.onError(e); }
        });
    }

    /** "premium_give" becomes "Premium give". */
    static String pretty(String action) {
        if (action == null || action.isEmpty()) return "";
        String s = action.replace('_', ' ');
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    static final class LogAdapter extends ListAdapter<AdminLog, LogAdapter.VH> {
        LogAdapter() {
            super(new DiffUtil.ItemCallback<AdminLog>() {
                @Override public boolean areItemsTheSame(@NonNull AdminLog a, @NonNull AdminLog b) { return a.id == b.id; }
                @Override public boolean areContentsTheSame(@NonNull AdminLog a, @NonNull AdminLog b) { return a.id == b.id; }
            });
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_admin_log, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            AdminLog l = getItem(pos);
            String who = l.admin != null ? l.admin : "?";
            h.title.setText(l.target != null ? pretty(l.action) + ": " + who + " → " + l.target : pretty(l.action) + ": " + who);
            h.details.setText(l.details == null ? "" : l.details);
            h.details.setVisibility(l.details == null || l.details.isEmpty() ? View.GONE : View.VISIBLE);
            h.time.setText(TimeUtil.utcToLocal(l.createdAt));
        }
        static final class VH extends RecyclerView.ViewHolder {
            final TextView title, details, time;
            VH(@NonNull View v) {
                super(v);
                title = v.findViewById(R.id.logTitle);
                details = v.findViewById(R.id.logDetails);
                time = v.findViewById(R.id.logTime);
            }
        }
    }
}
