package mamun.sound.fire.util;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import mamun.sound.fire.R;

import java.util.List;

/** Builds a row of selectable chips (category tabs, sort options, recent searches). */
public final class ChipRow {
    private ChipRow() {}

    public interface OnChip { void onChip(int index); }

    /** selected = -1 means no chip is highlighted (used for recent searches). */
    public static void bind(LinearLayout row, List<String> labels, int selected, OnChip cb) {
        Context c = row.getContext();
        float d = c.getResources().getDisplayMetrics().density;
        row.removeAllViews();
        for (int i = 0; i < labels.size(); i++) {
            final int index = i;
            TextView t = new TextView(c);
            t.setText(labels.get(i));
            t.setTextColor(c.getColorStateList(R.color.chip_text));
            t.setTextSize(15);
            t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            t.setGravity(Gravity.CENTER);
            t.setMinHeight((int) (48 * d));                       // 48dp touch target
            t.setPadding((int) (16 * d), 0, (int) (16 * d), 0);
            t.setBackgroundResource(R.drawable.bg_chip);
            t.setSelected(i == selected);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd((int) (8 * d));
            t.setLayoutParams(lp);
            t.setOnClickListener(v -> {
                for (int k = 0; k < row.getChildCount(); k++) {
                    View chip = row.getChildAt(k);
                    chip.setSelected(k == index && selected != -1);
                }
                cb.onChip(index);
            });
            row.addView(t);
        }
    }
}
