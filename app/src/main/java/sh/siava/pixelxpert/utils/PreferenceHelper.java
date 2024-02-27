package sh.siava.pixelxpert.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import java.util.Arrays;
import java.util.List;

import sh.siava.pixelxpert.BuildConfig;
import sh.siava.pixelxpert.R;
import sh.siava.rangesliderpreference.RangeSliderPreference;

public class PreferenceHelper {
	public static final int FULL_VERSION = 0;
	public static final int XPOSED_ONLY = 1;
	public static boolean showOverlays, showFonts;

	private final ExtendedSharedPreferences mPreferences;

	public static PreferenceHelper instance;

	public static void init(ExtendedSharedPreferences prefs) {
		new PreferenceHelper(prefs);
	}

	private PreferenceHelper(ExtendedSharedPreferences prefs) {
		mPreferences = prefs;

		int moduleType = XPOSED_ONLY;

		showOverlays = moduleType == FULL_VERSION;
		showFonts = moduleType == FULL_VERSION;

		instance = this;
	}

	public static boolean isVisible(String key) {
		if (instance == null) return true;

		switch (key) {
			case "nav_keyboard_height_cat":
			case "overlay_dependent":
			case "HideNavbarOverlay":
			case "CustomThemedIconsOverlay":
			case "UnreadMessagesNumberOverlay":
			case "QSTilesThemesOverlayEx":
			case "IconPacksOverlayEx":
			case "IconShapesOverlayEx":
			case "SignalIconsOverlayEx":
			case "DTStylesOverlayEx":
			case "ReduceKeyboardSpaceOverlay":
				return showOverlays;

			case "DisableLockScreenPill":
			case "ForceThemedLauncherIcons":
			case "DisableOngoingNotifDismiss":
				return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE;

			case "enablePowerMenuTheme":
				return  instance.mPreferences.getBoolean("LightQSPanel", false);

			case "font_dependent":
			case "enableCustomFonts":
				return showFonts;

			case "TaskbarAsRecents":
			case "taskbarHeightOverride":
			case "TaskbarRadiusOverride":
			case "TaskbarTransient":
				int taskBarMode = Integer.parseInt(instance.mPreferences.getString("taskBarMode", "0"));
				return taskBarMode == 1;

			case "gsans_override":
			case "FontsOverlayEx":
				if (!showFonts)
					return false;

				boolean customFontsEnabled = instance.mPreferences.getBoolean("enableCustomFonts", false);

				if (!customFontsEnabled && !instance.mPreferences.getString("FontsOverlayEx", "").equals("None")) {
					instance.mPreferences.edit().putString("FontsOverlayEx", "None").apply();
				}

				boolean gSansOverride = instance.mPreferences.getBoolean("gsans_override", false);
				boolean FontsOverlayExEnabled = !instance.mPreferences.getString("FontsOverlayEx", "None").equals("None");

				if ("gsans_override".equals(key)) {
					return customFontsEnabled && !FontsOverlayExEnabled;
				} else {
					return customFontsEnabled && !gSansOverride;
				}

			case "displayOverride":
				return instance.mPreferences.getBoolean("displayOverrideEnabled", false);

			case "leftKeyguardShortcut":
			case "leftKeyguardShortcutLongClick":
			case "rightKeyguardShortcut":
			case "rightKeyguardShortcutLongClick":
				return Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE;

			case "carrierTextValue":
				return instance.mPreferences.getBoolean("carrierTextMod", false);

			case "batteryFastChargingColor":
			case "batteryChargingColor":
			case "batteryWarningColor":
			case "batteryCriticalColor":

				boolean critZero = false, warnZero = false;
				List<Float> BBarLevels = instance.mPreferences.getSliderValues("batteryWarningRange", 0);

				if (!BBarLevels.isEmpty()) {
					critZero = BBarLevels.get(0) == 0;
					warnZero = BBarLevels.get(1) == 0;
				}
				boolean bBarEnabled = instance.mPreferences.getBoolean("BBarEnabled", false);
				boolean transitColors = instance.mPreferences.getBoolean("BBarTransitColors", false);

				switch (key) {
					case "batteryFastChargingColor":
						return instance.mPreferences.getBoolean("indicateFastCharging", false) && bBarEnabled;
					case "batteryChargingColor":
						return instance.mPreferences.getBoolean("indicateCharging", false) && bBarEnabled;
					case "batteryWarningColor":
						return !warnZero && bBarEnabled;
					default:  //batteryCriticalColor
						return (!critZero || transitColors) && bBarEnabled && !warnZero;
				}

			case "BBarTransitColors":
			case "BBarColorful":
			case "BBOnlyWhileCharging":
			case "BBOnBottom":
			case "BBOpacity":
			case "BBarHeight":
			case "BBSetCentered":
			case "indicateCharging":
			case "indicateFastCharging":
			case "batteryWarningRange":
				return instance.mPreferences.getBoolean("BBarEnabled", false);

			case "networkTrafficRXTop":
				return (instance.mPreferences.getBoolean("networkOnQSEnabled", false) || instance.mPreferences.getBoolean("networkOnSBEnabled", false)) && instance.mPreferences.getString("networkTrafficMode", "0").equals("0");

			case "networkTrafficColorful":
				return (instance.mPreferences.getBoolean("networkOnQSEnabled", false) || instance.mPreferences.getBoolean("networkOnSBEnabled", false)) && !instance.mPreferences.getString("networkTrafficMode", "0").equals("3");

			case "networkTrafficDLColor":
			case "networkTrafficULColor":
				return (instance.mPreferences.getBoolean("networkOnQSEnabled", false) || instance.mPreferences.getBoolean("networkOnSBEnabled", false)) && instance.mPreferences.getBoolean("networkTrafficColorful", true);

			case "DualToneBatteryOverlay":
				return Integer.parseInt(instance.mPreferences.getString("BatteryStyle", "0")) == 0 && showOverlays;

			case "BIconOpacity":
			case "BIconindicateFastCharging":
			case "BIconColorful":
			case "BIconTransitColors":
			case "BatteryChargingAnimationEnabled":
			case "BIconbatteryWarningRange":
				return Integer.parseInt(instance.mPreferences.getString("BatteryStyle", "0")) > 0 && Integer.parseInt(instance.mPreferences.getString("BatteryStyle", "0")) < 99;

			case "BatteryIconScaleFactor":
				return Integer.parseInt(instance.mPreferences.getString("BatteryStyle", "0")) < 99;

			case "BatteryShowPercent":
				return Integer.parseInt(instance.mPreferences.getString("BatteryStyle", "0")) == 1 || Integer.parseInt(instance.mPreferences.getString("BatteryStyle", "0")) == 2;

			case "BIconindicateCharging":
				return Integer.parseInt(instance.mPreferences.getString("BatteryStyle", "0")) == 3;

			case "batteryIconChargingColor":
				return Integer.parseInt(instance.mPreferences.getString("BatteryStyle", "0")) == 3 && instance.mPreferences.getBoolean("BIconindicateCharging", false);

			case "batteryIconFastChargingColor":
				return Integer.parseInt(instance.mPreferences.getString("BatteryStyle", "0")) > 0 && Integer.parseInt(instance.mPreferences.getString("BatteryStyle", "0")) < 99 && instance.mPreferences.getBoolean("BIconindicateFastCharging", false);

			case "BIconbatteryCriticalColor":
			case "BIconbatteryWarningColor":
				boolean BIcritZero = false, BIwarnZero = false;
				List<Float> BIconLevels = instance.mPreferences.getSliderValues("BIconbatteryWarningRange", 0);

				if (!BIconLevels.isEmpty()) {
					BIcritZero = BIconLevels.get(0) == 0;
					BIwarnZero = BIconLevels.get(1) == 0;
				}

				return "BIconbatteryCriticalColor".equals(key)
						? Integer.parseInt(instance.mPreferences.getString("BatteryStyle", "0")) > 0 && Integer.parseInt(instance.mPreferences.getString("BatteryStyle", "0")) < 99 && (instance.mPreferences.getBoolean("BIconColorful", false) || !BIcritZero)
						: Integer.parseInt(instance.mPreferences.getString("BatteryStyle", "0")) > 0 && Integer.parseInt(instance.mPreferences.getString("BatteryStyle", "0")) < 99 && (instance.mPreferences.getBoolean("BIconColorful", false) || !BIwarnZero);

			case "SBCBeforeClockColor":
			case "SBCClockColor":
			case "SBCAfterClockColor":
				return instance.mPreferences.getBoolean("SBCClockColorful", false);

			case "ThreeButtonLeft":
			case "ThreeButtonCenter":
			case "ThreeButtonRight":
				return instance.mPreferences.getBoolean("ThreeButtonLayoutMod", false);

			case "network_settings_header":
			case "networkTrafficPosition":
				return instance.mPreferences.getBoolean("networkOnSBEnabled", false);

			case "SyncNTPTimeNow":
			case "TimeSyncInterval":
			case "NTPServers":
				return instance.mPreferences.getBoolean("SyncNTPTime", false);

			case "systemIconSortPlan":
				return instance.mPreferences.getBoolean("systemIconsMultiRow", false);

			case "networkStatDLColor":
			case "networkStatULColor":
				return instance.mPreferences.getBoolean("networkStatsColorful", false);

			case "NetworkStatsStartTime":
				return instance.mPreferences.getString("NetworkStatsStartBase", "0").equals("0");

			case "NetworkStatsWeekStart":
				return instance.mPreferences.getString("NetworkStatsStartBase", "0").equals("1");

			case "NetworkStatsMonthStart":
				return instance.mPreferences.getString("NetworkStatsStartBase", "0").equals("2");

			case "wifi_cell":
				return instance.mPreferences.getBoolean("InternetTileModEnabled", true);

			case "QSPulldownPercent":
			case "QSPulldownSide":
				return instance.mPreferences.getBoolean("QSPullodwnEnabled", false);

			case "oneFingerPullupEnabled":
				return instance.mPreferences.getBoolean("QSPullodwnEnabled", false) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE;

			case "BSThickTrackOverlay":
				if (!showOverlays && instance.mPreferences.getBoolean("BSThickTrackOverlay", false)) {
					instance.mPreferences.edit().putBoolean("BSThickTrackOverlay", false).apply();
				}
				return !instance.mPreferences.getBoolean("QSBrightnessDisabled", false) && showOverlays;

			case "BrightnessSlierOnBottom":
				return !instance.mPreferences.getBoolean("QSBrightnessDisabled", false);

			case "QQSBrightnessEnabled":
				return instance.mPreferences.getBoolean("QQSBrightnessSupported", true) && !instance.mPreferences.getBoolean("QSBrightnessDisabled", false);

			case "QSFooterText":
				return instance.mPreferences.getBoolean("QSFooterMod", false);

			case "networkTrafficMode":
			case "networkTrafficShowIcons":
			case "networkTrafficInterval":
			case "networkTrafficThreshold":
			case "networkTrafficOpacity":
				return (instance.mPreferences.getBoolean("networkOnQSEnabled", false) || instance.mPreferences.getBoolean("networkOnSBEnabled", false));

			case "network_settings_header_qs":
				return instance.mPreferences.getBoolean("networkOnQSEnabled", false);

			case "isFlashLevelGlobal":
				return instance.mPreferences.getBoolean("leveledFlashTile", false);

			case "BackLeftHeight":
				return instance.mPreferences.getBoolean("BackFromLeft", true);

			case "BackRightHeight":
				return instance.mPreferences.getBoolean("BackFromRight", true);

			case "leftSwipeUpPercentage":
				return !instance.mPreferences.getString("leftSwipeUpAction", "-1").equals("-1");

			case "rightSwipeUpPercentage":
				return !instance.mPreferences.getString("rightSwipeUpAction", "-1").equals("-1");

			case "nav_pill_cat":
				return !instance.mPreferences.getBoolean("HideNavbarOverlay", false);

			case "UpdateWifiOnly":
				return instance.mPreferences.getBoolean("AutoUpdate", true);
		}
		return true;
	}

