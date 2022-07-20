package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
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
        Class<?> EdgeBackGestureHandlerClass = findClass("com.android.systemui.navigationbar.gestural.EdgeBackGestureHandler", lpparam.classLoader);
//      Class<?> UtilsClass = findClass("com.android.settingslib.Utils", lpparam.classLoader);

        //region Back gesture
        hookAllMethods(EdgeBackGestureHandlerClass,
                "isWithinInsets", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Point mDisplaySize = (Point) getObjectField(param.thisObject, "mDisplaySize");
                        boolean isLeftSide = (int) param.args[0] < (mDisplaySize.x/3);
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
                                - (float) getObjectField(param.thisObject, "mBottomGestureHeight")
                                - mEdgeHeight)
                        ) {
                            param.setResult(false);
                        }
                    }
                });
        //endregion

        //region pill color
        //putting static color instead of getting it from system
/*        hookAllConstructors(NavigationHandleClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Context context = (Context) param.args[0];
                Resources res = context.getResources();

                final int dualToneDarkTheme = (int) callStaticMethod(UtilsClass, "getThemeAttr", context, res.getIdentifier("darkIconTheme", "attr", context.getPackageName()));
                final int dualToneLightTheme = (int) callStaticMethod(UtilsClass, "getThemeAttr", context, res.getIdentifier("lightIconTheme", "attr", context.getPackageName()));

                Context lightContext = new ContextThemeWrapper(context, dualToneLightTheme);
                Context darkContext = new ContextThemeWrapper(context, dualToneDarkTheme);

                mLightColor = (int) callStaticMethod(UtilsClass, "getColorAttrDefaultColor", lightContext, res.getIdentifier("homeHandleColor", "attr", context.getPackageName()));
                mDarkColor = (int) callStaticMethod(UtilsClass, "getColorAttrDefaultColor", darkContext, res.getIdentifier("homeHandleColor", "attr", context.getPackageName()));
            }
        });*/

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

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
}
