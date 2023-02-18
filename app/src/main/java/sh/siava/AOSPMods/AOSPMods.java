package sh.siava.AOSPMods;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.content.Context;

import java.util.ArrayList;
import java.util.Calendar;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.allApps.OverScrollDisabler;
import sh.siava.AOSPMods.android.BrightnessRange;
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
	public static final String ACTION_SLEEP = "sh.siava.AOSPMods.ACTION_SLEEP";

	public AOSPMods() {
		//region Mod list definition
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
		//endregion
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		isSecondProcess = lpparam.processName.contains(":");

		//If example class isn't found, user is using an older version. Don't load the module at all
		if (lpparam.packageName.equals(SYSTEM_UI_PACKAGE)) {
			Class<?> A33R18Example = findClassIfExists("com.android.systemui.shade.NotificationPanelViewController", lpparam.classLoader);
			if (A33R18Example == null) return;
		}

		if (lpparam.packageName.equals(SYSTEM_UI_PACKAGE) && BuildConfig.DEBUG && false) {
			log("------------");
			Helpers.dumpClass("com.android.systemui.statusbar.notification.collection.NotifCollection", lpparam.classLoader);
			log("------------");
		}

		findAndHookMethod(Instrumentation.class, "newApplication", ClassLoader.class, String.class, Context.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (mContext == null)
				{
					mContext = (Context) param.args[2];

					XPrefs.init(mContext);

					if(bootLooped(mContext.getPackageName()))
					{
						return;
					}

					new SystemUtils(mContext, Xprefs.getBoolean("EnableCameraManager", true));
					XPrefs.loadEverything(mContext.getPackageName());
				}

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

	@SuppressLint("ApplySharedPref")
	private static boolean bootLooped(String packageName)
	{
		String loadTimeKey = String.format("packageLastLoad_%s", packageName);
		String strikeKey = String.format("packageStrike_%s", packageName);
		long currentTime = Calendar.getInstance().getTime().getTime();
		long lastLoadTime = Xprefs.getLong(loadTimeKey, 0);
		int strikeCount = Xprefs.getInt(strikeKey, 0);
		if (currentTime - lastLoadTime > 40000)
		{
			Xprefs.edit()
					.putLong(loadTimeKey, currentTime)
					.putInt(strikeKey, 0)
					.commit();
		}
		else if(strikeCount >= 3)
		{
			log(String.format("AOSPMods: Possible bootloop in %s. Will not load for now", packageName));
			return true;
		}
		else
		{
			Xprefs.edit().putInt(strikeKey, ++strikeCount).commit();
		}
		return false;
	}
}