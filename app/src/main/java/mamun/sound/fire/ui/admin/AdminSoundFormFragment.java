package mamun.sound.fire.ui.admin;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import mamun.sound.fire.R;
import mamun.sound.fire.model.admin.AdminCategoriesData;
import mamun.sound.fire.model.admin.AdminCategory;
import mamun.sound.fire.model.admin.AdminSound;
import mamun.sound.fire.model.admin.ManageResult;
import mamun.sound.fire.network.Api;
import mamun.sound.fire.network.ApiClient;
import mamun.sound.fire.network.ApiError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.RequestBody;

/** Add or edit a sound. Image (cropped to a 512 square by the server) and audio are picked from the phone. */
public class AdminSoundFormFragment extends AdminFormFragment {

    private static final String ARG_JSON = "json";
    private static final long MAX_IMAGE = 5L * 1024 * 1024, MAX_AUDIO = 8L * 1024 * 1024;   // same limits as the server

    public static AdminSoundFormFragment newInstance(@Nullable AdminSound s) {
        AdminSoundFormFragment f = new AdminSoundFormFragment();
        Bundle a = new Bundle();
        if (s != null) a.putString(ARG_JSON, new Gson().toJson(s));
        f.setArguments(a);
        return f;
    }

    private AdminSound sound;                       // null = adding
    private final List<Choice> categories = new ArrayList<>();

    @Override public void onCreate(@Nullable Bundle b) {
        super.onCreate(b);
        String json = requireArguments().getString(ARG_JSON);
        sound = json == null ? null : new Gson().fromJson(json, AdminSound.class);
    }

    @Override protected int titleRes() { return sound == null ? R.string.add_sound : R.string.edit_sound; }

    @Override protected void prepare(Runnable ready) {
        Api.call(ApiClient.admin().categories(), new Api.Result<AdminCategoriesData>() {
            @Override public void onSuccess(AdminCategoriesData d) {
                if (!isAdded()) return;
                categories.clear();
                if (d.categories != null) for (AdminCategory c : d.categories)
                    categories.add(new Choice(c.id, c.isActive == 0 ? c.name + " (" + getString(R.string.row_hidden) + ")" : c.name));
                if (categories.isEmpty()) failPrepare(R.string.err_no_categories);
                else ready.run();
            }
            @Override public void onError(ApiError e) { failPrepare(e); }
        });
    }

    @Override protected List<Field> createFields() {
        boolean edit = sound != null;
        List<Field> f = new ArrayList<>();
        f.add(Field.text("name", getString(R.string.f_name), edit ? sound.name : "", true));
        f.add(Field.choice("category", getString(R.string.f_category), categories, edit ? sound.categoryId : -1, true));
        f.add(Field.file("image", getString(R.string.f_image), "image/*", MAX_IMAGE, edit && sound.imageUrl != null && !sound.imageUrl.isEmpty(), false));
        f.add(Field.file("audio", getString(R.string.f_audio), "audio/*", MAX_AUDIO, edit, !edit));      // audio is required when adding
        f.add(Field.sw("premium", getString(R.string.f_premium), edit && sound.isPremium != 0));
        f.add(Field.sw("active", getString(R.string.f_visible), !edit || sound.isActive != 0));
        f.add(Field.number("sort", getString(R.string.f_sort), edit ? String.valueOf(sound.sortOrder) : "0"));
        return f;
    }

    @Override protected void save(Values v, Done done) {
        Map<String, RequestBody> parts = new HashMap<>();
        parts.put("action", text(sound == null ? "add" : "edit"));
        if (sound != null) parts.put("id", text(String.valueOf(sound.id)));
        parts.put("name", text(v.str("name")));
        parts.put("category_id", text(String.valueOf(v.choice("category"))));
        parts.put("is_premium", text(v.flag("premium") ? "1" : "0"));
        parts.put("is_active", text(v.flag("active") ? "1" : "0"));
        parts.put("sort_order", text(String.valueOf(v.number("sort", 0))));
        // Files are sent only when picked; an edit without new files keeps the current ones
        Api.call(ApiClient.admin().soundSave(parts, v.part("image", "image"), v.part("audio", "audio")), this.<ManageResult>result(done));
    }

    @Override protected boolean canDelete() { return sound != null; }
    @Override protected String deleteName() { return sound != null ? sound.name : ""; }

    @Override protected void delete(Done done) {
        Map<String, Object> body = new HashMap<>();
        body.put("action", "delete");
        body.put("id", sound.id);
        Api.call(ApiClient.admin().soundAction(body), this.<ManageResult>result(done));
    }
}
