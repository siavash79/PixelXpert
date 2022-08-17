package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedBridge.hookMethod;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findMethodExact;
import static de.robv.android.xposed.XposedHelpers.findMethodExactIfExists;
import static de.robv.android.xposed.XposedHelpers.getFloatField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.core.graphics.ColorUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.utils.Helpers;
import sh.siava.AOSPMods.utils.Overlays;

@SuppressWarnings("RedundantThrows")
public class QSHeaderManager extends XposedModPack {
    public static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;

    private static boolean lightQSHeaderEnabled = false;
    private static boolean dualToneQSEnabled = false;
    private static boolean brightnessThickTrackEnabled = false;

    private Object mBehindColors;
    private static Object QSPanelController = null, QuickQSPanelController = null;

    public QSHeaderManager(Context context) { super(context); }

    @Override
    public void updatePrefs(String...Key)
    {
        if(Xprefs == null) return;

        dualToneQSEnabled = Xprefs.getBoolean("dualToneQSEnabled", false);
        Helpers.setOverlay("QSDualToneOverlay", dualToneQSEnabled, true, false);

        setLightQSHeader(Xprefs.getBoolean("LightQSPanel", false));
        boolean newbrightnessThickTrackEnabled = Xprefs.getBoolean("BSThickTrackOverlay", false);
        if(newbrightnessThickTrackEnabled != brightnessThickTrackEnabled)
        {
            brightnessThickTrackEnabled = newbrightnessThickTrackEnabled;
            try {
                applyOverlays();
            } catch (Throwable ignored){}
        }
    }

