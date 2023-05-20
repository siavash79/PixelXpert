package sh.siava.AOSPMods.modpacks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.XPLauncher;
import sh.siava.AOSPMods.modpacks.XPrefs;
import sh.siava.AOSPMods.modpacks.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class FingerprintWhileDozing extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;
	private static boolean fingerprintWhileDozing = true;

	public FingerprintWhileDozing(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		fingerprintWhileDozing = XPrefs.Xprefs.getBoolean("fingerprintWhileDozing", true);
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(listenPackage)) return;

		Class<?> KeyguardUpdateMonitorClass = findClass("com.android.keyguard.KeyguardUpdateMonitor", lpparam.classLoader);

		hookAllMethods(KeyguardUpdateMonitorClass,
				"shouldListenForFingerprint", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						if (fingerprintWhileDozing) return;

						if(!getBooleanField(param.thisObject, "mDeviceInteractive"))
						{
							param.setResult(false);
						}
					}
				});
	}
}