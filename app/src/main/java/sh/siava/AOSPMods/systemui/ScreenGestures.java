//Credits of double tap to wake go to nijel8 @XDA Thanks!

package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedHelpers.*;
import static de.robv.android.xposed.XposedBridge.*;

import android.content.Context;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.Timer;
import java.util.TimerTask;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.Utils.SystemUtils;
import sh.siava.AOSPMods.XPrefs;
import sh.siava.AOSPMods.XposedModPack;

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
        
        
        findAndHookMethod(NotificationShadeWindowViewControllerClass,
                "setupExpandedStatusBar", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                        Object mGestureDetector = getObjectField(param.thisObject, "mGestureDetector");
                        Object mListener = getObjectField(mGestureDetector, "mListener");
                        Object mKeyguardStateController = getObjectField(param.thisObject, "mKeyguardStateController");
                        Object mStatusBarStateController = getObjectField(param.thisObject, "mStatusBarStateController");
    
                        //used in double tap to wake in AOD plan
                        findAndHookMethod(mListener.getClass(),
                                "onSingleTapConfirmed", MotionEvent.class, new XC_MethodHook() {
                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                        if(doubleTapToWake)
                                            param.setResult(false);
                                    }
                                });
                        
                        //used in double tap detection in AOD
                        findAndHookMethod(mListener.getClass(),
                                "onDoubleTap", MotionEvent.class, new XC_MethodHook() {
                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
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
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                MotionEvent ev = (MotionEvent) param.args[0];
                                int action = ev.getActionMasked();

                                if(doubleTap && action == MotionEvent.ACTION_UP)
                                {
                                    if(doubleTapToSleepEnabled && !isDozing)
                                        SystemUtils.Sleep();
                                    doubleTap = false;
                                }
    
                                if((boolean) callMethod(mKeyguardStateController, "isShowing"))
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
                    }
                });
        
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
