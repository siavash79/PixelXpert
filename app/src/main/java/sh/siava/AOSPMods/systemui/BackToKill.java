package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.view.View;

import com.topjohnwu.superuser.Shell;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class BackToKill extends XposedModPack {
    private static final String listenPackage = "com.android.systemui";
    private static boolean isEnabled = false;
    
    public BackToKill(Context context) { super(context); }
    
    @Override
    public void updatePrefs(String...Key)
    {
        if(XPrefs.Xprefs == null) return;
        isEnabled = XPrefs.Xprefs.getBoolean("BackLongPressKill", false);
    }


    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if(!lpparam.packageName.equals(listenPackage)) return;


        Class<?> NavBarClass = XposedHelpers.findClass("com.android.systemui.navigationbar.NavigationBar", lpparam.classLoader);

        View.OnLongClickListener listener = v -> {
            if(!isEnabled) return true;

            Shell.cmd("am force-stop $(dumpsys window | grep mCurrentFocus | cut -d \"/\" -f1 | cut -d \" \" -f5)").submit();

            return true;
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

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }

}
