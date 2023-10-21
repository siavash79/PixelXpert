package sh.siava.pixelxpert.modpacks.android;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XposedModPack;

public class RingerVolSeperator extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_FRAMEWORK_PACKAGE;

	private static final String VOLUME_SEPARATE_NOTIFICATION = "volume_separate_notification";

	private static boolean SeparateRingNotifVol = false;
	private Class<?> DeviceConfigClass;

	public RingerVolSeperator(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		SeparateRingNotifVol = Xprefs.getBoolean("SeparateRingNotifVol", false);

		try
		{
			if(Key.length > 0 && Key[0].equals("SeparateRingNotifVol")) {
				setSeparateRingerNotif(SeparateRingNotifVol);
			}
		}
		catch (Throwable ignored){}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

		try {
			DeviceConfigClass = findClass("android.provider.DeviceConfig", lpparam.classLoader);

			hookAllMethods(DeviceConfigClass, "setProperty", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if ("systemui_PixelXpert".equals(param.args[0])) {
						param.args[0] = "systemui";
						return;
					}
					if (SeparateRingNotifVol && VOLUME_SEPARATE_NOTIFICATION.equals(param.args[1])) {
						param.setResult(true);
					}
				}
			});

			if (Constants.SYSTEM_FRAMEWORK_PACKAGE.equals(lpparam.packageName))
				setSeparateRingerNotif(SeparateRingNotifVol);
		} catch (Throwable ignored) {}
	}
	private void setSeparateRingerNotif(boolean enabled)
	{
		try {
			callStaticMethod(DeviceConfigClass, "setProperty", "systemui_PixelXpert", VOLUME_SEPARATE_NOTIFICATION, String.valueOf(enabled), true);
		} catch (Throwable ignored){}
	}
}
