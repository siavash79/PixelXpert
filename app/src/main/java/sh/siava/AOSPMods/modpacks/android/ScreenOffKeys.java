package sh.siava.AOSPMods.modpacks.android;

import static de.robv.android.xposed.XposedBridge.hookMethod;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findMethodExact;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.SystemClock;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.view.KeyEvent;
import android.view.ViewConfiguration;

import java.lang.reflect.Method;
import java.util.Calendar;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.XPrefs;
import sh.siava.AOSPMods.modpacks.XposedModPack;
import sh.siava.AOSPMods.modpacks.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class ScreenOffKeys extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_FRAMEWORK_PACKAGE;
	private static boolean replaceAssistantwithTorch = false;
	private static boolean holdVolumeToSkip = false;
	private long wakeTime = 0;
	//    private boolean isVolumeLongPress = false;
	private boolean isVolDown = false;

	public ScreenOffKeys(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		holdVolumeToSkip = XPrefs.Xprefs.getBoolean("holdVolumeToSkip", false);
		replaceAssistantwithTorch = XPrefs.Xprefs.getBoolean("replaceAssistantwithTorch", false);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(listenPackage)) return;

		Class<?> PhoneWindowManager;
		Method powerLongPress;
		Method startedWakingUp;
		Method interceptKeyBeforeQueueing;

		try {
			PhoneWindowManager = findClass("com.android.server.policy.PhoneWindowManager", lpparam.classLoader);
			powerLongPress = findMethodExact(PhoneWindowManager, "powerLongPress", long.class);
			startedWakingUp = findMethodExact(PhoneWindowManager, "startedWakingUp", int.class);
			interceptKeyBeforeQueueing = findMethodExact(PhoneWindowManager, "interceptKeyBeforeQueueing", KeyEvent.class, int.class);

			Runnable mVolumeLongPress = () -> {
				try {
					Intent keyIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
					KeyEvent keyEvent = new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, (isVolDown) ? KeyEvent.KEYCODE_MEDIA_PREVIOUS : KeyEvent.KEYCODE_MEDIA_NEXT, 0);
					keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
					SystemUtils.AudioManager().dispatchMediaKeyEvent(keyEvent);

					keyEvent = KeyEvent.changeAction(keyEvent, KeyEvent.ACTION_UP);
					keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
					SystemUtils.AudioManager().dispatchMediaKeyEvent(keyEvent);

					SystemUtils.vibrate(VibrationEffect.EFFECT_TICK, VibrationAttributes.USAGE_ACCESSIBILITY);
				} catch (Throwable ignored) {
				}
			};

			hookMethod(interceptKeyBeforeQueueing, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if (!holdVolumeToSkip) return;
					try {
						Handler mHandler = (Handler) getObjectField(param.thisObject, "mHandler");

						KeyEvent e = (KeyEvent) param.args[0];
						int Keycode = e.getKeyCode();

						switch (e.getAction()) {
							case KeyEvent.ACTION_UP:
								if (mHandler.hasCallbacks(mVolumeLongPress)) {
									SystemUtils.AudioManager().adjustStreamVolume(AudioManager.STREAM_MUSIC, Keycode == KeyEvent.KEYCODE_VOLUME_DOWN ? AudioManager.ADJUST_LOWER : AudioManager.ADJUST_RAISE, 0);
									mHandler.removeCallbacks(mVolumeLongPress);
								}
								return;
							case KeyEvent.ACTION_DOWN:
								if (!SystemUtils.PowerManager().isInteractive() && (Keycode == KeyEvent.KEYCODE_VOLUME_DOWN || Keycode == KeyEvent.KEYCODE_VOLUME_UP) && SystemUtils.AudioManager().isMusicActive()) {
									isVolDown = (Keycode == KeyEvent.KEYCODE_VOLUME_DOWN);
									mHandler.postDelayed(mVolumeLongPress, ViewConfiguration.getLongPressTimeout());
									param.setResult(0);
								}
						}
					} catch (Throwable ignored) {
					}
				}
			});

			hookMethod(startedWakingUp, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if (!replaceAssistantwithTorch) return;
					int r = (int) param.args[0];

					if (r == 1) {
						wakeTime = Calendar.getInstance().getTimeInMillis();
					}
				}
			});


			hookMethod(powerLongPress, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if (!replaceAssistantwithTorch) return;
					if (Calendar.getInstance().getTimeInMillis() - wakeTime > 1000) return;

					try {
						int behavior = (int) callMethod(param.thisObject, "getResolvedLongPressOnPowerBehavior");

						if (behavior == 3) // this is a force shutdown event. never play with it (3=LONG_PRESS_POWER_SHUT_OFF_NO_CONFIRM)
						{
							return;
						}

						SystemUtils.ToggleFlash();

						SystemUtils.vibrate(VibrationEffect.EFFECT_TICK, VibrationAttributes.USAGE_ACCESSIBILITY);

						param.setResult(null);
						callMethod(SystemUtils.PowerManager(), "goToSleep", SystemClock.uptimeMillis());
					} catch (Throwable T) {
						T.printStackTrace();
					}
				}
			});
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}

}
