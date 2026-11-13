package mamun.sound.fire.ui.admin;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import mamun.sound.fire.R;
import mamun.sound.fire.model.admin.AdminCategory;
import mamun.sound.fire.model.admin.ManageResult;
import mamun.sound.fire.network.Api;
import mamun.sound.fire.network.ApiClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Add or edit a category. The server refuses to delete one that still has sounds. */
public class AdminCategoryFormFragment extends AdminFormFragment {

    private static final String ARG_JSON = "json";

    public static AdminCategoryFormFragment newInstance(@Nullable AdminCategory c) {
        AdminCategoryFormFragment f = new AdminCategoryFormFragment();
        Bundle a = new Bundle();
        if (c != null) a.putString(ARG_JSON, new Gson().toJson(c));
        f.setArguments(a);
        return f;
    }

    private AdminCategory category;

    @Override public void onCreate(@Nullable Bundle b) {
        super.onCreate(b);
        String json = requireArguments().getString(ARG_JSON);
        category = json == null ? null : new Gson().fromJson(json, AdminCategory.class);
    }

    @Override protected int titleRes() { return category == null ? R.string.add_category : R.string.edit_category; }

    @Override protected List<Field> createFields() {
        boolean edit = category != null;
        List<Field> f = new ArrayList<>();
        f.add(Field.text("name", getString(R.string.f_name), edit ? category.name : "", true));
        f.add(Field.url("icon", getString(R.string.f_icon_url), edit ? category.iconUrl : "", false).hint("https://"));
        f.add(Field.number("sort", getString(R.string.f_sort), edit ? String.valueOf(category.sortOrder) : "0"));
        f.add(Field.sw("active", getString(R.string.f_visible), !edit || category.isActive != 0));
        return f;
    }

    @Override protected void save(Values v, Done done) {
        Map<String, Object> m = new HashMap<>();
        m.put("action", category == null ? "add" : "edit");
        if (category != null) m.put("id", category.id);
        m.put("name", v.str("name"));
        m.put("icon_url", v.str("icon"));
        m.put("sort_order", v.number("sort", 0));
        m.put("is_active", v.flag("active") ? 1 : 0);
        Api.call(ApiClient.admin().categoryAction(m), this.<ManageResult>result(done));
    }

    @Override protected boolean canDelete() { return category != null; }
    @Override protected String deleteName() { return category != null ? category.name : ""; }

    @Override protected void delete(Done done) {
        Map<String, Object> m = new HashMap<>();
        m.put("action", "delete");
        m.put("id", category.id);
        Api.call(ApiClient.admin().categoryAction(m), this.<ManageResult>result(done));
    }
}
