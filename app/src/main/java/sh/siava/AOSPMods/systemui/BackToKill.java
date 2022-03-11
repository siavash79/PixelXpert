package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.view.View;

import java.io.IOException;
import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class BackToKill implements IXposedHookLoadPackage {
    private static final String listenPackage = "com.android.systemui";


    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if(!lpparam.packageName.equals(listenPackage)) return;



/*        XposedBridge.hookAllConstructors((Class)XposedHelpers.findClass("com.android.server.policy.PhoneWindowManager", lpparam.classLoader)
                , new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("SIAPOSED phone window did init");

                    }
                });

        XposedHelpers.findAndHookMethod("com.android.server.policy.PhoneWindowManager", lpparam.classLoader,
                "backLongPress", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param)  {
                        XposedBridge.log("SIAPOSED found back longpress");

                        XposedHelpers.setObjectField(param.thisObject, "mBackKeyHandled", true);
                        //Runtime.getRuntime().exec("su -c  am force-stop $(dumpsys window | grep mCurrentFocus | cut -d \"/\" -f1 | cut -d \" \" -f5)");
                        param.setResult(null);
                    }
                });
        XposedHelpers.findAndHookMethod("com.android.server.policy.PhoneWindowManager", lpparam.classLoader,
                "hasLongPressOnBackBehavior", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log("SIAPOSED hasLongPressOnBackBehavior called");

                        param.setResult(true);
                    }
                });

        XposedHelpers.findAndHookMethod("com.android.server.policy.PhoneWindowManager", lpparam.classLoader,
                "init", Context.class, "android.view.IWindowManager", "com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        XposedHelpers.setObjectField(param.thisObject, "mLongPressOnBackBehavior", 2);
                        XposedBridge.log("SIAPOSED init called");
                    }
                });
*/

        Class NavBarClass = XposedHelpers.findClass("com.android.systemui.navigationbar.NavigationBar", lpparam.classLoader);

        View.OnLongClickListener listener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                XposedBridge.log("SIAPOSED: back long detected");
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
