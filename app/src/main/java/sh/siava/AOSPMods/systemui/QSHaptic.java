package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.os.Vibrator;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.IXposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class QSHaptic implements IXposedModPack {
    public static final String listenPackage = "com.android.systemui";
    public static boolean QSHapticEnabled = false;
    public static boolean hasVibrator = false;
    private static Vibrator mVibrator;
    private Context mContext;

    public void updatePrefs(String...Key)
    {
        if(XPrefs.Xprefs == null) return;
        QSHapticEnabled = XPrefs.Xprefs.getBoolean("QSHapticEnabled", false);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam, Context context) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;

        mContext = context;

        Class<?> QSTileImplClass = XposedHelpers.findClass("com.android.systemui.qs.tileimpl.QSTileImpl", lpparam.classLoader);

        XposedBridge.hookAllConstructors(QSTileImplClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
                if(mVibrator.hasVibrator())
                {
                   hasVibrator = true;
                }
            }
        });

        XposedHelpers.findAndHookMethod(QSTileImplClass,
                "click", View.class , new vibrateFeedback());

        XposedHelpers.findAndHookMethod(QSTileImplClass,
                "longClick", View.class , new vibrateFeedback());
    }

    static class vibrateFeedback extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if(!QSHapticEnabled) return;
            XposedHelpers.callMethod(mVibrator, "vibrate", 8);
        }
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }


}
