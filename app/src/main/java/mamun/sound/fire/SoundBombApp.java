package mamun.sound.fire;

import android.app.Application;
import android.content.Intent;

import mamun.sound.fire.network.ApiClient;
import mamun.sound.fire.ui.update.ForceUpdateActivity;
import mamun.sound.fire.util.SecurePrefs;

public class SoundBombApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        SecurePrefs.init(this);     // must run before ApiClient (the interceptor reads the token)
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mamun.sound.fire.util.AppPrefs.nightMode(this));
        ApiClient.init(this);
        // Any API call answered with 426 sends the user to the non-cancelable update screen
        ApiClient.setForceUpdateListener(() -> {
            Intent i = new Intent(this, ForceUpdateActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });
    }
}
