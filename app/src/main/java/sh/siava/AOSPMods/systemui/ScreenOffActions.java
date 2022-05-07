//Credits of double tap to wake go to nijel8 @XDA Thanks!

package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;

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
    
    private static boolean doubleTapToWake = false;
    private static boolean holdScreenTorchEnabled = false;
    private static int upCount = 0;
    private static boolean turnedByTTT = false;
    private static boolean mDoubleTap = false;
    
    long flashOnEventTime = -1;
    
    public ScreenOffActions(Context context) { super(context); }
    
    
    @Override
    public void updatePrefs(String...Key) {
        doubleTapToWake = XPrefs.Xprefs.getBoolean("doubleTapToWake", false);
        holdScreenTorchEnabled = XPrefs.Xprefs.getBoolean("holdScreenTorchEnabled", false);
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
    
    
    
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;
        
        Class<?> NotificationShadeWindowViewControllerClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.NotificationShadeWindowViewController", lpparam.classLoader);
        Class<?> DozeTriggersClass = XposedHelpers.findClass("com.android.systemui.doze.DozeTriggers", lpparam.classLoader);

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
                        
                        XposedBridge.hookAllMethods(mGestureDetector.getClass(), "onTouchEvent", new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                if(!holdScreenTorchEnabled) return;
                                
                                MotionEvent ev = (MotionEvent) param.args[0];
                                
                                int action = ev.getActionMasked();
                                
                                if(action == MotionEvent.ACTION_UP && (boolean) XposedHelpers.callMethod(mKeyguardStateController, "isShowing"))
                                {
                                    upCount++;
                                    new Timer().schedule(new TimerTask() {
                                        @Override
                                        public void run() { if(upCount > 0) upCount --; }
                                    }, HOLD_DURATION * TAP_COUNT);
                                }
                                
                                if(action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE)
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
}
