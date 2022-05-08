//Credits of double tap to wake go to nijel8 @XDA Thanks!

package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.os.SystemClock;
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

public class ScreenOffActions extends XposedModPack {
    public static final String listenPackage = "com.android.systemui";

    private static final long HOLD_DURATION = 400;
    private static final int TAP_COUNT = 2;
    
    public static boolean doubleTapToSleepEnabled = false;
    
    private static boolean doubleTapToWake = false;
    private static boolean holdScreenTorchEnabled = false;
    private static int upCount = 0;
    private static boolean turnedByTTT = false;
    private static boolean mDoubleTap = false;
    
    long flashOnEventTime = -1;
    private Object NPVC;
    
    public ScreenOffActions(Context context) { super(context); }
    
    
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
        
        Class<?> NotificationShadeWindowViewControllerClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.NotificationShadeWindowViewController", lpparam.classLoader);
        Class<?> DozeTriggersClass = XposedHelpers.findClass("com.android.systemui.doze.DozeTriggers", lpparam.classLoader);
        Class<?> NotificationPanelViewControllerClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.NotificationPanelViewController", lpparam.classLoader);
    
        XposedBridge.hookAllConstructors(NotificationPanelViewControllerClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                NPVC = param.thisObject;
            }
        });
    
        XposedBridge.hookAllMethods(DozeTriggersClass,
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
        
        XposedHelpers.findAndHookMethod(NotificationShadeWindowViewControllerClass,
                "setupExpandedStatusBar", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                        Object mGestureDetector = XposedHelpers.getObjectField(param.thisObject, "mGestureDetector");
                        Object mListener = XposedHelpers.getObjectField(mGestureDetector, "mListener");
                        Object mKeyguardStateController = XposedHelpers.getObjectField(param.thisObject, "mKeyguardStateController");
    
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
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        if(!doubleTapToWake) return;

                                        param.setResult(true);
                                    }
                                });
                        
                        XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.NotificationPanelViewController", lpparam.classLoader,
                                "createTouchHandler", new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        Object touchHandler = param.getResult();
                                        Object ThisNotificationPanel = param.thisObject;
                    
                                        XposedHelpers.findAndHookMethod(touchHandler.getClass(),
                                                "onTouch", View.class, MotionEvent.class, new XC_MethodHook() {
                                                    @Override
                                                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                                        if(!doubleTapToSleepEnabled) return;
                                    
                                                        boolean mPulsing = (boolean) XposedHelpers.getObjectField(ThisNotificationPanel, "mPulsing");
                                                        boolean mDozing = (boolean) XposedHelpers.getObjectField(ThisNotificationPanel, "mDozing");
                                                        int mBarState = (int) XposedHelpers.getObjectField(ThisNotificationPanel, "mBarState");
                                                        
                                                        MotionEvent ev = (MotionEvent) param.args[0];
                                                        
                                                        XposedBridge.log("ac "+ ev.getActionMasked());
                                                        
                                                        if(ev.getActionMasked() == MotionEvent.ACTION_UP)
                                                        {
                                                            XposedBridge.log("received up");
                                                        }
                                    
                                                        if (!mPulsing && !mDozing
                                                                && mBarState < 2) {
                                                            OnUpAction();
                                                        }
                                                    }
                                                });
                                    }
                                });
    
    
                        XposedBridge.hookAllMethods(mGestureDetector.getClass(), "onTouchEvent", new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                MotionEvent ev = (MotionEvent) param.args[0];
                                
                                int action = ev.getActionMasked();
                                
                                if(action == MotionEvent.ACTION_UP)
                                {
                                    OnUpAction();
                                }
                                
                                if(!holdScreenTorchEnabled) return;
                                if((action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) && (boolean) XposedHelpers.callMethod(mKeyguardStateController, "isShowing"))
                                {
                                    if((upCount > TAP_COUNT - 2) && !System.isScreenCovered() && !System.isFlashOn() && SystemClock.uptimeMillis() - ev.getDownTime() > HOLD_DURATION)
                                    {
                                        flashOnEventTime = ev.getDownTime();
                                        turnedByTTT = true;
                                        XposedHelpers.callMethod(System.PowerManager(), "wakeUp", SystemClock.uptimeMillis());
                                        upCount = 0;
                                        System.setFlash(true);
                                    }
                                }
                                else if(turnedByTTT)
                                {
                                    turnedByTTT = false;
                                    System.setFlash(false);
                                }
                            }
                        });
                    }
                });
    }
    
    private void OnUpAction() {
        upCount++;
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() { if(upCount > 0) upCount --; }
        }, HOLD_DURATION * TAP_COUNT);
        XposedBridge.log("up "+ upCount);
        if(upCount >= 2 && doubleTapToSleepEnabled && !XposedHelpers.getBooleanField(NPVC, "mDozing"))
        {
            System.Sleep();
            upCount = 0;
        }
    }
}
