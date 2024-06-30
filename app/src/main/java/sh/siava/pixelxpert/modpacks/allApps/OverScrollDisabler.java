package sh.siava.pixelxpert.modpacks.allApps;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class OverScrollDisabler extends XposedModPack {
	private static boolean disableOverScroll = false;

	public OverScrollDisabler(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		disableOverScroll = Xprefs.getBoolean("disableOverScroll", false);
	}

	@Override
	public boolean listensTo(String packageName) {
		return true;
	} //This mod is compatible with every package

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {

		Class<?> ViewClass = findClass("android.view.View", lpParam.classLoader);

		if(disableOverScroll)
		{
			hookAllMethods(ViewClass, "setOverScrollMode", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					setObjectField(param.thisObject, "mOverScrollMode", View.OVER_SCROLL_NEVER);
					param.setResult(null);
				}
			});
		}
	}
}
