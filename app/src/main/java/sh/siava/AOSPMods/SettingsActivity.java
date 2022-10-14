package sh.siava.AOSPMods;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.slider.LabelFormatter;
import com.topjohnwu.superuser.Shell;

import java.util.List;
import java.util.Locale;

import sh.siava.AOSPMods.utils.PrefManager;
import sh.siava.AOSPMods.utils.SystemUtils;
import sh.siava.rangesliderpreference.RangeSliderPreference;

public class SettingsActivity extends AppCompatActivity implements
		PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

	public static final int FULL_VERSION = 0;
	public static final int XPOSED_ONLY = 1;

	private static final int REQUEST_IMPORT = 7;
	private static final int REQUEST_EXPORT = 9;
	private static final String TITLE_TAG = "settingsActivityTitle";
	Context DPContext;

	@SuppressWarnings("FieldCanBeLocal")
	public static int moduleType = XPOSED_ONLY;

	public static boolean showOverlays, showFonts;

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
	public boolean onCreateOptionsMenu(@NonNull Menu menu) {
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

		moduleType = getVersionType();

		showOverlays = moduleType == FULL_VERSION;
		showFonts = moduleType == FULL_VERSION;

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

	@Override
	protected void attachBaseContext(Context newBase) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(newBase.createDeviceProtectedStorageContext());

		String localeCode = prefs.getString("appLanguage", "");

		if (!localeCode.isEmpty()) {
			Locale locale = Locale.forLanguageTag(localeCode);

			Resources res = newBase.getResources();
			Configuration configuration = res.getConfiguration();

			configuration.setLocale(locale);

			LocaleList localeList = new LocaleList(locale);
			LocaleList.setDefault(localeList);
			configuration.setLocales(localeList);

			newBase = newBase.createConfigurationContext(configuration);
		}

		super.attachBaseContext(newBase);
	}

	public static int getVersionType() {
		try {
			return Integer.parseInt(Shell.cmd(String.format("cat %s/build.type", "/data/adb/modules/AOSPMods")).exec().getOut().get(0));
		} catch (Exception ignored) {
			return XPOSED_ONLY;
		}
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
	public void onSaveInstanceState(@NonNull Bundle outState) {
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
	public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, Preference pref) {
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
		//showing an alert before taking action
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

	@SuppressWarnings("ConstantConditions")
	public static class HeaderFragment extends PreferenceFragmentCompat {

		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			getPreferenceManager().setStorageDeviceProtected();
			setPreferencesFromResource(R.xml.header_preferences, rootKey);

			updateVisibility();
		}

		private void updateVisibility() {
			findPreference("theming_header").setVisible(showOverlays);
		}
	}

	@SuppressWarnings("ConstantConditions")
	public static class NavFragment extends PreferenceFragmentCompat {
		SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, s) -> updateVisibility(sharedPreferences);

		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			getPreferenceManager().setStorageDeviceProtected();
			setPreferencesFromResource(R.xml.nav_prefs, rootKey);
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().createDeviceProtectedStorageContext());
			prefs.registerOnSharedPreferenceChangeListener(listener);
			updateVisibility(prefs);
		}

		private void updateVisibility(SharedPreferences prefs) {
			findPreference("HideNavbarOverlay").setVisible(showOverlays);

			int taskBarMode = Integer.parseInt(prefs.getString("taskBarMode", "0"));
			findPreference("TaskbarAsRecents").setVisible(taskBarMode == 1);
			findPreference("taskbarHeightOverride").setVisible(taskBarMode == 1);
			findPreference("TaskbarRadiusOverride").setVisible(taskBarMode == 1);

			float taskbarHeightOverride = 100f;
			try {
				taskbarHeightOverride = RangeSliderPreference.getValues(prefs, "taskbarHeightOverride", 100f).get(0);
			} catch (Throwable ignored) {
			}
			findPreference("taskbarHeightOverride").setSummary(
					taskbarHeightOverride != 100f
							? taskbarHeightOverride + "%"
							: getString(R.string.word_default)
			);
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
			findPreference("album_art_category").setVisible(Build.VERSION.SDK_INT < 33);
			findPreference("carrierTextValue").setVisible(sharedPreferences.getBoolean("carrierTextMod", false));
			findPreference("albumArtLockScreenBlurLevel").setSummary(sharedPreferences.getInt("albumArtLockScreenBlurLevel", 0) + "%");
			findPreference("albumArtLockScreenBlurLevel").setVisible(sharedPreferences.getBoolean("albumArtLockScreenEnabled", false));

			float KeyGuardDimAmount = -1;
			try {
				KeyGuardDimAmount = RangeSliderPreference.getValues(sharedPreferences, "KeyGuardDimAmount", -1).get(0);
			} catch (Exception ignored) {
			}
			findPreference("KeyGuardDimAmount").setSummary(
					KeyGuardDimAmount < 0
							? getString(R.string.word_default)
							: KeyGuardDimAmount + "%");

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
				boolean critZero = false, warnZero = false;
				List<Float> BBarLevels = RangeSliderPreference.getValues(prefs, "batteryWarningRange", 0);

				if (!BBarLevels.isEmpty()) {
					critZero = BBarLevels.get(0) == 0;
					warnZero = BBarLevels.get(1) == 0;
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
			} catch (Exception ignore) {
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

				boolean critZero = false, warnZero = false;
				List<Float> BIconLevels = RangeSliderPreference.getValues(prefs, "BIconbatteryWarningRange", 0);

				if (!BIconLevels.isEmpty()) {
					critZero = BIconLevels.get(0) == 0;
					warnZero = BIconLevels.get(1) == 0;
				}

				boolean colorful = prefs.getBoolean("BIconColorful", false);

				findPreference("DualToneBatteryOverlay").setVisible(style == 0 && showOverlays);
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


	@SuppressWarnings("ConstantConditions")
	public static class MiscFragment extends PreferenceFragmentCompat {
		SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> updateVisibility(sharedPreferences);

		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			getPreferenceManager().setStorageDeviceProtected();
			setPreferencesFromResource(R.xml.misc_prefs, rootKey);
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().createDeviceProtectedStorageContext());
			prefs.registerOnSharedPreferenceChangeListener(listener);
			updateVisibility(prefs);

		}

		private void updateVisibility(SharedPreferences prefs) {
			try {
				int volumeStps = prefs.getInt("volumeStps", 0);
				findPreference("volumeStps").setSummary(String.format("%s - (%s)",
						volumeStps == 10
								? getString(R.string.word_default)
								: String.valueOf(volumeStps),
						getString(R.string.restart_needed)));

				findPreference("CustomThemedIconsOverlay").setVisible(showOverlays);

				float displayOverride = 100;
				try {
					displayOverride = RangeSliderPreference.getValues(prefs, "displayOverride", 100f).get(0);
				} catch (Exception ignored) {
				}
				double increasedArea = Math.round(Math.abs(Math.pow(displayOverride, 2) / 100 - 100));

				findPreference("displayOverride").setSummary(String.format("%s \n (%s)",
						displayOverride == 100
								? getString(R.string.word_default)
								: String.format("%s%% - %s%% %s",
								String.valueOf(displayOverride),
								String.valueOf(increasedArea),
								displayOverride > 100
										? getString(R.string.more_area)
										: getString(R.string.less_area)),
						getString(R.string.sysui_restart_needed)));
			} catch (Exception e) {
				e.printStackTrace();
			}
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

	@SuppressWarnings("ConstantConditions")
	public static class ThreeButtonNavFragment extends PreferenceFragmentCompat {

		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			getPreferenceManager().setStorageDeviceProtected();
			setPreferencesFromResource(R.xml.three_button_prefs, rootKey);
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().createDeviceProtectedStorageContext());
			prefs.registerOnSharedPreferenceChangeListener(listener);
			updateVisibility(prefs);

		}

		SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> updateVisibility(sharedPreferences);

		private void updateVisibility(SharedPreferences prefs) {
			try {
				boolean ThreeButtonLayoutMod = prefs.getBoolean("ThreeButtonLayoutMod", false);
				findPreference("ThreeButtonLeft").setVisible(ThreeButtonLayoutMod);
				findPreference("ThreeButtonCenter").setVisible(ThreeButtonLayoutMod);
				findPreference("ThreeButtonRight").setVisible(ThreeButtonLayoutMod);

				findPreference("BackLongPressKill").setVisible(Build.VERSION.SDK_INT < 33);
			} catch (Exception ignored) {
			}
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

				int statusbarHeightFactor = sharedPreferences.getInt("statusbarHeightFactor", 100);
				findPreference("statusbarHeightFactor").setSummary(statusbarHeightFactor == 100 ? getResources().getString(R.string.word_default) : statusbarHeightFactor + "%");
				findPreference("centerAreaFineTune").setSummary((sharedPreferences.getInt("centerAreaFineTune", 50) - 50) + "%");

				findPreference("systemIconSortPlan").setVisible(sharedPreferences.getBoolean("systemIconsMultiRow", false));

				findPreference("UnreadMessagesNumberOverlay").setVisible(showOverlays);
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
	public static class QuickSettingsFragment extends PreferenceFragmentCompat {
		LabelFormatter formatter = value -> (value + 100) + "%";

		SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> updateVisibililty(sharedPreferences);
		private FrameLayout pullDownIndicator;

		@SuppressLint("RtlHardcoded")
		private void updateVisibililty(SharedPreferences sharedPreferences) {
			try {
				boolean QSPullodwnEnabled = sharedPreferences.getBoolean("QSPullodwnEnabled", false);

				int displayWidth = getActivity().getWindowManager().getCurrentWindowMetrics().getBounds().width();

				findPreference("QSPulldownPercent").setVisible(QSPullodwnEnabled);
				findPreference("QSPulldownSide").setVisible(QSPullodwnEnabled);

				findPreference("BSThickTrackOverlay").setVisible(!sharedPreferences.getBoolean("QSBrightnessDisabled", false) && showOverlays);
				findPreference("BrightnessSlierOnBottom").setVisible(!sharedPreferences.getBoolean("QSBrightnessDisabled", false));
				findPreference("QQSBrightnessEnabled").setVisible(!sharedPreferences.getBoolean("QSBrightnessDisabled", false));
				findPreference("QSFooterText").setVisible(sharedPreferences.getBoolean("QSFooterMod", false));
				findPreference("QSPulldownPercent").setSummary(sharedPreferences.getInt("QSPulldownPercent", 25) + "%");
				findPreference("dualToneQSEnabled").setVisible(sharedPreferences.getBoolean("LightQSPanel", false));

				findPreference("network_settings_header").setVisible(sharedPreferences.getBoolean("networkOnQSEnabled", false));

				pullDownIndicator.setVisibility(findPreference("QSPulldownPercent").isVisible() ? View.VISIBLE : View.GONE);
				FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) pullDownIndicator.getLayoutParams();
				lp.width = Math.round(sharedPreferences.getInt("QSPulldownPercent", 25) * displayWidth / 100f);
				lp.gravity = Gravity.TOP | (Integer.parseInt(sharedPreferences.getString("QSPulldownSide", "1")) == 1 ? Gravity.RIGHT : Gravity.LEFT);
				pullDownIndicator.setLayoutParams(lp);

				int QSRowQty = sharedPreferences.getInt("QSRowQty", 0);
				findPreference("QSRowQty").setSummary((QSRowQty == 0) ? getResources().getString(R.string.word_default) : String.valueOf(QSRowQty));

				int QSColQty = sharedPreferences.getInt("QSColQty", 0);
				findPreference("QSColQty").setSummary((QSColQty == 0) ? getResources().getString(R.string.word_default) : String.valueOf(QSColQty));

				int QQSTileQty = sharedPreferences.getInt("QQSTileQty", 4);
				findPreference("QQSTileQty").setSummary((QQSTileQty == 4) ? getResources().getString(R.string.word_default) : String.valueOf(QQSTileQty));

				float QSLabelScaleFactor = 0;
				try {
					QSLabelScaleFactor = RangeSliderPreference.getValues(sharedPreferences, "QSLabelScaleFactor", 0f).get(0);
				} catch (Exception ignored) {
				}
				findPreference("QSLabelScaleFactor").setSummary((QSLabelScaleFactor + 100) + "% " + getString(R.string.toggle_dark_apply));

				float QSSecondaryLabelScaleFactor = 0;
				try {
					QSSecondaryLabelScaleFactor = RangeSliderPreference.getValues(sharedPreferences, "QSSecondaryLabelScaleFactor", 0f).get(0);
				} catch (Exception ignored) {
				}

				findPreference("QSSecondaryLabelScaleFactor").setSummary((QSSecondaryLabelScaleFactor + 100) + "% " + getString(R.string.toggle_dark_apply));

				try {
					((RangeSliderPreference) findPreference("QSLabelScaleFactor")).slider.setLabelFormatter(formatter);
				} catch (Exception ignored) {
				}
				try {
					((RangeSliderPreference) findPreference("QSSecondaryLabelScaleFactor")).slider.setLabelFormatter(formatter);
				} catch (Exception ignored) {
				}

				if (!showOverlays && sharedPreferences.getBoolean("BSThickTrackOverlay", false)) {
					sharedPreferences.edit().putBoolean("BSThickTrackOverlay", false).apply();
				}
				findPreference("QSTilesThemesOverlayEx").setVisible(showOverlays);

				findPreference("leveledFlashTile").setVisible(Build.VERSION.SDK_INT >= 33);
				findPreference("isFlashLevelGlobal").setVisible(findPreference("leveledFlashTile").isVisible() && sharedPreferences.getBoolean("leveledFlashTile", false));

				findPreference("QRTileInactiveColor").setVisible(Build.VERSION.SDK_INT >= 33);
			} catch (Exception ignored) {
			}
		}

		@Override
		public void onDestroy() {
			((ViewGroup) pullDownIndicator.getParent()).removeView(pullDownIndicator);
			super.onDestroy();
		}

		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			createPullDownIndicator();

			getPreferenceManager().setStorageDeviceProtected();
			setPreferencesFromResource(R.xml.quicksettings_prefs, rootKey);
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().createDeviceProtectedStorageContext());
			updateVisibililty(prefs);

			prefs.registerOnSharedPreferenceChangeListener(listener);
		}

		private void createPullDownIndicator() {
			pullDownIndicator = new FrameLayout(getContext());
			FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(0, 25);
			lp.gravity = Gravity.TOP;

			pullDownIndicator.setLayoutParams(lp);
			pullDownIndicator.setBackgroundColor(getContext().getColor(android.R.color.system_accent1_200));
			pullDownIndicator.setAlpha(.7f);
			pullDownIndicator.setVisibility(View.VISIBLE);

			((ViewGroup) getActivity().getWindow().getDecorView().getRootView()).addView(pullDownIndicator);
		}
	}

	@SuppressWarnings("ConstantConditions")
	public static class GestureNavFragment extends PreferenceFragmentCompat {

		FrameLayout leftBackGestureIndicator, rightBackGestureIndicator;
		FrameLayout leftSwipeGestureIndicator, rightSwipeGestureIndicator;

		SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> updateVisibility(sharedPreferences);

		private void updateVisibility(SharedPreferences sharedPreferences) {
			try {
				boolean HideNavbarOverlay = sharedPreferences.getBoolean("HideNavbarOverlay", false);

				int displayHeight = getActivity().getWindowManager().getCurrentWindowMetrics().getBounds().height();
				int displayWidth = getActivity().getWindowManager().getCurrentWindowMetrics().getBounds().width();

				findPreference("GesPillWidthModPos").setSummary(sharedPreferences.getInt("GesPillWidthModPos", 50) * 2 + getString(R.string.pill_width_summary));
				findPreference("GesPillHeightFactor").setSummary(sharedPreferences.getInt("GesPillHeightFactor", 100) + getString(R.string.pill_width_summary));

				findPreference("BackLeftHeight").setVisible(sharedPreferences.getBoolean("BackFromLeft", true));
				findPreference("BackRightHeight").setVisible(sharedPreferences.getBoolean("BackFromRight", true));
				findPreference("BackLeftHeight").setSummary(sharedPreferences.getInt("BackLeftHeight", 100) + "%");
				findPreference("BackRightHeight").setSummary(sharedPreferences.getInt("BackRightHeight", 100) + "%");

				findPreference("leftSwipeUpPercentage").setVisible(!sharedPreferences.getString("leftSwipeUpAction", "-1").equals("-1"));
				findPreference("rightSwipeUpPercentage").setVisible(!sharedPreferences.getString("rightSwipeUpAction", "-1").equals("-1"));

				float leftSwipeUpPercentage = 25f;
				try {
					leftSwipeUpPercentage = RangeSliderPreference.getValues(sharedPreferences, "leftSwipeUpPercentage", 25).get(0);
				} catch (Exception ignored) {
				}
				findPreference("leftSwipeUpPercentage").setSummary(leftSwipeUpPercentage + "%");

				float rightSwipeUpPercentage = 25f;
				try {
					rightSwipeUpPercentage = RangeSliderPreference.getValues(sharedPreferences, "rightSwipeUpPercentage", 25).get(0);
				} catch (Exception ignored) {
				}
				findPreference("rightSwipeUpPercentage").setSummary(rightSwipeUpPercentage + "%");

				float swipeUpPercentage = 20f;
				try {
					swipeUpPercentage = RangeSliderPreference.getValues(sharedPreferences, "swipeUpPercentage", 20).get(0);
				} catch (Exception ignored) {
				}
				findPreference("swipeUpPercentage").setSummary(swipeUpPercentage + "%");

				int edgeWidth = Math.round(displayWidth * leftSwipeUpPercentage / 100f);
				ViewGroup.LayoutParams lp = leftSwipeGestureIndicator.getLayoutParams();
				lp.width = edgeWidth;
				leftSwipeGestureIndicator.setLayoutParams(lp);

				edgeWidth = Math.round(displayWidth * rightSwipeUpPercentage / 100f);
				lp = rightSwipeGestureIndicator.getLayoutParams();
				lp.width = edgeWidth;
				rightSwipeGestureIndicator.setLayoutParams(lp);

				setVisibility(rightSwipeGestureIndicator, findPreference("rightSwipeUpPercentage").isVisible(), 400);
				setVisibility(leftSwipeGestureIndicator, findPreference("leftSwipeUpPercentage").isVisible(), 400);

				findPreference("nav_pill_cat").setVisible(!HideNavbarOverlay);
				findPreference("nav_keyboard_height_cat").setVisible(!HideNavbarOverlay);

				setVisibility(rightBackGestureIndicator, findPreference("BackRightHeight").isVisible(), 400);
				setVisibility(leftBackGestureIndicator, findPreference("BackLeftHeight").isVisible(), 400);

				int edgeHeight = Math.round(displayHeight * sharedPreferences.getInt("BackRightHeight", 100) / 100f);
				lp = rightBackGestureIndicator.getLayoutParams();
				lp.height = edgeHeight;
				rightBackGestureIndicator.setLayoutParams(lp);

				edgeHeight = Math.round(displayHeight * sharedPreferences.getInt("BackLeftHeight", 100) / 100f);
				lp = leftBackGestureIndicator.getLayoutParams();
				lp.height = edgeHeight;
				leftBackGestureIndicator.setLayoutParams(lp);

				findPreference("nav_keyboard_height_cat").setVisible(showOverlays);
			} catch (Exception ignored) {
			}
		}

		@SuppressLint("RtlHardcoded")
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			rightBackGestureIndicator = prepareBackGestureView(Gravity.RIGHT);
			leftBackGestureIndicator = prepareBackGestureView(Gravity.LEFT);

			rightSwipeGestureIndicator = prepareSwipeGestureView(Gravity.RIGHT);
			leftSwipeGestureIndicator = prepareSwipeGestureView(Gravity.LEFT);

			getPreferenceManager().setStorageDeviceProtected();
			setPreferencesFromResource(R.xml.gesture_nav_prefs, rootKey);
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().createDeviceProtectedStorageContext());
			updateVisibility(prefs);

			prefs.registerOnSharedPreferenceChangeListener(listener);
		}

		private FrameLayout prepareSwipeGestureView(int gravity) {
			int navigationBarHeight = 0;
			int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
			if (resourceId > 0) {
				navigationBarHeight = getContext().getResources().getDimensionPixelSize(resourceId);
			}

			FrameLayout result = new FrameLayout(getContext());
			FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(0, navigationBarHeight);
			lp.gravity = gravity | Gravity.BOTTOM;
			lp.bottomMargin = 0;
			result.setLayoutParams(lp);

			result.setBackgroundColor(getContext().getColor(android.R.color.system_accent1_300));
			result.setAlpha(.7f);
			((ViewGroup) getActivity().getWindow().getDecorView().getRootView()).addView(result);
			result.setVisibility(View.GONE);
			return result;
		}

		private FrameLayout prepareBackGestureView(int gravity) {
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

			result.setBackgroundColor(getContext().getColor(android.R.color.system_accent1_300));
			result.setAlpha(.7f);
			((ViewGroup) getActivity().getWindow().getDecorView().getRootView()).addView(result);
			result.setVisibility(View.GONE);
			return result;
		}

		@SuppressWarnings("SameParameterValue")
		private void setVisibility(View v, boolean visible, long duration) {
			if ((v.getVisibility() == View.VISIBLE) == visible) return;

			float basicAlpha = v.getAlpha();
			float destAlpha = (visible) ? 1f : 0f;

			if (visible) v.setAlpha(0f);
			v.setVisibility(View.VISIBLE);

			v.animate().setDuration(duration).alpha(destAlpha).setListener(new Animator.AnimatorListener() {
				@Override
				public void onAnimationStart(Animator animator) {
				}

				@Override
				public void onAnimationEnd(Animator animator) {
					if (!visible) v.setVisibility(View.GONE);
					v.setAlpha(basicAlpha);
				}

				@Override
				public void onAnimationCancel(Animator animator) {
				}

				@Override
				public void onAnimationRepeat(Animator animator) {
				}
			}).start();
		}

		@Override
		public void onDestroy() {
			((ViewGroup) rightBackGestureIndicator.getParent()).removeView(rightBackGestureIndicator);
			((ViewGroup) leftBackGestureIndicator.getParent()).removeView(leftBackGestureIndicator);

			((ViewGroup) rightSwipeGestureIndicator.getParent()).removeView(rightSwipeGestureIndicator);
			((ViewGroup) leftSwipeGestureIndicator.getParent()).removeView(leftSwipeGestureIndicator);

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

	@SuppressWarnings("ConstantConditions")
	public static class OwnPrefsFragment extends PreferenceFragmentCompat {

		SharedPreferences.OnSharedPreferenceChangeListener listener = this::onPrefChanged;

		private void onPrefChanged(SharedPreferences sharedPreferences, String key) {
			if (key.equals("appLanguage")) {
				try {
					getActivity().recreate();
				} catch (Exception ignored) {
				}
			}
		}

		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			getPreferenceManager().setStorageDeviceProtected();
			setPreferencesFromResource(R.xml.own_prefs_header, rootKey);
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().createDeviceProtectedStorageContext());


			prefs.registerOnSharedPreferenceChangeListener(listener);
		}
	}

}
