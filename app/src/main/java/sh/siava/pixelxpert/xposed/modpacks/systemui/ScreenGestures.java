//Credits of double tap to wake go to nijel8 @XDA Thanks!

package sh.siava.pixelxpert.xposed.modpacks.systemui;

import static android.os.SystemClock.uptimeMillis;
import static android.os.VibrationAttributes.USAGE_ACCESSIBILITY;
import static android.os.VibrationEffect.EFFECT_TICK;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static sh.siava.pixelxpert.xposed.XPrefs.Xprefs;
import static sh.siava.pixelxpert.xposed.utils.SystemUtils.sleep;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import java.util.Timer;
import java.util.TimerTask;

import io.github.libxposed.api.XposedModuleInterface;
import sh.siava.pixelxpert.xposed.XposedModPack;
import sh.siava.pixelxpert.xposed.annotations.SystemUIModPack;
import sh.siava.pixelxpert.xposed.utils.reflection.HookHelper;
import sh.siava.pixelxpert.xposed.utils.SystemUtils;
import sh.siava.pixelxpert.xposed.utils.reflection.ReflectedClass;

@SuppressWarnings("RedundantThrows")
@SystemUIModPack
public class ScreenGestures extends XposedModPack {
	private static final long HOLD_DURATION = 500;
	private static final int SHADE = 0; //frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/StatusBarState.java - screen unlocked - pulsing means screen is locked - shade locked means (Q)QS is open on lockscreen

	private static final int REASON_SENSOR_TAP = 9; // from DozeTriggers
	private static final int REASON_SENSOR_PICKUP = 3; // from DozeTriggers
	private static final int PULSE_REASON_INTENT = 0; //from DozeLog

	//settings
	public static boolean doubleTapToSleepStatusbarEnabled = false;
	private static boolean doubleTapToSleepLockscreenEnabled = false;
	private static boolean doubleTapToWake = false;
	private static boolean holdScreenTorchEnabled = false;

	private static boolean TapToShowAmbient = false;
	private static boolean PickToShowAmbient = false;
	private static boolean turnedByTTT = false;
	private static boolean mDoubleTap = false;  //double tap to wake when AOD off

	private boolean doubleTap; //double tap event for TTT

	GestureDetector mLockscreenDoubleTapToSleep; //event callback for double tap to sleep detection of statusbar only

	private boolean isDozing; //determiner for wakeup or sleep decision
	private Object NotificationPanelViewController;
	private Timer mTimer;
	private static boolean DisableLockScreenPill = false;
	private Object mStatusBarKeyguardViewManager;
	private Object mDozeTouchTrigger;

	public ScreenGestures(Context context) {
		super(context);
	}

	@Override
	public void onPreferenceUpdated(String... Key) {
		doubleTapToWake = Xprefs.getBoolean("doubleTapToWake", false);
		holdScreenTorchEnabled = Xprefs.getBoolean("holdScreenTorchEnabled", false);
		doubleTapToSleepStatusbarEnabled = Xprefs.getBoolean("DoubleTapSleep", false);
		doubleTapToSleepLockscreenEnabled = Xprefs.getBoolean("DoubleTapSleepLockscreen", false);
		TapToShowAmbient = Xprefs.getBoolean("TapToShowAmbient", false);
		PickToShowAmbient = Xprefs.getBoolean("PickToShowAmbient", false);
		DisableLockScreenPill = Xprefs.getBoolean("DisableLockScreenPill", false);
	}

