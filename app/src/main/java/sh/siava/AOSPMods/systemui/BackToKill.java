package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedHelpers.*;
import static de.robv.android.xposed.XposedBridge.*;

import android.content.Context;
import android.view.View;

import com.topjohnwu.superuser.Shell;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class BackToKill extends XposedModPack {
    private static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;
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


        Class<?> NavBarClass = findClass("com.android.systemui.navigationbar.NavigationBar", lpparam.classLoader);

        View.OnLongClickListener listener = v -> {
            if(!isEnabled) return true;

            Shell.cmd("am force-stop $(dumpsys window | grep mCurrentFocus | cut -d \"/\" -f1 | cut -d \" \" -f5)").submit();

            return true;
        };

        findAndHookMethod(NavBarClass,
                "getView", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object mNavigationBarView = getObjectField(param.thisObject, "mNavigationBarView");
                        Object backButton = callMethod(mNavigationBarView, "getBackButton");

                        callMethod(backButton, "setLongClickable", true);
                        callMethod(backButton, "setOnLongClickListener", listener);
                    }
                });
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }

}
