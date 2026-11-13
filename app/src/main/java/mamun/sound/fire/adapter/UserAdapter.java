package mamun.sound.fire.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import mamun.sound.fire.R;
import mamun.sound.fire.model.admin.AdminUser;
import mamun.sound.fire.util.TimeUtil;

import java.util.Objects;

/** Admin user list rows. */
public class UserAdapter extends ListAdapter<AdminUser, UserAdapter.VH> {

    public interface Listener { void onUserClick(AdminUser user); }
    private final Listener listener;

    public UserAdapter(Listener l) { super(DIFF); listener = l; }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_admin_user, p, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        AdminUser u = getItem(pos);
        android.content.Context c = h.itemView.getContext();
        h.avatar.setText(u.username.substring(0, 1).toUpperCase());
        h.username.setText(u.username);
        String last = u.lastLogin == null ? c.getString(R.string.user_never) : TimeUtil.utcToLocal(u.lastLogin);
        h.subline.setText(c.getString(R.string.user_last_login, last));
        h.premium.setVisibility(u.isPremium() ? View.VISIBLE : View.GONE);
        h.admin.setVisibility(u.isAdmin() ? View.VISIBLE : View.GONE);

        // Only problems get a label; "active" is the normal case and stays quiet
        if ("banned".equals(u.status)) { h.status.setText(R.string.status_banned); h.status.setTextColor(c.getColor(R.color.danger)); h.status.setVisibility(View.VISIBLE); }
        else if ("locked".equals(u.status)) { h.status.setText(R.string.status_locked); h.status.setTextColor(c.getColor(R.color.gold)); h.status.setVisibility(View.VISIBLE); }
        else h.status.setVisibility(View.GONE);

        h.itemView.setOnClickListener(v -> listener.onUserClick(u));
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView avatar, username, subline, status; final View premium, admin;
        VH(@NonNull View v) {
            super(v);
            avatar = v.findViewById(R.id.avatar);
            username = v.findViewById(R.id.username);
            subline = v.findViewById(R.id.subline);
            status = v.findViewById(R.id.status);
            premium = v.findViewById(R.id.iconPremium);
            admin = v.findViewById(R.id.iconAdmin);
        }
    }

    private static final DiffUtil.ItemCallback<AdminUser> DIFF = new DiffUtil.ItemCallback<AdminUser>() {
        @Override public boolean areItemsTheSame(@NonNull AdminUser a, @NonNull AdminUser b) { return a.id == b.id; }
        @Override public boolean areContentsTheSame(@NonNull AdminUser a, @NonNull AdminUser b) {
            return Objects.equals(a.username, b.username) && Objects.equals(a.status, b.status)
                    && a.isPremium == b.isPremium && Objects.equals(a.role, b.role) && Objects.equals(a.lastLogin, b.lastLogin);
        }
    };
}
