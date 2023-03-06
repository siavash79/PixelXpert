package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findFieldIfExists;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class FingerprintWhileDozing extends XposedModPack {
	private static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;
	private static boolean fingerprintWhileDozing = true;

	public FingerprintWhileDozing(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		fingerprintWhileDozing = Xprefs.getBoolean("fingerprintWhileDozing", true);
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(listenPackage)) return;

		Class<?> KeyguardUpdateMonitorClass = findClass("com.android.keyguard.KeyguardUpdateMonitor", lpparam.classLoader);

		String keyguardShowingField = (findFieldIfExists(KeyguardUpdateMonitorClass, "mKeyguardShowing") != null) ? "mKeyguardShowing" : "mKeyguardIsVisible"; // 13 QPR1 vs QPR2

		hookAllMethods(KeyguardUpdateMonitorClass,
				"shouldListenForFingerprint", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						if (fingerprintWhileDozing) return;
						boolean currentResult = (boolean) param.getResult();
						if (currentResult) {
							boolean userDoesNotHaveTrust = !(boolean) callMethod(param.thisObject,
									"getUserHasTrust",
									callMethod(param.thisObject, "getCurrentUser"));

							boolean shouldlisten2 =
									(getBooleanField(param.thisObject, keyguardShowingField)
											|| getBooleanField(param.thisObject, "mBouncerIsOrWillBeShowing")
											|| (boolean) callMethod(param.thisObject, "shouldListenForFingerprintAssistant")
											|| (getBooleanField(param.thisObject, "mKeyguardOccluded") && getBooleanField(param.thisObject, "mIsDreaming")))
											&& getBooleanField(param.thisObject, "mDeviceInteractive") && !getBooleanField(param.thisObject, "mGoingToSleep") && !getBooleanField(param.thisObject, "mKeyguardGoingAway")
											|| (getBooleanField(param.thisObject, "mKeyguardOccluded") && userDoesNotHaveTrust
											&& (getBooleanField(param.thisObject, "mOccludingAppRequestingFp") || (boolean) param.args[0]));

							param.setResult(shouldlisten2);
						}
					}
				});
	}
}
