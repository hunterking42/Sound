package mamun.sound.fire.ui.common;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;

import mamun.sound.fire.R;

/**
 * The app's one dialog style (admin actions, notices): plum, 28dp radius, one icon, one title, one message,
 * an optional input and buttons at the bottom. Build a Spec, then call show().
 */
public final class AppDialog {
    private AppDialog() {}

    public interface OnInput { void onInput(String text); }

    public static final class Spec {
        final @DrawableRes int icon;
        final String title, message;
        boolean hasInput, required, danger;
        String hint, primaryLabel, neutralLabel, cancelLabel;
        int inputType;
        OnInput onPrimary;
        Runnable onNeutral;

        public Spec(@DrawableRes int icon, String title, String message) { this.icon = icon; this.title = title; this.message = message; }

        public Spec input(String hint, int inputType, boolean required) { hasInput = true; this.hint = hint; this.inputType = inputType; this.required = required; return this; }
        public Spec primary(String label, boolean danger, OnInput cb) { primaryLabel = label; this.danger = danger; onPrimary = cb; return this; }
        public Spec cancel(String label) { cancelLabel = label; return this; }
        public Spec neutral(String label, Runnable cb) { neutralLabel = label; onNeutral = cb; return this; }

        public void show(Activity a) { AppDialog.show(a, this); }
    }

    private static void show(Activity a, Spec s) {
        Dialog d = new Dialog(a);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(R.layout.dialog_admin);
        d.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog);

        ImageView icon = d.findViewById(R.id.dlgIcon);
        icon.setImageResource(s.icon);
        icon.setColorFilter(a.getColor(s.danger ? R.color.danger : R.color.violet));
        ((TextView) d.findViewById(R.id.dlgTitle)).setText(s.title);
        TextView msg = d.findViewById(R.id.dlgMessage);
        if (s.message == null) msg.setVisibility(View.GONE); else msg.setText(s.message);

        View box = d.findViewById(R.id.dlgInputBox);
        EditText input = d.findViewById(R.id.dlgInput);
        TextView error = d.findViewById(R.id.dlgError);
        if (s.hasInput) {
            box.setVisibility(View.VISIBLE);
            input.setHint(s.hint);
            input.setInputType(s.inputType);
            input.requestFocus();
            d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                    | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        Button primary = d.findViewById(R.id.dlgPrimary);
        primary.setText(s.primaryLabel);
        primary.setBackgroundResource(s.danger ? R.drawable.bg_button_danger : R.drawable.bg_gradient_fuse);
        primary.setOnClickListener(v -> {
            String text = s.hasInput ? input.getText().toString().trim() : "";
            if (s.hasInput && s.required && text.isEmpty()) {
                box.setSelected(true);
                error.setText(R.string.err_required);
                error.setVisibility(View.VISIBLE);
                return;
            }
            d.dismiss();
            if (s.onPrimary != null) s.onPrimary.onInput(text);
        });

        Button neutral = d.findViewById(R.id.dlgNeutral);
        if (s.neutralLabel != null) {
            neutral.setVisibility(View.VISIBLE);
            neutral.setText(s.neutralLabel);
            neutral.setOnClickListener(v -> { d.dismiss(); if (s.onNeutral != null) s.onNeutral.run(); });
        }
        Button cancel = d.findViewById(R.id.dlgCancel);
        if (s.cancelLabel != null) cancel.setText(s.cancelLabel);
        cancel.setOnClickListener(v -> d.dismiss());

        d.show();
        d.getWindow().setLayout((int) (a.getResources().getDisplayMetrics().widthPixels * 0.9f), ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
