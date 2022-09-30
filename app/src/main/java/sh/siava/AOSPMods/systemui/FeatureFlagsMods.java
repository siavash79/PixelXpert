package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getStaticObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.content.Context;
import android.os.Build;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class FeatureFlagsMods extends XposedModPack {
	public static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;

	private static final int SIGNAL_DEFAULT = 0;
	@SuppressWarnings("unused")
	private static final int SIGNAL_FORCE_LTE = 1;
	private static final int SIGNAL_FORCE_4G = 2;

	public static int SBLTEIcon = SIGNAL_DEFAULT;

	public static boolean combinedSignalEnabled = false;

	public FeatureFlagsMods(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		if (Xprefs == null) return;
		boolean newcombinedSignalEnabled = Xprefs.getBoolean("combinedSignalEnabled", false);

		if (Key.length > 0 && newcombinedSignalEnabled != combinedSignalEnabled) {
			try {
				android.os.Process.killProcess(android.os.Process.myPid());
			} catch (Exception ignored) {
			}
		}
		combinedSignalEnabled = newcombinedSignalEnabled;

		SBLTEIcon = Integer.parseInt(Xprefs.getString(
				"LTE4GIconMod",
				String.valueOf(SIGNAL_DEFAULT)));

	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(listenPackage)) return;

		hookAllMethods(
				findClass("com.android.settingslib.mobile.MobileMappings$Config", lpparam.classLoader),
				"readConfig", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						if (SBLTEIcon == SIGNAL_DEFAULT) return;

						setObjectField(param.getResult(),
								"show4gForLte",
								SBLTEIcon == SIGNAL_FORCE_4G);
					}
				});

		if (Build.VERSION.SDK_INT < 32) return; //Feature flags is newly introduced!
		switch (Build.VERSION.SDK_INT) {
			case 31: //Feature flags is newly introduced!
				return;
			case 32: //A12.1
				Class<?> FeatureFlagsClass = findClass("com.android.systemui.flags.FeatureFlags", lpparam.classLoader);

				hookAllMethods(FeatureFlagsClass, "isCombinedStatusBarSignalIconsEnabled", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						param.setResult(combinedSignalEnabled);
					}
				});
				break;
			case 33: //A13
				Class<?> FlagsClass = findClass("com.android.systemui.flags.Flags", lpparam.classLoader);
				Class<?> FeatureFlagsReleaseClass = findClass("com.android.systemui.flags.FeatureFlagsRelease", lpparam.classLoader);

				Object COMBINED_STATUS_BAR_SIGNAL_ICONS = getStaticObjectField(FlagsClass, "COMBINED_STATUS_BAR_SIGNAL_ICONS");
				hookAllMethods(FeatureFlagsReleaseClass, "isEnabled", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						if (param.args[0].equals(COMBINED_STATUS_BAR_SIGNAL_ICONS)) {
							param.setResult(combinedSignalEnabled);
						}
					}
				});
				break;
		}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}
}
