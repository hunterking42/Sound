package mamun.sound.fire.ui.admin;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import mamun.sound.fire.R;
import mamun.sound.fire.model.admin.AdminBanner;
import mamun.sound.fire.model.admin.ManageResult;
import mamun.sound.fire.network.Api;
import mamun.sound.fire.network.ApiClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Add or edit a Home banner. Banner images are https links. */
public class AdminBannerFormFragment extends AdminFormFragment {

    private static final String ARG_JSON = "json";

    public static AdminBannerFormFragment newInstance(@Nullable AdminBanner b) {
        AdminBannerFormFragment f = new AdminBannerFormFragment();
        Bundle a = new Bundle();
        if (b != null) a.putString(ARG_JSON, new Gson().toJson(b));
        f.setArguments(a);
        return f;
    }

    private AdminBanner banner;

    @Override public void onCreate(@Nullable Bundle b) {
        super.onCreate(b);
        String json = requireArguments().getString(ARG_JSON);
        banner = json == null ? null : new Gson().fromJson(json, AdminBanner.class);
    }

    @Override protected int titleRes() { return banner == null ? R.string.add_banner : R.string.edit_banner; }

    @Override protected List<Field> createFields() {
        boolean edit = banner != null;
        List<Field> f = new ArrayList<>();
        f.add(Field.url("image", getString(R.string.f_image_url), edit ? banner.imageUrl : "", true).hint("https://"));
        f.add(Field.url("link", getString(R.string.f_link), edit ? banner.link : "", false).hint("https://"));
        f.add(Field.number("sort", getString(R.string.f_sort), edit ? String.valueOf(banner.sortOrder) : "0"));
        f.add(Field.sw("active", getString(R.string.f_visible), !edit || banner.isActive != 0));
        return f;
    }

    @Override protected void save(Values v, Done done) {
        Map<String, Object> m = new HashMap<>();
        m.put("action", banner == null ? "add" : "edit");
        if (banner != null) m.put("id", banner.id);
        m.put("image_url", v.str("image"));
        m.put("link", v.str("link"));
        m.put("sort_order", v.number("sort", 0));
        m.put("is_active", v.flag("active") ? 1 : 0);
        Api.call(ApiClient.admin().bannerAction(m), this.<ManageResult>result(done));
    }

    @Override protected boolean canDelete() { return banner != null; }
    @Override protected String deleteName() { return banner != null ? getString(R.string.row_banner, banner.id) : ""; }

    @Override protected void delete(Done done) {
        Map<String, Object> m = new HashMap<>();
        m.put("action", "delete");
        m.put("id", banner.id);
        Api.call(ApiClient.admin().bannerAction(m), this.<ManageResult>result(done));
    }
}
