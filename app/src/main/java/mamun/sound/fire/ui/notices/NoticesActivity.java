package mamun.sound.fire.ui.notices;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import mamun.sound.fire.R;
import mamun.sound.fire.data.SoundRepository;
import mamun.sound.fire.model.Notice;
import mamun.sound.fire.model.NoticesData;
import mamun.sound.fire.network.ApiError;
import mamun.sound.fire.ui.common.NoticeChecker;
import mamun.sound.fire.util.AppPrefs;
import mamun.sound.fire.util.Nav;
import mamun.sound.fire.util.TimeUtil;

import java.util.ArrayList;
import java.util.List;

/** Messages from the admin, newest first. Opening the list marks everything as read. Works offline from the saved copy. */
public class NoticesActivity extends AppCompatActivity {

    private RecyclerView list;
    private View loading, stateGroup;
    private TextView stateText;
    private View stateButton;
    private final NoticeAdapter adapter = new NoticeAdapter();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_notices);
        list = findViewById(R.id.list);
        loading = findViewById(R.id.loading);
        stateGroup = findViewById(R.id.stateGroup);
        stateText = findViewById(R.id.stateText);
        stateButton = findViewById(R.id.stateButton);
        findViewById(R.id.btnBack).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        stateButton.setOnClickListener(v -> load());
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        load();
    }

    private void load() {
        list.setVisibility(View.GONE);
        stateGroup.setVisibility(View.GONE);
        loading.setVisibility(View.VISIBLE);
        SoundRepository.get(this).notices(new SoundRepository.Callback<NoticesData>() {
            @Override public void onData(NoticesData d, boolean fromCache) {
                if (isFinishing()) return;
                List<Notice> items = d.notices != null ? d.notices : new ArrayList<Notice>();
                long max = 0;
                for (Notice n : items) max = Math.max(max, n.id);
                AppPrefs.setNoticeRead(NoticesActivity.this, Math.max(AppPrefs.noticeRead(NoticesActivity.this), max));
                AppPrefs.setNoticeLatest(NoticesActivity.this, Math.max(AppPrefs.noticeLatest(NoticesActivity.this), max));
                NoticeChecker.notifyChanged();                     // the Home bell loses its dot

                loading.setVisibility(View.GONE);
                if (items.isEmpty()) showMessage(R.string.notices_empty, false);
                else { adapter.submitList(items); list.setVisibility(View.VISIBLE); }
            }
            @Override public void onError(ApiError e) {
                if (isFinishing() || Nav.handleCommonError(NoticesActivity.this, e)) return;
                showMessage(e.isNetwork() ? R.string.error_no_internet : R.string.error_generic, true);
            }
        });
    }

    private void showMessage(@StringRes int text, boolean retry) {
        loading.setVisibility(View.GONE);
        stateText.setText(text);
        stateButton.setVisibility(retry ? View.VISIBLE : View.GONE);
        stateGroup.setVisibility(View.VISIBLE);
    }

    private static final class NoticeAdapter extends ListAdapter<Notice, NoticeAdapter.VH> {
        NoticeAdapter() {
            super(new DiffUtil.ItemCallback<Notice>() {
                @Override public boolean areItemsTheSame(@NonNull Notice a, @NonNull Notice b) { return a.id == b.id; }
                @Override public boolean areContentsTheSame(@NonNull Notice a, @NonNull Notice b) { return a.id == b.id; }
            });
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_notice, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            Notice n = getItem(pos);
            h.title.setText(n.title);
            h.message.setText(n.message);
            h.date.setText(TimeUtil.utcToLocal(n.createdAt));
        }
        static final class VH extends RecyclerView.ViewHolder {
            final TextView title, message, date;
            VH(@NonNull View v) {
                super(v);
                title = v.findViewById(R.id.noticeTitle);
                message = v.findViewById(R.id.noticeMessage);
                date = v.findViewById(R.id.noticeDate);
            }
        }
    }
}
