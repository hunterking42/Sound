package mamun.sound.fire.ui.maintenance;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import mamun.sound.fire.R;
import mamun.sound.fire.config.AppConfig;
import mamun.sound.fire.ui.splash.SplashActivity;

/** Maintenance mode (normal users only; admins pass through). Message comes from app_settings. */
public class MaintenanceActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_status);

        ImageView icon = findViewById(R.id.icon);
        icon.setImageResource(R.drawable.ic_maintenance);
        icon.setColorFilter(getColor(R.color.volt));
        ((TextView) findViewById(R.id.title)).setText(R.string.maintenance_title);

        String msg = getIntent().getStringExtra(AppConfig.EXTRA_MESSAGE);
        ((TextView) findViewById(R.id.message)).setText(
                msg == null || msg.isEmpty() ? getString(R.string.maintenance_default_msg) : msg);

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
