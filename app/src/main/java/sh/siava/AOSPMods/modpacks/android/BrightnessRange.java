package sh.siava.AOSPMods.modpacks.android;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.XPrefs;
import sh.siava.AOSPMods.modpacks.XposedModPack;
import sh.siava.rangesliderpreference.RangeSliderPreference;

@SuppressWarnings("RedundantThrows")
public class BrightnessRange extends XposedModPack {
	private final List<String> listenPacks = new ArrayList<>();

	private static float minimumBrightnessLevel = 0f;
	private static float maximumBrightnessLevel = 1f;

	public BrightnessRange(Context context) {
		super(context);
		listenPacks.add(Constants.SYSTEM_FRAMEWORK_PACKAGE);
		listenPacks.add(Constants.SYSTEM_UI_PACKAGE);
	}

	@Override
	public void updatePrefs(String... Key) {
		if (XPrefs.Xprefs == null) return;

		try {
			List<Float> BrightnessRange = RangeSliderPreference.getValues(XPrefs.Xprefs, "BrightnessRange", 100f);
			if (BrightnessRange.size() == 2) {
				minimumBrightnessLevel = BrightnessRange.get(0) / 100;
				maximumBrightnessLevel = BrightnessRange.get(1) / 100;
			}
		} catch (Throwable ignored) {
		}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPacks.contains(packageName);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		try { //framework
			Class<?> DisplayPowerControllerClass = findClass("com.android.server.display.DisplayPowerController", lpparam.classLoader);

			hookAllMethods(DisplayPowerControllerClass, "clampScreenBrightness", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if (minimumBrightnessLevel == 0f && maximumBrightnessLevel == 1f) return;

					param.args[0] = Math.min(
							Math.max(
									(float) param.args[0],
									minimumBrightnessLevel),
							maximumBrightnessLevel);
				}
			});
		} catch (Throwable ignored) {
		}

		try { //SystemUI
			Class<?> BrightnessInfoClass = findClass("android.hardware.display.BrightnessInfo", lpparam.classLoader);

			hookAllConstructors(BrightnessInfoClass, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					if (minimumBrightnessLevel > 0f) {
						setObjectField(param.thisObject, "brightnessMinimum", minimumBrightnessLevel);
					}
					if (maximumBrightnessLevel < 1f) {
						setObjectField(param.thisObject, "brightnessMaximum", maximumBrightnessLevel);
					}
				}
			});
		} catch (Throwable ignored) {
		}
	}
}