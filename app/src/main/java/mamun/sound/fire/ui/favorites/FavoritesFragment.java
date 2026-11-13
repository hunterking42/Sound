package mamun.sound.fire.ui.favorites;

import mamun.sound.fire.R;
import mamun.sound.fire.data.SoundRepository;
import mamun.sound.fire.model.Sound;
import mamun.sound.fire.model.SoundsData;
import mamun.sound.fire.ui.common.SoundGridFragment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The user's saved sounds. Stays in sync with hearts tapped on any other screen. */
public class FavoritesFragment extends SoundGridFragment {

    @Override protected void loadPage(int offset, SoundRepository.Callback<SoundsData> cb) {
        repo.favoriteSounds(cb);          // one page, cached for offline
    }

    @Override protected int emptyTextRes() { return R.string.state_empty_favorites; }

    @Override public void onFavoritesChanged(Set<Long> ids) {
        super.onFavoritesChanged(ids);                       // update the hearts
        if (!isAdded()) return;
        List<Sound> current = getItems();
        Set<Long> have = new HashSet<>();
        for (Sound s : current) have.add(s.id);

        if (!have.containsAll(ids)) { reload(); return; }    // a new favorite: fetch it from the server

        List<Sound> kept = new ArrayList<>();                // unfavorited: drop from the list at once
        for (Sound s : current) if (ids.contains(s.id)) kept.add(s);
        if (kept.size() != current.size()) setItems(kept);
    }
}
