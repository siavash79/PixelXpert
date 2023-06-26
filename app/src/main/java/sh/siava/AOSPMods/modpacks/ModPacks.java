package sh.siava.AOSPMods.modpacks;

import java.util.ArrayList;

import sh.siava.AOSPMods.modpacks.allApps.OverScrollDisabler;
import sh.siava.AOSPMods.modpacks.android.BrightnessRange;
import sh.siava.AOSPMods.modpacks.android.HotSpotController;
import sh.siava.AOSPMods.modpacks.android.PackageManager;
import sh.siava.AOSPMods.modpacks.android.PhoneWindowManager;
import sh.siava.AOSPMods.modpacks.android.ScreenOffKeys;
import sh.siava.AOSPMods.modpacks.android.ScreenRotation;
import sh.siava.AOSPMods.modpacks.android.StatusbarSize;
import sh.siava.AOSPMods.modpacks.dialer.RecordingMessage;
import sh.siava.AOSPMods.modpacks.launcher.AOSPModsIconUpdater;
import sh.siava.AOSPMods.modpacks.launcher.ClearAllButtonMod;
import sh.siava.AOSPMods.modpacks.launcher.CustomNavGestures;
import sh.siava.AOSPMods.modpacks.launcher.FeatureFlags;
import sh.siava.AOSPMods.modpacks.launcher.TaskbarActivator;
import sh.siava.AOSPMods.modpacks.settings.AppCloneEnabler;
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
import sh.siava.AOSPMods.modpacks.systemui.QSQuickPullDown;
import sh.siava.AOSPMods.modpacks.systemui.QSThemeManager;
import sh.siava.AOSPMods.modpacks.systemui.QSTileGrid;
import sh.siava.AOSPMods.modpacks.systemui.ScreenGestures;
import sh.siava.AOSPMods.modpacks.systemui.ScreenshotMuter;
import sh.siava.AOSPMods.modpacks.systemui.SettingsLibUtilsProvider;
import sh.siava.AOSPMods.modpacks.systemui.StatusbarMods;
import sh.siava.AOSPMods.modpacks.systemui.ThermalProvider;
import sh.siava.AOSPMods.modpacks.systemui.ThreeButtonNavMods;
import sh.siava.AOSPMods.modpacks.systemui.UDFPSManager;
import sh.siava.AOSPMods.modpacks.systemui.VolumeTile;
import sh.siava.AOSPMods.modpacks.telecom.CallVibrator;


public class ModPacks {

	public static ArrayList<Class> getMods()
	{
		ArrayList<Class> modPacks = new ArrayList<>();

		//SystemUI

		//Should be loaded before others
		modPacks.add(ThermalProvider.class);
		modPacks.add(SettingsLibUtilsProvider.class);

		modPacks.add(NotificationExpander.class);
		modPacks.add(QSTileGrid.class);
		modPacks.add(BrightnessSlider.class);
		modPacks.add(FeatureFlagsMods.class);
		modPacks.add(ThreeButtonNavMods.class);
		modPacks.add(QSThemeManager.class);
		modPacks.add(ScreenGestures.class);
		modPacks.add(miscSettings.class);
		modPacks.add(AOSPSettingsLauncher.class);
		modPacks.add(QSQuickPullDown.class);
		modPacks.add(KeyguardMods.class);
		modPacks.add(UDFPSManager.class);
		modPacks.add(EasyUnlock.class);
		modPacks.add(MultiStatusbarRows.class);
		modPacks.add(StatusbarMods.class);
		modPacks.add(BatteryStyleManager.class);
		modPacks.add(GestureNavbarManager.class);
		modPacks.add(QSFooterTextManager.class);
		modPacks.add(ScreenshotMuter.class);
		modPacks.add(KeyGuardPinScrambler.class);
		modPacks.add(FingerprintWhileDozing.class);
		modPacks.add(StatusbarSize.class);
		modPacks.add(FlashLightLevel.class);
		modPacks.add(CustomNavGestures.class);
		modPacks.add(NotificationManager.class);
		modPacks.add(VolumeTile.class);
		//Telecom
		modPacks.add(CallVibrator.class);
		//Framework
		modPacks.add(PackageManager.class);
		modPacks.add(BrightnessRange.class);
		modPacks.add(PhoneWindowManager.class);
		modPacks.add(ScreenRotation.class);
		modPacks.add(ScreenOffKeys.class);
		modPacks.add(HotSpotController.class);
		//Launcher
		modPacks.add(TaskbarActivator.class);
		modPacks.add(ClearAllButtonMod.class);
		modPacks.add(AOSPModsIconUpdater.class);
		modPacks.add(FeatureFlags.class);
		//Settings
		modPacks.add(AppCloneEnabler.class);
		//Dialer
		modPacks.add(RecordingMessage.class);
		//All Apps
		modPacks.add(OverScrollDisabler.class);

		return modPacks;
	}
}