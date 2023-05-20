package sh.siava.AOSPMods.modpacks.android;

import static java.lang.Math.round;
import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.XPrefs;
import sh.siava.AOSPMods.modpacks.XposedModPack;
import sh.siava.rangesliderpreference.RangeSliderPreference;

@SuppressWarnings("RedundantThrows")
public class HotSpotController extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_FRAMEWORK_PACKAGE;

	private static long hotSpotTimeoutMillis = 0;
	private static boolean hotSpotHideSSID = false;
	private static int hotSpotMaxClients = 0;

	public HotSpotController(Context context) { super(context); }

	@Override
	public void updatePrefs(String... Key) {
		long timeout = 0;
		try
		{
			timeout = (long) (RangeSliderPreference.getValues(XPrefs.Xprefs, "hotSpotTimeoutSecs", 0).get(0) * 1000L);
		}
		catch (Throwable ignored){}

		int clients = 0;
		try
		{
			clients = round(RangeSliderPreference.getValues(XPrefs.Xprefs, "hotSpotMaxClients", 0).get(0));
		}
		catch (Throwable ignored){}

		hotSpotTimeoutMillis = (long) timeout;
		hotSpotHideSSID = XPrefs.Xprefs.getBoolean("hotSpotHideSSID", false);
		hotSpotMaxClients = clients;
	}

	@Override
	public boolean listensTo(String packageName) { return listenPackage.equals(packageName);	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(listenPackage)) return;

		try
		{
			Class<?> SoftApConfiguration = findClass("android.net.wifi.SoftApConfiguration", lpparam.classLoader);

			hookAllConstructors(SoftApConfiguration, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					setObjectField(param.thisObject, "mHiddenSsid", hotSpotHideSSID);

					if(hotSpotTimeoutMillis > 0)
					{
						setObjectField(param.thisObject, "mShutdownTimeoutMillis", hotSpotTimeoutMillis);
					}

					if(hotSpotMaxClients > 0)
					{
						setObjectField(param.thisObject, "mMaxNumberOfClients", hotSpotMaxClients);
					}
				}
			});
		}
		catch (Throwable ignored){}
	}
}
