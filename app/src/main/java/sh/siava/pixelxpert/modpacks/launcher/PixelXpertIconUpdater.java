package sh.siava.pixelxpert.modpacks.launcher;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClass;

import android.content.Context;
import android.os.UserHandle;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.BuildConfig;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class PixelXpertIconUpdater extends XposedModPack {
	private static final String listenPackage = Constants.LAUNCHER_PACKAGE;
	private Object LauncherModel;

	public PixelXpertIconUpdater(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		Class<?> LauncherModelClass = findClass("com.android.launcher3.LauncherModel", lpparam.classLoader);
		Class<?> BaseDraggingActivityClass = findClass("com.android.launcher3.BaseDraggingActivity", lpparam.classLoader);

		hookAllMethods(BaseDraggingActivityClass, "onResume", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				try {
					XposedHelpers.callMethod(LauncherModel, "onAppIconChanged", BuildConfig.APPLICATION_ID, UserHandle.getUserHandleForUid(0));
				}catch (Throwable ignored){}
			}
		});
		hookAllConstructors(LauncherModelClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				LauncherModel = param.thisObject;
			}
		});

	}
}