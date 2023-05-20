package sh.siava.AOSPMods.modpacks.android;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

import android.content.Context;

import java.util.Timer;
import java.util.TimerTask;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.XPrefs;
import sh.siava.AOSPMods.modpacks.XposedModPack;

public class PackageManager extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_FRAMEWORK_PACKAGE;

	private static final int AUTO_DISABLE_MINUTES = 5;
	private static final String ALLOW_SIGNATURE_PREF = "PM_AllowMismatchedSignature";
	private static final String ALLOW_DOWNGRADE_PREF = "PM_AllowDowngrade";

	public static final int PERMISSION = 4;

	private static boolean PM_AllowMismatchedSignature = false;
	private static boolean PM_AllowDowngrade = false;

	public PackageManager(Context context) {super(context);}

	@Override
	public void updatePrefs(String... Key) {
		PM_AllowMismatchedSignature = XPrefs.Xprefs.getBoolean(ALLOW_SIGNATURE_PREF, false);
		PM_AllowDowngrade = XPrefs.Xprefs.getBoolean(ALLOW_DOWNGRADE_PREF, false);

		if(PM_AllowDowngrade || PM_AllowMismatchedSignature) {
			if (Key.length == 0) {
				disablePMMods();
			}
			else if (Key[0].equals(ALLOW_SIGNATURE_PREF) || Key[0].equals(ALLOW_DOWNGRADE_PREF)){
				new Timer().schedule(new TimerTask() {
					@Override
					public void run() {
						disablePMMods();
					}
				},
						AUTO_DISABLE_MINUTES * 60000);
			}
		}
	}

	private void disablePMMods() {
		XPrefs.Xprefs.edit()
				.putBoolean(ALLOW_SIGNATURE_PREF, false)
				.putBoolean(ALLOW_DOWNGRADE_PREF, false)
				.apply();
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