	@Override
	public void onPackageLoaded(XposedModuleInterface.PackageReadyParam PRParam) throws Throwable {
		mLockscreenDoubleTapToSleep = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onDoubleTap(@NonNull MotionEvent e) {
				sleep();
				return true;
			}
		});

		ReflectedClass NotificationShadeWindowViewControllerClass = ReflectedClass.of("com.android.systemui.shade.NotificationShadeWindowViewController");
		ReflectedClass NotificationPanelViewControllerClass = ReflectedClass.of("com.android.systemui.shade.NotificationPanelViewController");
		ReflectedClass DozeTriggersClass = ReflectedClass.of("com.android.systemui.doze.DozeTriggers");
		ReflectedClass PhoneStatusBarViewClass = ReflectedClass.of("com.android.systemui.statusbar.phone.PhoneStatusBarView");
		ReflectedClass TriggerSensorClass = ReflectedClass.of("com.android.systemui.doze.DozeSensors$TriggerSensor");
		ReflectedClass DefaultSettingsPopupMenuSectionClass = ReflectedClass.of("com.android.systemui.keyguard.ui.view.layout.sections.DefaultSettingsPopupMenuSection");

		PhoneStatusBarViewClass
				.before("onTouchEvent")
				.run(param -> {
					if (!doubleTapToSleepStatusbarEnabled) return;

					//double tap to sleep, statusbar only
					if (!(boolean) getObjectField(NotificationPanelViewController, "mPulsing")
							&& !(boolean) getObjectField(NotificationPanelViewController, "mDozing")
							&& (int) getObjectField(NotificationPanelViewController, "mBarState") == SHADE
							&& (boolean) callMethod(NotificationPanelViewController, "isFullyCollapsed")) {
						mLockscreenDoubleTapToSleep.onTouchEvent((MotionEvent) param.args[param.args.length - 1]);
					}
				});

		TriggerSensorClass
				.afterConstruction()
				.run(param -> {
					if(getObjectField(param.thisObject, "mPulseReason").equals(REASON_SENSOR_TAP))
					{
						mDozeTouchTrigger = param.thisObject;
					}

				});

		//double tap detector for screen off AOD disabled sensor
		DozeTriggersClass
				.before("onSensor")
				.run(param -> {
					if ((TapToShowAmbient && !doubleTapToWake && param.args[0].equals(REASON_SENSOR_TAP)) ||
							(PickToShowAmbient && param.args[0].equals(REASON_SENSOR_PICKUP))) {
						showAmbientDisplay(param.thisObject);
						param.setResult(null);
						return; //don't proceed anymore
					}

					if (doubleTapToWake && ((int) param.args[0]) == REASON_SENSOR_TAP) {
						if (!mDoubleTap) {
							callMethod(mDozeTouchTrigger, "updateListening"); //we wasted the event! let's listen again
							mDoubleTap = true;
							mTimer = new Timer();
							mTimer.schedule(new TimerTask() {
								@Override
								public void run() {
									mDoubleTap = false;
									if (TapToShowAmbient)
										showAmbientDisplay(param.thisObject);
								}
							}, 400);

							param.setResult(null);
						}
						else
						{
							mTimer.cancel();
							mDoubleTap = false;
						}
					}
				});

		DefaultSettingsPopupMenuSectionClass
				.before("bindData") //we prevent binding to happen in the first place. otherwise, very hard to control
				.run(param -> {
					if(DisableLockScreenPill) {
						param.setResult(null);
					}
				});

		NotificationShadeWindowViewControllerClass
				.afterConstruction()
				.run(param -> new Thread(() -> {
					SystemUtils.threadSleep(5000); //for some reason lsposed doesn't find methods in the class. so we'll hook to constructor and wait a bit!
					setHooks(param);
				}).start());

		NotificationPanelViewControllerClass
				.afterConstruction()
				.run(param -> {
					NotificationPanelViewController = param.thisObject;

					mStatusBarKeyguardViewManager = getObjectField(param.thisObject, "mStatusBarKeyguardViewManager");
				});

		NotificationPanelViewControllerClass
				.after("createTouchHandler")
				.run(param -> NotificationPanelViewController = param.thisObject);
	}

	private void showAmbientDisplay(Object dozeTrigger) {
		if(mDoubleTap) return;

		new Handler(Looper.getMainLooper()).post(() -> //call must be made from main thread
				callMethod(dozeTrigger, "requestPulse", PULSE_REASON_INTENT, false /* performedProxCheck */, null /* onPulseSuppressedListener */));
	}

	private void setHooks(HookHelper.RunParam param) {
		try {
		Object mPulsingWakeupGestureHandler = getObjectField(param.thisObject, "mPulsingWakeupGestureHandler");//A13 R18

		Object mListener = getObjectField(mPulsingWakeupGestureHandler, "mListener");

		Object mStatusBarStateController = getObjectField(param.thisObject, "mStatusBarStateController");

		//used in double tap to wake in AOD plan

		ReflectedClass listenerClass = ReflectedClass.of(mListener.getClass());

		listenerClass
				.before("onSingleTapUp")
				.run(param3 -> {
					if (doubleTapToWake)
						param3.setResult(false);
				}); //A13 R18

		//used in double tap detection in AOD

		ReflectedClass listenerClass2 = ReflectedClass.of(mListener.getClass());

		listenerClass2
				.before("onDoubleTapEvent")
				.run(param2 -> {
					if (isQSExpanded() || getBooleanField(NotificationPanelViewController, "mBouncerShowing")) {
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
				}); //A13 R18

		//detect hold event for TTT and DTS on lockscreen
		ReflectedClass.of(mPulsingWakeupGestureHandler.getClass())
				.before("onTouchEvent")
				.run(param1 -> {
					if (keyguardNotShowing(mStatusBarKeyguardViewManager)) {
						return;
					}
					MotionEvent ev = (MotionEvent) param1.args[0];

					int action = ev.getActionMasked();

					if (doubleTap && action == ACTION_UP) {
						if (doubleTapToSleepLockscreenEnabled && !isDozing)
							sleep();
						doubleTap = false;
					}

					if (!holdScreenTorchEnabled) return;

					if ((action == ACTION_DOWN || action == ACTION_MOVE)) {
						if(doubleTap || turnedByTTT) //we really don't want to see swipe gestures during TTT
						{
							ev.setAction(ACTION_DOWN);
						}
						if (doubleTap && !SystemUtils.isFlashOn() && uptimeMillis() - ev.getDownTime() > HOLD_DURATION) {
							turnedByTTT = true;

							callMethod(SystemUtils.PowerManager(), "wakeUp", uptimeMillis());
							SystemUtils.setFlash(true, false);
							SystemUtils.vibrate(EFFECT_TICK, USAGE_ACCESSIBILITY);

							new Thread(() -> { //if keyguard is dismissed for any reason (face or udfps touch), then:
								while (turnedByTTT) {
									try {
										SystemUtils.threadSleep(200);
										if (keyguardNotShowing(mStatusBarKeyguardViewManager)) {
											turnOffTTT();
										}
									} catch (Throwable ignored) {}
								}
							}).start();
						}
					} else if (turnedByTTT) {
						turnOffTTT();
					}
				});
		} catch (Throwable ignored) {
			// mPulsingWakeupGestureHandler removed in Android 17 QPR1 Beta 3+ (CP31.260508.005)
		}
	}

	private boolean isQSExpanded() {
		return (boolean) callMethod(NotificationPanelViewController, "isShadeFullyExpanded");
	}

	private boolean keyguardNotShowing(Object mStatusBarKeyguardViewManager) {
		try {
			return !((boolean) callMethod(mStatusBarKeyguardViewManager, "isShowing"));
		} catch (Throwable ignored) {
			return !getBooleanField(mStatusBarKeyguardViewManager, "mLastShowing");
		}
	}

	private void turnOffTTT() {
		turnedByTTT = false;

		SystemUtils.setFlash(false, false);
	}
}
