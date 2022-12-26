package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class FeatureFlagsMods extends XposedModPack {
	public static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;

	public static final String CLIPBOARD_OVERLAY_SHOW_ACTIONS = "clipboard_overlay_show_actions";
	public static final String NAMESPACE_SYSTEMUI = "systemui";

	private static final int SIGNAL_DEFAULT = 0;
	@SuppressWarnings("unused")
	private static final int SIGNAL_FORCE_LTE = 1;
	private static final int SIGNAL_FORCE_4G = 2;

	public static int SBLTEIcon = SIGNAL_DEFAULT;

	private static boolean HideRoamingState = false;
	private static boolean EnableClipboardSmartActions = false;

	public FeatureFlagsMods(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		if (Xprefs == null) return;
		HideRoamingState = Xprefs.getBoolean("HideRoamingState", false);

		SBLTEIcon = Integer.parseInt(Xprefs.getString(
				"LTE4GIconMod",
				String.valueOf(SIGNAL_DEFAULT)));

		EnableClipboardSmartActions = Xprefs.getBoolean("EnableClipboardSmartActions", false);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(listenPackage)) return;

		Class<?> MobileSignalController = findClass("com.android.systemui.statusbar.connectivity.MobileSignalController", lpparam.classLoader);
		Class<?> DeviceConfigClass = findClass("android.provider.DeviceConfig", lpparam.classLoader);

		hookAllMethods(DeviceConfigClass, "getBoolean", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if(param.args[0].equals(NAMESPACE_SYSTEMUI) && param.args[1].equals(CLIPBOARD_OVERLAY_SHOW_ACTIONS))
				{
					param.setResult(EnableClipboardSmartActions);
				}
			}
		});

		hookAllMethods(MobileSignalController, "notifyListeners", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if(HideRoamingState) {
					setObjectField(
							getObjectField(
									param.thisObject,
									"mCurrentState"),
							"roaming",
							false);
				}
			}
		});

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
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}
}
