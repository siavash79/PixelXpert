package sh.siava.AOSPMods.modpacks.systemui;

import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

import android.content.Context;
import android.content.res.ColorStateList;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.XPLauncher;
import sh.siava.AOSPMods.modpacks.XposedModPack;

public class SettingsLibUtilsProvider extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;
	private static Class<?> UtilsClass = null;

	public static ColorStateList getColorAttr(int resID, Context context)
	{
		if(UtilsClass == null) return null;

		return (ColorStateList) callStaticMethod(UtilsClass,"getColorAttr", resID, context);
	}

	public static int getColorAttrDefaultColor(int resID, Context context)
	{
		if(UtilsClass == null) return 0;

		try
		{ //13 QPR2
			return (int) callStaticMethod(UtilsClass, "getColorAttrDefaultColor", context, resID, 0);
		}
		catch (Throwable ignored)
		{ //13 QPR1
			return (int) callStaticMethod(UtilsClass, "getColorAttrDefaultColor", resID, context);
		}
	}
	public SettingsLibUtilsProvider(Context context) {
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
		UtilsClass = findClass("com.android.settingslib.Utils", lpparam.classLoader);
	}
}
