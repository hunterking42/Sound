package mamun.sound.fire.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import mamun.sound.fire.R;
import mamun.sound.fire.model.admin.AdminStats;
import mamun.sound.fire.network.Api;
import mamun.sound.fire.network.ApiClient;
import mamun.sound.fire.network.ApiError;
import mamun.sound.fire.util.Nav;

import java.text.NumberFormat;

/** Dashboard numbers, top sounds, and the list of admin tools. */
public class AdminHomeFragment extends Fragment {

    private LinearLayout cardStats, cardTop, cardTools;
    private TextView statsError;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = i.inflate(R.layout.fragment_admin_home, c, false);
        cardStats = v.findViewById(R.id.cardStats);
        cardTop = v.findViewById(R.id.cardTop);
        cardTools = v.findViewById(R.id.cardTools);
        statsError = v.findViewById(R.id.statsError);

        addTool(R.drawable.ic_group, R.string.admin_users, () -> admin().push(new AdminUsersFragment()));
        addTool(R.drawable.ic_wave, R.string.admin_sounds, () -> admin().push(new AdminSoundsFragment()));
        addTool(R.drawable.ic_grid, R.string.admin_categories, () -> admin().push(new AdminCategoriesFragment()));
        addTool(R.drawable.ic_overlay, R.string.admin_banners, () -> admin().push(new AdminBannersFragment()));
        addTool(R.drawable.ic_send, R.string.admin_broadcast, () -> admin().push(new AdminBroadcastFragment()));
        addTool(R.drawable.ic_settings, R.string.admin_settings, () -> admin().push(new AdminSettingsFormFragment()));
        addTool(R.drawable.ic_update, R.string.admin_update, () -> admin().push(new AdminUpdateFormFragment()));
        addTool(R.drawable.ic_history, R.string.admin_logs, () -> admin().push(new AdminLogsFragment()));
        return v;
    }

    @Override public void onResume() {
        super.onResume();
        admin().setScreenTitle(R.string.admin_title);
        loadStats();
    }

    private AdminActivity admin() { return (AdminActivity) requireActivity(); }

    private void loadStats() {
        statsError.setVisibility(View.GONE);
        Api.call(ApiClient.admin().stats(), new Api.Result<AdminStats>() {
            @Override public void onSuccess(AdminStats s) { if (isAdded()) showStats(s); }
            @Override public void onError(ApiError e) {
                if (!isAdded() || Nav.handleCommonError(requireActivity(), e)) return;
                statsError.setText(e.isNetwork() ? R.string.error_no_internet : R.string.error_generic);
                statsError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showStats(AdminStats s) {
        NumberFormat nf = NumberFormat.getIntegerInstance();
        cardStats.removeAllViews();
        addStat(cardStats, getString(R.string.stat_users), nf.format(s.usersTotal));
        addStat(cardStats, getString(R.string.stat_premium), nf.format(s.premium));
        addStat(cardStats, getString(R.string.stat_banned), nf.format(s.banned));
        addStat(cardStats, getString(R.string.stat_locked), nf.format(s.locked));
        addStat(cardStats, getString(R.string.stat_active_today), nf.format(s.activeToday));
        addStat(cardStats, getString(R.string.stat_new_week), nf.format(s.newWeek));
        addStat(cardStats, getString(R.string.stat_sounds), nf.format(s.soundsActive) + " / " + nf.format(s.soundsTotal));
        addStat(cardStats, getString(R.string.stat_plays), nf.format(s.playsTotal));

        cardTop.removeAllViews();
        if (s.topSounds != null) {
            int n = Math.min(5, s.topSounds.size());
            for (int k = 0; k < n; k++) addStat(cardTop, s.topSounds.get(k).name, nf.format(s.topSounds.get(k).playCount));
        }
    }

    private void addStat(LinearLayout parent, String label, String value) {
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_stat_row, parent, false);
        ((TextView) row.findViewById(R.id.statLabel)).setText(label);
        ((TextView) row.findViewById(R.id.statValue)).setText(value);
        parent.addView(row);
    }

    private void addTool(@DrawableRes int icon, @StringRes int title, Runnable onClick) {
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_setting_row, cardTools, false);
        ((ImageView) row.findViewById(R.id.rowIcon)).setImageResource(icon);
        ((TextView) row.findViewById(R.id.rowTitle)).setText(title);
        row.setOnClickListener(x -> onClick.run());
        cardTools.addView(row);
    }
}
