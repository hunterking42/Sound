package mamun.sound.fire.ui.admin;

import mamun.sound.fire.ui.common.AppDialog;

import mamun.sound.fire.R;
import mamun.sound.fire.model.admin.BroadcastResult;
import mamun.sound.fire.network.Api;
import mamun.sound.fire.network.ApiClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Write a notice for all users. It appears in their notifications list, and as a pop-up if you choose. */
public class AdminBroadcastFormFragment extends AdminFormFragment {

    @Override protected int titleRes() { return R.string.add_notice; }

    @Override protected List<Field> createFields() {
        List<Field> f = new ArrayList<>();
        f.add(Field.text("title", getString(R.string.f_title), "", true));
        f.add(Field.multiline("message", getString(R.string.f_message), "", true));
        f.add(Field.sw("popup", getString(R.string.f_popup), true));
        return f;
    }

    /** A notice goes to every user and cannot be edited, so ask once. */
    @Override protected boolean confirmBeforeSave(Values v, Runnable proceed) {
        new AppDialog.Spec(R.drawable.ic_send, getString(R.string.dlg_broadcast_title), getString(R.string.dlg_broadcast_msg))
                .primary(getString(R.string.btn_send), false, t -> proceed.run())
                .show(requireActivity());
        return true;
    }

    @Override protected void save(Values v, Done done) {
        Map<String, Object> m = new HashMap<>();
        m.put("action", "send");
        m.put("title", v.str("title"));
        m.put("message", v.str("message"));
        m.put("show_popup", v.flag("popup") ? 1 : 0);
        Api.call(ApiClient.admin().broadcastAction(m), this.<BroadcastResult>result(done));
    }
}
