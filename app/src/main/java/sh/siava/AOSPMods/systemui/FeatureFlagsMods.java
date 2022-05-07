package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.os.Build;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class FeatureFlagsMods extends XposedModPack {
    public static final String listenPackage = "com.android.systemui";

    public static boolean combinedSignalEnabled = false;
    
    public FeatureFlagsMods(Context context) { super(context); }

    @Override
    public void updatePrefs(String...Key)
    {
        if(XPrefs.Xprefs == null) return;
        combinedSignalEnabled = XPrefs.Xprefs.getBoolean("combinedSignalEnabled", false);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;

        if(Build.VERSION.SDK_INT < 32) return; //Feature flags is newly introduced!

        Class<?> FeatureFlagsClass = XposedHelpers.findClass("com.android.systemui.flags.FeatureFlags", lpparam.classLoader);

        XposedHelpers.findAndHookMethod(FeatureFlagsClass, "isCombinedStatusBarSignalIconsEnabled", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(combinedSignalEnabled);
            }
        });
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }

}
