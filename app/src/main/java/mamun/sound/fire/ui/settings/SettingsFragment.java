package mamun.sound.fire.ui.settings;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import mamun.sound.fire.BuildConfig;
import mamun.sound.fire.R;
import mamun.sound.fire.data.SoundRepository;
import mamun.sound.fire.model.AppSettings;
import mamun.sound.fire.network.ApiError;
import mamun.sound.fire.ui.profile.ProfileActivity;
import mamun.sound.fire.util.AppPrefs;
import mamun.sound.fire.util.ChipRow;
import mamun.sound.fire.util.Links;
import mamun.sound.fire.util.Logout;
import mamun.sound.fire.util.Nav;
import mamun.sound.fire.util.OverlayController;
import mamun.sound.fire.util.OverlayPermission;
import mamun.sound.fire.util.SoundPlayer;

import java.util.Arrays;

/**
 * Volume, sounds per row, account, community links and about.
 * Not built yet because they need other pieces: theme switch (light theme), language (Bangla strings),
 * vibration (play hook) and the overlay switch (OverlayService, Step 6).
 */
public class SettingsFragment extends Fragment {

    private LinearLayout listAccount, listCommunity, listAbout, listPrefs;
    private AppSettings settings;     // null until loaded (or cached) from the server

    private SwitchCompat overlaySwitch;
    private boolean syncing;          // true while we set the switch from code (no reaction)
    private boolean pendingEnable;    // user was sent to the permission page: turn on when they return
    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (isAdded()) OverlayController.enable(requireContext());   // runs with or without the notification
            });

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle b) {
        View v = inflater.inflate(R.layout.fragment_settings, container, false);
        listAccount = v.findViewById(R.id.listAccount);
        listCommunity = v.findViewById(R.id.listCommunity);
        listAbout = v.findViewById(R.id.listAbout);
        listPrefs = v.findViewById(R.id.listPrefs);
        buildPrefs();
        buildOverlayRow(v);

        // Volume: applies at once to the sound that is playing, saved when the finger lifts
        SeekBar seek = v.findViewById(R.id.seekVolume);
        seek.setProgress(Math.round(AppPrefs.volume(requireContext()) * 100));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser) SoundPlayer.get(requireContext()).setVolume(p / 100f);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {
                AppPrefs.setVolume(requireContext(), s.getProgress() / 100f);
            }
        });

        // Sounds per row: 2 / 3 / 4. The grids pick it up when the user returns to their tab
        ChipRow.bind(v.findViewById(R.id.columnChips), Arrays.asList("2", "3", "4"),
                AppPrefs.gridColumns(requireContext()) - 2,
                index -> AppPrefs.setGridColumns(requireContext(), index + 2));

        buildLists();                                          // works offline, without links
        SoundRepository.get(requireContext()).appSettings(new SoundRepository.Callback<AppSettings>() {
            @Override public void onData(AppSettings s, boolean fromCache) {
                if (!isAdded()) return;
                settings = s;
                buildLists();                                  // now with Telegram and policy links
            }
            @Override public void onError(ApiError e) {
                if (isAdded()) Nav.handleCommonError(requireActivity(), e);
            }
        });
        return v;
    }

    // ---- preferences: language, theme, vibration ----
    private void buildPrefs() {
        Context c = requireContext();
        listPrefs.removeAllViews();
        addRow(listPrefs, R.drawable.ic_language, R.string.settings_language, languageName(), false, this::pickLanguage);
        addRow(listPrefs, R.drawable.ic_theme, R.string.settings_theme, themeName(), false, this::pickTheme);

        View row = LayoutInflater.from(c).inflate(R.layout.item_setting_switch, listPrefs, false);
        ((ImageView) row.findViewById(R.id.rowIcon)).setImageResource(R.drawable.ic_vibration);
        ((TextView) row.findViewById(R.id.rowTitle)).setText(R.string.settings_vibration);
        ((TextView) row.findViewById(R.id.rowSubtitle)).setText(R.string.settings_vibration_hint);
        SwitchCompat sw = row.findViewById(R.id.rowSwitch);
        sw.setChecked(AppPrefs.vibration(c));
        sw.setOnCheckedChangeListener((b, on) -> AppPrefs.setVibration(c, on));
        row.setOnClickListener(x -> sw.toggle());
        listPrefs.addView(row);
    }

    private String languageName() {
        LocaleListCompat l = AppCompatDelegate.getApplicationLocales();
        if (l.isEmpty() || l.get(0) == null) return getString(R.string.lang_system);
        return "bn".equals(l.get(0).getLanguage()) ? getString(R.string.lang_bn) : getString(R.string.lang_en);
    }

    private void pickLanguage() {
        String[] labels = {getString(R.string.lang_system), getString(R.string.lang_en), getString(R.string.lang_bn)};
        new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.settings_language).setItems(labels, (d, which) -> {
            // The screens restart in the new language by themselves; the choice is remembered by AppCompat
            AppCompatDelegate.setApplicationLocales(which == 0 ? LocaleListCompat.getEmptyLocaleList()
                    : LocaleListCompat.forLanguageTags(which == 1 ? "en" : "bn"));
        }).show();
    }

    private String themeName() {
        switch (AppPrefs.themeMode(requireContext())) {
            case "light":  return getString(R.string.theme_light);
            case "system": return getString(R.string.theme_system);
            default:       return getString(R.string.theme_dark);
        }
    }

    private void pickTheme() {
        String[] labels = {getString(R.string.theme_dark), getString(R.string.theme_light), getString(R.string.theme_system)};
        String[] modes = {"dark", "light", "system"};
        new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.settings_theme).setItems(labels, (d, which) -> {
            AppPrefs.setThemeMode(requireContext(), modes[which]);
            AppCompatDelegate.setDefaultNightMode(AppPrefs.nightMode(requireContext()));   // screens restart in the new theme
        }).show();
    }

    @Override public void onResume() {
        super.onResume();
        if (overlaySwitch == null) return;
        if (pendingEnable && OverlayPermission.isGranted(requireContext())) {
            pendingEnable = false;
            setSwitchSilently(true);
            onOverlayToggled(true);                 // permission was just granted: continue
        } else {
            syncOverlaySwitch();
        }
    }

    // ---- floating bubble switch ----
    private void buildOverlayRow(View root) {
        LinearLayout list = root.findViewById(R.id.listOverlay);
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_setting_switch, list, false);
        ((ImageView) row.findViewById(R.id.rowIcon)).setImageResource(R.drawable.ic_overlay);
        ((TextView) row.findViewById(R.id.rowTitle)).setText(R.string.settings_overlay);
        ((TextView) row.findViewById(R.id.rowSubtitle)).setText(R.string.settings_overlay_hint);
        overlaySwitch = row.findViewById(R.id.rowSwitch);
        row.setOnClickListener(x -> overlaySwitch.toggle());
        overlaySwitch.setOnCheckedChangeListener((b, checked) -> { if (!syncing) onOverlayToggled(checked); });
        list.addView(row);
        syncOverlaySwitch();
    }

    private void setSwitchSilently(boolean on) {
        syncing = true;
        overlaySwitch.setChecked(on);
        syncing = false;
    }

    /** Show the real state: the permission may have been revoked in system settings. */
    private void syncOverlaySwitch() {
        Context c = requireContext();
        if (AppPrefs.overlayEnabled(c) && !OverlayPermission.isGranted(c)) OverlayController.disable(c);
        setSwitchSilently(AppPrefs.overlayEnabled(c));
    }

    private void onOverlayToggled(boolean on) {
        Context c = requireContext();
        if (!on) { pendingEnable = false; OverlayController.disable(c); return; }

        if (!OverlayPermission.isGranted(c)) {                 // 1. "Display over other apps"
            pendingEnable = true;
            setSwitchSilently(false);
            Toast.makeText(c, R.string.overlay_need_permission, Toast.LENGTH_LONG).show();
            OverlayPermission.openSettings(requireActivity());
            return;
        }
        if (Build.VERSION.SDK_INT >= 33                        // 2. Android 13+: ask to show the notification
                && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        OverlayController.enable(c);
    }

    /** Rebuilds the three lists. Rows for links that the admin left empty are simply not shown. */
    private void buildLists() {
        listAccount.removeAllViews();
        addRow(listAccount, R.drawable.ic_user, R.string.settings_profile, null, false,
                () -> startActivity(new Intent(requireContext(), ProfileActivity.class)));
        addRow(listAccount, R.drawable.ic_crown, R.string.settings_premium, null, false,
                () -> Nav.openPremium(requireActivity()));
        if (mamun.sound.fire.util.Session.isAdmin())     // only a convenience: the server re-checks the role on every call
            addRow(listAccount, R.drawable.ic_shield, R.string.settings_admin, null, false,
                    () -> startActivity(new Intent(requireContext(), mamun.sound.fire.ui.admin.AdminActivity.class)));
        addRow(listAccount, R.drawable.ic_key, R.string.settings_change_password, null, false,
                () -> ChangePasswordDialog.show(requireActivity()));
        addRow(listAccount, R.drawable.ic_logout, R.string.settings_logout, null, true,
                () -> Logout.perform(requireActivity()));

        listCommunity.removeAllViews();
        if (settings != null) {
            if (has(settings.telegramChannel)) addTelegramRow(R.string.settings_join_channel, settings.telegramChannel);
            if (has(settings.telegramGroup))   addTelegramRow(R.string.settings_join_group, settings.telegramGroup);
            if (has(settings.telegramAdmin))   addTelegramRow(R.string.settings_contact_admin, settings.telegramAdmin);
        }
        addRow(listCommunity, R.drawable.ic_share, R.string.settings_share_app, null, false, this::shareApp);

        listAbout.removeAllViews();
        addRow(listAbout, R.drawable.ic_info, R.string.settings_version, BuildConfig.VERSION_NAME, false, null);
        if (settings != null) {
            if (has(settings.privacyUrl)) addWebRow(R.string.settings_privacy, settings.privacyUrl);
            if (has(settings.termsUrl))   addWebRow(R.string.settings_terms, settings.termsUrl);
        }
    }

    private void addTelegramRow(@StringRes int title, String link) {
        addRow(listCommunity, R.drawable.ic_send, title, null, false, () -> {
            if (!Links.openTelegram(requireActivity(), link))
                Toast.makeText(requireContext(), R.string.err_cannot_open, Toast.LENGTH_SHORT).show();
        });
    }

    private void addWebRow(@StringRes int title, String url) {
        addRow(listAbout, R.drawable.ic_policy, title, null, false, () -> {
            if (!Links.openWeb(requireActivity(), url))
                Toast.makeText(requireContext(), R.string.err_cannot_open, Toast.LENGTH_SHORT).show();
        });
    }

    /** Share sheet: covers Telegram, WhatsApp, Facebook and anything else installed. */
    private void shareApp() {
        String text = settings != null && has(settings.shareText) ? settings.shareText : getString(R.string.share_app_default);
        Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(send, getString(R.string.settings_share_app)));
    }

    private void addRow(LinearLayout parent, @DrawableRes int icon, @StringRes int title,
                        @Nullable String value, boolean danger, @Nullable Runnable onClick) {
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_setting_row, parent, false);
        ImageView iv = row.findViewById(R.id.rowIcon);
        TextView tv = row.findViewById(R.id.rowTitle), val = row.findViewById(R.id.rowValue);
        iv.setImageResource(icon);
        tv.setText(title);
        if (danger) {
            iv.setColorFilter(requireContext().getColor(R.color.danger));
            tv.setTextColor(requireContext().getColor(R.color.danger));
        }
        if (value != null) { val.setText(value); val.setVisibility(View.VISIBLE); }
        if (onClick != null) row.setOnClickListener(x -> onClick.run());
        else { row.setClickable(false); row.setBackground(null); }
        parent.addView(row);
    }

    private static boolean has(String s) { return s != null && !s.trim().isEmpty(); }
}
