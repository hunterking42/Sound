package mamun.sound.fire.ui.profile;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import mamun.sound.fire.R;
import mamun.sound.fire.model.User;
import mamun.sound.fire.util.Nav;
import mamun.sound.fire.util.Session;
import mamun.sound.fire.util.TimeUtil;

/** Username, premium badge and expiry. Reads the saved user, which Splash and Premium keep fresh. */
public class ProfileActivity extends AppCompatActivity {

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_profile);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnPremium).setOnClickListener(v -> Nav.openPremium(this));
    }

    @Override protected void onResume() {
        super.onResume();
        User u = Session.get();
        if (u == null) { finish(); return; }

        ((TextView) findViewById(R.id.avatar)).setText(u.username.substring(0, 1).toUpperCase());
        ((TextView) findViewById(R.id.username)).setText(u.username);

        TextView badge = findViewById(R.id.badge), expiry = findViewById(R.id.expiry);
        if (u.isPremiumNow()) {
            badge.setText(R.string.profile_badge_premium);
            badge.setTextColor(getColor(R.color.gold));
            badge.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_crown, 0, 0, 0);
            badge.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.gold)));
            boolean timed = u.premiumExpiry != null && !u.premiumExpiry.isEmpty();
            expiry.setText(timed ? getString(R.string.premium_status_until, TimeUtil.utcToLocal(u.premiumExpiry))
                                 : getString(R.string.premium_status_lifetime));
            expiry.setVisibility(android.view.View.VISIBLE);
        } else {
            badge.setText(R.string.profile_badge_free);
            badge.setTextColor(getColor(R.color.mist));
            badge.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
            expiry.setVisibility(android.view.View.GONE);
        }
    }
}
