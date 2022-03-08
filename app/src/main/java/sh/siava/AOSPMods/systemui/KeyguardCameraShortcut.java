package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.util.AttributeSet;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class KeyguardCameraShortcut implements IXposedHookLoadPackage {
    public static final String listenPackage = "com.android.systemui";
    public static boolean showCameraOnLockscreen = true;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage) || !showCameraOnLockscreen) return;

        XposedHelpers.findAndHookConstructor("com.android.systemui.statusbar.phone.KeyguardBottomAreaView", lpparam.classLoader,
                Context.class, AttributeSet.class, int.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedHelpers.setObjectField( param.thisObject, "mShowCameraAffordance", true);
                    }
                });
    }
}
