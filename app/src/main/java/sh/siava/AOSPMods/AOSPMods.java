package sh.siava.AOSPMods;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import android.app.Instrumentation;
import android.content.Context;

import java.util.ArrayList;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.allApps.OverScrollDisabler;
import sh.siava.AOSPMods.android.BrightnessRange;
import sh.siava.AOSPMods.android.PhoneWindowManager;
import sh.siava.AOSPMods.android.ScreenOffKeys;
import sh.siava.AOSPMods.android.ScreenRotation;
import sh.siava.AOSPMods.android.StatusbarSize;
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
import sh.siava.AOSPMods.systemui.KeyguardBottomArea;
import sh.siava.AOSPMods.systemui.KeyguardCustomText;
import sh.siava.AOSPMods.systemui.KeyguardDimmer;
import sh.siava.AOSPMods.systemui.LockscreenAlbumArt;
import sh.siava.AOSPMods.systemui.MultiStatusbarRows;
import sh.siava.AOSPMods.systemui.NotificationExpander;
import sh.siava.AOSPMods.systemui.NotificationManager;
import sh.siava.AOSPMods.systemui.QSFooterTextManager;
import sh.siava.AOSPMods.systemui.QSQuickPullDown;
import sh.siava.AOSPMods.systemui.QSThemeManager;
import sh.siava.AOSPMods.systemui.QSThemeManager_12;
import sh.siava.AOSPMods.systemui.QSTileGrid;
import sh.siava.AOSPMods.systemui.ScreenGestures;
import sh.siava.AOSPMods.systemui.ScreenshotController;
import sh.siava.AOSPMods.systemui.StatusbarMods;
import sh.siava.AOSPMods.systemui.ThreeButtonNavMods;
import sh.siava.AOSPMods.systemui.UDFPSManager;
import sh.siava.AOSPMods.telecom.CallVibrator;
import sh.siava.AOSPMods.utils.Helpers;
import sh.siava.AOSPMods.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class AOSPMods implements IXposedHookLoadPackage {
	public static final String SYSTEM_UI_PACKAGE = "com.android.systemui";
	public static final String SYSTEM_FRAMEWORK_PACKAGE = "android";
	public static final String TELECOM_SERVER_PACKAGE = "com.android.server.telecom";
	public static final String LAUNCHER_PACKAGE = "com.google.android.apps.nexuslauncher";

	public static boolean isSecondProcess = false;

	public static ArrayList<Class<?>> modPacks = new ArrayList<>();
	public static ArrayList<XposedModPack> runningMods = new ArrayList<>();
	public Context mContext = null;

	public static final String ACTION_SCREENSHOT = "sh.siava.AOSPMods.ACTION_SCREENSHOT";
	public static final String ACTION_INSECURE_SCREENSHOT = "sh.siava.AOSPMods.ACTION_INSECURE_SCREENSHOT";
	public static final String ACTION_BACK = "sh.siava.AOSPMods.ACTION_BACK";

	public AOSPMods() {
		//region Mod list definition
		modPacks.add(NotificationExpander.class); //13 OK
		modPacks.add(QSTileGrid.class); //
		modPacks.add(BrightnessSlider.class); //13 OK
		modPacks.add(FeatureFlagsMods.class); //13 OK
		modPacks.add(ThreeButtonNavMods.class); //13 not planned//13 OK
		modPacks.add(LockscreenAlbumArt.class); //13 not planned
		modPacks.add(QSThemeManager.class); //A13 LightQSTheme
		modPacks.add(QSThemeManager_12.class); //A12 LightQSTheme
		modPacks.add(ScreenGestures.class); //13 OK
		modPacks.add(miscSettings.class); //13 OK except for internet tile
		modPacks.add(AOSPSettingsLauncher.class); //13 OK
		modPacks.add(QSQuickPullDown.class); //13 OK
		modPacks.add(KeyguardCustomText.class); //13 OK
		modPacks.add(KeyguardBottomArea.class); //13 OK
		modPacks.add(UDFPSManager.class); //13 OK
		modPacks.add(EasyUnlock.class); //13 OK
		modPacks.add(MultiStatusbarRows.class); //13 OK
		modPacks.add(StatusbarMods.class); //13 OK
		modPacks.add(BatteryStyleManager.class); //13 OK
		modPacks.add(GestureNavbarManager.class); //13 OK
		modPacks.add(QSFooterTextManager.class); //13 OK
		modPacks.add(ScreenshotController.class); //13 OK
		modPacks.add(ScreenOffKeys.class); //13 OK
		modPacks.add(TaskbarActivator.class); //13 OK
		modPacks.add(KeyGuardPinScrambler.class); //13 OK
		modPacks.add(OverScrollDisabler.class); //13 OK
		modPacks.add(FingerprintWhileDozing.class); //13 OK
		modPacks.add(StatusbarSize.class); //13 OK
		modPacks.add(ScreenRotation.class); //13 OK
		modPacks.add(CallVibrator.class); //13 OK
		modPacks.add(FlashLightLevel.class); //13 based
		modPacks.add(KeyguardDimmer.class);
		modPacks.add(CustomNavGestures.class);
		modPacks.add(PhoneWindowManager.class);
		modPacks.add(BrightnessRange.class);
		modPacks.add(ClearAllButtonMod.class);
		modPacks.add(NotificationManager.class);
		//endregion
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		isSecondProcess = lpparam.processName.contains(":");

		if (lpparam.packageName.equals(SYSTEM_UI_PACKAGE) && false) {
			log("------------");
			Helpers.dumpClass("com.android.systemui.qs.QuickStatusBarHeader", lpparam.classLoader);
			log("------------");
		}

		findAndHookMethod(Instrumentation.class, "newApplication", ClassLoader.class, String.class, Context.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (mContext == null) setContext((Context) param.args[2]);

				for (Class<?> mod : modPacks) {
					try {
						XposedModPack instance = ((XposedModPack) mod.getConstructor(Context.class).newInstance(mContext));
						if (!instance.listensTo(lpparam.packageName)) continue;
						try {
							instance.updatePrefs();
						} catch (Throwable ignored) {
						}
						instance.handleLoadPackage(lpparam);
						runningMods.add(instance);
					} catch (Throwable T) {
						log("Start Error Dump - Occurred in " + mod.getName());
						T.printStackTrace();
					}
				}
			}
		});

	}

	private void setContext(Context context) {
		mContext = context;
		new SystemUtils(context);
		XPrefs.loadPrefs(mContext);
	}
}