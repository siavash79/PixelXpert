package sh.siava.pixelxpert.modpacks.settings;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import android.content.Context;
import android.content.Intent;

import androidx.core.content.res.ResourcesCompat;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.BuildConfig;
import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.ResourceManager;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class PXSettingsLauncher extends XposedModPack {
	private static final String listenPackage = Constants.SETTINGS_PACKAGE;

	private Object TopLevelSettings;

	public PXSettingsLauncher(Context context) {
		super(context);
	}


	@Override
	public void updatePrefs(String... Key) {}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		Class<?> SettingsPreferenceFragmentClass = findClass("com.android.settings.SettingsPreferenceFragment", lpparam.classLoader);
		Class<?> HomepagePreferenceClass = findClass("com.android.settings.widget.HomepagePreference", lpparam.classLoader);
		Class<?> TopLevelSettingsClass = findClass("com.android.settings.homepage.TopLevelSettings", lpparam.classLoader);
		hookAllMethods(TopLevelSettingsClass, "onPreferenceTreeClick", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if(ResourceManager.modRes.getString(R.string.app_name).equals(getObjectField(param.args[0], "mTitle")))
				{
					param.setResult(true);

					Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(BuildConfig.APPLICATION_ID);
					mContext.startActivity(intent);
				}
			}
		});

		hookAllConstructors(TopLevelSettingsClass, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				TopLevelSettings = param.thisObject;
			}
		});

		hookAllMethods(SettingsPreferenceFragmentClass, "setPreferenceScreen", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if(param.thisObject != TopLevelSettings) return;

				Object PXPreference = HomepagePreferenceClass.getConstructor(Context.class).newInstance(mContext);

				callMethod(PXPreference, "setIcon",
						ResourcesCompat.getDrawable(ResourceManager.modRes,
								R.drawable.ic_notification_foreground,
								mContext.getTheme()));
				callMethod(PXPreference, "setTitle", ResourceManager.modRes.getString(R.string.app_name));
				callMethod(PXPreference, "setOrder", 9999);

				callMethod(param.args[0], "addPreference", PXPreference);
			}
		});
	}
}
