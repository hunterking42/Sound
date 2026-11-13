package mamun.sound.fire.ui.admin;

import mamun.sound.fire.ui.common.AppDialog;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import mamun.sound.fire.R;
import mamun.sound.fire.model.User;
import mamun.sound.fire.model.admin.AdminLog;
import mamun.sound.fire.model.admin.AdminUser;
import mamun.sound.fire.model.admin.AdminUserDetail;
import mamun.sound.fire.model.admin.UserActionRequest;
import mamun.sound.fire.model.admin.UserActionResult;
import mamun.sound.fire.network.Api;
import mamun.sound.fire.network.ApiClient;
import mamun.sound.fire.network.ApiError;
import mamun.sound.fire.util.Nav;
import mamun.sound.fire.util.Session;
import mamun.sound.fire.util.TimeUtil;

import java.util.List;

/**
 * One user: details, the actions that make sense for their state, and the recent admin activity on them.
 * Every action asks first. The server applies the same rules again (no harmful actions on admins or yourself).
 */
public class AdminUserDetailFragment extends Fragment {

    private static final String ARG_ID = "id";

    public static AdminUserDetailFragment newInstance(long id) {
        AdminUserDetailFragment f = new AdminUserDetailFragment();
        Bundle a = new Bundle();
        a.putLong(ARG_ID, id);
        f.setArguments(a);
        return f;
    }

