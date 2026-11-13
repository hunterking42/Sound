package mamun.sound.fire.util;

import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import mamun.sound.fire.R;

/** Small helpers so every input looks and behaves the same (design system: violet focus, danger error). */
public final class FormUtil {
    private FormUtil() {}

    /** Violet stroke while focused; clears the error stroke as soon as the user types. */
    public static void bindInput(View box, EditText edit) {
        edit.setOnFocusChangeListener((v, focused) -> box.setActivated(focused));
        edit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { box.setSelected(false); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    public static void setError(View box, boolean error) { box.setSelected(error); }

    /** Eye icon shows/hides the password. */
    public static void bindPasswordToggle(EditText edit, ImageView eye) {
        eye.setOnClickListener(v -> {
            boolean hidden = edit.getTransformationMethod() instanceof PasswordTransformationMethod;
            edit.setTransformationMethod(hidden
                    ? HideReturnsTransformationMethod.getInstance()
                    : PasswordTransformationMethod.getInstance());
            eye.setImageResource(hidden ? R.drawable.ic_eye : R.drawable.ic_eye_off);
            edit.setSelection(edit.length());
        });
    }
}
