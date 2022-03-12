package sh.siava.AOSPMods.systemui;

import android.content.Context;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.XPrefs;

public class LTEiconChange implements IXposedHookLoadPackage {
    public static final String listenPackage = "com.android.systemui";
    public static boolean isEnabled = false;

    public static void updatePrefs()
    {
        isEnabled = XPrefs.Xprefs.getBoolean("Show4GIcon", false);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;

        XposedHelpers.findAndHookMethod("com.android.settingslib.mobile.MobileMappings$Config", lpparam.classLoader,
                "readConfig", Context.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(!isEnabled) return;

                        Object config = param.getResult();
                        XposedHelpers.setObjectField(config, "show4gForLte", true);
                    }
                });
    }
}