    private long userId;
    private AdminUser user;
    private View scroll, loading, stateGroup;
    private TextView stateText, avatar, username;
    private LinearLayout cardInfo, cardActions, cardLogs;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = i.inflate(R.layout.fragment_admin_user_detail, c, false);
        userId = requireArguments().getLong(ARG_ID);
        scroll = v.findViewById(R.id.scroll);
        loading = v.findViewById(R.id.loading);
        stateGroup = v.findViewById(R.id.stateGroup);
        stateText = v.findViewById(R.id.stateText);
        avatar = v.findViewById(R.id.avatar);
        username = v.findViewById(R.id.username);
        cardInfo = v.findViewById(R.id.cardInfo);
        cardActions = v.findViewById(R.id.cardActions);
        cardLogs = v.findViewById(R.id.cardLogs);
        v.findViewById(R.id.stateButton).setOnClickListener(x -> load());
        load();
        return v;
    }

    @Override public void onResume() {
        super.onResume();
        ((AdminActivity) requireActivity()).setScreenTitle(user != null ? user.username : getString(R.string.admin_users));
    }

    // ---------- loading ----------
    private void load() {
        scroll.setVisibility(View.GONE);
        stateGroup.setVisibility(View.GONE);
        loading.setVisibility(View.VISIBLE);
        Api.call(ApiClient.admin().userDetail(userId), new Api.Result<AdminUserDetail>() {
            @Override public void onSuccess(AdminUserDetail d) {
                if (!isAdded()) return;
                user = d.user;
                ((AdminActivity) requireActivity()).setScreenTitle(user.username);
                render(d.logs);
            }
            @Override public void onError(ApiError e) {
                if (!isAdded() || Nav.handleCommonError(requireActivity(), e)) return;
                loading.setVisibility(View.GONE);
                stateText.setText(e.isNetwork() ? R.string.error_no_internet : R.string.error_generic);
                stateGroup.setVisibility(View.VISIBLE);
            }
        });
    }

    private void render(List<AdminLog> logs) {
        loading.setVisibility(View.GONE);
        scroll.setVisibility(View.VISIBLE);
        avatar.setText(user.username.substring(0, 1).toUpperCase());
        username.setText(user.username);

        // ---- info ----
        cardInfo.removeAllViews();
        addInfo(R.string.info_role, getString(user.isAdmin() ? R.string.badge_admin : R.string.role_user));
        addInfo(R.string.info_status, statusText(user.status));
        if (user.banReason != null && !user.banReason.isEmpty()) addInfo(R.string.info_ban_reason, user.banReason);
        if ("locked".equals(user.status) && user.lockUntil != null) addInfo(R.string.info_lock_until, TimeUtil.utcToLocal(user.lockUntil));
        addInfo(R.string.info_premium, !user.isPremium() ? getString(R.string.info_none)
                : (user.premiumExpiry == null ? getString(R.string.info_lifetime) : TimeUtil.utcToLocal(user.premiumExpiry)));
        addInfo(R.string.info_device, user.deviceId == null ? getString(R.string.info_device_unset) : user.deviceId);
        addInfo(R.string.info_joined, TimeUtil.utcToLocal(user.createdAt));
        addInfo(R.string.info_last_login, user.lastLogin == null ? getString(R.string.user_never) : TimeUtil.utcToLocal(user.lastLogin));

        // ---- actions ----
        cardActions.removeAllViews();
        User me = Session.get();
        boolean protectedUser = user.isAdmin() || (me != null && me.id == user.id);   // the server refuses these too
        if (!protectedUser) {
            if ("banned".equals(user.status)) addAction(R.drawable.ic_block, R.string.act_unban, false, this::askUnban);
            else addAction(R.drawable.ic_block, R.string.act_ban, true, this::askBan);
            if ("locked".equals(user.status)) addAction(R.drawable.ic_lock, R.string.act_unlock, false, this::askUnlock);
            else if (!"banned".equals(user.status)) addAction(R.drawable.ic_lock, R.string.act_lock, false, this::askLock);
        }
        addAction(R.drawable.ic_crown, user.isPremium() ? R.string.act_extend_premium : R.string.act_give_premium, false, this::askGivePremium);
        if (user.isPremium()) addAction(R.drawable.ic_crown, R.string.act_remove_premium, true, this::askRemovePremium);
        addAction(R.drawable.ic_phone, R.string.act_reset_device, false, this::askResetDevice);
        if (!protectedUser) {
            addAction(R.drawable.ic_key, R.string.act_reset_password, false, this::askResetPassword);
            addAction(R.drawable.ic_delete, R.string.act_delete, true, this::askDelete);
        }

        // ---- recent activity ----
        cardLogs.removeAllViews();
        if (logs == null || logs.isEmpty()) {
            addLogRow(null, getString(R.string.admin_no_logs));
        } else {
            for (AdminLog l : logs) {
                String what = AdminLogsFragment.pretty(l.action) + (l.details != null && !l.details.isEmpty() ? ": " + l.details : "");
                addLogRow(TimeUtil.utcToLocal(l.createdAt) + (l.admin != null ? "  •  " + l.admin : ""), what);
            }
        }
    }

    // ---------- the dialogs ----------
    private void askBan() {
        new AppDialog.Spec(R.drawable.ic_block, getString(R.string.act_ban), getString(R.string.dlg_ban_msg))
                .input(getString(R.string.dlg_reason_hint), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, true)
                .primary(getString(R.string.btn_ban), true, reason -> run(UserActionRequest.ban(userId, reason)))
                .show(requireActivity());
    }

    private void askUnban() {
        confirm(R.drawable.ic_block, R.string.act_unban, getString(R.string.dlg_unban_msg, user.username), R.string.btn_confirm, false,
                () -> run(UserActionRequest.unban(userId)));
    }

    private void askLock() {
        new AppDialog.Spec(R.drawable.ic_lock, getString(R.string.act_lock), getString(R.string.dlg_lock_msg, user.username))
                .input(getString(R.string.dlg_minutes_hint), InputType.TYPE_CLASS_NUMBER, false)
                .primary(getString(R.string.btn_lock), false, text -> {
                    Integer minutes = null;
                    if (!text.isEmpty()) { minutes = parseInt(text); if (minutes == null) return; }
                    run(UserActionRequest.lock(userId, minutes));
                }).show(requireActivity());
    }

    private void askUnlock() {
        confirm(R.drawable.ic_lock, R.string.act_unlock, getString(R.string.dlg_unlock_msg, user.username), R.string.btn_confirm, false,
                () -> run(UserActionRequest.unlock(userId)));
    }

    private void askGivePremium() {
        new AppDialog.Spec(R.drawable.ic_crown, getString(user.isPremium() ? R.string.act_extend_premium : R.string.act_give_premium),
                getString(R.string.dlg_premium_msg, user.username))
                .input(getString(R.string.dlg_days_hint), InputType.TYPE_CLASS_NUMBER, true)
                .primary(getString(R.string.btn_give), false, text -> {
                    Integer days = parseInt(text);
                    if (days != null) run(UserActionRequest.givePremium(userId, days, false));
                })
                .neutral(getString(R.string.dlg_lifetime), () -> run(UserActionRequest.givePremium(userId, null, true)))
                .show(requireActivity());
    }

    private void askRemovePremium() {
        confirm(R.drawable.ic_crown, R.string.act_remove_premium, getString(R.string.dlg_remove_premium_msg, user.username), R.string.btn_confirm, true,
                () -> run(UserActionRequest.removePremium(userId)));
    }

    private void askResetDevice() {
        confirm(R.drawable.ic_phone, R.string.act_reset_device, getString(R.string.dlg_reset_device_msg, user.username), R.string.btn_confirm, false,
                () -> run(UserActionRequest.resetDevice(userId)));
    }

    private void askResetPassword() {
        confirm(R.drawable.ic_key, R.string.act_reset_password, getString(R.string.dlg_reset_password_msg, user.username), R.string.btn_confirm, false,
                () -> run(UserActionRequest.resetPassword(userId)));
    }

    private void askDelete() {
        confirm(R.drawable.ic_delete, R.string.act_delete, getString(R.string.dlg_delete_msg, user.username), R.string.btn_delete, true,
                () -> run(UserActionRequest.delete(userId)));
    }

    private void confirm(@DrawableRes int icon, @StringRes int title, String message, @StringRes int button, boolean danger, Runnable onYes) {
        new AppDialog.Spec(icon, getString(title), message)
                .primary(getString(button), danger, text -> onYes.run())
                .show(requireActivity());
    }

    private Integer parseInt(String text) {
        try { return Integer.parseInt(text.trim()); }
        catch (NumberFormatException e) { Toast.makeText(requireContext(), R.string.err_number, Toast.LENGTH_SHORT).show(); return null; }
    }

    // ---------- run an action ----------
    private void run(UserActionRequest req) {
        scroll.setVisibility(View.INVISIBLE);
        loading.setVisibility(View.VISIBLE);
        Api.call(ApiClient.admin().userAction(req), new Api.Result<UserActionResult>() {
            @Override public void onSuccess(UserActionResult r) {
                if (!isAdded()) return;
                if (r.deleted != null) {
                    Toast.makeText(requireContext(), R.string.admin_user_deleted, Toast.LENGTH_SHORT).show();
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                    return;
                }
                if (r.temporaryPassword != null) showTemporaryPassword(r.temporaryPassword);
                else Toast.makeText(requireContext(), R.string.admin_done, Toast.LENGTH_SHORT).show();
                load();                               // fresh details and a new log line
            }
            @Override public void onError(ApiError e) {
                if (!isAdded() || Nav.handleCommonError(requireActivity(), e)) return;
                loading.setVisibility(View.GONE);
                scroll.setVisibility(View.VISIBLE);
                Toast.makeText(requireContext(), e.isNetwork() ? getString(R.string.error_no_internet)
                        : (e.message != null ? e.message : getString(R.string.error_generic)), Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Shown once. Never written to a log or to storage by the app. */
    private void showTemporaryPassword(String temp) {
        new AppDialog.Spec(R.drawable.ic_key, getString(R.string.dlg_temp_password_title), getString(R.string.dlg_temp_password_msg, temp))
                .primary(getString(R.string.btn_done), false, null)
                .neutral(getString(R.string.btn_copy), () -> {
                    ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("password", temp);
                    if (Build.VERSION.SDK_INT >= 33) {                    // ask Android to hide the copied text in its preview
                        PersistableBundle extras = new PersistableBundle();
                        extras.putBoolean("android.content.extra.IS_SENSITIVE", true);
                        clip.getDescription().setExtras(extras);
                    }
                    cm.setPrimaryClip(clip);
                    Toast.makeText(requireContext(), R.string.copied, Toast.LENGTH_SHORT).show();
                })
                .show(requireActivity());
    }

    // ---------- row builders ----------
    private String statusText(String status) {
        if ("banned".equals(status)) return getString(R.string.status_banned);
        if ("locked".equals(status)) return getString(R.string.status_locked);
        return getString(R.string.status_active);
    }

    private void addInfo(@StringRes int label, String value) {
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_info_row, cardInfo, false);
        ((TextView) row.findViewById(R.id.infoLabel)).setText(label);
        ((TextView) row.findViewById(R.id.infoValue)).setText(value);
        cardInfo.addView(row);
    }

    private void addLogRow(@Nullable String label, String value) {
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_info_row, cardLogs, false);
        TextView l = row.findViewById(R.id.infoLabel);
        if (label == null) l.setVisibility(View.GONE); else l.setText(label);
        ((TextView) row.findViewById(R.id.infoValue)).setText(value);
        cardLogs.addView(row);
    }

    private void addAction(@DrawableRes int icon, @StringRes int title, boolean danger, Runnable onClick) {
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_setting_row, cardActions, false);
        ImageView iv = row.findViewById(R.id.rowIcon);
        TextView tv = row.findViewById(R.id.rowTitle);
        iv.setImageResource(icon);
        tv.setText(title);
        if (danger) {
            iv.setColorFilter(requireContext().getColor(R.color.danger));
            tv.setTextColor(requireContext().getColor(R.color.danger));
        }
        row.setOnClickListener(x -> onClick.run());
        cardActions.addView(row);
    }
}
