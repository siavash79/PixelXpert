//Credits of double tap to wake go to nijel8 @XDA Thanks!

package sh.siava.AOSPMods.modpacks.systemui;

import static android.os.SystemClock.uptimeMillis;
import static android.os.VibrationAttributes.USAGE_ACCESSIBILITY;
import static android.os.VibrationEffect.EFFECT_TICK;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;
import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.util.Timer;
import java.util.TimerTask;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.XPLauncher;
import sh.siava.AOSPMods.modpacks.XPrefs;
import sh.siava.AOSPMods.modpacks.XposedModPack;
import sh.siava.AOSPMods.modpacks.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class ScreenGestures extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	private static final long HOLD_DURATION = 500;
	private static final int SHADE = 0; //frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/StatusBarState.java - screen unlocked - pulsing means screen is locked - shade locked means (Q)QS is open on lockscreen

	//settings
	public static boolean doubleTapToSleepStatusbarEnabled = false;
	private static boolean doubleTapToSleepLockscreenEnabled = false;
	private static boolean doubleTapToWake = false;
	private static boolean holdScreenTorchEnabled = false;

	private static boolean turnedByTTT = false;
	private static boolean mDoubleTap = false;  //double tap to wake when AOD off

	private boolean doubleTap; //double tap event for TTT

	GestureDetector mLockscreenDoubleTapToSleep; //event callback for double tap to sleep detection of statusbar only

	private boolean isDozing; //determiner for wakeup or sleep decision
	private Object NotificationPanelViewController;

	public ScreenGestures(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		doubleTapToWake = XPrefs.Xprefs.getBoolean("doubleTapToWake", false);
		holdScreenTorchEnabled = XPrefs.Xprefs.getBoolean("holdScreenTorchEnabled", false);
		doubleTapToSleepStatusbarEnabled = XPrefs.Xprefs.getBoolean("DoubleTapSleep", false);
		doubleTapToSleepLockscreenEnabled = XPrefs.Xprefs.getBoolean("DoubleTapSleepLockscreen", false);
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(listenPackage)) return;

		mLockscreenDoubleTapToSleep = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onDoubleTap(MotionEvent e) {
				SystemUtils.Sleep();
				return true;
			}
		});

		Class<?> NotificationShadeWindowViewControllerClass = findClass("com.android.systemui.shade.NotificationShadeWindowViewController", lpparam.classLoader);
		Class<?> NotificationPanelViewControllerClass = findClass("com.android.systemui.shade.NotificationPanelViewController", lpparam.classLoader);
		Class<?> DozeTriggersClass = findClass("com.android.systemui.doze.DozeTriggers", lpparam.classLoader);

		Class<?> PhoneStatusBarViewControllerClass = findClass("com.android.systemui.statusbar.phone.PhoneStatusBarViewController", lpparam.classLoader);

		try { //13 QPR3
			hookTouchHandler(PhoneStatusBarViewControllerClass);
		}catch (Throwable ignored){}

		//double tap detector for screen off AOD disabled sensor
		hookAllMethods(DozeTriggersClass,
				"onSensor", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						if (doubleTapToWake && ((int) param.args[0]) == 9) {
							if (!mDoubleTap) {
								param.setResult(null);
								mDoubleTap = true;
								new Timer().schedule(new TimerTask() {
									@Override
									public void run() {
										mDoubleTap = false;
									}
								}, 400);
							}
						}
					}
				});

		hookAllConstructors(NotificationShadeWindowViewControllerClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				new Thread(() -> {
					try {
						Thread.sleep(5000); //for some reason lsposed doesn't find methods in the class. so we'll hook to constructor and wait a bit!
					} catch (Exception ignored) {
					}
					setHooks(param);
				}).start();
			}
		});

		hookAllConstructors(NotificationPanelViewControllerClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				NotificationPanelViewController = param.thisObject;
				try
				{
					hookTouchHandler(getObjectField(param.thisObject, "mStatusBarViewTouchEventHandler").getClass());
				}
				catch (Throwable ignored) {}
			}
		});


	hookAllMethods(NotificationPanelViewControllerClass, "createTouchHandler", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				NotificationPanelViewController = param.thisObject;
				hookTouchHandler(param.getResult().getClass());
			}
		});
	}

	private void hookTouchHandler(Class<?> TouchHanlderClass) {
		XC_MethodHook touchHook = new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (!doubleTapToSleepStatusbarEnabled) return;

				//double tap to sleep, statusbar only
				if (!(boolean) getObjectField(NotificationPanelViewController, "mPulsing")
						&& !(boolean) getObjectField(NotificationPanelViewController, "mDozing")
						&& (int) getObjectField(NotificationPanelViewController, "mBarState") == SHADE
						&& (boolean) callMethod(NotificationPanelViewController, "isFullyCollapsed")) {
					mLockscreenDoubleTapToSleep.onTouchEvent((MotionEvent) param.args[param.args.length - 1]);
				}
			}
		};

		hookAllMethods(TouchHanlderClass, "onTouch", touchHook); //13 QPR2
		hookAllMethods(TouchHanlderClass, "handleTouchEvent", touchHook); //A13 R18
	}

	private void setHooks(XC_MethodHook.MethodHookParam param) {
		Object mPulsingWakeupGestureHandler = getObjectField(param.thisObject, "mPulsingWakeupGestureHandler");//A13 R18

		Object mListener = getObjectField(mPulsingWakeupGestureHandler, "mListener");

		Object mStatusBarKeyguardViewManager = getObjectField(param.thisObject,"mStatusBarKeyguardViewManager");

		Object mStatusBarStateController = getObjectField(param.thisObject, "mStatusBarStateController");

		//used in double tap to wake in AOD plan
		XC_MethodHook singleTapHook = new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param1) throws Throwable {
				if (doubleTapToWake)
					param1.setResult(false);
			}
		};
		hookAllMethods(mListener.getClass(), "onSingleTapUp", singleTapHook); //A13 R18
		hookAllMethods(mListener.getClass(), "onSingleTapConfirmed", singleTapHook); //older


		//used in double tap detection in AOD
		XC_MethodHook doubleTapHook = new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param1) throws Throwable {

				boolean isQSExpanded;
				try { //13QPR3
					isQSExpanded = getBooleanField(
							getObjectField(NotificationPanelViewController, "mQsController"),
							"mExpanded");
				}
				catch (Throwable ignored)
				{
					isQSExpanded = getBooleanField(NotificationPanelViewController, "mQsExpanded"); //13QPR2,1
				}

				if (isQSExpanded || getBooleanField(NotificationPanelViewController, "mBouncerShowing")) {
					return;
				}
				doubleTap = true;
				new Timer().schedule(new TimerTask() {
					@Override
					public void run() {
						doubleTap = false;
					}
				}, HOLD_DURATION * 2);

				isDozing = (boolean) callMethod(mStatusBarStateController, "isDozing");
			}
		};
		hookAllMethods(mListener.getClass(), "onDoubleTapEvent", doubleTapHook); //A13 R18
		hookAllMethods(mListener.getClass(), "onDoubleTap", doubleTapHook); //older

		//detect hold event for TTT and DTS on lockscreen
		hookAllMethods(mPulsingWakeupGestureHandler.getClass(), "onTouchEvent", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param1) throws Throwable {
				if(keyguardNotShowing(mStatusBarKeyguardViewManager))
				{
					return;
				}
				MotionEvent ev = (MotionEvent) param1.args[0];

				int action = ev.getActionMasked();

				if (doubleTap && action == ACTION_UP) {
					if (doubleTapToSleepLockscreenEnabled && !isDozing)
						SystemUtils.Sleep();
					doubleTap = false;
				}

				if (!holdScreenTorchEnabled) return;
				if ((action == ACTION_DOWN || action == ACTION_MOVE)) {
					if (doubleTap && !SystemUtils.isFlashOn() && uptimeMillis() - ev.getDownTime() > HOLD_DURATION) {
						turnedByTTT = true;
						callMethod(SystemUtils.PowerManager(), "wakeUp", uptimeMillis());
						SystemUtils.setFlash(true);
						SystemUtils.vibrate(EFFECT_TICK, USAGE_ACCESSIBILITY);

						new Thread(() -> { //if keyguard is dismissed for any reason (face or udfps touch), then:
							while(turnedByTTT)
							{
								try
								{
									//noinspection BusyWait
									Thread.sleep(200);
									if(keyguardNotShowing(mStatusBarKeyguardViewManager))
									{
										turnOffTTT();
									}
								}
								catch (Throwable ignored){}
							}
						}).start();
					}
					if (turnedByTTT) {
						ev.setAction(ACTION_DOWN);
					}
				} else if (turnedByTTT) {
					turnOffTTT();
				}
			}
		});
	}

	private boolean keyguardNotShowing(Object mStatusBarKeyguardViewManager) {
		try
		{
			return !((boolean) callMethod(mStatusBarKeyguardViewManager, "isShowing"));
		}
		catch (Throwable ignored)
		{
			return !getBooleanField(mStatusBarKeyguardViewManager, "mLastShowing");
		}
	}

	private void turnOffTTT() {
		turnedByTTT = false;
		SystemUtils.setFlash(false);
	}
}
