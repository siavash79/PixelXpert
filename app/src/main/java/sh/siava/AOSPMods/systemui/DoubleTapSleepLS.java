package sh.siava.AOSPMods.systemui;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import android.content.Context;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class DoubleTapSleepLS implements IXposedHookLoadPackage {
    public static final String listenPackage = "com.android.systemui";
    public static boolean doubleTapToSleepEnabled = false;

    final Context[] context = new Context[1];

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if(!lpparam.packageName.equals(listenPackage)) return;

        GestureDetector mLockscreenDoubleTapToSleep = new GestureDetector(context[0], new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                //Xposedbridge.log("got DT");

                PowerManager pm = (PowerManager) context[0].getSystemService(Context.POWER_SERVICE);
                if (pm != null) {
                    XposedHelpers.callMethod(pm, "goToSleep", SystemClock.uptimeMillis());

                }
                return true;
            }
        });

        Class NotificationPanelViewControllerClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.NotificationPanelViewController", lpparam.classLoader);
        XposedBridge.hookAllConstructors(NotificationPanelViewControllerClass,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object mView = XposedHelpers.getObjectField(param.thisObject, "mView");
                        context[0] = (Context) XposedHelpers.callMethod(mView, "getContext");
                    }
                });
        XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.NotificationPanelViewController", lpparam.classLoader,
                "createTouchHandler", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object touchHandler = param.getResult();
                        boolean mPulsing = (boolean) XposedHelpers.getObjectField(param.thisObject, "mPulsing");
                        boolean mDozing = (boolean) XposedHelpers.getObjectField(param.thisObject, "mDozing");
                        int mBarState = (int) XposedHelpers.getObjectField(param.thisObject, "mBarState");

                        XposedHelpers.findAndHookMethod((Class)touchHandler.getClass(),
                                "onTouch", View.class, MotionEvent.class, new XC_MethodHook() {
                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                        //Xposedbridge.log("puls " + mPulsing);
                                        //Xposedbridge.log("doze " + mDozing);
                                        //Xposedbridge.log("bar " + mBarState);
                                        if (doubleTapToSleepEnabled && !mPulsing && !mDozing
                                                && mBarState == 0) {
                                            //Xposedbridge.log("check for DT " + mBarState);
                                            mLockscreenDoubleTapToSleep.onTouchEvent((MotionEvent) param.args[1]);
                                        }
                                    }
                                });
                    }
                });

    }
}
