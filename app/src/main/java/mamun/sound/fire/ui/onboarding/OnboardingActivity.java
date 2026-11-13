package mamun.sound.fire.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import mamun.sound.fire.R;
import mamun.sound.fire.ui.auth.SignupActivity;
import mamun.sound.fire.util.OverlayPermission;
import mamun.sound.fire.util.SecurePrefs;

/** First launch only: 3 slides, then the overlay permission explanation. The permission is optional here. */
public class OnboardingActivity extends AppCompatActivity {

    private static final class Slide {
        final @DrawableRes int icon; final @StringRes int title, body;
        Slide(int icon, int title, int body) { this.icon = icon; this.title = title; this.body = body; }
    }

    private static final Slide[] SLIDES = {
            new Slide(R.drawable.ic_wave,    R.string.onb1_title, R.string.onb1_body),
            new Slide(R.drawable.ic_call,    R.string.onb2_title, R.string.onb2_body),
            new Slide(R.drawable.ic_crown,   R.string.onb3_title, R.string.onb3_body),
            new Slide(R.drawable.ic_overlay, R.string.onb4_title, R.string.onb4_body),   // overlay explanation
    };
    private static final int LAST = SLIDES.length - 1;

    private ViewPager2 pager;
    private Button btnPrimary;
    private TextView btnSkip;
    private LinearLayout dots;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_onboarding);
        pager = findViewById(R.id.pager);
        btnPrimary = findViewById(R.id.btnPrimary);
        btnSkip = findViewById(R.id.btnSkip);
        dots = findViewById(R.id.dots);

        pager.setAdapter(new SlideAdapter());
        buildDots();
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) { render(position); }
        });
        btnSkip.setOnClickListener(v -> finishOnboarding());

        // Back goes to the previous slide; on the first slide it closes the app as usual
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (pager.getCurrentItem() > 0) pager.setCurrentItem(pager.getCurrentItem() - 1);
                else finish();
            }
        });
        render(0);
    }

    /** The user may come back from the overlay settings page: refresh the buttons. */
    @Override protected void onResume() {
        super.onResume();
        if (pager != null) render(pager.getCurrentItem());
    }

    private void render(int pos) {
        for (int i = 0; i < dots.getChildCount(); i++) dots.getChildAt(i).setActivated(i == pos);

        if (pos < LAST) {
            btnPrimary.setText(R.string.btn_next);
            btnPrimary.setOnClickListener(v -> pager.setCurrentItem(pos + 1));
            btnSkip.setText(R.string.btn_skip);
            btnSkip.setVisibility(View.VISIBLE);
        } else if (OverlayPermission.isGranted(this)) {
            btnPrimary.setText(R.string.btn_get_started);
            btnPrimary.setOnClickListener(v -> finishOnboarding());
            btnSkip.setVisibility(View.GONE);
        } else {
            btnPrimary.setText(R.string.btn_allow_overlay);
            btnPrimary.setOnClickListener(v -> OverlayPermission.openSettings(this));
            btnSkip.setText(R.string.btn_not_now);
            btnSkip.setVisibility(View.VISIBLE);
        }
    }

    private void buildDots() {
        int size = (int) (8 * getResources().getDisplayMetrics().density);
        int gap = size / 2;
        for (int i = 0; i < SLIDES.length; i++) {
            View d = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(gap, 0, gap, 0);
            d.setLayoutParams(lp);
            d.setBackgroundResource(R.drawable.bg_dot);
            dots.addView(d);
        }
    }

    private void finishOnboarding() {
        SecurePrefs.setOnboardingDone();
        Intent i = new Intent(this, SignupActivity.class);   // new users create an account first
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private static final class SlideAdapter extends RecyclerView.Adapter<SlideAdapter.VH> {
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_onboarding, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int i) {
            h.icon.setImageResource(SLIDES[i].icon);
            h.title.setText(SLIDES[i].title);
            h.body.setText(SLIDES[i].body);
        }
        @Override public int getItemCount() { return SLIDES.length; }

        static final class VH extends RecyclerView.ViewHolder {
            final ImageView icon; final TextView title, body;
            VH(@NonNull View v) {
                super(v);
                icon = v.findViewById(R.id.icon);
                title = v.findViewById(R.id.title);
                body = v.findViewById(R.id.body);
            }
        }
    }
}
