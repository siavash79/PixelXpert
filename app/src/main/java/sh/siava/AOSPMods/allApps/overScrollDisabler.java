package sh.siava.AOSPMods.allApps;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.IXposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class overScrollDisabler implements IXposedModPack {
	private static boolean disableOverScroll = false;
	
	@Override
	public void updatePrefs(String... Key) {
		disableOverScroll = XPrefs.Xprefs.getBoolean("disableOverScroll", false);
	}
	
	@Override
	public boolean listensTo(String packageName) { return true; } //This mod is compatible with every package
	
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
	
		Class<?> ViewClass = XposedHelpers.findClass("android.view.View", lpparam.classLoader);
		
		XposedBridge.hookAllMethods(ViewClass, "setOverScrollMode", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if(!disableOverScroll) return;
				XposedHelpers.setObjectField(param.thisObject, "mOverScrollMode", 2);
				param.setResult(null);
			}
		});
	}
}
