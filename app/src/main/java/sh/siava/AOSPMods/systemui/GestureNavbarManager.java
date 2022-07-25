package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.Utils.Helpers.tryHookAllMethods;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
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
    private boolean colorReplaced = false;
    private static boolean navPillColorAccent = false;
    private static final int mLightColor = Color.parseColor("#EBffffff"), mDarkColor = Color.parseColor("#99000000"); //original navbar colors
    //endregion

    public GestureNavbarManager(Context context) { super(context); }

    public void updatePrefs(String...Key)
    {
        if(Xprefs == null) return;

        //region Back gesture
        leftEnabled = Xprefs.getBoolean("BackFromLeft", true);
        rightEnabled = Xprefs.getBoolean("BackFromRight", true);
        backGestureHeightFractionLeft = Xprefs.getInt("BackLeftHeight", 100) / 100f;
        backGestureHeightFractionRight = Xprefs.getInt("BackRightHeight", 100) / 100f;
        //endregion

        //region pill size
        widthFactor = Xprefs.getInt("GesPillWidthModPos", 50) * .02f;
        GesPillHeightFactor = Xprefs.getInt("GesPillHeightFactor", 100);

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
        navPillColorAccent = Xprefs.getBoolean("navPillColorAccent", false);
        //endregion
    }

    //region pill size
    private void refreshNavbar() {
        try
        {
            callMethod(mNavigationBarInflaterView, "clearViews");
            Object defaultLayout = callMethod(mNavigationBarInflaterView, "getDefaultLayout");
            callMethod(mNavigationBarInflaterView, "inflateLayout", defaultLayout);
        }catch (Throwable ignored){}
    }
    //endregion

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;

        Class<?> NavigationBarInflaterViewClass = findClass("com.android.systemui.navigationbar.NavigationBarInflaterView", lpparam.classLoader);
        Class<?> NavigationHandleClass = findClass("com.android.systemui.navigationbar.gestural.NavigationHandle", lpparam.classLoader);
        Class<?> EdgeBackGestureHandlerClass = findClassIfExists("com.android.systemui.navigationbar.gestural.EdgeBackGestureHandler", lpparam.classLoader);
        Class<?> NavigationBarEdgePanelClass = findClassIfExists("com.android.systemui.navigationbar.gestural.NavigationBarEdgePanel", lpparam.classLoader);

        tryHookAllMethods(NavigationBarEdgePanelClass, "onMotionEvent", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                MotionEvent event = (MotionEvent) param.args[0];
                if(notWithinInsets(event.getX(), event.getY(), (Point) getObjectField(param.thisObject, "mDisplaySize"), 0))
                {
                    param.setResult(null);
                }
            }
        });
        //region Back gesture
        hookAllMethods(EdgeBackGestureHandlerClass,
                "isWithinInsets", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if(notWithinInsets((float) param.args[0], (float) param.args[1], (Point) getObjectField(param.thisObject, "mDisplaySize"), (float) getObjectField(param.thisObject, "mBottomGestureHeight")))
                        {
                            param.setResult(false);
                        }
                    }
                });
        //endregion

        //region pill color
        hookAllMethods(NavigationHandleClass, "setDarkIntensity", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if(navPillColorAccent || colorReplaced) {
                    setObjectField(param.thisObject, "mLightColor", (navPillColorAccent) ? mContext.getResources().getColor(android.R.color.system_accent1_200, mContext.getTheme()) : mLightColor);
                    setObjectField(param.thisObject, "mDarkColor", (navPillColorAccent) ? mContext.getResources().getColor(android.R.color.system_accent1_600, mContext.getTheme()) : mDarkColor);
                    colorReplaced = true;
                }
            }
        });
        //endregion

        //region pill size
        hookAllMethods(NavigationHandleClass,
                "onDraw", new XC_MethodHook() {
                    int mRadius = 0;
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if(GesPillHeightFactor != 100) {
                            mRadius = getIntField(param.thisObject, "mRadius");
                            setObjectField(param.thisObject, "mRadius", Math.round(mRadius * GesPillHeightFactor / 100f));
                        }
                    }
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(mRadius > 0) {
                            setObjectField(param.thisObject, "mRadius", mRadius);
                        }
                    }
                });


        hookAllConstructors(NavigationBarInflaterViewClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mNavigationBarInflaterView = param.thisObject;
                refreshNavbar();
            }
        });

        hookAllMethods(NavigationBarInflaterViewClass,
                "createView", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(widthFactor != 1f) {
                            String button = (String) callMethod(param.thisObject, "extractButton", param.args[0]);
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

    //region Back gesture
    private boolean notWithinInsets(float x, float y, Point mDisplaySize, float mBottomGestureHeight)
    {
        boolean isLeftSide = x < (mDisplaySize.x/3f);
        if((isLeftSide && !leftEnabled)
                || (!isLeftSide && !rightEnabled))
        {
            return true;
        }

        int mEdgeHeight = isLeftSide ?
                Math.round(mDisplaySize.y * backGestureHeightFractionLeft) :
                Math.round(mDisplaySize.y * backGestureHeightFractionRight);

        return mEdgeHeight != 0
                && y < (mDisplaySize.y
                - mBottomGestureHeight
                - mEdgeHeight);
    }
    //endregion

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
}
