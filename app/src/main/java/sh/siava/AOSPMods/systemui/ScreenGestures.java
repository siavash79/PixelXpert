//Credits of double tap to wake go to nijel8 @XDA Thanks!

package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.util.Timer;
import java.util.TimerTask;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class ScreenGestures extends XposedModPack {
	public static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;

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
	private long lastButtonClick = 0;

	public ScreenGestures(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		doubleTapToWake = Xprefs.getBoolean("doubleTapToWake", false);
		holdScreenTorchEnabled = Xprefs.getBoolean("holdScreenTorchEnabled", false);
		doubleTapToSleepStatusbarEnabled = Xprefs.getBoolean("DoubleTapSleep", false);
		doubleTapToSleepLockscreenEnabled = Xprefs.getBoolean("DoubleTapSleepLockscreen", false);
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
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

		Class<?> NotificationShadeWindowViewControllerClass;
		Class<?> NotificationPanelViewControllerClass;
		try
		{ //A13 R18
			NotificationShadeWindowViewControllerClass = findClass("com.android.systemui.shade.NotificationShadeWindowViewController", lpparam.classLoader);
			NotificationPanelViewControllerClass = findClass("com.android.systemui.shade.NotificationPanelViewController", lpparam.classLoader);
		}
		catch (Throwable ignored)
		{ //Older
			NotificationShadeWindowViewControllerClass = findClass("com.android.systemui.statusbar.phone.NotificationShadeWindowViewController", lpparam.classLoader);
			NotificationPanelViewControllerClass = findClass("com.android.systemui.statusbar.phone.NotificationPanelViewController", lpparam.classLoader);
		}
		Class<?> DozeTriggersClass = findClass("com.android.systemui.doze.DozeTriggers", lpparam.classLoader);
		Class<?> KeyguardAbsKeyInputViewControllerClass = findClass("com.android.keyguard.KeyguardAbsKeyInputViewController", lpparam.classLoader);

		hookAllMethods(KeyguardAbsKeyInputViewControllerClass, "onUserInput", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				lastButtonClick = SystemClock.uptimeMillis();
			}
		});

		//double tap detector for screen off AOD disabled sensor
		hookAllMethods(DozeTriggersClass,
				"onSensor", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						if (!doubleTapToWake) return;
						if (((int) param.args[0]) == 9) {
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

		if (Build.VERSION.SDK_INT == 33) {
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

			Class<?> finalNotificationPanelViewControllerClass = NotificationPanelViewControllerClass;
			hookAllConstructors(NotificationPanelViewControllerClass, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					Object mTouchHandler = finalNotificationPanelViewControllerClass.getField("mStatusBarViewTouchEventHandler").get(param.thisObject);
					if(mTouchHandler == null)
					{
						mTouchHandler = finalNotificationPanelViewControllerClass.getField("mTouchHandler").get(param.thisObject);
					}
					hookTouchHandler(param, mTouchHandler);
				}
			});

		} else {
			findAndHookMethod(NotificationShadeWindowViewControllerClass,
					"setupExpandedStatusBar", new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param) throws Throwable {
							setHooks(param);
						}
					});

			findAndHookMethod(NotificationPanelViewControllerClass,
					"createTouchHandler", new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param) throws Throwable {
							Object touchHandler = param.getResult();
							hookTouchHandler(param, touchHandler);
						}
					});
		}
	}

	private void hookTouchHandler(XC_MethodHook.MethodHookParam param, Object mTouchHandler) {
		Object ThisNotificationPanel = param.thisObject;

		XC_MethodHook touchHook = new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (!doubleTapToSleepStatusbarEnabled) return;

				//double tap to sleep, statusbar only
				if (!(boolean) getObjectField(ThisNotificationPanel, "mPulsing")
						&& !(boolean) getObjectField(ThisNotificationPanel, "mDozing")
						&& (int) getObjectField(ThisNotificationPanel, "mBarState") == SHADE
						&& (boolean) callMethod(ThisNotificationPanel, "isFullyCollapsed")) {
					mLockscreenDoubleTapToSleep.onTouchEvent((MotionEvent) param.args[param.args.length-1]);
				}
			}
		};

		hookAllMethods(mTouchHandler.getClass(), "handleTouchEvent", touchHook); //A13 R18
		hookAllMethods(mTouchHandler.getClass(), "onTouch", touchHook); //older
	}

	private void setHooks(XC_MethodHook.MethodHookParam param) {
		Object mGestureDetector = getObjectField(param.thisObject, "mPulsingWakeupGestureHandler");//A13 R18
		if(mGestureDetector == null)
		{
			mGestureDetector = getObjectField(param.thisObject, "mGestureDetector"); //older
		}
		Object mListener = getObjectField(mGestureDetector, "mListener");

		Object mStatusBarKeyguardViewManager = getObjectField(param.thisObject,
				(Build.VERSION.SDK_INT == 33)
						? "mStatusBarKeyguardViewManager" //A13
						: "mKeyguardStateController"); // SDK 31, 32

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
				if (SystemClock.uptimeMillis() - lastButtonClick < 300) {
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
		hookAllMethods(mGestureDetector.getClass(), "onTouchEvent", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param1) throws Throwable {
				if(!(boolean) callMethod(mStatusBarKeyguardViewManager, "isShowing"))
				{
					return;
				}
				MotionEvent ev = (MotionEvent) param1.args[0];

				int action = ev.getActionMasked();

				if (doubleTap && action == MotionEvent.ACTION_UP) {
					if (doubleTapToSleepLockscreenEnabled && !isDozing)
						SystemUtils.Sleep();
					doubleTap = false;
				}

				if (!holdScreenTorchEnabled) return;
				if ((action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE)) {
					if (doubleTap && !SystemUtils.isFlashOn() && SystemClock.uptimeMillis() - ev.getDownTime() > HOLD_DURATION) {
						turnedByTTT = true;
						callMethod(SystemUtils.PowerManager(), "wakeUp", SystemClock.uptimeMillis());
						SystemUtils.setFlash(true);
						SystemUtils.vibrate(VibrationEffect.EFFECT_TICK);
					}
					if (turnedByTTT) {
						ev.setAction(MotionEvent.ACTION_DOWN);
					}
				} else if (turnedByTTT) {
					turnedByTTT = false;
					SystemUtils.setFlash(false);
				}
			}
		});
	}
}
