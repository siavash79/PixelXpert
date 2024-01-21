package sh.siava.pixelxpert.modpacks.android;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class HotSpotController extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_FRAMEWORK_PACKAGE;

	private static long hotSpotTimeoutMillis = 0;
	private static boolean hotSpotHideSSID = false;
	private static int hotSpotMaxClients = 0;

	public HotSpotController(Context context) { super(context); }

	@Override
	public void updatePrefs(String... Key) {

		int clients = Xprefs.getSliderInt("hotSpotMaxClients", 0);

		hotSpotTimeoutMillis = (long) (Xprefs.getSliderFloat( "hotSpotTimeoutSecs", 0) * 1000L);
		hotSpotHideSSID = Xprefs.getBoolean("hotSpotHideSSID", false);
		hotSpotMaxClients = clients;
	}

	@Override
	public boolean listensTo(String packageName) { return listenPackage.equals(packageName);	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
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
