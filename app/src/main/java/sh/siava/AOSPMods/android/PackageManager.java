package sh.siava.AOSPMods.android;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static sh.siava.AOSPMods.XPrefs.Xprefs;
import static sh.siava.AOSPMods.utils.Helpers.dumpClass;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;

public class PackageManager extends XposedModPack {
	public static final String listenPackage = AOSPMods.SYSTEM_FRAMEWORK_PACKAGE;

	public static final int PERMISSION = 4;

	private static boolean PM_AllowMismatchedSignature = false;
	private static boolean PM_AllowDowngrade = false;

	public PackageManager(Context context) {super(context);}

	@Override
	public void updatePrefs(String... Key) {
		PM_AllowMismatchedSignature = Xprefs.getBoolean("PM_AllowMismatchedSignature", false);
		PM_AllowDowngrade = Xprefs.getBoolean("PM_AllowDowngrade", false);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(listenPackage)) return;

		try {
			Class<?> InstallPackageHelperClass = findClass("com.android.server.pm.InstallPackageHelper", lpparam.classLoader);
			Class<?> PackageManagerServiceUtilsClass = findClass("com.android.server.pm.PackageManagerServiceUtils", lpparam.classLoader);
			Class<?> SigningDetailsClass = findClass("android.content.pm.SigningDetails", lpparam.classLoader);

			hookAllMethods(PackageManagerServiceUtilsClass, "checkDowngrade", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if(PM_AllowDowngrade) {
						param.setResult(null);
					}
				}
			});

			hookAllMethods(SigningDetailsClass, "checkCapability", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if(PM_AllowMismatchedSignature && !param.args[1].equals(PERMISSION)) {
						param.setResult(true);
					}
				}
			});

			hookAllMethods(PackageManagerServiceUtilsClass, "verifySignatures", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					try {
						if(PM_AllowMismatchedSignature &&
								callMethod(
										callMethod(param.args[0], "getSigningDetails"),
										"getSignatures"
								) != null) {
							param.setResult(true);
						}
					}
					catch (Throwable ignored){}
				}
			});

			hookAllMethods(InstallPackageHelperClass, "doesSignatureMatchForPermissions", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					try {
						if(PM_AllowMismatchedSignature
								&& callMethod(param.args[1], "getPackageName").equals(param.args[0])
								&& ((String)callMethod(param.args[1], "getBaseApkPath")).startsWith("/data")) {
							param.setResult(true);
						}
					}
					catch (Throwable ignored){}
				}
			});
		}
		catch (Throwable ignored){}
	}

	@Override
	public boolean listensTo(String packageName) { return listenPackage.equals(packageName);}

}
