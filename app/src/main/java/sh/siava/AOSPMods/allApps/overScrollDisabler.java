package sh.siava.AOSPMods.allApps;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import android.content.Context;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.XPrefs;
import sh.siava.AOSPMods.XposedModPack;

public class overScrollDisabler extends XposedModPack
{
	private static boolean disableOverScroll = false;
	
	public overScrollDisabler(Context context)
	{
		super(context);
	}
	
	@Override
	public void updatePrefs(String... Key)
	{
		disableOverScroll = XPrefs.Xprefs.getBoolean("disableOverScroll", false);
	}
	
	@Override
	public boolean listensTo(String packageName)
	{
		return true;
	} //This mod is compatible with every package
	
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable
	{
		
		Class<?> ViewClass = findClass("android.view.View", lpparam.classLoader);
		
		hookAllMethods(ViewClass, "setOverScrollMode", new XC_MethodHook()
		{
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable
			{
				if(! disableOverScroll)
					return;
				setObjectField(param.thisObject, "mOverScrollMode", View.OVER_SCROLL_NEVER);
				param.setResult(null);
			}
		});
	}
}
