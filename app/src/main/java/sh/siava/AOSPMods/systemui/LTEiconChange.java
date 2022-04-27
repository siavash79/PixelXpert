package sh.siava.AOSPMods.systemui;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.IXposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class LTEiconChange implements IXposedModPack {
    public static final String listenPackage = "com.android.systemui";
    public static int SBLTEIcon = 0;

    private static final int DEFAULT = 0;
    private static final int FORCE_LTE = 1;
    private static final int FORCE_4G = 2;

    public void updatePrefs(String...Key)
    {
        if(XPrefs.Xprefs == null) return;
        SBLTEIcon = Integer.parseInt(XPrefs.Xprefs.getString("LTE4GIconMod", "0"));
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;


        XposedHelpers.findAndHookMethod("com.android.settingslib.mobile.MobileMappings$Config", lpparam.classLoader,
                "readConfig", Context.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(SBLTEIcon == DEFAULT) return;

                        Object config = param.getResult();
                        if(SBLTEIcon == FORCE_LTE) {
                            XposedHelpers.setObjectField(config, "show4gForLte", false);
                        }
                        else
                        {
                            XposedHelpers.setObjectField(config, "show4gForLte", true);
                        }
                    }
                });
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }


}