    public void setLightQSHeader(boolean state)
    {
        if(lightQSHeaderEnabled != state) {
            lightQSHeaderEnabled = state;

            try {
                applyOverlays();
            } catch (Throwable ignored) {}
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if(!lpparam.packageName.equals(listenPackage)) return;

        Class<?> QSTileViewImplClass = findClass("com.android.systemui.qs.tileimpl.QSTileViewImpl", lpparam.classLoader);
        Class<?> UtilsClass = findClass("com.android.settingslib.Utils", lpparam.classLoader);

        Class<?> FragmentHostManagerClass = findClass("com.android.systemui.fragments.FragmentHostManager", lpparam.classLoader);
        Class<?> ScrimControllerClass = findClass("com.android.systemui.statusbar.phone.ScrimController", lpparam.classLoader);
        Class<?> GradientColorsClass = findClass("com.android.internal.colorextraction.ColorExtractor$GradientColors", lpparam.classLoader);

        Class<?> QSPanelControllerClass = findClass("com.android.systemui.qs.QSPanelController", lpparam.classLoader);
        Class<?> QuickQSPanelControllerClass = findClass("com.android.systemui.qs.QuickQSPanelController", lpparam.classLoader);
        Class<?> InterestingConfigChangesClass = findClass("com.android.settingslib.applications.InterestingConfigChanges", lpparam.classLoader);
        Class<?> ScrimStateEnum = findClass("com.android.systemui.statusbar.phone.ScrimState", lpparam.classLoader);

        if(Build.VERSION.SDK_INT == 33)
        {
            Class<?> CentralSurfacesImplClass = findClass("com.android.systemui.statusbar.phone.CentralSurfacesImpl", lpparam.classLoader);

            findAndHookMethod(CentralSurfacesImplClass,
                    "updateTheme", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            applyOverlays();
                            new Timer().schedule(new QSLightColorCorrector(), 1500);
                        }
                    });
        }
        else
        {
            Class<?> StatusbarClass = findClass("com.android.systemui.statusbar.phone.StatusBar", lpparam.classLoader);
            Class<?> OngoingPrivacyChipClass = findClass("com.android.systemui.privacy.OngoingPrivacyChip", lpparam.classLoader);

            hookAllConstructors(StatusbarClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    hookAllMethods(getObjectField(param.thisObject,
                                    "mOnColorsChangedListener").getClass(),
                            "onColorsChanged", new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    applyOverlays();
                                    Overlays.setAll(true);  //reset all overlays
                                }
                            });
                }
            });

            findAndHookMethod(StatusbarClass,
                    "updateTheme", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            new Timer().schedule(new QSLightColorCorrector(), 1500);
                        }
                    });

            findAndHookMethod(OngoingPrivacyChipClass,
                    "updateResources", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if(!lightQSHeaderEnabled) return;
                            Resources res = mContext.getResources();

                            int iconColor = mContext.getColor(res.getIdentifier("android:color/system_neutral1_900", "color", mContext.getPackageName()));
                            setObjectField(param.thisObject, "iconColor", iconColor);
                        }
                    });
        }

        Method applyStateMethod = findMethodExactIfExists(ScrimControllerClass, "applyStateToAlpha");
        if(applyStateMethod == null)
        {
            applyStateMethod = findMethodExact(ScrimControllerClass, "applyState");
        }

        try {
            mBehindColors = GradientColorsClass.newInstance();
        } catch (Exception ignored) {}

        hookAllMethods(ScrimControllerClass,
                "onThemeChanged", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mBehindColors = GradientColorsClass.newInstance();
                    }
                });

        hookAllMethods(QuickQSPanelControllerClass, "onInit", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                QuickQSPanelController = param.thisObject;
            }
        });


        hookAllConstructors(QSPanelControllerClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                QSPanelController = param.thisObject;
            }
        });

        hookAllMethods(ScrimControllerClass,
                "updateScrims", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(!dualToneQSEnabled) return;

                        Object mScrimBehind = getObjectField(param.thisObject, "mScrimBehind");
                        boolean mBlankScreen = (boolean) getObjectField(param.thisObject, "mBlankScreen");
                        float alpha = getFloatField(mScrimBehind, "mViewAlpha");
                        boolean animateBehindScrim =  alpha!= 0 && !mBlankScreen;

                        callMethod(mScrimBehind, "setColors", mBehindColors, animateBehindScrim);
                    }
                });

        hookAllMethods(ScrimControllerClass,
                "updateThemeColors", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(!dualToneQSEnabled) return;

//                        Object mScrimBehind = getObjectField(param.thisObject, "mScrimBehind");

                        ColorStateList states = (ColorStateList) callStaticMethod(UtilsClass,
                                "getColorAttr",
                                mContext,
                                mContext.getResources().getIdentifier("android:attr/colorSurfaceHeader", "attr", listenPackage));
                        int surfaceBackground = states.getDefaultColor();

                        ColorStateList accentStates = (ColorStateList) callStaticMethod(UtilsClass, "getColorAttr", mContext, mContext.getResources().getIdentifier("colorAccent", "attr", "android"));
                        int accent = accentStates.getDefaultColor();

                        callMethod(mBehindColors, "setMainColor", surfaceBackground);
                        callMethod(mBehindColors, "setSecondaryColor", accent);

                        double contrast = ColorUtils.calculateContrast((int) callMethod(mBehindColors, "getMainColor"), Color.WHITE);

                        callMethod(mBehindColors, "setSupportsDarkText", contrast > 4.5);
                    }
                });


        hookAllConstructors(QSTileViewImplClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if(!lightQSHeaderEnabled) return;

                Object colorActive = callStaticMethod(UtilsClass, "getColorAttrDefaultColor",
                        mContext,
                        mContext.getResources().getIdentifier("android:attr/colorAccent", "attr", lpparam.packageName));

                setObjectField(param.thisObject, "colorActive", colorActive);
            }
        });

        hookMethod(applyStateMethod, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!lightQSHeaderEnabled) return;

                boolean mClipsQsScrim = (boolean) getObjectField(param.thisObject, "mClipsQsScrim");
                if (mClipsQsScrim) {
                    setObjectField(param.thisObject, "mBehindTint", Color.TRANSPARENT);
                }
            }
        });

        Object[] constants =  ScrimStateEnum.getEnumConstants();
        for (Object constant : constants) {
            String enumVal = constant.toString();
            switch (enumVal) {
                case "KEYGUARD":
                    findAndHookMethod(constant.getClass(),
                            "prepare", ScrimStateEnum, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    if (!lightQSHeaderEnabled) return;
                                    boolean mClipQsScrim = (boolean) getObjectField(param.thisObject, "mClipQsScrim");
                                    if (mClipQsScrim) {
                                        Object mScrimBehind = getObjectField(param.thisObject,"mScrimBehind");
                                        int mTintColor = getIntField(mScrimBehind, "mTintColor");
                                        if(mTintColor != Color.TRANSPARENT)
                                        {
                                            setObjectField(mScrimBehind, "mTintColor", Color.TRANSPARENT);
                                            callMethod(mScrimBehind, "updateColorWithTint", false);
                                        }

                                        callMethod(mScrimBehind, "setViewAlpha", 1f);
                                    }
                                }
                            });
                    break;
                case "BOUNCER":
                    findAndHookMethod(constant.getClass(),
                            "prepare", ScrimStateEnum, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    if (!lightQSHeaderEnabled) return;

                                    setObjectField(param.thisObject, "mBehindTint", Color.TRANSPARENT);
                                }
                            });
                    break;
                case "SHADE_LOCKED":
                    hookAllMethods(constant.getClass(),
                            "prepare", new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    if (!lightQSHeaderEnabled) return;

                                    setObjectField(param.thisObject, "mBehindTint", Color.TRANSPARENT);
                                    boolean mClipQsScrim = (boolean) getObjectField(param.thisObject, "mClipQsScrim");
                                    if (mClipQsScrim) {
                                        Object mScrimBehind = getObjectField(param.thisObject,"mScrimBehind");
                                        int mTintColor = getIntField(mScrimBehind, "mTintColor");
                                        if(mTintColor != Color.TRANSPARENT)
                                        {
                                            setObjectField(mScrimBehind, "mTintColor", Color.TRANSPARENT);
                                            callMethod(mScrimBehind, "updateColorWithTint", false);
                                        }

                                        callMethod(mScrimBehind, "setViewAlpha", 1f);
                                    }
                                }
                            });
                    hookAllMethods(constant.getClass(),
                            "getBehindTint", new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    if (!lightQSHeaderEnabled) return;
                                    param.setResult(Color.TRANSPARENT);
                                }
                            });
                    break;

                case "UNLOCKED":
                    findAndHookMethod(constant.getClass(),
                            "prepare", ScrimStateEnum, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    if (!lightQSHeaderEnabled) return;

                                    setObjectField(param.thisObject, "mBehindTint", Color.TRANSPARENT);

                                    Object mScrimBehind = getObjectField(param.thisObject,"mScrimBehind");
                                    int mTintColor = getIntField(mScrimBehind, "mTintColor");
                                    if(mTintColor != Color.TRANSPARENT)
                                    {
                                        setObjectField(mScrimBehind, "mTintColor", Color.TRANSPARENT);
                                        callMethod(mScrimBehind, "updateColorWithTint", false);
                                    }
                                    callMethod(mScrimBehind, "setViewAlpha", 1f);
                                }
                            });
                    break;
            }
        }

        hookAllConstructors(FragmentHostManagerClass, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                setObjectField(param.thisObject,
                        "mConfigChanges",
                        InterestingConfigChangesClass.getDeclaredConstructor(int.class).newInstance(0x40000000 | 0x0004 | 0x0100 | 0x80000000 | 0x0200));
            }
        });

    }

    private void applyOverlays() throws Throwable {
        boolean isDark = getIsDark();

        Helpers.setOverlay("QSLightThemeOverlay", false, true, false);
        Helpers.setOverlay("QSLightThemeBSTOverlay", false, false, false);

        Thread.sleep(50);

        if (lightQSHeaderEnabled && !isDark) {

            Helpers.setOverlay("QSLightThemeOverlay", !brightnessThickTrackEnabled, true, false);
            Helpers.setOverlay("QSLightThemeBSTOverlay", brightnessThickTrackEnabled, false, false);

            new Timer().schedule(new QSLightColorCorrector(), 1500);
        }
    }

    class QSLightColorCorrector extends TimerTask {

        @SuppressWarnings("rawtypes")
        @Override
        public void run() {
            if(!lightQSHeaderEnabled || getIsDark()) return;
            Resources res = mContext.getResources();

            int colorUnavailable = res.getColor(
                    res.getIdentifier("android:color/system_neutral1_10", "color", listenPackage),
                    mContext.getTheme());

            int colorInactive = res.getColor(
                    res.getIdentifier("android:color/system_accent1_10", "color", listenPackage),
                    mContext.getTheme());

            try {
                ArrayList<?> mRecords = (ArrayList<?>) getObjectField(QSPanelController, "mRecords");
                //noinspection unchecked
                mRecords.addAll((ArrayList) getObjectField(QuickQSPanelController, "mRecords"));
                mRecords.forEach((r) -> fixTileColor(r, colorInactive, colorUnavailable));
            }catch (Throwable ignored){}
        }
    }

    private void fixTileColor(Object tileRec, int colorInactive, int colorUnavailable)
    {
        Object tile = getObjectField(tileRec, "tileView");
        setObjectField(tile, "colorInactive", colorInactive);
        setObjectField(tile, "colorUnavailable", colorUnavailable);

        try {
            Drawable colorBackgroundDrawable = (Drawable) getObjectField(tile, "colorBackgroundDrawable");
            int color = (Integer) callMethod(tile,
                    "getBackgroundColorForState",
                    getObjectField(tile,
                            "lastState"));
            colorBackgroundDrawable.mutate().setTint(color);
            setObjectField(tile,"paintColor", color);
        }catch (Throwable ignored){}
    }

    private boolean getIsDark() {
        return (mContext.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_YES) == Configuration.UI_MODE_NIGHT_YES;
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
}
