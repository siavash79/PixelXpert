package sh.siava.pixelxpert.modpacks.settings;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.Menu;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.ResourceManager;
import sh.siava.pixelxpert.modpacks.XposedModPack;

@SuppressWarnings({"RedundantThrows"})
public class AppCloneEnabler extends XposedModPack {
	private static final String listenPackage = Constants.SETTINGS_PACKAGE;
	private static final int AVAILABLE = 0;
	private static final int LIST_TYPE_CLONED_APPS = 17;
	private Class<?> UtilsClass;

	public AppCloneEnabler(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU;
	}

	@SuppressLint("ResourceType")
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

		Class<?> ClonedAppsPreferenceControllerClass = findClass("com.android.settings.applications.ClonedAppsPreferenceController", lpparam.classLoader);
		Class<?> AppStateClonedAppsBridgeClass = findClass("com.android.settings.applications.AppStateClonedAppsBridge", lpparam.classLoader);

		/* Private Space
		Class<?> FlagsClass = findClass("android.os.Flags", lpparam.classLoader);

		hookAllMethods(FlagsClass, "allowPrivateProfile", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				param.setResult(true);
			}
		});*/

		hookAllConstructors(AppStateClonedAppsBridgeClass, new XC_MethodHook() {
			@SuppressLint("QueryPermissionsNeeded")
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				ArrayList<String> packageList = new ArrayList<>();
				PackageManager packageManager = mContext.getPackageManager();

				int cloneUserID = getCloneUserID();

				List<String> clonePackageNames = new ArrayList<>();
				if(cloneUserID > 0)
				{
					//noinspection unchecked
					List<PackageInfo> cloneUserPackages = (List<PackageInfo>) callMethod(packageManager, "getInstalledPackagesAsUser", PackageManager.GET_ACTIVITIES, cloneUserID);

					cloneUserPackages.forEach(clonePackage -> {
						if(clonePackage.packageName != null)
							clonePackageNames.add(clonePackage.packageName);
					});
				}

				for(PackageInfo installedPackage : packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES))
				{
					if(installedPackage.packageName != null && installedPackage.packageName.length() > 0)
					{
						ApplicationInfo applicationInfo = packageManager.getApplicationInfo(installedPackage.packageName, PackageManager.GET_META_DATA);
						//Clone user profile is present and many system apps are auto-cloned. We don't need to display them.
						// For some reason, some system apps are not auto-cloned. We don't remove them from the list
						if((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0 && clonePackageNames.contains(installedPackage.packageName))
						{
							continue;
						}

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
				param.setResult(AVAILABLE);
			}
		});

		hookAllMethods(ClonedAppsPreferenceControllerClass, "updateSummary", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				callMethod(
						getObjectField(param.thisObject, "mPreference"),
						"setSummary",
						ResourceManager.modRes.getText(R.string.settings_cloned_apps_active));

				param.setResult(null);
			}
		});
	}

	private int getCloneUserID() {
		return (int) callStaticMethod(UtilsClass, "getCloneUserId", mContext);
	}
}