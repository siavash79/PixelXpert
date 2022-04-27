//Credits go to nijel8 @XDA Thanks!

package sh.siava.AOSPMods.systemui;

import android.view.MotionEvent;

import java.util.Timer;
import java.util.TimerTask;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.IXposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class DoubleTaptoWake implements IXposedModPack {
    public static final String listenPackage = "com.android.systemui";

    public static boolean doubleTapToWake = false;

    private static boolean mDoubleTap = false;

    @Override
    public void updatePrefs(String...Key) {
        doubleTapToWake = XPrefs.Xprefs.getBoolean("doubleTapToWake", false);
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;

        Class<?> NotificationShadeWindowViewControllerClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.NotificationShadeWindowViewController", lpparam.classLoader);
        Class<?> DozeTriggersClass = XposedHelpers.findClass("com.android.systemui.doze.DozeTriggers", lpparam.classLoader);

        XposedHelpers.findAndHookMethod(DozeTriggersClass,
                "onSensor", int.class, float.class, float.class, float[].class, new XC_MethodHook() {
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


                    }
                });
    }
}
