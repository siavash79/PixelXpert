package sh.siava.AOSPMods.systemui;

import android.view.View;

import java.io.IOException;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.XPrefs;

public class BackToKill implements IXposedHookLoadPackage {
    private static final String listenPackage = "com.android.systemui";
    private static boolean isEnabled = false;

    public static void updatePrefs()
    {
        isEnabled = XPrefs.Xprefs.getBoolean("BackLongPressKill", false);
    }


    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if(!lpparam.packageName.equals(listenPackage)) return;


        Class NavBarClass = XposedHelpers.findClass("com.android.systemui.navigationbar.NavigationBar", lpparam.classLoader);

        View.OnLongClickListener listener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(!isEnabled) return true;
                try {
                    Runtime.getRuntime().exec("su -c am force-stop $(dumpsys window | grep mCurrentFocus | cut -d \"/\" -f1 | cut -d \" \" -f5)");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                
                return true;
            }
        };

        XposedHelpers.findAndHookMethod(NavBarClass,
                "getView", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object mNavigationBarView = XposedHelpers.getObjectField(param.thisObject, "mNavigationBarView");
                        Object backButton = XposedHelpers.callMethod(mNavigationBarView, "getBackButton");

                        XposedHelpers.callMethod(backButton, "setLongClickable", true);
                        XposedHelpers.callMethod(backButton, "setOnLongClickListener", listener);
                    }
                });
    }
}
