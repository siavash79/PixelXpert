package sh.siava.AOSPMods.utils;

import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

import android.content.Context;
import android.content.res.ColorStateList;

public class SettingsLibUtils {
	private static Class<?> UtilsClass = null;

	public static void init(ClassLoader classLoader)
	{
		if(UtilsClass == null)
			UtilsClass = findClass("com.android.settingslib.Utils", classLoader);
	}

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
}
