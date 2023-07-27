package sh.siava.AOSPMods.modpacks;

import static android.content.Context.CONTEXT_IGNORE_SECURITY;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static sh.siava.AOSPMods.BuildConfig.APPLICATION_ID;
import static sh.siava.AOSPMods.BuildConfig.DEBUG;
import static sh.siava.AOSPMods.modpacks.utils.BootLoopProtector.isBootLooped;
import static sh.siava.AOSPMods.modpacks.Constants.SYSTEM_UI_PACKAGE;

import android.app.Instrumentation;
import android.content.Context;

import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.modpacks.utils.Helpers;
import sh.siava.AOSPMods.modpacks.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class XPLauncher {

	public static boolean isChildProcess = false;
	public static ArrayList<XposedModPack> runningMods = new ArrayList<>();
	public Context mContext = null;

	public XPLauncher() {
	}

	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		isChildProcess = lpparam.processName.contains(":");

		//If example class isn't found, user is using an older version. Don't load the module at all
		if (lpparam.packageName.equals(SYSTEM_UI_PACKAGE)) {
			Class<?> A33R18Example = findClassIfExists("com.android.systemui.shade.NotificationPanelViewController", lpparam.classLoader);
			if (A33R18Example == null) return;
		}

		if (lpparam.packageName.equals(SYSTEM_UI_PACKAGE) && DEBUG && false) {
			log("------------");
			Helpers.dumpClass("com.android.systemui.statusbar.notification.collection.NotifCollection", lpparam.classLoader);
			log("------------");
		}

		findAndHookMethod(Instrumentation.class, "newApplication", ClassLoader.class, String.class, Context.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (mContext == null)
				{
					mContext = (Context) param.args[2];

					XPrefs.init(mContext);

					ResourceManager.modRes = mContext.createPackageContext(APPLICATION_ID, CONTEXT_IGNORE_SECURITY)
							.getResources();

					if(isBootLooped(mContext.getPackageName()))
					{
						log(String.format("AOSPMods: Possible bootloop in %s. Will not load for now", mContext.getPackageName()));
						return;
					}

					new SystemUtils(mContext);
					XPrefs.setPackagePrefs(mContext.getPackageName());
				}

				for (Class<? extends XposedModPack> mod : ModPacks.getMods(lpparam.packageName)) {
					try {
						XposedModPack instance = mod.getConstructor(Context.class).newInstance(mContext);
						if (!instance.listensTo(lpparam.packageName)) continue;
						try {
							instance.updatePrefs();
						} catch (Throwable ignored) {}
						instance.handleLoadPackage(lpparam);
						runningMods.add(instance);
					} catch (Throwable T) {
						log("Start Error Dump - Occurred in " + mod.getName());
						T.printStackTrace();
					}
				}
			}
		});

	}
}