package mamun.sound.fire.ui.admin;

import mamun.sound.fire.ui.common.AppDialog;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import mamun.sound.fire.R;
import mamun.sound.fire.network.Api;
import mamun.sound.fire.network.ApiError;
import mamun.sound.fire.util.FormUtil;
import mamun.sound.fire.util.Nav;
import mamun.sound.fire.util.UriRequestBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * One generic form for admin add / edit screens. A subclass lists its fields and says what Save and Delete do;
 * this class draws the fields, checks required ones, picks files (system file picker, no storage permission),
 * shows busy and error states, and goes back after success.
 */
public abstract class AdminFormFragment extends Fragment {

    protected static final int T_TEXT = 0, T_MULTI = 1, T_NUMBER = 2, T_URL = 3, T_SWITCH = 4, T_CHOICE = 5, T_FILE = 6;

    protected static final class Choice {
        public final long id; public final String label;
        public Choice(long id, String label) { this.id = id; this.label = label; }
    }

    protected static final class Field {
        final String key, label; final int type;
        boolean required; String hint = ""; String text = ""; boolean checked;
        List<Choice> choices; long choiceId = -1;
        String mime; long maxBytes; boolean hasCurrent;
        Uri fileUri; String fileName, fileMime; long fileSize = -1;
        View box; TextView valueView; EditText input; SwitchCompat sw;

        private Field(String key, String label, int type) { this.key = key; this.label = label; this.type = type; }

        public static Field text(String key, String label, @Nullable String value, boolean required) {
            Field f = new Field(key, label, T_TEXT); f.text = value == null ? "" : value; f.required = required; return f;
        }
        public static Field multiline(String key, String label, @Nullable String value, boolean required) {
            Field f = new Field(key, label, T_MULTI); f.text = value == null ? "" : value; f.required = required; return f;
        }
        public static Field number(String key, String label, @Nullable String value) {
            Field f = new Field(key, label, T_NUMBER); f.text = value == null ? "" : value; return f;
        }
        public static Field url(String key, String label, @Nullable String value, boolean required) {
            Field f = new Field(key, label, T_URL); f.text = value == null ? "" : value; f.required = required; return f;
        }
        public static Field sw(String key, String label, boolean checked) {
            Field f = new Field(key, label, T_SWITCH); f.checked = checked; return f;
        }
        public static Field choice(String key, String label, List<Choice> choices, long selectedId, boolean required) {
            Field f = new Field(key, label, T_CHOICE); f.choices = choices; f.choiceId = selectedId; f.required = required; return f;
        }
        public static Field file(String key, String label, String mime, long maxBytes, boolean hasCurrent, boolean required) {
            Field f = new Field(key, label, T_FILE); f.mime = mime; f.maxBytes = maxBytes; f.hasCurrent = hasCurrent; f.required = required; return f;
        }
        public Field hint(String h) { hint = h; return this; }
    }

    /** What the user entered, handed to save(). */
    protected static final class Values {
        private final Map<String, Field> byKey = new HashMap<>();
        private final ContentResolver resolver;
        Values(List<Field> fields, ContentResolver r) { for (Field f : fields) byKey.put(f.key, f); resolver = r; }

        public String str(String key) { return byKey.get(key).text.trim(); }
        public int number(String key, int def) { String s = str(key); return s.isEmpty() ? def : Integer.parseInt(s); }   // validated before save()
        public boolean flag(String key) { return byKey.get(key).checked; }
        public long choice(String key) { return byKey.get(key).choiceId; }
        public boolean hasFile(String key) { return byKey.get(key).fileUri != null; }

        /** A multipart file part, or null if the user did not pick a file. */
        @Nullable public MultipartBody.Part part(String key, String partName) {
            Field f = byKey.get(key);
            if (f.fileUri == null) return null;
            return MultipartBody.Part.createFormData(partName, f.fileName != null ? f.fileName : partName,
                    new UriRequestBody(resolver, f.fileUri, f.fileMime, f.fileSize));
        }
    }

    protected interface Done { void onDone(); void onError(ApiError e); }

    // ---------- what subclasses provide ----------
    protected abstract @StringRes int titleRes();
    protected abstract List<Field> createFields();
    protected abstract void save(Values values, Done done);
    /** Load anything the fields need (for example categories), then call ready.run(). */
    protected void prepare(Runnable ready) { ready.run(); }
    /**
     * Ask "are you sure?" before saving risky changes. Return true if you showed a dialog (then call proceed.run()
     * when the admin confirms), false to save straight away.
     */
    protected boolean confirmBeforeSave(Values values, Runnable proceed) { return false; }
    protected boolean canDelete() { return false; }
    protected String deleteName() { return ""; }
    protected void delete(Done done) {}

