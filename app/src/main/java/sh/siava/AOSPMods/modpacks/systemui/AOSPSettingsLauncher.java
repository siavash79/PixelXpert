package sh.siava.AOSPMods.modpacks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.BuildConfig;
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.XPLauncher;
import sh.siava.AOSPMods.modpacks.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class AOSPSettingsLauncher extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;


	private static Object activityStarter = null;

	public AOSPSettingsLauncher(Context context) {
		super(context);
	}


	@Override
	public void updatePrefs(String... Key) {
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(listenPackage)) return;

		Class<?> QSFragmentClass = findClass("com.android.systemui.qs.QSFragment", lpparam.classLoader);
		Class<?> FooterActionsInteractorImplClass = findClass("com.android.systemui.qs.footer.domain.interactor.FooterActionsInteractorImpl", lpparam.classLoader);

		View.OnLongClickListener listener = v -> {
			try {
				Intent launchIntent = mContext.getPackageManager().getLaunchIntentForPackage(BuildConfig.APPLICATION_ID);
				callMethod(activityStarter, "startActivity", launchIntent, true, null);
			} catch (Exception ignored) {
			}
			return true;
		};

		hookAllConstructors(FooterActionsInteractorImplClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				activityStarter = getObjectField(param.thisObject, "activityStarter");
			}
		});

		hookAllMethods(QSFragmentClass, "onViewCreated", new XC_MethodHook() {
			@SuppressLint("DiscouragedApi")
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				View QSView = (View) param.args[0];

				QSView.findViewById(mContext.getResources()
						.getIdentifier("settings_button_container", "id", mContext.getPackageName()))
						.setOnLongClickListener(listener);
			}
		});
	}
}
