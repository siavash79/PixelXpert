package sh.siava.AOSPMods.systemui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.IXposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class NavBarResizer implements IXposedModPack {
    public static final String listenPackage = "com.android.systemui";
    public static boolean isEnabled = false;
    public static float sizeFactor = 1f;

    private static Object mNavigationBarInflaterView = null;

    public void updatePrefs(String...Key)
    {
        if(XPrefs.Xprefs == null) return;

        boolean newisEnabled = XPrefs.Xprefs.getBoolean("GesPillWidthMod", false);
        float newsizeFactor = XPrefs.Xprefs.getInt("GesPillWidthModPos", 50) * .02f;

        if(isEnabled != newisEnabled || sizeFactor != newsizeFactor)
        {
            isEnabled = newisEnabled;
            sizeFactor = newsizeFactor;

            if(mNavigationBarInflaterView != null)
            {
                XposedHelpers.callMethod(mNavigationBarInflaterView, "clearViews");
                Object defaultLayout = XposedHelpers.callMethod(mNavigationBarInflaterView, "getDefaultLayout");
                XposedHelpers.callMethod(mNavigationBarInflaterView, "inflateLayout", defaultLayout);
            }
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;

        Class<?> NavigationBarInflaterViewClass = XposedHelpers.findClass("com.android.systemui.navigationbar.NavigationBarInflaterView", lpparam.classLoader);

        XposedBridge.hookAllConstructors(NavigationBarInflaterViewClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mNavigationBarInflaterView = param.thisObject;
            }
        });

        XposedHelpers.findAndHookMethod("com.android.systemui.navigationbar.NavigationBarInflaterView", lpparam.classLoader,
                "createView", String.class, ViewGroup.class, LayoutInflater.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(!isEnabled) return;

                        String button = (String) XposedHelpers.callMethod(param.thisObject, "extractButton", param.args[0]);
                        if(!button.equals("home_handle")) return;

                        View v = (View) param.getResult();
                        final ViewGroup.LayoutParams lp = v.getLayoutParams();
                        lp.width = Math.round(lp.width * sizeFactor);
                        v.setLayoutParams(lp);
                    }
                });
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }


}
