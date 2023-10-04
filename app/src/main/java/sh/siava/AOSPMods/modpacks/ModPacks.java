package sh.siava.AOSPMods.modpacks;

import android.os.Build;

import java.util.ArrayList;

import sh.siava.AOSPMods.modpacks.allApps.HookTester;
import sh.siava.AOSPMods.modpacks.allApps.OverScrollDisabler;
import sh.siava.AOSPMods.modpacks.android.BrightnessRange;
import sh.siava.AOSPMods.modpacks.android.HotSpotController;
import sh.siava.AOSPMods.modpacks.android.PackageManager;
import sh.siava.AOSPMods.modpacks.android.PhoneWindowManager;
import sh.siava.AOSPMods.modpacks.android.ScreenOffKeys;
import sh.siava.AOSPMods.modpacks.android.ScreenRotation;
import sh.siava.AOSPMods.modpacks.android.StatusbarSize;
import sh.siava.AOSPMods.modpacks.android.SystemScreenRecord;
import sh.siava.AOSPMods.modpacks.dialer.RecordingMessage;
import sh.siava.AOSPMods.modpacks.launcher.AOSPModsIconUpdater;
import sh.siava.AOSPMods.modpacks.launcher.ClearAllButtonMod;
import sh.siava.AOSPMods.modpacks.launcher.CustomNavGestures;
import sh.siava.AOSPMods.modpacks.launcher.FeatureFlags;
import sh.siava.AOSPMods.modpacks.launcher.TaskbarActivator;
import sh.siava.AOSPMods.modpacks.settings.AppCloneEnabler;
import sh.siava.AOSPMods.modpacks.android.RingerVolSeperator;
import sh.siava.AOSPMods.modpacks.systemui.AOSPSettingsLauncher;
import sh.siava.AOSPMods.modpacks.systemui.BatteryStyleManager;
import sh.siava.AOSPMods.modpacks.systemui.BrightnessSlider;
import sh.siava.AOSPMods.modpacks.systemui.EasyUnlock;
import sh.siava.AOSPMods.modpacks.systemui.FeatureFlagsMods;
import sh.siava.AOSPMods.modpacks.systemui.FingerprintWhileDozing;
import sh.siava.AOSPMods.modpacks.systemui.FlashLightLevel;
import sh.siava.AOSPMods.modpacks.systemui.GestureNavbarManager;
import sh.siava.AOSPMods.modpacks.systemui.KeyGuardPinScrambler;
import sh.siava.AOSPMods.modpacks.systemui.KeyguardMods;
import sh.siava.AOSPMods.modpacks.systemui.MultiStatusbarRows;
import sh.siava.AOSPMods.modpacks.systemui.NotificationExpander;
import sh.siava.AOSPMods.modpacks.systemui.NotificationManager;
import sh.siava.AOSPMods.modpacks.systemui.QSFooterTextManager;
import sh.siava.AOSPMods.modpacks.systemui.StatusbarGestures;
import sh.siava.AOSPMods.modpacks.systemui.ThemeManager_13;
import sh.siava.AOSPMods.modpacks.systemui.QSTileGrid;
import sh.siava.AOSPMods.modpacks.systemui.ScreenGestures;
import sh.siava.AOSPMods.modpacks.systemui.ScreenRecord;
import sh.siava.AOSPMods.modpacks.systemui.ScreenshotManager;
import sh.siava.AOSPMods.modpacks.systemui.SettingsLibUtilsProvider;
import sh.siava.AOSPMods.modpacks.systemui.StatusbarMods;
import sh.siava.AOSPMods.modpacks.systemui.ThemeManager_14;
import sh.siava.AOSPMods.modpacks.systemui.ThermalProvider;
import sh.siava.AOSPMods.modpacks.systemui.ThreeButtonNavMods;
import sh.siava.AOSPMods.modpacks.systemui.UDFPSManager;
import sh.siava.AOSPMods.modpacks.systemui.VolumeTile;
import sh.siava.AOSPMods.modpacks.telecom.CallVibrator;
import sh.siava.AOSPMods.modpacks.utils.Helpers;


public class ModPacks {

	public static ArrayList<Class<? extends XposedModPack>> getMods(String packageName)
	{
		ArrayList<Class<? extends XposedModPack>> modPacks = new ArrayList<>();

		//Should be loaded before others
		modPacks.add(ThermalProvider.class);
		modPacks.add(SettingsLibUtilsProvider.class);
		modPacks.add(HookTester.class);

		switch (packageName)
		{
			case Constants.SYSTEM_FRAMEWORK_PACKAGE:
				modPacks.add(StatusbarSize.class);
				modPacks.add(PackageManager.class);
				modPacks.add(BrightnessRange.class);
				modPacks.add(PhoneWindowManager.class);
				modPacks.add(ScreenRotation.class);
				modPacks.add(ScreenOffKeys.class);
				modPacks.add(HotSpotController.class);
				modPacks.add(RingerVolSeperator.class);
				modPacks.add(SystemScreenRecord.class);
				break;
			case Constants.SYSTEM_UI_PACKAGE:
				if(XPLauncher.isChildProcess && XPLauncher.processName.contains("screenshot"))
				{
					modPacks.add(ScreenshotManager.class);
				}
				else
				{
					if(Build.VERSION.SDK_INT == Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
					{
						Helpers.setOverlay("QSLightThemeOverlay", false, true, true);
						modPacks.add(ThemeManager_14.class);
					}
					else
					{
						modPacks.add(ThemeManager_13.class);
					}

					modPacks.add(BrightnessRange.class);
					modPacks.add(NotificationExpander.class);
					modPacks.add(QSTileGrid.class);
					modPacks.add(BrightnessSlider.class);
					modPacks.add(FeatureFlagsMods.class);
					modPacks.add(ThreeButtonNavMods.class);
					modPacks.add(ScreenGestures.class);
					modPacks.add(miscSettings.class);
					modPacks.add(AOSPSettingsLauncher.class);
					modPacks.add(StatusbarGestures.class);
					modPacks.add(KeyguardMods.class);
					modPacks.add(UDFPSManager.class);
					modPacks.add(EasyUnlock.class);
					modPacks.add(MultiStatusbarRows.class);
					modPacks.add(StatusbarMods.class);
					modPacks.add(BatteryStyleManager.class);
					modPacks.add(GestureNavbarManager.class);
					modPacks.add(QSFooterTextManager.class);
					modPacks.add(KeyGuardPinScrambler.class);
					modPacks.add(FingerprintWhileDozing.class);
					modPacks.add(StatusbarSize.class);
					modPacks.add(FlashLightLevel.class);
					modPacks.add(NotificationManager.class);
					modPacks.add(VolumeTile.class);
					modPacks.add(ScreenRecord.class);
				}
				break;
			case Constants.LAUNCHER_PACKAGE:
				modPacks.add(TaskbarActivator.class);
				modPacks.add(CustomNavGestures.class);
				modPacks.add(ClearAllButtonMod.class);
				modPacks.add(AOSPModsIconUpdater.class);
				modPacks.add(FeatureFlags.class);
				break;
			case Constants.TELECOM_SERVER_PACKAGE:
				modPacks.add(CallVibrator.class);
				break;
			case Constants.SETTINGS_PACKAGE:
				if(Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU)
					modPacks.add(AppCloneEnabler.class);
				break;
			case Constants.DIALER_PACKAGE:
				modPacks.add(RecordingMessage.class);
				break;
		}

		//All Apps
		modPacks.add(OverScrollDisabler.class);

		return modPacks;
	}
}