	public static boolean isEnabled(String key) {
		switch (key) {
			case "BBarTransitColors":
				return !instance.mPreferences.getBoolean("BBarColorful", false);

			case "BBarColorful":
				return !instance.mPreferences.getBoolean("BBarTransitColors", false);

			case "BIconColorful":
				return !instance.mPreferences.getBoolean("BIconTransitColors", false);

			case "BIconTransitColors":
				return !instance.mPreferences.getBoolean("BIconColorful", false);
		}
		return true;
	}

	/**
	 * @noinspection UnnecessaryCallToStringValueOf
	 */
	@SuppressLint("DefaultLocale")
	@Nullable
	public static String getSummary(Context fragmentCompat, @NonNull String key) {
		switch (key) {
			case "VolumeDialogTimeout":
				int VolumeDialogTimeout = instance.mPreferences.getSliderInt("VolumeDialogTimeout", 3000);
				return VolumeDialogTimeout == 3000
						? fragmentCompat.getString(R.string.word_default)
						: String.format("%s %s", VolumeDialogTimeout, fragmentCompat.getString(R.string.milliseconds));

			case "taskbarHeightOverride":
				int taskbarHeightOverride = instance.mPreferences.getSliderInt("taskbarHeightOverride", 100);
				return taskbarHeightOverride != 100f
						? taskbarHeightOverride + "%"
						: fragmentCompat.getString(R.string.word_default);

			case "KeyGuardDimAmount":
				int KeyGuardDimAmount = instance.mPreferences.getSliderInt("KeyGuardDimAmount", -1);
				return KeyGuardDimAmount < 0
						? fragmentCompat.getString(R.string.word_default)
						: KeyGuardDimAmount + "%";

			case "BBOpacity":
				return instance.mPreferences.getSliderInt("BBOpacity", 100) + "%";

			case "BBarHeight":
				return instance.mPreferences.getSliderInt("BBarHeight", 100) + "%";

			case "networkTrafficInterval":
				return instance.mPreferences.getSliderInt("networkTrafficInterval", 1) + " second(s)";

			case "BatteryIconScaleFactor":
				return instance.mPreferences.getSliderInt("BatteryIconScaleFactor", 50) * 2 + fragmentCompat.getString(R.string.battery_size_summary);

			case "BIconOpacity":
				return instance.mPreferences.getSliderInt("BIconOpacity", 100) + "%";

			case "volumeStps":
				int volumeStps = instance.mPreferences.getSliderInt("volumeStps", 0);
				return String.format("%s - (%s)", volumeStps == 10 ? fragmentCompat.getString(R.string.word_default) : String.valueOf(volumeStps), fragmentCompat.getString(R.string.restart_needed));

			case "displayOverride":
				float displayOverride = instance.mPreferences.getSliderFloat("displayOverride", 100f);

				double increasedArea = Math.round(Math.abs(Math.pow(displayOverride, 2) / 100 - 100));

				return String.format("%s \n (%s)", displayOverride == 100 ? fragmentCompat.getString(R.string.word_default) : String.format("%s%% - %s%% %s", String.valueOf(displayOverride), String.valueOf(increasedArea), displayOverride > 100 ? fragmentCompat.getString(R.string.more_area) : fragmentCompat.getString(R.string.less_area)), fragmentCompat.getString(R.string.sysui_restart_needed));

			case "HeadupAutoDismissNotificationDecay":
				int headsupDecayMillis = instance.mPreferences.getSliderInt("HeadupAutoDismissNotificationDecay", 5000);

				return headsupDecayMillis + " " + fragmentCompat.getString(R.string.milliseconds);

			case "TimeSyncInterval":
				return instance.mPreferences.getSliderInt("TimeSyncInterval", 24) + " " + fragmentCompat.getString(R.string.hours);

			case "hotSpotTimeoutSecs":
				long timeout = (long) (instance.mPreferences.getSliderFloat( "hotSpotTimeoutSecs", 0) * 1L);

				return timeout > 0
						? String.format("%d %s", timeout / 60, fragmentCompat.getString(R.string.minutes_word))
						: fragmentCompat.getString(R.string.word_default);

			case "hotSpotMaxClients":
				int clients = instance.mPreferences.getSliderInt("hotSpotMaxClients", 0);
				return clients > 0
						? String.valueOf(clients)
						: fragmentCompat.getString(R.string.word_default);


			case "statusbarHeightFactor":
				int statusbarHeightFactor = instance.mPreferences.getSliderInt("statusbarHeightFactor", 100);
				return statusbarHeightFactor == 100 ? fragmentCompat.getString(R.string.word_default) : statusbarHeightFactor + "%";

			case "QSColQty":
				int QSColQty = instance.mPreferences.getSliderInt("QSColQty", 2);

				if (instance.mPreferences.getSliderInt("QSColQtyL", 2) > QSColQty) {
					instance.mPreferences.edit().putInt("QSColQtyL", QSColQty).apply();
				}

				return (QSColQty == 1) ? fragmentCompat.getString(R.string.word_default) : String.valueOf(QSColQty);

			case "QSRowQty":
				int QSRowQty = instance.mPreferences.getSliderInt("QSRowQty", 0);
				return (QSRowQty == 0) ? fragmentCompat.getString(R.string.word_default) : String.valueOf(QSRowQty);

			case "QQSTileQty":
				int QQSTileQty = instance.mPreferences.getSliderInt("QQSTileQty", 4);
				return (QQSTileQty == 4) ? fragmentCompat.getString(R.string.word_default) : String.valueOf(QQSTileQty);

			case "QSRowQtyL":
				int QSRowQtyL = instance.mPreferences.getSliderInt("QSRowQtyL", 0);
				return (QSRowQtyL == 0) ? fragmentCompat.getString(R.string.word_default) : String.valueOf(QSRowQtyL);

			case "QSColQtyL":
				int QSColQtyL = instance.mPreferences.getSliderInt("QSColQtyL", 1);
				return (QSColQtyL == 1) ? fragmentCompat.getString(R.string.word_default) : String.valueOf(QSColQtyL);

			case "QQSTileQtyL":
				int QQSTileQtyL = instance.mPreferences.getSliderInt("QQSTileQtyL", 4);
				return (QQSTileQtyL == 4) ? fragmentCompat.getString(R.string.word_default) : String.valueOf(QQSTileQtyL);

			case "QSPulldownPercent":
				return instance.mPreferences.getSliderInt("QSPulldownPercent", 25) + "%";

			case "QSLabelScaleFactor":
				float QSLabelScaleFactor = instance.mPreferences.getSliderFloat( "QSLabelScaleFactor", 0f);
				return (QSLabelScaleFactor + 100) + "% " + fragmentCompat.getString(R.string.toggle_dark_apply);

			case "QSSecondaryLabelScaleFactor":
				float QSSecondaryLabelScaleFactor = instance.mPreferences.getSliderFloat( "QSSecondaryLabelScaleFactor", 0f);
				return (QSSecondaryLabelScaleFactor + 100) + "% " + fragmentCompat.getString(R.string.toggle_dark_apply);

			case "GesPillWidthModPos":
				return instance.mPreferences.getSliderInt("GesPillWidthModPos", 50) * 2 + fragmentCompat.getString(R.string.pill_width_summary);

			case "GesPillHeightFactor":
				return instance.mPreferences.getSliderInt("GesPillHeightFactor", 100) + fragmentCompat.getString(R.string.pill_width_summary);

			case "BackLeftHeight":
				return instance.mPreferences.getSliderInt("BackLeftHeight", 100) + "%";

			case "BackRightHeight":
				return instance.mPreferences.getSliderInt("BackRightHeight", 100) + "%";

			case "leftSwipeUpPercentage":
				float leftSwipeUpPercentage = instance.mPreferences.getSliderFloat( "leftSwipeUpPercentage", 25);
				return leftSwipeUpPercentage + "%";

			case "rightSwipeUpPercentage":
				float rightSwipeUpPercentage = instance.mPreferences.getSliderFloat( "rightSwipeUpPercentage", 25);
				return rightSwipeUpPercentage + "%";

			case "swipeUpPercentage":
				return instance.mPreferences.getSliderFloat("swipeUpPercentage", 20f) + "%";

			case "appLanguage":
				int current_language_code = Arrays.asList(fragmentCompat.getResources().getStringArray(R.array.languages_values)).indexOf(instance.mPreferences.getString("appLanguage", fragmentCompat.getResources().getConfiguration().getLocales().get(0).getLanguage()));
				int selected_language_code = current_language_code < 0 ? Arrays.asList(fragmentCompat.getResources().getStringArray(R.array.languages_values)).indexOf("en") : current_language_code;
				return Arrays.asList(fragmentCompat.getResources().getStringArray(R.array.languages_names)).get(selected_language_code);

			case "CheckForUpdate":
				return fragmentCompat.getString(R.string.current_version, BuildConfig.VERSION_NAME);



		}
		return null;
	}

