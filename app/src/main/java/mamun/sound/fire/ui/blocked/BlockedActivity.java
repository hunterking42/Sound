package mamun.sound.fire.ui.blocked;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import mamun.sound.fire.R;
import mamun.sound.fire.config.AppConfig;
import mamun.sound.fire.ui.splash.SplashActivity;
import mamun.sound.fire.util.TimeUtil;

/** Banned or locked. Cannot be dismissed: Back closes the app. "Check again" re-runs Splash (the lock may have ended). */
public class BlockedActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_status);

        String status = getIntent().getStringExtra(AppConfig.EXTRA_STATUS);
        String reason = getIntent().getStringExtra(AppConfig.EXTRA_REASON);
        String until = TimeUtil.utcToLocal(getIntent().getStringExtra(AppConfig.EXTRA_LOCK_UNTIL));
        boolean banned = "banned".equals(status);

        ImageView icon = findViewById(R.id.icon);
        icon.setImageResource(R.drawable.ic_warning);
        icon.setColorFilter(getColor(R.color.spark));
        ((TextView) findViewById(R.id.title)).setText(banned ? R.string.blocked_banned_title : R.string.blocked_locked_title);

        StringBuilder msg = new StringBuilder(getString(banned ? R.string.blocked_banned_msg : R.string.blocked_locked_msg));
        if (reason != null && !reason.isEmpty()) msg.append("\n\n").append(getString(R.string.blocked_reason, reason));
        if (!banned && until != null) msg.append("\n").append(getString(R.string.blocked_until, until));
        ((TextView) findViewById(R.id.message)).setText(msg);

        findViewById(R.id.btnCheck).setOnClickListener(v -> {
            startActivity(new Intent(this, SplashActivity.class));
            finish();
        });
        findViewById(R.id.btnExit).setOnClickListener(v -> finishAffinity());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { finishAffinity(); }
        });
    }
}
