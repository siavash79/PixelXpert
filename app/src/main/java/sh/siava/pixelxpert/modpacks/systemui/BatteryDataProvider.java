package sh.siava.pixelxpert.modpacks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.XposedModPack;

/** @noinspection RedundantThrows */
public class BatteryDataProvider extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	public static final int CHARGING_FAST = 2;

	public static final int BATTERY_STATUS_DISCHARGING = 3;

	@SuppressLint("StaticFieldLeak")
	private static BatteryDataProvider instance = null;

	List<BatteryStatusCallback> mStatusCallbacks = new ArrayList<>();
	private boolean mCharging;
	private int mCurrentLevel = 0;


	private final ArrayList<BatteryInfoCallback> mInfoCallbacks = new ArrayList<>();
	private boolean mPowerSave = false;
	private boolean mIsFastCharging = false;


	public BatteryDataProvider(Context context) {
		super(context);
		instance = this;
	}

	@Override
	public void updatePrefs(String... Key) {}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		Class<?> BatteryStatusClass = findClass("com.android.settingslib.fuelgauge.BatteryStatus", lpparam.classLoader);
		Class<?> BatteryControllerImplClass = findClass("com.android.systemui.statusbar.policy.BatteryControllerImpl", lpparam.classLoader);

		//once an intent is received, it's either battery level change, powersave change, or demo mode. we don't expect demo
		// intents normally. So it's safe to assume we'll need to update battery anyway
		hookAllMethods(BatteryControllerImplClass, "onReceive", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				mCurrentLevel = getIntField(param.thisObject, "mLevel");
				mCharging = getBooleanField(param.thisObject, "mPluggedIn")
						|| getBooleanField(param.thisObject, "mCharging")
						|| getBooleanField(param.thisObject, "mWirelessCharging");
				mPowerSave = getBooleanField(param.thisObject, "mPowerSave");

				fireBatteryInfoChanged();
			}
		});


		hookAllConstructors(BatteryStatusClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				mIsFastCharging = callMethod(param.thisObject, "getChargingSpeed", mContext).equals(CHARGING_FAST);
				if(param.args.length > 0) {
					onBatteryStatusChanged((int) getObjectField(param.thisObject, "status"), (Intent) param.args[0]);
				}
			}
		});
	}

	private void onBatteryStatusChanged(int status, Intent intent) {
		for(BatteryStatusCallback callback : mStatusCallbacks)
		{
			try
			{
				callback.onBatteryStatusChanged(status, intent);
			}
			catch (Throwable ignored){}
		}
	}

	public static void registerStatusCallback(BatteryStatusCallback callback)
	{
		try
		{
			instance.mStatusCallbacks.add(callback);
		}
		catch (Throwable ignored) {}
	}

	/** @noinspection unused*/
	public static void unRegisterStatusCallback(BatteryStatusCallback callback)
	{
		try {
			instance.mStatusCallbacks.remove(callback);
		}
		catch (Throwable ignored){}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}

	public static void registerInfoCallback(BatteryInfoCallback callback)
	{
		try
		{
			instance.mInfoCallbacks.add(callback);
		}
		catch (Throwable ignored){}
	}

	/** @noinspection unused*/
	public static void unRegisterInfoCallback(BatteryInfoCallback callback)
	{
		try {
			instance.mInfoCallbacks.remove(callback);
		}
		catch (Throwable ignored){}
	}

	public static boolean isCharging()
	{
		try {
			return instance.mCharging;
		}
		catch (Throwable ignored)
		{
			return false;
		}
	}

	public static int getCurrentLevel()
	{
		try {
			return instance.mCurrentLevel;
		}
		catch (Throwable ignored)
		{
			return 0;
		}
	}

	public static boolean isPowerSaving()
	{
		try {
			return instance.mPowerSave;
		}
		catch (Throwable ignored)
		{
			return false;
		}
	}

	public static boolean isFastCharging()
	{
		try {
			return instance.mCharging && instance.mIsFastCharging;
		}
		catch (Throwable ignored)
		{
			return false;
		}
	}

	private void fireBatteryInfoChanged() {
		for(BatteryInfoCallback callback : mInfoCallbacks)
		{
			try
			{
				callback.onBatteryInfoChanged();
			}
			catch (Throwable ignored){}
		}
	}
	public interface BatteryInfoCallback
	{
		void onBatteryInfoChanged();
	}


	public interface BatteryStatusCallback
	{
		void onBatteryStatusChanged(int batteryStatus, Intent batteryStatusIntent);
	}
}

