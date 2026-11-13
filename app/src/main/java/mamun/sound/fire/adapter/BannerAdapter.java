package mamun.sound.fire.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import mamun.sound.fire.R;
import mamun.sound.fire.model.Banner;

import java.util.List;

/** Slides for the Home banner pager. */
public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.VH> {

    public interface Listener { void onBannerClick(Banner banner); }

    private final List<Banner> items;
    private final Listener listener;

    public BannerAdapter(List<Banner> items, Listener l) { this.items = items; this.listener = l; }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH((ImageView) LayoutInflater.from(p.getContext()).inflate(R.layout.item_banner, p, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Banner b = items.get(pos);
        Glide.with(h.image).load(b.imageUrl).centerCrop().into(h.image);
        h.image.setOnClickListener(v -> listener.onBannerClick(b));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView image;
        VH(@NonNull ImageView v) { super(v); image = v; }
    }
}
