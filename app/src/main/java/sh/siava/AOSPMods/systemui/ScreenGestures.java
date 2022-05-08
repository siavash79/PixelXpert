//Credits of double tap to wake go to nijel8 @XDA Thanks!

package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.Timer;
import java.util.TimerTask;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.Utils.System;
import sh.siava.AOSPMods.XPrefs;
import sh.siava.AOSPMods.XposedModPack;

public class ScreenGestures extends XposedModPack {
    public static final String listenPackage = "com.android.systemui";

    private static final long HOLD_DURATION = 500;
    
    public static boolean doubleTapToSleepEnabled = false;
    
    private static boolean doubleTapToWake = false;
    private static boolean holdScreenTorchEnabled = false;
    private static boolean turnedByTTT = false;
    private static boolean mDoubleTap = false;
    
    long flashOnEventTime = -1;
    private boolean doubleTap;
    GestureDetector mLockscreenDoubleTapToSleep;
    private boolean isDozing;
    
    public ScreenGestures(Context context) { super(context); }
    
    @Override
    public void updatePrefs(String...Key) {
        doubleTapToWake = XPrefs.Xprefs.getBoolean("doubleTapToWake", false);
        holdScreenTorchEnabled = XPrefs.Xprefs.getBoolean("holdScreenTorchEnabled", false);
        doubleTapToSleepEnabled = XPrefs.Xprefs.getBoolean("DoubleTapSleep", false);
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
    
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;
    
        mLockscreenDoubleTapToSleep = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                System.Sleep();
                return true;
            }
        });
    
        Class<?> NotificationShadeWindowViewControllerClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.NotificationShadeWindowViewController", lpparam.classLoader);
        Class<?> DozeTriggersClass = XposedHelpers.findClass("com.android.systemui.doze.DozeTriggers", lpparam.classLoader);
        Class<?> NotificationPanelViewControllerClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.NotificationPanelViewController", lpparam.classLoader);
    
    
        XposedBridge.hookAllMethods(DozeTriggersClass,
                "onSensor", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if(!doubleTapToWake) return;
                        if (((int)param.args[0]) == 9) {
                            if (!mDoubleTap) {
                                param.setResult(null);
                                mDoubleTap = true;
                            }
                        }
                    }
                });
        
        XposedHelpers.findAndHookMethod(NotificationShadeWindowViewControllerClass,
                "setupExpandedStatusBar", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                        Object mGestureDetector = XposedHelpers.getObjectField(param.thisObject, "mGestureDetector");
                        Object mListener = XposedHelpers.getObjectField(mGestureDetector, "mListener");
                        Object mKeyguardStateController = XposedHelpers.getObjectField(param.thisObject, "mKeyguardStateController");
                        Object mStatusBarStateController = XposedHelpers.getObjectField(param.thisObject, "mStatusBarStateController");
    
                        XposedHelpers.findAndHookMethod(mListener.getClass(),
                                "onSingleTapConfirmed", MotionEvent.class, new XC_MethodHook() {
                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                        if(!doubleTapToWake) return;
                                        
                                        param.setResult(false);
                                    }
                                });
                        XposedHelpers.findAndHookMethod(mListener.getClass(),
                                "onDoubleTap", MotionEvent.class, new XC_MethodHook() {
                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                        doubleTap = true;
                                        new Timer().schedule(new TimerTask() {
                                            @Override
                                            public void run() {
                                                doubleTap = false;
                                            }
                                        }, HOLD_DURATION*2);
    
                                        isDozing = (boolean) XposedHelpers.callMethod(mStatusBarStateController, "isDozing");
                                    }
                                });
    
                        XposedBridge.hookAllMethods(mGestureDetector.getClass(), "onTouchEvent", new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                MotionEvent ev = (MotionEvent) param.args[0];
                                int action = ev.getActionMasked();
    
                                if(doubleTap && action == MotionEvent.ACTION_UP)
                                {
                                    if(doubleTapToSleepEnabled && !isDozing)
                                        System.Sleep();
                                    doubleTap = false;
                                }
    
                                if((boolean) XposedHelpers.callMethod(mKeyguardStateController, "isShowing"))
                                {
    
                                    if(!holdScreenTorchEnabled) return;
                                    if((action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE))
                                    {
                                        if(doubleTap && !System.isScreenCovered() && !System.isFlashOn() && SystemClock.uptimeMillis() - ev.getDownTime() > HOLD_DURATION)
                                        {
                                            flashOnEventTime = ev.getDownTime();
                                            turnedByTTT = true;
                                            XposedHelpers.callMethod(System.PowerManager(), "wakeUp", SystemClock.uptimeMillis());
                                            System.setFlash(true);
                                        }
                                        if(turnedByTTT)
                                        {
                                            ev.setAction(MotionEvent.ACTION_DOWN);
                                        }
                                    }
                                    else if(turnedByTTT)
                                    {
                                        turnedByTTT = false;
                                        System.setFlash(false);
                                    }
                                }
                            }
                        });
                    }
                });
        
        XposedHelpers.findAndHookMethod(NotificationPanelViewControllerClass,
                "createTouchHandler", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object touchHandler = param.getResult();
                        Object ThisNotificationPanel = param.thisObject;
                    
                        XposedHelpers.findAndHookMethod(touchHandler.getClass(),
                                "onTouch", View.class, MotionEvent.class, new XC_MethodHook() {
                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                        if (!doubleTapToSleepEnabled) return;
                                    
                                        boolean mPulsing = (boolean) XposedHelpers.getObjectField(ThisNotificationPanel, "mPulsing");
                                        boolean mDozing = (boolean) XposedHelpers.getObjectField(ThisNotificationPanel, "mDozing");
                                        int mBarState = (int) XposedHelpers.getObjectField(ThisNotificationPanel, "mBarState");
                                    
                                        if (!mPulsing && !mDozing
                                                && mBarState == 0 && mLockscreenDoubleTapToSleep != null) {
                                            mLockscreenDoubleTapToSleep.onTouchEvent((MotionEvent) param.args[1]);
                                        }
                                    }
                                });
                    }
                });
    
    }
    
    
}
