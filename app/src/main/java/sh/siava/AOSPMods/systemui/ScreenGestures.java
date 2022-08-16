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
import android.view.View;

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

    //settings
    public static boolean doubleTapToSleepEnabled = false;
    private static boolean doubleTapToWake = false;
    private static boolean holdScreenTorchEnabled = false;

    private static boolean turnedByTTT = false;
    private static boolean mDoubleTap = false;  //double tap to wake when AOD off

    private boolean doubleTap; //double tap event for TTT

    GestureDetector mLockscreenDoubleTapToSleep; //event callback for double tap to sleep detection of statusbar only

    private boolean isDozing; //determiner for wakeup or sleep decision
    private long lastButtonClick = 0;

    public ScreenGestures(Context context) { super(context); }

    @Override
    public void updatePrefs(String...Key) {
        doubleTapToWake = Xprefs.getBoolean("doubleTapToWake", false);
        holdScreenTorchEnabled = Xprefs.getBoolean("holdScreenTorchEnabled", false);
        doubleTapToSleepEnabled = Xprefs.getBoolean("DoubleTapSleep", false);
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;

        mLockscreenDoubleTapToSleep = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                SystemUtils.Sleep();
                return true;
            }
        });

        Class<?> NotificationShadeWindowViewControllerClass = findClass("com.android.systemui.statusbar.phone.NotificationShadeWindowViewController", lpparam.classLoader);
        Class<?> DozeTriggersClass = findClass("com.android.systemui.doze.DozeTriggers", lpparam.classLoader);
        Class<?> NotificationPanelViewControllerClass = findClass("com.android.systemui.statusbar.phone.NotificationPanelViewController", lpparam.classLoader);
        Class<?> NumPadKeyClass = findClass("com.android.keyguard.NumPadKey", lpparam.classLoader);

        hookAllMethods(NumPadKeyClass, "userActivity", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                lastButtonClick = SystemClock.uptimeMillis();
            }
        });

        //double tap detector for screen off AOD disabled sensor
        hookAllMethods(DozeTriggersClass,
                "onSensor", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if(!doubleTapToWake) return;
                        if (((int)param.args[0]) == 9) {
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
                    } catch (Exception ignored) {}

                    Object mGestureDetector = getObjectField(param.thisObject, "mGestureDetector");
                    Object mListener = getObjectField(mGestureDetector, "mListener");

                    Object mStatusBarKeyguardViewManager = getObjectField(param.thisObject,
                            (Build.VERSION.SDK_INT == 33)
                                    ? "mStatusBarKeyguardViewManager" //A13
                                    : "mKeyguardStateController"); // SDK 31, 32

                    Object mStatusBarStateController = getObjectField(param.thisObject, "mStatusBarStateController");

                    //used in double tap to wake in AOD plan
                    findAndHookMethod(mListener.getClass(),
                            "onSingleTapConfirmed", MotionEvent.class, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param1) throws Throwable {
                                    if(doubleTapToWake)
                                        param1.setResult(false);
                                }
                            });

                    //used in double tap detection in AOD
                    findAndHookMethod(mListener.getClass(),
                            "onDoubleTap", MotionEvent.class, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param1) throws Throwable {
                                    if(SystemClock.uptimeMillis() - lastButtonClick < 300)
                                    {
                                        return;
                                    }
                                    doubleTap = true;
                                    new Timer().schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            doubleTap = false;
                                        }
                                    }, HOLD_DURATION*2);

                                    isDozing = (boolean) callMethod(mStatusBarStateController, "isDozing");
                                }
                            });

                    //detect hold event for TTT
                    hookAllMethods(mGestureDetector.getClass(), "onTouchEvent", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param1) throws Throwable {
                            MotionEvent ev = (MotionEvent) param1.args[0];
                            int action = ev.getActionMasked();

                            if(doubleTap && action == MotionEvent.ACTION_UP)
                            {
                                if(doubleTapToSleepEnabled && !isDozing)
                                    SystemUtils.Sleep();
                                doubleTap = false;
                            }
                            if((boolean) callMethod(mStatusBarKeyguardViewManager, "isShowing"))
                            {
                                if(!holdScreenTorchEnabled) return;
                                if((action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE))
                                {
                                    if(doubleTap && !SystemUtils.isFlashOn() && SystemClock.uptimeMillis() - ev.getDownTime() > HOLD_DURATION)
                                    {
                                        turnedByTTT = true;
                                        callMethod(SystemUtils.PowerManager(), "wakeUp", SystemClock.uptimeMillis());
                                        SystemUtils.setFlash(true);
                                        SystemUtils.vibrate(VibrationEffect.EFFECT_TICK);
                                    }
                                    if(turnedByTTT)
                                    {
                                        ev.setAction(MotionEvent.ACTION_DOWN);
                                    }
                                }
                                else if(turnedByTTT)
                                {
                                    turnedByTTT = false;
                                    SystemUtils.setFlash(false);
                                }
                            }
                        }
                    });
                }).start();
            }
        });

        // SDK 31 - Won't hook since method is removed
        findAndHookMethod(NotificationPanelViewControllerClass,
                "createTouchHandler", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object touchHandler = param.getResult();
                        Object ThisNotificationPanel = param.thisObject;

                        //touch detection of statusbar to forward to detector
                        findAndHookMethod(touchHandler.getClass(),
                                "onTouch", View.class, MotionEvent.class, new XC_MethodHook() {
                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                        if (!doubleTapToSleepEnabled) return;

                                        boolean mPulsing = (boolean) getObjectField(ThisNotificationPanel, "mPulsing");
                                        boolean mDozing = (boolean) getObjectField(ThisNotificationPanel, "mDozing");
                                        int mBarState = (int) getObjectField(ThisNotificationPanel, "mBarState");

                                        if (!mPulsing && !mDozing
                                                && mBarState == 0) {
                                            mLockscreenDoubleTapToSleep.onTouchEvent((MotionEvent) param.args[1]);
                                        }
                                    }
                                });
                    }
                });
    }
}
