package sh.siava.AOSPMods.modpacks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getFloatField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static sh.siava.AOSPMods.modpacks.XPrefs.Xprefs;

import android.content.Context;

import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.XPLauncher;
import sh.siava.AOSPMods.modpacks.XposedModPack;

@SuppressWarnings({"RedundantThrows", "unused"})
public class ThermalProvider extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	public static final int CPU = 0;
	public static final int GPU = 1;
	public static final int BATTERY = 2;
	public static final int SKIN = 3;

	private static boolean TemperatureUnitF = false;

	private static Object thermalService = null;

	public ThermalProvider(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key)
	{
		TemperatureUnitF = Xprefs.getBoolean("TemperatureUnitF", false);
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		Class<?> PowerUIClass = findClass("com.android.systemui.power.PowerUI", lpparam.classLoader);

		hookAllMethods(PowerUIClass, "start", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				thermalService = getObjectField(param.thisObject, "mThermalService");
			}
		});
	}

	public static float getTemperatureMaxFloat(int type)
	{
		Object[] temperatures = getTemperatures(type);

		final float[] maxValue = {-999};

		Arrays.stream(temperatures).forEach(temperature -> maxValue[0] = Math.max(maxValue[0], getFloatField(temperature, "mValue")));

		if(TemperatureUnitF)
		{
			maxValue[0] = toFahrenheit(maxValue[0]);
		}

		return maxValue[0];
	}

	public static int getTemperatureMaxInt(int type)
	{
		return Math.round(getTemperatureMaxFloat(type));
	}

	public static float getTemperatureAvgFloat(int type)
	{
		Object[] temperatures = getTemperatures(type);

		if (temperatures.length > 0)
		{
			final float[] totalValue = {0};
			Arrays.stream(temperatures).forEach(temperature -> totalValue[0] += getFloatField(temperature, "mValue"));

			float ret = totalValue[0]/temperatures.length;

			if(TemperatureUnitF)
			{
				ret = toFahrenheit(ret);
			}

			return ret;
		}
		return -999;
	}

	public static int getTemperatureAvgInt(int type)
	{
		return Math.round(getTemperatureAvgFloat(type));
	}

	private static Object[] getTemperatures(int type)
	{
		try {
			return (Object[]) callMethod(thermalService, "getCurrentTemperaturesWithType", type);
		}
		catch (Throwable ignored)
		{
			return new Object[0];
		}
	}

	private static float toFahrenheit(float celsius)
	{
		return (celsius * 1.8f) + 32f;
	}
}
