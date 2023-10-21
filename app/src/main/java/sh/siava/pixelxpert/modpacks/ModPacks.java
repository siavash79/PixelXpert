package sh.siava.pixelxpert.modpacks;

import android.os.Build;

import java.util.ArrayList;

import sh.siava.pixelxpert.modpacks.allApps.HookTester;
import sh.siava.pixelxpert.modpacks.allApps.OverScrollDisabler;
import sh.siava.pixelxpert.modpacks.android.BrightnessRange;
import sh.siava.pixelxpert.modpacks.android.HotSpotController;
import sh.siava.pixelxpert.modpacks.android.PackageManager;
import sh.siava.pixelxpert.modpacks.android.PhoneWindowManager;
import sh.siava.pixelxpert.modpacks.android.ScreenOffKeys;
import sh.siava.pixelxpert.modpacks.android.ScreenRotation;
import sh.siava.pixelxpert.modpacks.android.StatusbarSize;
import sh.siava.pixelxpert.modpacks.android.SystemScreenRecord;
import sh.siava.pixelxpert.modpacks.dialer.RecordingMessage;
import sh.siava.pixelxpert.modpacks.launcher.PixelXpertIconUpdater;
import sh.siava.pixelxpert.modpacks.launcher.ClearAllButtonMod;
import sh.siava.pixelxpert.modpacks.launcher.CustomNavGestures;
import sh.siava.pixelxpert.modpacks.launcher.FeatureFlags;
import sh.siava.pixelxpert.modpacks.launcher.TaskbarActivator;
import sh.siava.pixelxpert.modpacks.settings.AppCloneEnabler;
import sh.siava.pixelxpert.modpacks.android.RingerVolSeperator;
import sh.siava.pixelxpert.modpacks.systemui.AOSPSettingsLauncher;
import sh.siava.pixelxpert.modpacks.systemui.BatteryStyleManager;
import sh.siava.pixelxpert.modpacks.systemui.BrightnessSlider;
import sh.siava.pixelxpert.modpacks.systemui.EasyUnlock;
import sh.siava.pixelxpert.modpacks.systemui.FeatureFlagsMods;
import sh.siava.pixelxpert.modpacks.systemui.FingerprintWhileDozing;
import sh.siava.pixelxpert.modpacks.systemui.FlashLightLevel;
import sh.siava.pixelxpert.modpacks.systemui.GestureNavbarManager;
import sh.siava.pixelxpert.modpacks.systemui.KeyGuardPinScrambler;
import sh.siava.pixelxpert.modpacks.systemui.KeyguardMods;
import sh.siava.pixelxpert.modpacks.systemui.MultiStatusbarRows;
import sh.siava.pixelxpert.modpacks.systemui.NotificationExpander;
import sh.siava.pixelxpert.modpacks.systemui.NotificationManager;
import sh.siava.pixelxpert.modpacks.systemui.QSFooterTextManager;
import sh.siava.pixelxpert.modpacks.systemui.StatusbarGestures;
import sh.siava.pixelxpert.modpacks.systemui.ThemeManager_13;
import sh.siava.pixelxpert.modpacks.systemui.QSTileGrid;
import sh.siava.pixelxpert.modpacks.systemui.ScreenGestures;
import sh.siava.pixelxpert.modpacks.systemui.ScreenRecord;
import sh.siava.pixelxpert.modpacks.systemui.ScreenshotManager;
import sh.siava.pixelxpert.modpacks.systemui.SettingsLibUtilsProvider;
import sh.siava.pixelxpert.modpacks.systemui.StatusbarMods;
import sh.siava.pixelxpert.modpacks.systemui.ThemeManager_14;
import sh.siava.pixelxpert.modpacks.systemui.ThermalProvider;
import sh.siava.pixelxpert.modpacks.systemui.ThreeButtonNavMods;
import sh.siava.pixelxpert.modpacks.systemui.UDFPSManager;
import sh.siava.pixelxpert.modpacks.systemui.VolumeTile;
import sh.siava.pixelxpert.modpacks.telecom.CallVibrator;
import sh.siava.pixelxpert.modpacks.utils.Helpers;


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
				modPacks.add(PixelXpertIconUpdater.class);
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