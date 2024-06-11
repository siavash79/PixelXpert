package sh.siava.pixelxpert.modpacks.android;

import static android.os.VibrationAttributes.USAGE_ACCESSIBILITY;
import static android.os.VibrationEffect.EFFECT_TICK;
import static android.view.KeyEvent.ACTION_DOWN;
import static android.view.KeyEvent.KEYCODE_CAMERA;
import static android.view.KeyEvent.KEYCODE_POWER;
import static android.view.KeyEvent.KEYCODE_VOLUME_DOWN;
import static android.view.KeyEvent.KEYCODE_VOLUME_UP;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;
import static sh.siava.pixelxpert.modpacks.utils.SystemUtils.sleep;
import static sh.siava.pixelxpert.modpacks.utils.SystemUtils.threadSleep;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Display;
import android.view.KeyEvent;
import android.view.ViewConfiguration;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.pixelxpert.modpacks.utils.SystemUtils;
import sh.siava.pixelxpert.modpacks.utils.toolkit.ReflectionTools;

@SuppressWarnings("RedundantThrows")
public class ScreenOffKeys extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_FRAMEWORK_PACKAGE;

	public static final int PHYSICAL_ACTION_NONE = 0;
	public static final int PHYSICAL_ACTION_TORCH = 1;
	public static final int PHYSICAL_ACTION_CAMERA = 2;
	public static final int PHYSICAL_ACTION_ASSISTANT = 3;
	/** @noinspection unused*/
	public static final int PHYSICAL_ACTION_DND = 4;
	public static final int PHYSICAL_ACTION_PLAY_PAUSE = 5;
	public static final int PHYSICAL_ACTION_MEDIA_NEXT = 6;
	public static final int PHYSICAL_ACTION_MEDIA_PREV = 7;
	public static final int WAKE_REASON_POWER_BUTTON = 1;
	public static final int CAMERA_LAUNCH_SOURCE_POWER_DOUBLE_TAP = 1;
	private static final int INVOCATION_TYPE_POWER_BUTTON_LONG_PRESS = 6;


	private static int longPressPowerButtonScreenOff = 0;
	private static int longPressPowerButtonScreenOn = 0;
	private static int doublePressPowerButtonScreenOff = 0;
	private static int doublePressPowerButtonScreenOn = 0;
	private static int longPressVolumeUpButtonScreenOff = 0;
	private static int longPressVolumeDownButtonScreenOff = 0;

	Method launchAssistActionMethod;
	private Object windowMan;
	private long mWakeTime = 0;

	VolumeLongPressRunnable mVolumeLongPress = new VolumeLongPressRunnable(PHYSICAL_ACTION_NONE);

	public ScreenOffKeys(Context context) {
		super(context);
	}

	@SuppressLint("CheckResult")
	@Override
	public void updatePrefs(String... Key) {
		try {
			longPressPowerButtonScreenOff = Integer.parseInt(Xprefs.getString("longPressPowerButtonScreenOff", "0"));
			longPressPowerButtonScreenOn = Integer.parseInt(Xprefs.getString("longPressPowerButtonScreenOn", "0"));

			doublePressPowerButtonScreenOff = Integer.parseInt(Xprefs.getString("doublePressPowerButtonScreenOff", "0"));
			doublePressPowerButtonScreenOn = Integer.parseInt(Xprefs.getString("doublePressPowerButtonScreenOn", "0"));

			longPressVolumeUpButtonScreenOff = Integer.parseInt(Xprefs.getString("longPressVolumeUpButtonScreenOff", "0"));
			longPressVolumeDownButtonScreenOff = Integer.parseInt(Xprefs.getString("longPressVolumeDownButtonScreenOff", "0"));
			//noinspection ResultOfMethodCallIgnored
			SystemUtils.CameraManager(); //init CameraManager to listen to flash status
		}
		catch (Throwable ignored){}
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		try {
			Class<?> PhoneWindowManagerClass = findClass("com.android.server.policy.PhoneWindowManager", lpParam.classLoader);
			Class<?> PowerKeyRuleClass = findClass("com.android.server.policy.PhoneWindowManager$PowerKeyRule", lpParam.classLoader);
			Class<?> GestureLauncherServiceClass = findClass("com.android.server.GestureLauncherService", lpParam.classLoader);
			launchAssistActionMethod = ReflectionTools.findMethod(PhoneWindowManagerClass, "launchAssistAction");

			hookAllMethods(GestureLauncherServiceClass, "handleCameraGesture", new XC_MethodHook() { //double tap on power is handled here
						@Override
						protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
							boolean screenIsOn = screenIsOn(true);

							boolean handled = launchAction(resolveAction(KEYCODE_CAMERA, screenIsOn),
									screenIsOn,
									true);

							if(handled)
								param.setResult(true);
						}
					});

			hookAllMethods(PhoneWindowManagerClass, "enableScreen", new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param) throws Throwable {
							windowMan = param.thisObject;

							setObjectField(getObjectField(param.thisObject,"mGestureLauncherService"),
									"mCameraDoubleTapPowerEnabled",
									true);
						}
					});

			hookAllMethods(PowerKeyRuleClass, "onLongPress", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					boolean screenIsOn = screenIsOn(true);

					if(launchAction(resolveAction(KEYCODE_POWER, screenIsOn),
							screenIsOn,
							false))
						param.setResult(null);
				}
			});

			hookAllMethods(PhoneWindowManagerClass, "startedWakingUp", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if ((int) param.args[param.args.length-1] == WAKE_REASON_POWER_BUTTON) {
						mWakeTime = SystemClock.uptimeMillis();
					}
				}
			});

			hookAllMethods(PhoneWindowManagerClass, "interceptKeyBeforeQueueing", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					try {
						KeyEvent event = (KeyEvent) param.args[0];
						int keyCode = event.getKeyCode();

						if(!screenIsOn(false) &&
								((keyCode == KEYCODE_VOLUME_UP && longPressVolumeUpButtonScreenOff != PHYSICAL_ACTION_NONE) ||
										(keyCode == KEYCODE_VOLUME_DOWN && longPressVolumeDownButtonScreenOff != PHYSICAL_ACTION_NONE)))
						{
							Handler handler = (Handler) getObjectField(param.thisObject, "mHandler");

							switch (event.getAction()) {
								case KeyEvent.ACTION_UP:
									if (handler.hasCallbacks(mVolumeLongPress)) {
										SystemUtils.AudioManager().adjustStreamVolume(AudioManager.STREAM_MUSIC, keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ? AudioManager.ADJUST_LOWER : AudioManager.ADJUST_RAISE, 0);
										handler.removeCallbacks(mVolumeLongPress);
									}
									return;
								case KeyEvent.ACTION_DOWN:
									int action = resolveAction(keyCode, false);

									mVolumeLongPress = new VolumeLongPressRunnable(action);
									if(isActionLaunchable(action))
									{
										handler.postDelayed(mVolumeLongPress, ViewConfiguration.getLongPressTimeout());
										param.setResult(0);
									}
									break;
							}
						}
					} catch (Throwable ignored)
					{}

				}
			});
		} catch (Throwable ignored) {}
	}

	private int resolveAction(int keyCode, boolean screenIsOn) {
		boolean flashIsOn = SystemUtils.isFlashOn();

		switch (keyCode)
		{
			case KEYCODE_CAMERA:
				if(doublePressPowerButtonScreenOff == PHYSICAL_ACTION_TORCH && flashIsOn)
				{
					return PHYSICAL_ACTION_TORCH;
				}
				return screenIsOn
						? doublePressPowerButtonScreenOn
						: doublePressPowerButtonScreenOff;
			case KEYCODE_VOLUME_DOWN:
				return longPressVolumeDownButtonScreenOff;
			case KEYCODE_VOLUME_UP:
				return longPressVolumeUpButtonScreenOff;
			case KEYCODE_POWER:
				if(longPressPowerButtonScreenOff == PHYSICAL_ACTION_TORCH && flashIsOn) {
					return PHYSICAL_ACTION_TORCH;
				}
				return screenIsOn
						? longPressPowerButtonScreenOn
						: longPressPowerButtonScreenOff;
			default:
				return PHYSICAL_ACTION_NONE;
		}
	}

	private boolean isActionLaunchable(int action) {
		switch (action)
		{
			case PHYSICAL_ACTION_MEDIA_NEXT:
			case PHYSICAL_ACTION_MEDIA_PREV:
				//noinspection DataFlowIssue
				return SystemUtils.AudioManager().isMusicActive();
			default:
				return true;
		}
	}

	private boolean screenIsOn(boolean useWakeTime) { //for power button, display state isn't reliable enough because pressing power will trigger it
		if(useWakeTime) {
			return SystemClock.uptimeMillis() - mWakeTime > 1000;
		}
		Display defaultDisplay = (Display) getObjectField(windowMan, "mDefaultDisplay");
		return defaultDisplay.getState() == Display.STATE_ON;
	}

	/** @noinspection DataFlowIssue*/
	private boolean launchAction(int action, boolean screenIsOn, boolean delaySleep)
	{
		try {
			boolean handled = false;
			boolean shouldSleep = true;

			switch (action)
			{
				case PHYSICAL_ACTION_TORCH:
					SystemUtils.toggleFlash();
					handled = true;
					break;
				case PHYSICAL_ACTION_CAMERA:
					try {
						Object gestureLauncherService = getObjectField(windowMan, "mGestureLauncherService");
						handled = (boolean) callMethod(gestureLauncherService, "handleCameraGesture", false, CAMERA_LAUNCH_SOURCE_POWER_DOUBLE_TAP);
						shouldSleep = false;
					} catch (Throwable ignored){}
					break;
				case PHYSICAL_ACTION_ASSISTANT:
					try {
						launchAssistActionMethod.invoke(windowMan, null, -2, SystemClock.uptimeMillis(), INVOCATION_TYPE_POWER_BUTTON_LONG_PRESS);
						handled = true;
						shouldSleep = false;
					}
					catch (Throwable ignored){}
					break;
				case PHYSICAL_ACTION_PLAY_PAUSE:
					dispatchAudioKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
					handled = true;
					break;
				case PHYSICAL_ACTION_MEDIA_NEXT:
					if(SystemUtils.AudioManager().isMusicActive())
					{
						dispatchAudioKey(KeyEvent.KEYCODE_MEDIA_NEXT);
					}
					handled = true;
					break;
				case PHYSICAL_ACTION_MEDIA_PREV:
					if(SystemUtils.AudioManager().isMusicActive())
					{
						dispatchAudioKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
					}
					handled = true;
					break;
			}

			if(handled)
			{
				SystemUtils.vibrate(EFFECT_TICK, USAGE_ACCESSIBILITY);
				if(!screenIsOn && shouldSleep)
				{
					new Thread(() -> {
						if(delaySleep)
						{
							threadSleep(500);
						}
						sleep();
					}).start();
				}
			}

			return handled;
		}
		catch (Throwable ignored){}
		return false;
	}

	private void dispatchAudioKey(int keyCode) {
		//noinspection DataFlowIssue
		SystemUtils.AudioManager().dispatchMediaKeyEvent(new KeyEvent(ACTION_DOWN, keyCode));

		SystemUtils.AudioManager().dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}

	class VolumeLongPressRunnable implements Runnable {
		int mAction;
		public VolumeLongPressRunnable(int action)
		{
			mAction = action;
		}
		@Override
		public void run() {
			launchAction(mAction, false, false);
		}
	}
}