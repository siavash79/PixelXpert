package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XPrefs;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class NavBarResizer extends XposedModPack {
    public static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;
    public static float widthFactor = 1f;

    private static Object mNavigationBarInflaterView = null;
    private static int GesPillHeightFactor =100;

    public NavBarResizer(Context context) { super(context); }
    
    public void updatePrefs(String...Key)
    {
        if(XPrefs.Xprefs == null) return;

        widthFactor = XPrefs.Xprefs.getInt("GesPillWidthModPos", 50) * .02f;
        GesPillHeightFactor = XPrefs.Xprefs.getInt("GesPillHeightFactor", 100);

        if(Key.length > 0)
        {
            switch (Key[0])
            {
                case "GesPillWidthModPos":
                case "GesPillHeightFactor":
                    refreshNavbar();
                    break;
            }
        }
    }

    private void refreshNavbar() {
        try
        {
            XposedHelpers.callMethod(mNavigationBarInflaterView, "clearViews");
            Object defaultLayout = XposedHelpers.callMethod(mNavigationBarInflaterView, "getDefaultLayout");
            XposedHelpers.callMethod(mNavigationBarInflaterView, "inflateLayout", defaultLayout);
        }catch (Throwable ignored){}
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;

        Class<?> NavigationBarInflaterViewClass = XposedHelpers.findClass("com.android.systemui.navigationbar.NavigationBarInflaterView", lpparam.classLoader);
        Class<?> NavigationHandleClass = XposedHelpers.findClass("com.android.systemui.navigationbar.gestural.NavigationHandle", lpparam.classLoader);

        XposedBridge.hookAllMethods(NavigationHandleClass,
                "onDraw", new XC_MethodHook() {
                    int mRadius = 0;
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if(GesPillHeightFactor != 100) {
                            mRadius = XposedHelpers.getIntField(param.thisObject, "mRadius");
                            XposedHelpers.setObjectField(param.thisObject, "mRadius", Math.round(mRadius * GesPillHeightFactor / 100f));
                        }
//                        Paint mPaint = (Paint) XposedHelpers.getObjectField(param.thisObject, "mPaint");
                    }
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(mRadius > 0) {
                            XposedHelpers.setObjectField(param.thisObject, "mRadius", mRadius);
                        }
                    }
                });


        XposedBridge.hookAllConstructors(NavigationBarInflaterViewClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mNavigationBarInflaterView = param.thisObject;
                refreshNavbar();
            }
        });

        XposedBridge.hookAllMethods(NavigationBarInflaterViewClass,
                "createView", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(widthFactor != 1f) {
                            String button = (String) XposedHelpers.callMethod(param.thisObject, "extractButton", param.args[0]);
                            if (!button.equals("home_handle")) return;

                            View result = (View) param.getResult();
                            ViewGroup.LayoutParams resultLayoutParams = result.getLayoutParams();
                            resultLayoutParams.width = Math.round(resultLayoutParams.width * widthFactor);
                            result.setLayoutParams(resultLayoutParams);
                        }
                    }
                });
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }


}
