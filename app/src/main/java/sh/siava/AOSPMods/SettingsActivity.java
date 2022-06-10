package sh.siava.AOSPMods;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.nfx.android.rangebarpreference.RangeBarHelper;
import com.topjohnwu.superuser.Shell;

import sh.siava.AOSPMods.Utils.PrefManager;
import sh.siava.AOSPMods.Utils.SystemUtils;

public class SettingsActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static final int REQUEST_IMPORT = 7;
    private static final int REQUEST_EXPORT = 9;
    private static final String TITLE_TAG = "settingsActivityTitle";
    Context DPContext;

    public void backButtonEnabled() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public void backButtonDisabled() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DPContext = this.createDeviceProtectedStorageContext();
        DPContext.moveSharedPreferencesFrom(this, BuildConfig.APPLICATION_ID + "_preferences");
        super.onCreate(savedInstanceState);

        backButtonDisabled();
        createNotificationChannel();

        try {
            Shell.setDefaultBuilder(Shell.Builder.create().setFlags(Shell.FLAG_MOUNT_MASTER)); //access full filesystem
        } catch (Exception ignored) {
        }

        setContentView(R.layout.settings_activity);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new HeaderFragment())
                    .commit();
        } else {
            setTitle(savedInstanceState.getCharSequence(TITLE_TAG));
        }

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                setTitle(R.string.title_activity_settings);
                backButtonDisabled();
            }
        });
    }

    private void createNotificationChannel() {
        CharSequence name = getString(R.string.update_channel_name);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;

        NotificationChannel channel = new NotificationChannel("updates", name, importance);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getTitle() == getString(R.string.title_activity_settings)) {
            backButtonDisabled();
        } else {
            backButtonEnabled();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, getTitle());
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().popBackStackImmediate()) {
            return true;
        }
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit();
        setTitle(pref.getTitle());
        backButtonEnabled();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext().createDeviceProtectedStorageContext());

        int itemID = item.getItemId();

        if (itemID == android.R.id.home) {
            onBackPressed();
        } else if (itemID == R.id.menu_clearPrefs) {
            PrefManager.clearPrefs(prefs);
            SystemUtils.RestartSystemUI();
        } else if (itemID == R.id.menu_exportPrefs) {
            importExportSettings(true);
        } else if (itemID == R.id.menu_importPrefs) {
            importExportSettings(false);
        } else if (itemID == R.id.menu_netstat_clear) {
            clearNetstatClick();
        } else if (itemID == R.id.menu_restart) {
            SystemUtils.Restart();
        } else if (itemID == R.id.menu_restartSysUI) {
            SystemUtils.RestartSystemUI();
        } else if (itemID == R.id.menu_Updates) {
            startActivity(new Intent(this, UpdateActivity.class));
        }
        return true;
    }


    @SuppressLint("ApplySharedPref")
    private void clearNetstatClick() {
        //showinng an alert before taking action
        new AlertDialog.Builder(this).setTitle(R.string.nestat_caution_title)
                .setMessage(R.string.nestat_caution_text)
                .setPositiveButton(R.string.netstat_caution_yes, (dialogInterface, i) -> {
                    try {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext().createDeviceProtectedStorageContext());
                        boolean currentStatus = prefs.getBoolean("NetworkStatsEnabled", false);

                        prefs.edit().putBoolean("NetworkStatsEnabled", false).commit();
                        Shell.cmd("rm -rf /data/user_de/0/com.android.systemui/netStats").exec();
                        Thread.sleep(100);

                        prefs.edit().putBoolean("NetworkStatsEnabled", true).commit();
                        Thread.sleep(100);
                        prefs.edit().putBoolean("NetworkStatsEnabled", currentStatus).apply();
                    } catch (Exception ignored) {
                    }
                })
                .setNegativeButton(R.string.netstat_caution_no, (dialogInterface, i) -> {/*nothing happens*/})
                .setCancelable(true)
                .create()
                .show();
    }

    private void importExportSettings(boolean export) {
        Intent fileIntent = new Intent();
        fileIntent.setAction(export ? Intent.ACTION_CREATE_DOCUMENT : Intent.ACTION_GET_CONTENT);
        fileIntent.setType("*/*");
        startActivityForResult(fileIntent, export ? REQUEST_EXPORT : REQUEST_IMPORT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data == null) return; //user hit cancel. Nothing to do

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext().createDeviceProtectedStorageContext());
        switch (requestCode) {
            case REQUEST_IMPORT:
                try {
                    PrefManager.importPath(prefs,
                            getContentResolver().openInputStream(data.getData()));
                    SystemUtils.RestartSystemUI();
                } catch (Exception ignored) {
                }
                break;
            case REQUEST_EXPORT:
                try {
                    PrefManager.exportPrefs(prefs,
                            getContentResolver().openOutputStream(data.getData()));
                } catch (Exception ignored) {
                }
                break;
        }
    }

    public static class HeaderFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setStorageDeviceProtected();
            setPreferencesFromResource(R.xml.header_preferences, rootKey);
        }
    }

    public static class NavFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setStorageDeviceProtected();
            setPreferencesFromResource(R.xml.nav_prefs, rootKey);
        }

    }

    @SuppressWarnings("ConstantConditions")
    public static class ThemingFragment extends PreferenceFragmentCompat {

        SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, s) -> updateFontPrefs(sharedPreferences);

        @SuppressLint("ApplySharedPref")
        private void updateFontPrefs(SharedPreferences sharedPreferences) {
            try {

                boolean customFontsEnabled = sharedPreferences.getBoolean("enableCustomFonts", false);

                if (!customFontsEnabled) {
                    sharedPreferences.edit().putString("FontsOverlayEx", "None").commit();
                }

                boolean gSansOverride = sharedPreferences.getBoolean("gsans_override", false);
                boolean FontsOverlayExEnabled = !sharedPreferences.getString("FontsOverlayEx", "None").equals("None");

                findPreference("gsans_override").setVisible(customFontsEnabled && !FontsOverlayExEnabled);
                findPreference("FontsOverlayEx").setVisible(customFontsEnabled && !gSansOverride);

            } catch (Exception ignored) {
            }

        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setStorageDeviceProtected();
            setPreferencesFromResource(R.xml.theming_prefs, rootKey);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().createDeviceProtectedStorageContext());
            updateFontPrefs(prefs);
            prefs.registerOnSharedPreferenceChangeListener(listener);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static class LockScreenFragment extends PreferenceFragmentCompat {

        SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, s) -> updateVisibility(sharedPreferences);

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setStorageDeviceProtected();
            setPreferencesFromResource(R.xml.lock_screen_prefs, rootKey);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().createDeviceProtectedStorageContext());
            updateVisibility(prefs);
            prefs.registerOnSharedPreferenceChangeListener(listener);
        }

        private void updateVisibility(SharedPreferences sharedPreferences) {
            findPreference("carrierTextValue").setVisible(sharedPreferences.getBoolean("carrierTextMod", false));
            findPreference("albumArtLockScreenBlurLevel").setSummary(sharedPreferences.getInt("albumArtLockScreenBlurLevel",0) + "%");
            findPreference("albumArtLockScreenBlurLevel").setVisible(sharedPreferences.getBoolean("albumArtLockScreenEnabled",false));
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static class SBBBFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setStorageDeviceProtected();
            setPreferencesFromResource(R.xml.statusbar_batterybar_prefs, rootKey);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().createDeviceProtectedStorageContext());
            prefs.registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> updateVisibility(prefs));
            updateVisibility(prefs);
        }

        private void updateVisibility(SharedPreferences prefs) {
            try {
                String json = prefs.getString("batteryWarningRange", "");
                boolean critZero = false;
                boolean warnZero = false;

                if(!json.isEmpty())
                {
                    critZero = RangeBarHelper.getLowValueFromJsonString(json) == 0;
                    warnZero = RangeBarHelper.getHighValueFromJsonString(json) == 0;
                }
                boolean bBarEnabled = prefs.getBoolean("BBarEnabled", false);
                boolean isColorful = prefs.getBoolean("BBarColorful", false);
                boolean transitColors = prefs.getBoolean("BBarTransitColors", false);

                findPreference("batteryFastChargingColor").setVisible(prefs.getBoolean("indicateFastCharging", false) && bBarEnabled);
                findPreference("batteryChargingColor").setVisible(prefs.getBoolean("indicateCharging", false) && bBarEnabled);
                findPreference("batteryWarningColor").setVisible(!warnZero && bBarEnabled);
                findPreference("batteryCriticalColor").setVisible((!critZero || transitColors) && bBarEnabled && findPreference("batteryWarningColor").isVisible());

                findPreference("BBarTransitColors").setVisible(bBarEnabled && !isColorful);
                findPreference("BBOnlyWhileCharging").setVisible(bBarEnabled);
                findPreference("BBOnBottom").setVisible(bBarEnabled);
                findPreference("BBarColorful").setVisible(bBarEnabled);
                findPreference("BBOpacity").setVisible(bBarEnabled);
                findPreference("BBOpacity").setSummary(prefs.getInt("BBOpacity", 100) + "%");
                findPreference("BBarHeight").setVisible(bBarEnabled);
                findPreference("BBarHeight").setSummary(prefs.getInt("BBarHeight", 50) + "%");
                findPreference("BBSetCentered").setVisible(bBarEnabled);
                findPreference("indicateCharging").setVisible(bBarEnabled);
                findPreference("indicateFastCharging").setVisible(bBarEnabled);
                findPreference("batteryWarningRange").setVisible(bBarEnabled);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @SuppressWarnings("ConstantConditions")
    public static class networkFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setStorageDeviceProtected();
            setPreferencesFromResource(R.xml.sbqs_network_prefs, rootKey);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().createDeviceProtectedStorageContext());
            prefs.registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> updateVisibility(prefs));
            updateVisibility(prefs);
        }

        private void updateVisibility(SharedPreferences prefs) {
            try {
                findPreference("networkTrafficRXTop").setVisible(prefs.getString("networkTrafficMode", "0").equals("0"));
                findPreference("networkTrafficColorful").setVisible(!prefs.getString("networkTrafficMode", "0").equals("3"));
                boolean colorful = prefs.getBoolean("networkTrafficColorful", true);
                findPreference("networkTrafficDLColor").setVisible(colorful);
                findPreference("networkTrafficULColor").setVisible(colorful);
                findPreference("networkTrafficInterval").setSummary(prefs.getInt("networkTrafficInterval", 1) + " second(s)");

            } catch (Exception ignored) {
            }
        }
    }


    @SuppressWarnings("ConstantConditions")
    public static class SBBIconFragment extends PreferenceFragmentCompat {

        SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> updateVisibility(sharedPreferences);

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setStorageDeviceProtected();
            setPreferencesFromResource(R.xml.statusbar_batteryicon_prefs, rootKey);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().createDeviceProtectedStorageContext());
            prefs.registerOnSharedPreferenceChangeListener(listener);
            updateVisibility(prefs);
        }

        private void updateVisibility(SharedPreferences prefs) {
            try {
                findPreference("BatteryIconScaleFactor").setSummary(prefs.getInt("BatteryIconScaleFactor", 50) * 2 + getString(R.string.battery_size_summary));
                findPreference("BatteryIconScaleFactor").setSummary(prefs.getInt("BatteryIconScaleFactor", 50) * 2 + getString(R.string.battery_size_summary));

                int style = Integer.parseInt(prefs.getString("BatteryStyle", "0"));
                String json = prefs.getString("BIconbatteryWarningRange", "");
                boolean critZero = RangeBarHelper.getLowValueFromJsonString(json) == 0;
                boolean warnZero = RangeBarHelper.getHighValueFromJsonString(json) == 0;
                boolean colorful = prefs.getBoolean("BIconColorful", false);
                findPreference("DualToneBatteryOverlay").setVisible(style == 0);
                findPreference("BIconOpacity").setVisible(style > 0 && style < 99);
                findPreference("BIconOpacity").setSummary(prefs.getInt("BIconOpacity", 100) + "%");
                findPreference("BatteryIconScaleFactor").setVisible(style < 99);
                findPreference("BatteryShowPercent").setVisible(style == 1 || style == 2);
                findPreference("BIconindicateCharging").setVisible(style == 3);
                findPreference("batteryIconChargingColor").setVisible(style == 3 && prefs.getBoolean("BIconindicateCharging", false));
                findPreference("BIconindicateFastCharging").setVisible(style > 0 && style < 99);
                findPreference("batteryIconFastChargingColor").setVisible(style > 0 && style < 99 && prefs.getBoolean("BIconindicateFastCharging", false));
                findPreference("BIconColorful").setVisible(style > 0 && style < 99 && !prefs.getBoolean("BIconTransitColors", false));
                findPreference("BIconTransitColors").setVisible(style > 0 && style < 99 && !prefs.getBoolean("BIconColorful", false));
                findPreference("BIconbatteryWarningRange").setVisible(style > 0 && style < 99);
                findPreference("BIconbatteryCriticalColor").setVisible(style > 0 && style < 99 && (colorful || !critZero));
                findPreference("BIconbatteryWarningColor").setVisible(style > 0 && style < 99 && (colorful || !warnZero));
            } catch (Exception ignored) {
            }
        }
    }


    public static class MiscFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setStorageDeviceProtected();
            setPreferencesFromResource(R.xml.misc_prefs, rootKey);

        }
    }

    @SuppressWarnings("ConstantConditions")
    public static class SBCFragment extends PreferenceFragmentCompat {

        SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> updateVisibility(sharedPreferences);

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setStorageDeviceProtected();
            setPreferencesFromResource(R.xml.statusbar_clock_prefs, rootKey);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().createDeviceProtectedStorageContext());
            prefs.registerOnSharedPreferenceChangeListener(listener);
            updateVisibility(prefs);
        }

        private void updateVisibility(SharedPreferences prefs) {
            boolean colorfullEnabled = prefs.getBoolean("SBCClockColorful", false);
            findPreference("SBCBeforeClockColor").setVisible(colorfullEnabled);
            findPreference("SBCClockColor").setVisible(colorfullEnabled);
            findPreference("SBCAfterClockColor").setVisible(colorfullEnabled);
        }
    }

    public static class ThreeButtonNavFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setStorageDeviceProtected();
            setPreferencesFromResource(R.xml.three_button_prefs, rootKey);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static class StatusbarFragment extends PreferenceFragmentCompat {

        SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, s) -> setVisibility(sharedPreferences);

        private void setVisibility(SharedPreferences sharedPreferences) {
            try {
                boolean networkOnSBEnabled = sharedPreferences.getBoolean("networkOnSBEnabled", false);
                findPreference("network_settings_header").setVisible(networkOnSBEnabled);
                findPreference("networkTrafficPosition").setVisible(networkOnSBEnabled);

                findPreference("centerAreaFineTune").setSummary((sharedPreferences.getInt("centerAreaFineTune", 50) - 50) + "%");
            } catch (Exception ignored) {
            }
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setStorageDeviceProtected();
            setPreferencesFromResource(R.xml.statusbar_settings, rootKey);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().createDeviceProtectedStorageContext());
            setVisibility(prefs);
            prefs.registerOnSharedPreferenceChangeListener(listener);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static class QuicksettingsFragment extends PreferenceFragmentCompat {

        SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> updateVisibililty(sharedPreferences);

        private void updateVisibililty(SharedPreferences sharedPreferences) {
            try {
                boolean QSPullodwnEnabled = sharedPreferences.getBoolean("QSPullodwnEnabled", false);

                findPreference("QSPulldownPercent").setVisible(QSPullodwnEnabled);
                findPreference("QSPulldownSide").setVisible(QSPullodwnEnabled);

                findPreference("BSThickTrackOverlay").setVisible(!sharedPreferences.getBoolean("QSBrightnessDisabled", false));
                findPreference("BrightnessSlierOnBottom").setVisible(!sharedPreferences.getBoolean("QSBrightnessDisabled", false));
                findPreference("QQSBrightnessEnabled").setVisible(!sharedPreferences.getBoolean("QSBrightnessDisabled", false));
                findPreference("QSFooterText").setVisible(sharedPreferences.getBoolean("QSFooterMod", false));
                findPreference("QSPulldownPercent").setSummary(sharedPreferences.getInt("QSPulldownPercent", 25) + "%");
                findPreference("dualToneQSEnabled").setVisible(sharedPreferences.getBoolean("LightQSPanel", false));

                findPreference("network_settings_header").setVisible(sharedPreferences.getBoolean("networkOnQSEnabled", false));

            } catch (Exception ignored) {
            }

        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setStorageDeviceProtected();
            setPreferencesFromResource(R.xml.quicksettings_prefs, rootKey);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().createDeviceProtectedStorageContext());
            updateVisibililty(prefs);

            prefs.registerOnSharedPreferenceChangeListener(listener);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static class GestureNavFragment extends PreferenceFragmentCompat {

        FrameLayout leftGestureIndicator, rightGestureIndicator;

        SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> updateVisibility(sharedPreferences);

        private void updateVisibility(SharedPreferences sharedPreferences) {
            try {
                boolean HideNavbarOverlay = sharedPreferences.getBoolean("HideNavbarOverlay", false);

                int displayHeight =  getActivity().getWindowManager().getCurrentWindowMetrics().getBounds().height();

                findPreference("GesPillWidthModPos").setSummary(sharedPreferences.getInt("GesPillWidthModPos", 50) * 2 + getString(R.string.pill_width_summary));
                findPreference("GesPillHeightFactor").setSummary(sharedPreferences.getInt("GesPillHeightFactor", 100) + getString(R.string.pill_width_summary));

                findPreference("BackLeftHeight").setVisible(sharedPreferences.getBoolean("BackFromLeft", true));
                findPreference("BackRightHeight").setVisible(sharedPreferences.getBoolean("BackFromRight", true));
                findPreference("BackLeftHeight").setSummary(sharedPreferences.getInt("BackLeftHeight", 100) + "%");
                findPreference("BackRightHeight").setSummary(sharedPreferences.getInt("BackRightHeight", 100) + "%");

                findPreference("nav_pill_cat").setVisible(!HideNavbarOverlay);
                findPreference("nav_keyboard_height_cat").setVisible(!HideNavbarOverlay);

                rightGestureIndicator.setVisibility(findPreference("BackRightHeight").isVisible() ? View.VISIBLE : View.GONE);
                leftGestureIndicator.setVisibility(findPreference("BackLeftHeight").isVisible() ? View.VISIBLE : View.GONE);

                int edgeHeight = Math.round(displayHeight * sharedPreferences.getInt("BackRightHeight",100)/100f);
                ViewGroup.LayoutParams lp = rightGestureIndicator.getLayoutParams();
                lp.height = edgeHeight;
                rightGestureIndicator.setLayoutParams(lp);

                edgeHeight = Math.round(displayHeight * sharedPreferences.getInt("BackLeftHeight",100)/100f);
                lp = leftGestureIndicator.getLayoutParams();
                lp.height = edgeHeight;
                leftGestureIndicator.setLayoutParams(lp);
            } catch (Exception ignored) {}
        }

        @SuppressLint("RtlHardcoded")
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            rightGestureIndicator = prepareGestureView(Gravity.RIGHT);
            leftGestureIndicator = prepareGestureView(Gravity.LEFT);

            getPreferenceManager().setStorageDeviceProtected();
            setPreferencesFromResource(R.xml.gesture_nav_perfs, rootKey);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().createDeviceProtectedStorageContext());
            updateVisibility(prefs);

            prefs.registerOnSharedPreferenceChangeListener(listener);
        }

        private FrameLayout prepareGestureView(int gravity) {
            int navigationBarHeight = 0;
            int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                navigationBarHeight = getContext().getResources().getDimensionPixelSize(resourceId);
            }

            FrameLayout result = new FrameLayout(getContext());
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(50, 0);
            lp.gravity = gravity | Gravity.BOTTOM;
            lp.bottomMargin = navigationBarHeight;
            result.setLayoutParams(lp);
            result.setBackgroundColor(Color.BLACK);
            result.setAlpha(.5f);
            result.setVisibility(View.VISIBLE);

            ((ViewGroup)getActivity().getWindow().getDecorView().getRootView()).addView(result);
            return result;
        }

        @Override
        public void onDestroy()
        {
            ((ViewGroup) rightGestureIndicator.getParent()).removeView(rightGestureIndicator);
            ((ViewGroup) leftGestureIndicator.getParent()).removeView(leftGestureIndicator);

            super.onDestroy();
        }

    }

    @SuppressWarnings("ConstantConditions")
    public static class NetworkStatFragment extends PreferenceFragmentCompat {

        SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> updateVisibility(sharedPreferences);

        private void updateVisibility(SharedPreferences sharedPreferences) {
            try {
                boolean netstatEnabled = sharedPreferences.getBoolean("NetworkStatsEnabled", false);
                boolean networkTrafficColorful = sharedPreferences.getBoolean("networkStatsColorful", false);

                findPreference("netstatSaveInterval").setSummary(String.format("%s %s", sharedPreferences.getInt("netstatSaveInterval", 5), getString(R.string.minutes_word)));

                findPreference("networkStatsColorful").setVisible(netstatEnabled);
                findPreference("netstatSaveInterval").setVisible(netstatEnabled);
                findPreference("networkStatDLColor").setVisible(netstatEnabled && networkTrafficColorful);
                findPreference("networkStatULColor").setVisible(netstatEnabled && networkTrafficColorful);

            } catch (Exception ignored) {
            }
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setStorageDeviceProtected();
            setPreferencesFromResource(R.xml.lsqs_custom_text, rootKey);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().createDeviceProtectedStorageContext());
            updateVisibility(prefs);

            prefs.registerOnSharedPreferenceChangeListener(listener);
        }
    }
}