    protected static RequestBody text(String s) { return RequestBody.create(s, MediaType.parse("text/plain; charset=utf-8")); }

    /** Turns an API call result into Done. */
    protected final <D> Api.Result<D> result(Done done) {
        return new Api.Result<D>() {
            @Override public void onSuccess(D d) { done.onDone(); }
            @Override public void onError(ApiError e) { done.onError(e); }
        };
    }

    // ---------- internals ----------
    private final List<Field> fields = new ArrayList<>();
    private View formScroll, loading;
    private TextView errorMsg, loadError;
    private Button btnSave, btnDelete;
    private LinearLayout fieldsBox;
    private Field pendingFile;

    private final ActivityResultLauncher<String> picker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && pendingFile != null && isAdded()) onFilePicked(pendingFile, uri);
            });

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle b) {
        View v = inflater.inflate(R.layout.fragment_admin_form, container, false);
        formScroll = v.findViewById(R.id.formScroll);
        loading = v.findViewById(R.id.loading);
        errorMsg = v.findViewById(R.id.errorMsg);
        loadError = v.findViewById(R.id.loadError);
        btnSave = v.findViewById(R.id.btnSave);
        btnDelete = v.findViewById(R.id.btnDelete);
        fieldsBox = v.findViewById(R.id.fields);
        prepare(this::buildForm);
        return v;
    }

    @Override public void onResume() {
        super.onResume();
        ((AdminActivity) requireActivity()).setScreenTitle(titleRes());
    }

    /** For subclasses whose prepare() failed. */
    protected final void failPrepare(ApiError e) {
        if (!isAdded() || Nav.handleCommonError(requireActivity(), e)) return;
        failPrepare(e.isNetwork() ? R.string.error_no_internet : R.string.error_generic);
    }
    protected final void failPrepare(@StringRes int message) {
        if (!isAdded()) return;
        loading.setVisibility(View.GONE);
        loadError.setText(message);
        loadError.setVisibility(View.VISIBLE);
    }

    private void buildForm() {
        if (!isAdded()) return;
        fields.clear();
        fields.addAll(createFields());
        for (Field f : fields) addFieldView(f);
        loading.setVisibility(View.GONE);
        formScroll.setVisibility(View.VISIBLE);

        btnSave.setOnClickListener(x -> onSaveClicked());
        if (canDelete()) {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(x -> new AppDialog.Spec(R.drawable.ic_delete, getString(R.string.btn_delete),
                    getString(R.string.dlg_delete_item_msg, deleteName()))
                    .primary(getString(R.string.btn_delete), true, t -> runDelete())
                    .show(requireActivity()));
        }
    }

    private void addFieldView(Field f) {
        LayoutInflater inf = LayoutInflater.from(requireContext());
        View row;
        switch (f.type) {
            case T_SWITCH:
                row = inf.inflate(R.layout.item_form_switch, fieldsBox, false);
                ((TextView) row.findViewById(R.id.label)).setText(f.label);
                f.sw = row.findViewById(R.id.switchView);
                f.sw.setChecked(f.checked);
                row.setOnClickListener(x -> f.sw.toggle());
                break;
            case T_CHOICE:
            case T_FILE:
                row = inf.inflate(R.layout.item_form_pick, fieldsBox, false);
                ((TextView) row.findViewById(R.id.label)).setText(f.label);
                f.box = row.findViewById(R.id.box);
                f.valueView = row.findViewById(R.id.value);
                f.valueView.setText(f.type == T_CHOICE ? choiceLabel(f) : fileLabel(f));
                f.box.setOnClickListener(x -> {
                    if (f.type == T_CHOICE) showChoices(f);
                    else { pendingFile = f; picker.launch(f.mime); }
                });
                break;
            default:   // text, multiline, number, url
                row = inf.inflate(R.layout.item_form_text, fieldsBox, false);
                ((TextView) row.findViewById(R.id.label)).setText(f.label);
                f.box = row.findViewById(R.id.box);
                f.input = row.findViewById(R.id.input);
                f.input.setText(f.text);
                f.input.setHint(f.hint);
                if (f.type == T_NUMBER) f.input.setInputType(InputType.TYPE_CLASS_NUMBER);
                else if (f.type == T_URL) f.input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
                else if (f.type == T_MULTI) {
                    f.input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                    f.input.setMinLines(3);
                    f.input.setMaxLines(8);
                } else f.input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                FormUtil.bindInput(f.box, f.input);
        }
        fieldsBox.addView(row);
    }

    private String choiceLabel(Field f) {
        for (Choice c : f.choices) if (c.id == f.choiceId) return c.label;
        return getString(R.string.pick_category);
    }
    private String fileLabel(Field f) {
        if (f.fileName != null) return f.fileName;
        return getString(f.hasCurrent ? R.string.file_current : R.string.file_none);
    }

    private void showChoices(Field f) {
        String[] labels = new String[f.choices.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = f.choices.get(i).label;
        new MaterialAlertDialogBuilder(requireContext()).setTitle(f.label).setItems(labels, (d, which) -> {
            f.choiceId = f.choices.get(which).id;
            f.valueView.setText(labels[which]);
            f.box.setSelected(false);
        }).show();
    }

    // ---------- files ----------
    private void onFilePicked(Field f, Uri uri) {
        ContentResolver cr = requireContext().getContentResolver();
        String name = null; long size = -1;
        try (Cursor c = cr.query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME), s = c.getColumnIndex(OpenableColumns.SIZE);
                if (n >= 0) name = c.getString(n);
                if (s >= 0 && !c.isNull(s)) size = c.getLong(s);
            }
        }
        if (size > f.maxBytes) {       // check here so the admin does not wait for a long upload that will fail
            Toast.makeText(requireContext(), getString(R.string.file_too_big, Formatter.formatShortFileSize(requireContext(), f.maxBytes)),
                    Toast.LENGTH_LONG).show();
            return;
        }
        f.fileUri = uri; f.fileName = name != null ? name : "file"; f.fileSize = size; f.fileMime = cr.getType(uri);
        f.valueView.setText(f.fileName);
        f.box.setSelected(false);
    }

    // ---------- save / delete ----------
    private void onSaveClicked() {
        errorMsg.setVisibility(View.GONE);
        for (Field f : fields) {                      // copy what is on screen into the fields
            if (f.input != null) f.text = f.input.getText().toString();
            if (f.sw != null) f.checked = f.sw.isChecked();
        }
        if (!validate()) return;
        Values vals = new Values(fields, requireContext().getContentResolver());
        if (!confirmBeforeSave(vals, () -> startSave(vals))) startSave(vals);
    }

    private void startSave(Values vals) {
        setBusy(true);
        save(vals, new Done() {
            @Override public void onDone() {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), R.string.form_saved, Toast.LENGTH_SHORT).show();
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
            @Override public void onError(ApiError e) { failed(e); }
        });
    }

    private void runDelete() {
        setBusy(true);
        delete(new Done() {
            @Override public void onDone() {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), R.string.item_deleted, Toast.LENGTH_SHORT).show();
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
            @Override public void onError(ApiError e) { failed(e); }
        });
    }

    private void failed(ApiError e) {
        if (!isAdded()) return;
        setBusy(false);
        if (Nav.handleCommonError(requireActivity(), e)) return;
        errorMsg.setText(e.isNetwork() ? getString(R.string.error_no_internet)
                : (e.message != null ? e.message : getString(R.string.error_generic)));
        errorMsg.setVisibility(View.VISIBLE);
    }

    private void setBusy(boolean busy) {
        btnSave.setEnabled(!busy);
        btnSave.setText(busy ? R.string.btn_please_wait : R.string.btn_save);
        btnDelete.setEnabled(!busy);
    }

    /** Checks required fields and numbers. The server checks everything again. */
    private boolean validate() {
        for (Field f : fields) {
            int err = 0;
            switch (f.type) {
                case T_TEXT: case T_MULTI: case T_URL:
                    if (f.required && f.text.trim().isEmpty()) err = R.string.err_required; break;
                case T_NUMBER:
                    if (!f.text.trim().isEmpty() && !f.text.trim().matches("\\d{1,9}")) err = R.string.err_number; break;
                case T_CHOICE:
                    if (f.required && f.choiceId < 0) err = R.string.err_choose_one; break;
                case T_FILE:
                    if (f.required && f.fileUri == null && !f.hasCurrent) err = R.string.err_file_required; break;
                default: break;
            }
            if (err != 0) {
                if (f.box != null) f.box.setSelected(true);
                errorMsg.setText(getString(err) + "  (" + f.label + ")");
                errorMsg.setVisibility(View.VISIBLE);
                return false;
            }
        }
        return true;
    }
}
