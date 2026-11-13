package mamun.sound.fire.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import mamun.sound.fire.R;
import mamun.sound.fire.model.Sound;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Shared adapter for every sound grid (Home, Category, Favorites, Search).
 * It only DISPLAYS state (playing, loading, favorites, premium). The screen owns the logic
 * and tells the adapter what changed through the setters below.
 */
public class SoundAdapter extends ListAdapter<Sound, SoundAdapter.VH> {

    public interface Listener {
        void onPlayClick(Sound sound);                    // tap: play / stop / open Premium screen
        void onFavoriteClick(Sound sound);                // heart: screen calls the API, then setFavoriteIds()
        void onMenuClick(Sound sound, View anchor);       // long press: Favorite, Timer, Share, Add to Combo
    }

    private static final Object PAYLOAD_STATE = new Object();   // "only state changed" marker

    private final Listener listener;
    private boolean userPremium = false;
    private Set<Long> favoriteIds = new HashSet<>();
    private long playingId = -1;
    private long loadingId = -1;

    public SoundAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
        setHasStableIds(false);
    }

    // ---- state setters: each one refreshes only the visual state, not images ----
    public void setUserPremium(boolean premium) { userPremium = premium; refreshState(); }
    public void setFavoriteIds(Set<Long> ids) { favoriteIds = new HashSet<>(ids); refreshState(); }
    public void setPlayingId(long id) { playingId = id; refreshState(); }     // -1 = nothing playing
    public void setLoadingId(long id) { loadingId = id; refreshState(); }     // -1 = nothing downloading

    private void refreshState() {
        if (getItemCount() > 0) notifyItemRangeChanged(0, getItemCount(), PAYLOAD_STATE);
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sound, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Sound s = getItem(position);
        h.name.setText(s.name);
        // Glide caches; placeholder doubles as the fallback if the URL is missing or fails
        Glide.with(h.image)
                .load(s.imageUrl)
                .placeholder(R.drawable.img_sound_placeholder)
                .error(R.drawable.img_sound_placeholder)
                .centerCrop()
                .into(h.image);
        h.image.setContentDescription(h.itemView.getContext().getString(R.string.cd_sound_image, s.name));
        bindState(h, s);

        h.root.setOnClickListener(v -> listener.onPlayClick(s));
        h.root.setOnLongClickListener(v -> { listener.onMenuClick(s, v); return true; });
        h.favorite.setOnClickListener(v -> {
            // heart pop: small scale up then back (feedback for the user's own tap)
            v.animate().scaleX(1.3f).scaleY(1.3f).setDuration(90)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(90).start()).start();
            listener.onFavoriteClick(s);
        });
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position, @NonNull List<Object> payloads) {
        if (payloads.contains(PAYLOAD_STATE)) bindState(h, getItem(position));   // skip image reload
        else onBindViewHolder(h, position);
    }

    /** Everything that changes without the sound itself changing. */
    private void bindState(VH h, Sound s) {
        boolean playing = s.id == playingId;
        boolean loading = s.id == loadingId;
        boolean fav = favoriteIds.contains(s.id);

        h.root.setActivated(playing);                                          // volt glow ring
        h.loading.setVisibility(loading ? View.VISIBLE : View.GONE);
        h.lock.setVisibility(s.isPremium() && !userPremium ? View.VISIBLE : View.GONE);

        h.favorite.setImageResource(fav ? R.drawable.ic_heart : R.drawable.ic_heart_outline);
        h.favorite.setColorFilter(h.itemView.getContext().getColor(fav ? R.color.spark : R.color.on_fuse));
        h.favorite.setContentDescription(h.itemView.getContext().getString(
                fav ? R.string.cd_remove_favorite : R.string.cd_add_favorite, s.name));

        // One badge at most: HOT wins over NEW
        if (s.isHot()) {
            h.badge.setText(R.string.badge_hot);
            h.badge.setBackgroundResource(R.drawable.bg_badge_hot);
            h.badge.setVisibility(View.VISIBLE);
        } else if (s.isNew()) {
            h.badge.setText(R.string.badge_new);
            h.badge.setBackgroundResource(R.drawable.bg_badge_new);
            h.badge.setVisibility(View.VISIBLE);
        } else {
            h.badge.setVisibility(View.GONE);
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        final View root; final ImageView image, lock, favorite;
        final TextView name, badge; final ProgressBar loading;
        VH(@NonNull View v) {
            super(v);
            root = v.findViewById(R.id.soundRoot);
            image = v.findViewById(R.id.soundImage);
            lock = v.findViewById(R.id.iconLock);
            favorite = v.findViewById(R.id.btnFavorite);
            badge = v.findViewById(R.id.badge);
            loading = v.findViewById(R.id.loading);
            name = v.findViewById(R.id.soundName);
        }
    }

    private static final DiffUtil.ItemCallback<Sound> DIFF = new DiffUtil.ItemCallback<Sound>() {
        @Override public boolean areItemsTheSame(@NonNull Sound a, @NonNull Sound b) { return a.id == b.id; }
        @Override public boolean areContentsTheSame(@NonNull Sound a, @NonNull Sound b) {
            return Objects.equals(a.name, b.name) && Objects.equals(a.imageUrl, b.imageUrl)
                    && Objects.equals(a.audioUrl, b.audioUrl) && a.isPremium == b.isPremium
                    && a.playCount == b.playCount;
        }
    };
}
