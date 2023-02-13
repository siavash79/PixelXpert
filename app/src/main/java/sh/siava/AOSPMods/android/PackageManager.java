package sh.siava.AOSPMods.android;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedBridge.log;
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
					if(PM_AllowMismatchedSignature) {
						param.setResult(true);
					}
				}
			});

			hookAllMethods(PackageManagerServiceUtilsClass, "verifySignatures", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if(PM_AllowMismatchedSignature) {
						param.setResult(true);
					}
				}
			});

			hookAllMethods(InstallPackageHelperClass, "doesSignatureMatchForPermissions", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if(PM_AllowMismatchedSignature) {
						param.setResult(true);
					}
				}
			});
		}
		catch (Throwable ignored){}
	}

	@Override
	public boolean listensTo(String packageName) { return listenPackage.equals(packageName);}

}
