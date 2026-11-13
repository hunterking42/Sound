package mamun.sound.fire.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import mamun.sound.fire.data.FavoritesStore;
import mamun.sound.fire.data.SoundRepository;
import mamun.sound.fire.model.EmptyData;
import mamun.sound.fire.network.Api;
import mamun.sound.fire.network.ApiClient;
import mamun.sound.fire.network.ApiError;
import mamun.sound.fire.ui.auth.LoginActivity;

/** Sign out: revoke the token on the server (best effort), then wipe everything user-specific on the phone. */
public final class Logout {
    private Logout() {}

    public static void perform(Activity a) {
        Context app = a.getApplicationContext();

        // Token passed explicitly because we clear our own copy straight away
        String token = SecurePrefs.getToken();
        if (token != null) {
            Api.call(ApiClient.get().logout("Bearer " + token), new Api.Result<EmptyData>() {
                @Override public void onSuccess(EmptyData d) {}
                @Override public void onError(ApiError e) { /* offline is fine: the phone is already signed out */ }
            });
        }

        OverlayController.disable(app);                   // bubble must not outlive the session
        Session.clear();                                  // token + saved user
        SoundRepository.get(app).wipe();                  // cached lists
        SoundPlayer.get(app).wipe();                      // stops sound, deletes downloaded audio
        FavoritesStore.get(app).clear();
        AppPrefs.clearRecentSearches(app);
        AppPrefs.clearNoticeState(app);                   // a new account starts with a clean baseline

        Intent i = new Intent(a, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        a.startActivity(i);
    }
}
