package mamun.sound.fire.ui.premium;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import mamun.sound.fire.R;
import mamun.sound.fire.data.SoundRepository;
import mamun.sound.fire.model.AppSettings;
import mamun.sound.fire.model.MeData;
import mamun.sound.fire.model.User;
import mamun.sound.fire.network.Api;
import mamun.sound.fire.network.ApiClient;
import mamun.sound.fire.network.ApiError;
import mamun.sound.fire.util.Links;
import mamun.sound.fire.util.Nav;
import mamun.sound.fire.util.Session;
import mamun.sound.fire.util.TimeUtil;

/** Benefits, current status and expiry, and "Buy via Telegram" (opens the admin chat with a prefilled message). */
public class PremiumActivity extends AppCompatActivity {

    private TextView status;
    private Button btnBuy;
    private AppSettings settings;       // admin Telegram link; may come from the offline cache

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_premium);
        status = findViewById(R.id.status);
        btnBuy = findViewById(R.id.btnBuy);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnBuy.setOnClickListener(v -> buy());

        render();                                                    // saved user first, so the screen is never empty

        // Fresh status: the admin may have just granted or removed premium
        Api.call(ApiClient.get().me(), new Api.Result<MeData>() {
            @Override public void onSuccess(MeData me) { Session.set(me.user); render(); }
            @Override public void onError(ApiError e) { Nav.handleCommonError(PremiumActivity.this, e); }
        });
        SoundRepository.get(this).appSettings(new SoundRepository.Callback<AppSettings>() {
            @Override public void onData(AppSettings s, boolean fromCache) { settings = s; }
            @Override public void onError(ApiError e) {}
        });
    }

    private void render() {
        User u = Session.get();
        boolean premium = u != null && u.isPremiumNow();
        if (!premium) status.setText(R.string.premium_status_free);
        else if (u.premiumExpiry == null || u.premiumExpiry.isEmpty()) status.setText(R.string.premium_status_lifetime);
        else status.setText(getString(R.string.premium_status_until, TimeUtil.utcToLocal(u.premiumExpiry)));
        btnBuy.setText(premium ? R.string.premium_extend : R.string.premium_buy);
    }

    private void buy() {
        User u = Session.get();
        String admin = settings != null ? settings.telegramAdmin : null;
        if (u == null || !Links.buyPremium(this, admin, u.username))
            Toast.makeText(this, R.string.err_link_unavailable, Toast.LENGTH_SHORT).show();
    }
}
