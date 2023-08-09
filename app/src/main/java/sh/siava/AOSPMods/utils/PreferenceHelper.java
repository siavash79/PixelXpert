package sh.siava.AOSPMods.utils;

import static java.lang.Math.round;

import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;

import com.topjohnwu.superuser.Shell;

import java.util.List;

import sh.siava.AOSPMods.R;
import sh.siava.rangesliderpreference.RangeSliderPreference;

public class PreferenceHelper {
	public static final int FULL_VERSION = 0;
	public static final int XPOSED_ONLY = 1;
	public static boolean showOverlays, showFonts;

	private SharedPreferences preferences;

	public static PreferenceHelper instance;

	public static void init(SharedPreferences prefs)
	{
		new PreferenceHelper(prefs);
	}

	private PreferenceHelper(SharedPreferences prefs)
	{
		preferences = prefs;

		int moduleType = getVersionType();

		showOverlays = moduleType == FULL_VERSION;
		showFonts = moduleType == FULL_VERSION;

		instance = this;
	}
	public static boolean isVisible(String key)
	{
		if(instance == null) return true;

		switch(key)
		{
			case "theming_header":
			case "HideNavbarOverlay":
			case "CustomThemedIconsOverlay":
			case "UnreadMessagesNumberOverlay":
				return showOverlays;

			case "TaskbarAsRecents":
			case "taskbarHeightOverride":
			case "TaskbarRadiusOverride":
			case "TaskbarTransient":
				int taskBarMode = Integer.parseInt(instance.preferences.getString("taskBarMode", "0"));
				return taskBarMode == 1;

			case "gsans_override":
			case "FontsOverlayEx":
				boolean customFontsEnabled = instance.preferences.getBoolean("enableCustomFonts", false);

				if (!customFontsEnabled && !instance.preferences.getString("FontsOverlayEx", "").equals("None")) {
					instance.preferences.edit().putString("FontsOverlayEx", "None").apply();
				}

				boolean gSansOverride = instance.preferences.getBoolean("gsans_override", false);
				boolean FontsOverlayExEnabled = !instance.preferences.getString("FontsOverlayEx", "None").equals("None");

				if("gsans_override".equals(key))
				{
					return customFontsEnabled && !FontsOverlayExEnabled;
				}
				else
				{
					return customFontsEnabled && !gSansOverride;
				}

			case "leftKeyguardShortcut":
			case "leftKeyguardShortcutLongClick":
			case "rightKeyguardShortcut":
			case "rightKeyguardShortcutLongClick":
				return Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE;

			case "carrierTextValue":
				return instance.preferences.getBoolean("carrierTextMod", false);

			case "batteryFastChargingColor":
			case "batteryChargingColor":
			case "batteryWarningColor":
			case "batteryCriticalColor":

				boolean critZero = false, warnZero = false;
				List<Float> BBarLevels = RangeSliderPreference.getValues(instance.preferences, "batteryWarningRange", 0);

				if (!BBarLevels.isEmpty()) {
					critZero = BBarLevels.get(0) == 0;
					warnZero = BBarLevels.get(1) == 0;
				}
				boolean bBarEnabled = instance.preferences.getBoolean("BBarEnabled", false);
				boolean isColorful = instance.preferences.getBoolean("BBarColorful", false);
				boolean transitColors = instance.preferences.getBoolean("BBarTransitColors", false);

				if("batteryFastChargingColor".equals(key))
				{
					return instance.preferences.getBoolean("indicateFastCharging", false) && bBarEnabled;
				} else if ("batteryChargingColor".equals(key)) {
					return instance.preferences.getBoolean("indicateCharging", false) && bBarEnabled;
				} else if ("batteryWarningColor".equals(key)) {
					return !warnZero && bBarEnabled;
				} else { //batteryCriticalColor
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
				return instance.preferences.getBoolean("BBarEnabled", false);

			case "networkTrafficRXTop":
				return instance.preferences.getString("networkTrafficMode", "0").equals("0");

			case "networkTrafficColorful":
				return !instance.preferences.getString("networkTrafficMode", "0").equals("3");

			case "networkTrafficDLColor":
			case "networkTrafficULColor":
				return instance.preferences.getBoolean("networkTrafficColorful", true);

			case "DualToneBatteryOverlay":
				return Integer.parseInt(instance.preferences.getString("BatteryStyle", "0")) == 0 && showOverlays;

			case "BIconOpacity":
			case "BIconindicateFastCharging":
			case "BIconColorful":
			case "BIconTransitColors":
			case "BatteryChargingAnimationEnabled":
			case "BIconbatteryWarningRange":
				return Integer.parseInt(instance.preferences.getString("BatteryStyle", "0")) > 0 && Integer.parseInt(instance.preferences.getString("BatteryStyle", "0")) < 99;

			case "BatteryIconScaleFactor":
				return Integer.parseInt(instance.preferences.getString("BatteryStyle", "0")) < 99;

			case "BatteryShowPercent":
				return Integer.parseInt(instance.preferences.getString("BatteryStyle", "0")) == 1 || Integer.parseInt(instance.preferences.getString("BatteryStyle", "0")) == 2;

			case "BIconindicateCharging":
				return Integer.parseInt(instance.preferences.getString("BatteryStyle", "0")) == 3;

			case "batteryIconChargingColor":
				return Integer.parseInt(instance.preferences.getString("BatteryStyle", "0")) == 3 && instance.preferences.getBoolean("BIconindicateCharging", false);

			case "batteryIconFastChargingColor":
				return Integer.parseInt(instance.preferences.getString("BatteryStyle", "0")) > 0 && Integer.parseInt(instance.preferences.getString("BatteryStyle", "0")) < 99 && instance.preferences.getBoolean("BIconindicateFastCharging", false);

			case "BIconbatteryCriticalColor":
			case "BIconbatteryWarningColor":
				boolean BIcritZero = false, BIwarnZero = false;
				List<Float> BIconLevels = RangeSliderPreference.getValues(instance.preferences, "BIconbatteryWarningRange", 0);

				if (!BIconLevels.isEmpty()) {
					BIcritZero = BIconLevels.get(0) == 0;
					BIwarnZero = BIconLevels.get(1) == 0;
				}
				
				return "BIconbatteryCriticalColor".equals(key)
					? Integer.parseInt(instance.preferences.getString("BatteryStyle", "0")) > 0 && Integer.parseInt(instance.preferences.getString("BatteryStyle", "0")) < 99 && (instance.preferences.getBoolean("BIconColorful", false) || !BIcritZero)
					: Integer.parseInt(instance.preferences.getString("BatteryStyle", "0")) > 0 && Integer.parseInt(instance.preferences.getString("BatteryStyle", "0")) < 99 && (instance.preferences.getBoolean("BIconColorful", false) || !BIwarnZero);

			case "SBCBeforeClockColor":
			case "SBCClockColor":
			case "SBCAfterClockColor":
				return instance.preferences.getBoolean("SBCClockColorful", false);

			case "ThreeButtonLeft":
			case "ThreeButtonCenter":
			case "ThreeButtonRight":
				return instance.preferences.getBoolean("ThreeButtonLayoutMod", false);

			case "network_settings_header":
			case "networkTrafficPosition":
				return instance.preferences.getBoolean("networkOnSBEnabled", false);

			case "systemIconSortPlan":
				return instance.preferences.getBoolean("systemIconsMultiRow", false);

			case "networkStatDLColor":
			case "networkStatULColor":
				return instance.preferences.getBoolean("networkStatsColorful", false);

			case "NetworkStatsStartTime":
				return instance.preferences.getString("NetworkStatsStartBase", "0").equals("0");

			case "NetworkStatsWeekStart":
				return instance.preferences.getString("NetworkStatsStartBase", "0").equals("1");

			case "NetworkStatsMonthStart":
				return instance.preferences.getString("NetworkStatsStartBase", "0").equals("2");

			case "wifi_cell":
				return instance.preferences.getBoolean("InternetTileModEnabled", true);

			case "QSPulldownPercent":
			case "QSPulldownSide":
				return instance.preferences.getBoolean("QSPullodwnEnabled", false);


		}
		return true;
	}

	public static boolean isEnabled(String key)
	{
		switch (key)
		{
			case "BBarTransitColors":
				return !instance.preferences.getBoolean("BBarColorful", false);

			case "BBarColorful":
				return !instance.preferences.getBoolean("BBarTransitColors", false);

			case "BIconColorful":
				return !instance.preferences.getBoolean("BIconTransitColors", false);

			case "BIconTransitColors":
				return !instance.preferences.getBoolean("BIconColorful", false);


		}
		return true;
	}

	@Nullable
	public static String getSummary(PreferenceFragmentCompat fragmentCompat, @NonNull String key)
	{
		switch (key)
		{
			case "taskbarHeightOverride":
				float taskbarHeightOverride = 100f;
				try {
					taskbarHeightOverride = RangeSliderPreference.getValues(instance.preferences, "taskbarHeightOverride", 100f).get(0);
				} catch (Throwable ignored) {}
				return taskbarHeightOverride != 100f
						? taskbarHeightOverride + "%"
						: fragmentCompat.getString(R.string.word_default);

			case "KeyGuardDimAmount":
				float KeyGuardDimAmount = -1;
				try {
					KeyGuardDimAmount = RangeSliderPreference.getValues(instance.preferences, "KeyGuardDimAmount", -1).get(0);
				} catch (Exception ignored) {}
				return KeyGuardDimAmount < 0
						? fragmentCompat.getString(R.string.word_default)
						: KeyGuardDimAmount + "%";

			case "BBOpacity":
				return instance.preferences.getInt("BBOpacity", 100) + "%";

			case "BBarHeight":
				return instance.preferences.getInt("BBarHeight", 100) + "%";

			case "networkTrafficInterval":
				return instance.preferences.getInt("networkTrafficInterval", 1) + " second(s)";

			case "BatteryIconScaleFactor":
				return instance.preferences.getInt("BatteryIconScaleFactor", 50) * 2 + fragmentCompat.getString(R.string.battery_size_summary);

			case "BIconOpacity":
				return instance.preferences.getInt("BIconOpacity", 100) + "%";

			case "volumeStps":
				int volumeStps = instance.preferences.getInt("volumeStps", 0);
				return String.format("%s - (%s)", volumeStps == 10 ? fragmentCompat.getString(R.string.word_default) : String.valueOf(volumeStps), fragmentCompat.getString(R.string.restart_needed));

			case "displayOverride":
				float displayOverride = 100;
				try {
					displayOverride = RangeSliderPreference.getValues(instance.preferences, "displayOverride", 100f).get(0);
				} catch (Exception ignored) {}

				double increasedArea = Math.round(Math.abs(Math.pow(displayOverride, 2) / 100 - 100));

				return String.format("%s \n (%s)", displayOverride == 100 ? fragmentCompat.getString(R.string.word_default) : String.format("%s%% - %s%% %s", String.valueOf(displayOverride), String.valueOf(increasedArea), displayOverride > 100 ? fragmentCompat.getString(R.string.more_area) : fragmentCompat.getString(R.string.less_area)), fragmentCompat.getString(R.string.sysui_restart_needed));

			case "HeadupAutoDismissNotificationDecay":
				float headsupDecayMillis = 5000;
				try {
					headsupDecayMillis = RangeSliderPreference.getValues(instance.preferences, "HeadupAutoDismissNotificationDecay", -1).get(0);
				} catch (Exception ignored) {}

				return ((int) headsupDecayMillis) + " " + fragmentCompat.getString(R.string.milliseconds);


			case "hotSpotTimeoutSecs":
				long timeout = 0;
				try {
					timeout = (long) (RangeSliderPreference.getValues(instance.preferences, "hotSpotTimeoutSecs", 0).get(0) * 1L);
				} catch (Throwable ignored) {}

				return 	timeout > 0
						? String.format("%d %s", timeout / 60, fragmentCompat.getString(R.string.minutes_word))
						: fragmentCompat.getString(R.string.word_default);

			case "hotSpotMaxClients":
				int clients = 0;
				try {
					clients = round(RangeSliderPreference.getValues(instance.preferences, "hotSpotMaxClients", 0).get(0));
				} catch (Throwable ignored) {}

				return clients > 0
						? String.valueOf(clients)
						: fragmentCompat.getString(R.string.word_default);


			case "statusbarHeightFactor":
				int statusbarHeightFactor = instance.preferences.getInt("statusbarHeightFactor", 100);
				return statusbarHeightFactor == 100 ? fragmentCompat.getString(R.string.word_default) : statusbarHeightFactor + "%";

			case "QSColQty":
				int QSColQty = instance.preferences.getInt("QSColQty", 1);

				if (instance.preferences.getInt("QSColQtyL", 1) > QSColQty) {
					instance.preferences.edit().putInt("QSColQtyL", QSColQty).apply();
				}

				return (QSColQty == 1) ? fragmentCompat.getString(R.string.word_default) : String.valueOf(QSColQty);

			case "QSRowQty":
				int QSRowQty = instance.preferences.getInt("QSRowQty", 0);
				return (QSRowQty == 0) ? fragmentCompat.getString(R.string.word_default) : String.valueOf(QSRowQty);

			case "QQSTileQty":
				int QQSTileQty = instance.preferences.getInt("QQSTileQty", 4);
				return (QQSTileQty == 4) ? fragmentCompat.getString(R.string.word_default) : String.valueOf(QQSTileQty);

			case "QSRowQtyL":
				int QSRowQtyL = instance.preferences.getInt("QSRowQtyL", 0);
				return (QSRowQtyL == 0) ? fragmentCompat.getString(R.string.word_default) : String.valueOf(QSRowQtyL);

			case "QSColQtyL":
				int QSColQtyL = instance.preferences.getInt("QSColQtyL", 1);
				return (QSColQtyL == 1) ? fragmentCompat.getString(R.string.word_default) : String.valueOf(QSColQtyL);

			case "QQSTileQtyL":
				int QQSTileQtyL = instance.preferences.getInt("QQSTileQtyL", 4);
				return (QQSTileQtyL == 4) ? fragmentCompat.getString(R.string.word_default) : String.valueOf(QQSTileQtyL);

		}
		return null;
	}

	/** @noinspection DataFlowIssue*/
	public static void setupPreference(PreferenceFragmentCompat fragmentCompat, String key)
	{
		try
		{
			Preference preference = fragmentCompat.findPreference(key);

			preference.setVisible(isVisible(key));
			preference.setEnabled(isEnabled(key));

			String summary = getSummary(fragmentCompat, key);
			if(summary != null)
			{
				preference.setSummary(summary);
			}

			//Other special cases
			if("QSColQtyL".equals(key))
			{
				((SeekBarPreference) preference).setMax(instance.preferences.getInt("QSColQty", 1));
			}
		}
		catch (Throwable ignored){}
	}

/*	public static void setupPreferences(PreferenceFragmentCompat fragmentCompat, String... keys)
	{
		for(String key : keys)
		{
			setupPreference(fragmentCompat,key);
		}
	}*/

	public static void setupAllPreferences(PreferenceFragmentCompat fragmentCompat)
	{
		for(String key : instance.preferences.getAll().keySet())
		{
			setupPreference(fragmentCompat, key);
		}
	}

	public static int getVersionType() {
		try {
			return Integer.parseInt(Shell.cmd(String.format("cat %s/build.type", "/data/adb/modules/AOSPMods")).exec().getOut().get(0));
		} catch (Exception ignored) {
			return XPOSED_ONLY;
		}
	}
}