	/**
	 *
	 */
	public static void setupPreference(Preference preference) {
		try {
			String key = preference.getKey();

			preference.setVisible(isVisible(key));
			preference.setEnabled(isEnabled(key));

			String summary = getSummary(preference.getContext(), key);
			if (summary != null) {
				preference.setSummary(summary);
			}

			//Other special cases
			switch (key) {
				case "QSColQtyL":
					int QSColQty = instance.mPreferences.getSliderInt( "QSColQty", 1);
					QSColQty += QSColQty == 1 ? 1 : 0;
					((RangeSliderPreference) preference).setMax(QSColQty);
					break;

				case "QSLabelScaleFactor":
				case "QSSecondaryLabelScaleFactor":
					((RangeSliderPreference) preference).slider.setLabelFormatter(value -> (value + 100) + "%");
					break;
			}
		} catch (Throwable ignored) {
		}
	}

	public static void setupAllPreferences(PreferenceGroup group) {
		for (int i = 0; ; i++) {
			try {
				Preference thisPreference = group.getPreference(i);

				PreferenceHelper.setupPreference(thisPreference);

				if (thisPreference instanceof PreferenceGroup) {
					setupAllPreferences((PreferenceGroup) thisPreference);
				}
			} catch (Throwable ignored) {
				break;
			}
		}
	}

}