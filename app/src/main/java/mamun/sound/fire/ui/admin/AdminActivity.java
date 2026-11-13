package mamun.sound.fire.ui.admin;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import mamun.sound.fire.R;
import mamun.sound.fire.util.Session;

/**
 * Admin panel shell. Hiding the entry point and the check below are only convenience:
 * the SERVER verifies role = admin on every admin call (Golden Rule 1).
 */
public class AdminActivity extends AppCompatActivity {

    private TextView title;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!Session.isAdmin()) { finish(); return; }
        setContentView(R.layout.activity_admin);
        title = findViewById(R.id.screenTitle);
        findViewById(R.id.btnBack).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        if (b == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.container, new AdminHomeFragment()).commit();
        }
    }

    /** Fragments call this in onResume so the title is right after going back. */
    public void setScreenTitle(@StringRes int res) { title.setText(res); }
    public void setScreenTitle(String text) { title.setText(text); }

    public void push(Fragment f) {
        getSupportFragmentManager().beginTransaction().replace(R.id.container, f).addToBackStack(null).commit();
    }
}
