package sh.siava.AOSPMods.modpacks.telecom;

import static android.os.SystemClock.uptimeMillis;
import static android.os.VibrationAttributes.USAGE_NOTIFICATION;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;

import android.content.Context;
import android.os.VibrationEffect;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.XPrefs;
import sh.siava.AOSPMods.modpacks.XposedModPack;
import sh.siava.AOSPMods.modpacks.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class CallVibrator extends XposedModPack {
	private static final String listenPackage = Constants.TELECOM_SERVER_PACKAGE;

	public static final int DIALING = 3;
	public static final int ACTIVE = 5;
	public static final int DISCONNECTED = 7;

	private static final VibrationEffect vibrationEffect = VibrationEffect.createWaveform(new long[]{0, 100, 100, 100}, -1); //100ms on, 100 off, 100 again. don't repeat
	private static long lastActiveVibration = 0, lastDropVibration = 0;
	private static boolean vibrateOnAnswered = false, vibrateOnDrop = false;

	public CallVibrator(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		vibrateOnAnswered = XPrefs.Xprefs.getBoolean("vibrateOnAnswered", false);
		vibrateOnDrop = XPrefs.Xprefs.getBoolean("vibrateOnDrop", false);
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(listenPackage)) return;

		try {
			Class<?> InCallControllerClass = findClassIfExists("com.android.server.telecom.InCallController", lpparam.classLoader);
			if (InCallControllerClass == null) return;

			hookAllMethods(InCallControllerClass, "onCallStateChanged", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					try {
						int oldState = (int) param.args[1];
						int newState = (int) param.args[2];

						if (vibrateOnAnswered
								&& oldState == DIALING
								&& newState == ACTIVE
								&& uptimeMillis() - lastActiveVibration > 5000L /* Don't vibrate on concurrent method calls */) {
							lastActiveVibration = uptimeMillis();
							SystemUtils.vibrate(vibrationEffect, USAGE_NOTIFICATION);
						} else if (vibrateOnDrop
								&& oldState == ACTIVE
								&& newState == DISCONNECTED
								&& uptimeMillis() - lastDropVibration > 5000L) {
							lastDropVibration = uptimeMillis();
							SystemUtils.vibrate(vibrationEffect, USAGE_NOTIFICATION);
						}
					} catch (Throwable ignored) {
					}
				}
			});
		} catch (Throwable ignored) {
		}
	}
}