package sh.siava.AOSPMods.modpacks.android;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static sh.siava.AOSPMods.modpacks.XPrefs.Xprefs;

import android.content.Context;
import android.os.Binder;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class SystemScreenRecord extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_FRAMEWORK_PACKAGE;

	private static boolean InsecureScreenRecord = false;

	public SystemScreenRecord(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		InsecureScreenRecord = Xprefs.getBoolean("InsecureScreenRecord", false);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		try
		{
			Class<?> DisplayManagerServiceClass = findClass("com.android.server.display.DisplayManagerService", lpparam.classLoader);

			hookAllMethods(DisplayManagerServiceClass, "canProjectSecureVideo", new XC_MethodHook() { //Granting the required permission to systemui
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					try
					{
						if(InsecureScreenRecord && (boolean) callMethod(param.thisObject, "validatePackageName"
								, Binder.getCallingUid()
								, "com.android.systemui"))
							param.setResult(true);
					}
					catch (Throwable ignored) {}
				}
			});
		}
		catch (Throwable ignored) {}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}
}