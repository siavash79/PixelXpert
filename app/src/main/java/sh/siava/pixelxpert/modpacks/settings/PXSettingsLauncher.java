package sh.siava.pixelxpert.modpacks.settings;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.res.ResourcesCompat;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

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

	private static boolean PXInSettings = true;

	private boolean mNewSettings = true;

	public PXSettingsLauncher(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		PXInSettings = Xprefs.getBoolean("PXInSettings", true);
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		Class<?> HomepagePreferenceClass = findClass("com.android.settings.widget.HomepagePreference", lpParam.classLoader);
		Class<?> TopLevelSettingsClass = findClass("com.android.settings.homepage.TopLevelSettings", lpParam.classLoader);
		Class<?> OnPreferenceClickListenerInterface = findClass("androidx.preference.Preference$OnPreferenceClickListener", lpParam.classLoader);

		Class<?> PreferenceCategoryClass = findClassIfExists("androidx.preference.PreferenceCategory", lpParam.classLoader);
		Class<?> PreferenceManagerClass = findClassIfExists("androidx.preference.PreferenceManager", lpParam.classLoader);

		hookAllMethods(TopLevelSettingsClass, "getPreferenceScreenResId", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				@SuppressLint("DiscouragedApi")
				int oldResName = mContext.getResources().getIdentifier("top_level_settings", "xml", mContext.getPackageName());

				if(param.getResult().equals(oldResName))
				{
					mNewSettings = false;
				}
			}
		});
		hookAllMethods(TopLevelSettingsClass, "onCreateAdapter", new XC_MethodHook() {
			@SuppressLint("DiscouragedApi")
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (PXInSettings) {
					Object PXPreference = HomepagePreferenceClass.getConstructor(Context.class).newInstance(mContext);

					callMethod(PXPreference, "setIcon",
							ResourcesCompat.getDrawable(ResourceManager.modRes,
									R.drawable.ic_notification_foreground,
									mContext.getTheme()));
					callMethod(PXPreference, "setTitle", ResourceManager.modRes.getString(R.string.app_name));

					Object onClickListener = Proxy.newProxyInstance(
							OnPreferenceClickListenerInterface.getClassLoader(),
							new Class[] { OnPreferenceClickListenerInterface },
							new PXClickListener());

					setObjectField(PXPreference, "mOnClickListener", onClickListener);

					if(mNewSettings)
					{
						callMethod(PXPreference, "setSummary", ResourceManager.modRes.getString(R.string.xposed_desc));
						Object PXPreferenceCategory = PreferenceCategoryClass.getConstructor(Context.class).newInstance(mContext);

						setObjectField(PXPreferenceCategory,
								"mPreferenceManager",
								PreferenceManagerClass.getConstructor(Context.class).newInstance(mContext));
						callMethod(PXPreferenceCategory, "setOrder", 9999);

						int layoutID = mContext.getResources().getIdentifier(
								"settingslib_preference_category_no_title",
								"layout",
								mContext.getPackageName());

						if(layoutID != 0)
							callMethod(PXPreferenceCategory, "setLayoutResource",layoutID);

						callMethod(PXPreferenceCategory, "addPreference", PXPreference);

						callMethod(param.args[0], "addPreference", PXPreferenceCategory);
					}
					else
					{
						callMethod(PXPreference, "setOrder", 9999);
						callMethod(param.args[0], "addPreference", PXPreference);
					}
				}
			}
		});
	}

	class PXClickListener implements InvocationHandler {
		/** @noinspection SuspiciousInvocationHandlerImplementation*/
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(BuildConfig.APPLICATION_ID);
			mContext.startActivity(intent);

			return true;
		}
	}
}