package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.telecom.Call;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import javax.security.auth.callback.Callback;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XPrefs;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class GestureNavbarManager extends XposedModPack {
    public static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;

    //region Back gesture
    private static float backGestureHeightFractionLeft = 1f; // % of screen height. can be anything between 0 to 1
    private static float backGestureHeightFractionRight = 1f; // % of screen height. can be anything between 0 to 1
    private static boolean leftEnabled = true;
    private static boolean rightEnabled = true;
    //endregion

    //region pill size
    private static int GesPillHeightFactor =100;
    public static float widthFactor = 1f;

    private Object mNavigationBarInflaterView = null;
    //endregion

    //region pill color
    private static boolean navPillColorAccent = false;
    private int mLightColor, mDarkColor; //original navbar colors
    //endregion

    public GestureNavbarManager(Context context) { super(context); }
    
    public void updatePrefs(String...Key)
    {
        if(XPrefs.Xprefs == null) return;

        //region Back gesture
        leftEnabled = XPrefs.Xprefs.getBoolean("BackFromLeft", true);
        rightEnabled = XPrefs.Xprefs.getBoolean("BackFromRight", true);
        backGestureHeightFractionLeft = XPrefs.Xprefs.getInt("BackLeftHeight", 100) / 100f;
        backGestureHeightFractionRight = XPrefs.Xprefs.getInt("BackRightHeight", 100) / 100f;
        //endregion

        //region pill size
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
        //endregion

        //region pill color
        navPillColorAccent = XPrefs.Xprefs.getBoolean("navPillColorAccent", false);
        //endregion
    }

    //region pill size
    private void refreshNavbar() {
        try
        {
            XposedHelpers.callMethod(mNavigationBarInflaterView, "clearViews");
            Object defaultLayout = XposedHelpers.callMethod(mNavigationBarInflaterView, "getDefaultLayout");
            XposedHelpers.callMethod(mNavigationBarInflaterView, "inflateLayout", defaultLayout);
        }catch (Throwable ignored){}
    }
    //endregion

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;

        Class<?> NavigationBarInflaterViewClass = XposedHelpers.findClass("com.android.systemui.navigationbar.NavigationBarInflaterView", lpparam.classLoader);
        Class<?> NavigationHandleClass = XposedHelpers.findClass("com.android.systemui.navigationbar.gestural.NavigationHandle", lpparam.classLoader);
        Class<?> EdgeBackGestureHandlerClass = XposedHelpers.findClass("com.android.systemui.navigationbar.gestural.EdgeBackGestureHandler", lpparam.classLoader);

        //region Back gesture
        XposedBridge.hookAllMethods(EdgeBackGestureHandlerClass,
                "isWithinInsets", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Point mDisplaySize = (Point) XposedHelpers.getObjectField(param.thisObject, "mDisplaySize");
                        boolean isLeftSide = (int)(int) param.args[0] < (mDisplaySize.x/3);
                        if((isLeftSide && !leftEnabled)
                                || (!isLeftSide && !rightEnabled))
                        {
                            param.setResult(false);
                            return;
                        }

                        int mEdgeHeight = isLeftSide ?
                                Math.round(mDisplaySize.y * backGestureHeightFractionLeft) :
                                Math.round(mDisplaySize.y * backGestureHeightFractionRight);

                        if (mEdgeHeight != 0
                                && (int) param.args[1] < (mDisplaySize.y
                                - (float) XposedHelpers.getObjectField(param.thisObject, "mBottomGestureHeight")
                                - mEdgeHeight)
                        ) {
                            param.setResult(false);
                        }
                    }
                });
        //endregion

        //region pill color
        XposedBridge.hookAllConstructors(NavigationHandleClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                //Let's remember the original colors
                mLightColor = XposedHelpers.getIntField(param.thisObject, "mLightColor");
                mDarkColor = XposedHelpers.getIntField(param.thisObject, "mDarkColor");
            }
        });

        XposedBridge.hookAllMethods(NavigationHandleClass, "setDarkIntensity", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedHelpers.setObjectField(param.thisObject, "mLightColor", (navPillColorAccent) ? mContext.getResources().getColor(android.R.color.system_accent1_200, mContext.getTheme()) : mLightColor);
                XposedHelpers.setObjectField(param.thisObject, "mDarkColor", (navPillColorAccent) ? mContext.getResources().getColor(android.R.color.system_accent1_600, mContext.getTheme()) : mDarkColor);
            }
        });
        //endregion

        //region pill size
        XposedBridge.hookAllMethods(NavigationHandleClass,
                "onDraw", new XC_MethodHook() {
                    int mRadius = 0;
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if(GesPillHeightFactor != 100) {
                            mRadius = XposedHelpers.getIntField(param.thisObject, "mRadius");
                            XposedHelpers.setObjectField(param.thisObject, "mRadius", Math.round(mRadius * GesPillHeightFactor / 100f));
                        }
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
        //endregion

    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
}
