package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.os.Build;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class FeatureFlagsMods extends XposedModPack {
    public static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;

    private static final int SIGNAL_DEFAULT = 0;
    private static final int SIGNAL_FORCE_LTE = 1;
    private static final int SIGNAL_FORCE_4G = 2;

    public static int SBLTEIcon = SIGNAL_DEFAULT;

    public static boolean combinedSignalEnabled = false;

    public FeatureFlagsMods(Context context) { super(context); }

    @Override
    public void updatePrefs(String...Key)
    {
        if(XPrefs.Xprefs == null) return;
        boolean newcombinedSignalEnabled = XPrefs.Xprefs.getBoolean("combinedSignalEnabled", false);

        if(Key.length > 0 && newcombinedSignalEnabled != combinedSignalEnabled)
        {
            try {
                android.os.Process.killProcess(android.os.Process.myPid());
            }catch(Exception ignored){}
        }
        combinedSignalEnabled = newcombinedSignalEnabled;

        SBLTEIcon = Integer.parseInt(XPrefs.Xprefs.getString(
                "LTE4GIconMod",
                String.valueOf(SIGNAL_DEFAULT)));

    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;

        XposedBridge.hookAllMethods(
                XposedHelpers.findClass("com.android.settingslib.mobile.MobileMappings$Config", lpparam.classLoader),
                "readConfig", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(SBLTEIcon == SIGNAL_DEFAULT) return;

                        XposedHelpers.setObjectField(param.getResult(),
                                "show4gForLte",
                                SBLTEIcon == SIGNAL_FORCE_4G);
                    }
                });

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
