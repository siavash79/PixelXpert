package sh.siava.pixelxpert.modpacks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import android.annotation.SuppressLint;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.XposedModPack;

/** @noinspection unused, RedundantThrows */
public class BatteryInfoUpdateIndicator extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	public static final int BATTERY_STATUS_DISCHARGING = 3;

	@SuppressLint("StaticFieldLeak")
	private static BatteryInfoUpdateIndicator instance = null;

	List<BatteryInfoCallback> callbacks = new ArrayList<>();
	private int speed;

	public BatteryInfoUpdateIndicator(Context context) {
		super(context);
		instance = this;
	}

	@Override
	public void updatePrefs(String... Key) {}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		Class<?> BatteryStatusClass = findClass("com.android.settingslib.fuelgauge.BatteryStatus", lpparam.classLoader);
		hookAllConstructors(BatteryStatusClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				informCallbacks((int)getObjectField(param.thisObject, "status"));
			}
		});
	}

	private void informCallbacks(int status) {
		for(BatteryInfoCallback callback : callbacks)
		{
			try
			{
				callback.onInfoUpdated(status);
			}
			catch (Throwable ignored){}
		}
	}

	public static void registerCallback(BatteryInfoCallback callback)
	{
		instance.callbacks.add(callback);
	}

	public static void unRegisterCallback(BatteryInfoCallback callback)
	{
		instance.callbacks.remove(callback);
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}

	public interface BatteryInfoCallback
	{
		void onInfoUpdated(int batteryStatus);
	}
}

