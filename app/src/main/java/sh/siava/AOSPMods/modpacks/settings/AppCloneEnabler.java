package sh.siava.AOSPMods.modpacks.settings;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.XposedModPack;

@SuppressWarnings({"RedundantThrows", "deprecation"})
public class AppCloneEnabler extends XposedModPack {
	private static final String listenPackage = Constants.SETTINGS_PACKAGE;

	public AppCloneEnabler(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}

	@SuppressLint("ResourceType")
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if(!lpparam.packageName.equals(listenPackage)) return;

		Class<?> ClonedAppsPreferenceControllerClass = findClass("com.android.settings.applications.ClonedAppsPreferenceController", lpparam.classLoader);
		Class<?> AppStateClonedAppsBridgeClass = findClass("com.android.settings.applications.AppStateClonedAppsBridge", lpparam.classLoader);

		hookAllConstructors(AppStateClonedAppsBridgeClass, new XC_MethodHook() {
			@SuppressLint("QueryPermissionsNeeded")
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				ArrayList<String> packageList = new ArrayList<>();
				PackageManager pm = mContext.getPackageManager();

				for(@SuppressWarnings("SingleStatementInBlock") PackageInfo installedPackage : pm.getInstalledPackages(PackageManager.GET_ACTIVITIES))
				{
					if(installedPackage.packageName != null && installedPackage.packageName.length() > 0)
					{
						packageList.add(installedPackage.packageName);
					}
				}

				setObjectField(param.thisObject, "mAllowedApps", packageList);
			}
		});

		//the way to manually clone the app
/*		Class<?> CloneBackendClass = findClass("com.android.settings.applications.manageapplications.CloneBackend", lpparam.classLoader);

		Object cb = callStaticMethod(CloneBackendClass, "getInstance", mContext);
		callMethod(cb, "installCloneApp", "com.whatsapp");*/

		//Adding the menu to settings app
		hookAllMethods(ClonedAppsPreferenceControllerClass, "getAvailabilityStatus", new XC_MethodHook() {
			@SuppressLint("ResourceType")
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				param.setResult(0);
			}
		});
	}
}
