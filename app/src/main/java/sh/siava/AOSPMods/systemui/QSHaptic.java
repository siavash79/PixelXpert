package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.os.Vibrator;
import android.view.View;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.XPrefs;

public class QSHaptic implements IXposedHookLoadPackage {
    public static final String listenPackage = "com.android.systemui";
    public static boolean QSHapticEnabled = true;
    public static boolean hasVibrator = false;
    private static Vibrator mVibrator;

    public static void updatePrefs()
    {
        QSHapticEnabled = XPrefs.Xprefs.getBoolean("QSHapticEnabled", true);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;

        Class QSTileImplClass = XposedHelpers.findClass("com.android.systemui.qs.tileimpl.QSTileImpl", lpparam.classLoader);
        XposedBridge.hookAllConstructors(QSTileImplClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
                if(mVibrator.hasVibrator())
                {
                   hasVibrator = true;
                }
            }
        });

        XposedHelpers.findAndHookMethod(QSTileImplClass,
                "click", View.class ,new vibrateFeedback());

        XposedHelpers.findAndHookMethod(QSTileImplClass,
                "longClick", View.class ,new vibrateFeedback());
    }

    class vibrateFeedback extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            XposedHelpers.callMethod(mVibrator, "vibrate", 8);
        }
    }


}
