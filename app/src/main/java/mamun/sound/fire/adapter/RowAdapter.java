package mamun.sound.fire.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import mamun.sound.fire.R;

/** Shared adapter for admin lists made of AdminRow. */
public class RowAdapter extends ListAdapter<AdminRow, RowAdapter.VH> {

    public interface Listener { void onRowClick(AdminRow row); }
    private final Listener listener;

    public RowAdapter(Listener l) {
        super(new DiffUtil.ItemCallback<AdminRow>() {
            @Override public boolean areItemsTheSame(@NonNull AdminRow a, @NonNull AdminRow b) { return a.id == b.id; }
            @Override public boolean areContentsTheSame(@NonNull AdminRow a, @NonNull AdminRow b) { return a.signature().equals(b.signature()); }
        });
        listener = l;
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_admin_row, p, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        AdminRow r = getItem(pos);
        h.title.setText(r.title);
        h.subtitle.setText(r.subtitle);
        if (r.badge == null) h.badge.setVisibility(View.GONE);
        else {
            h.badge.setText(r.badge);
            h.badge.setTextColor(h.itemView.getContext().getColor(r.badgeColor));
            h.badge.setVisibility(View.VISIBLE);
        }
        h.thumb.setVisibility(r.showThumb ? View.VISIBLE : View.GONE);
        if (r.showThumb) {
            Glide.with(h.thumb).load(r.imageUrl).placeholder(R.drawable.img_sound_placeholder)
                    .error(R.drawable.img_sound_placeholder).centerCrop().into(h.thumb);
        }
        h.itemView.setOnClickListener(v -> listener.onRowClick(r));
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView title, subtitle, badge; final ImageView thumb;
        VH(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.rowTitle);
            subtitle = v.findViewById(R.id.rowSubtitle);
            badge = v.findViewById(R.id.rowBadge);
            thumb = v.findViewById(R.id.thumb);
        }
    }
}
