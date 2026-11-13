package mamun.sound.fire.data;

import android.content.Context;

import mamun.sound.fire.model.FavoriteIds;
import mamun.sound.fire.model.FavoriteResult;
import mamun.sound.fire.network.Api;
import mamun.sound.fire.network.ApiError;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The user's favorite ids, shared by every screen so a heart tapped on Home is instantly
 * filled on Category and appears on Favorites. Updates are optimistic: the heart changes
 * at once and is reverted if the server call fails. Main thread only.
 */
public final class FavoritesStore {

    public interface Listener { void onFavoritesChanged(Set<Long> ids); }

    private static FavoritesStore instance;
    public static synchronized FavoritesStore get(Context c) {
        if (instance == null) instance = new FavoritesStore(c.getApplicationContext());
        return instance;
    }

    private final Context app;
    private final Set<Long> ids = new HashSet<>();
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    private FavoritesStore(Context app) { this.app = app; }

    public Set<Long> ids() { return new HashSet<>(ids); }
    public boolean contains(long id) { return ids.contains(id); }
    public void addListener(Listener l) { listeners.add(l); }
    public void removeListener(Listener l) { listeners.remove(l); }

    public void refresh() {
        SoundRepository.get(app).favoriteIds(new SoundRepository.Callback<FavoriteIds>() {
            @Override public void onData(FavoriteIds d, boolean fromCache) {
                ids.clear();
                if (d != null && d.ids != null) ids.addAll(d.ids);
                notifyAllListeners();
            }
            @Override public void onError(ApiError e) { /* keep what we have */ }
        });
    }

    /** onFailure runs (on main) if the server call fails and the heart was reverted. */
    public void toggle(long id, Runnable onFailure) {
        final boolean add = !ids.contains(id);
        if (add) ids.add(id); else ids.remove(id);
        notifyAllListeners();
        SoundRepository.get(app).setFavorite(id, add, new Api.Result<FavoriteResult>() {
            @Override public void onSuccess(FavoriteResult d) {}
            @Override public void onError(ApiError e) {
                if (add) ids.remove(id); else ids.add(id);
                notifyAllListeners();
                if (onFailure != null) onFailure.run();
            }
        });
    }

    /** On logout. */
    public void clear() { ids.clear(); notifyAllListeners(); }

    private void notifyAllListeners() {
        Set<Long> copy = ids();
        for (Listener l : listeners) l.onFavoritesChanged(copy);
    }
}
