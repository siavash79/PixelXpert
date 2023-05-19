package sh.siava.AOSPMods;

import java.util.ArrayList;

import sh.siava.AOSPMods.allApps.OverScrollDisabler;
import sh.siava.AOSPMods.android.BrightnessRange;
import sh.siava.AOSPMods.android.HotSpotController;
import sh.siava.AOSPMods.android.PackageManager;
import sh.siava.AOSPMods.android.PhoneWindowManager;
import sh.siava.AOSPMods.android.ScreenOffKeys;
import sh.siava.AOSPMods.android.ScreenRotation;
import sh.siava.AOSPMods.android.StatusbarSize;
import sh.siava.AOSPMods.launcher.AOSPModsIconUpdater;
import sh.siava.AOSPMods.launcher.ClearAllButtonMod;
import sh.siava.AOSPMods.launcher.CustomNavGestures;
import sh.siava.AOSPMods.launcher.TaskbarActivator;
import sh.siava.AOSPMods.systemui.AOSPSettingsLauncher;
import sh.siava.AOSPMods.systemui.BatteryStyleManager;
import sh.siava.AOSPMods.systemui.BrightnessSlider;
import sh.siava.AOSPMods.systemui.EasyUnlock;
import sh.siava.AOSPMods.systemui.FeatureFlagsMods;
import sh.siava.AOSPMods.systemui.FingerprintWhileDozing;
import sh.siava.AOSPMods.systemui.FlashLightLevel;
import sh.siava.AOSPMods.systemui.GestureNavbarManager;
import sh.siava.AOSPMods.systemui.KeyGuardPinScrambler;
import sh.siava.AOSPMods.systemui.KeyguardMods;
import sh.siava.AOSPMods.systemui.MultiStatusbarRows;
import sh.siava.AOSPMods.systemui.NotificationExpander;
import sh.siava.AOSPMods.systemui.NotificationManager;
import sh.siava.AOSPMods.systemui.QSFooterTextManager;
import sh.siava.AOSPMods.systemui.QSQuickPullDown;
import sh.siava.AOSPMods.systemui.QSThemeManager;
import sh.siava.AOSPMods.systemui.QSTileGrid;
import sh.siava.AOSPMods.systemui.ScreenGestures;
import sh.siava.AOSPMods.systemui.ScreenshotMuter;
import sh.siava.AOSPMods.systemui.StatusbarMods;
import sh.siava.AOSPMods.systemui.ThreeButtonNavMods;
import sh.siava.AOSPMods.systemui.UDFPSManager;
import sh.siava.AOSPMods.systemui.VolumeTile;
import sh.siava.AOSPMods.telecom.CallVibrator;

public class ModPacks {

	public static ArrayList<Class> getMods()
	{
		ArrayList<Class> modPacks = new ArrayList<>();

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
		modPacks.add(ScreenOffKeys.class);
		modPacks.add(TaskbarActivator.class);
		modPacks.add(KeyGuardPinScrambler.class);
		modPacks.add(OverScrollDisabler.class);
		modPacks.add(FingerprintWhileDozing.class);
		modPacks.add(StatusbarSize.class);
		modPacks.add(ScreenRotation.class);
		modPacks.add(CallVibrator.class);
		modPacks.add(FlashLightLevel.class);
		modPacks.add(CustomNavGestures.class);
		modPacks.add(PhoneWindowManager.class);
		modPacks.add(BrightnessRange.class);
		modPacks.add(ClearAllButtonMod.class);
		modPacks.add(NotificationManager.class);
		modPacks.add(PackageManager.class);
		modPacks.add(AOSPModsIconUpdater.class);
		modPacks.add(VolumeTile.class);
		modPacks.add(HotSpotController.class);

		return modPacks;
	}
}