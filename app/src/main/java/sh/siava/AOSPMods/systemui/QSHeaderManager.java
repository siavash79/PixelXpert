package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;

import androidx.core.graphics.ColorUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.Utils.Helpers;
import sh.siava.AOSPMods.XPrefs;
import sh.siava.AOSPMods.XposedModPack;

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
        if(XPrefs.Xprefs == null) return;
        
        boolean lightThemeEnabled = XPrefs.Xprefs.getBoolean("LightQSPanel", false);

        boolean dualToneQSEnabled = false;
        if(lightThemeEnabled)
        {
            dualToneQSEnabled = XPrefs.Xprefs.getBoolean("dualToneQSEnabled", false);
        }
        Helpers.setOverlay("QSDualToneOverlay", dualToneQSEnabled, true);
        
        setLightQSHeader(lightThemeEnabled);
    
        boolean newbrightnessThickTrackEnabled = XPrefs.Xprefs.getBoolean("BSThickTrackOverlay", false);
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

        Class<?> QSTileViewImplClass = XposedHelpers.findClass("com.android.systemui.qs.tileimpl.QSTileViewImpl", lpparam.classLoader);
        Class<?> UtilsClass = XposedHelpers.findClass("com.android.settingslib.Utils", lpparam.classLoader);
        Class<?> OngoingPrivacyChipClass = XposedHelpers.findClass("com.android.systemui.privacy.OngoingPrivacyChip", lpparam.classLoader);
        Class<?> FragmentClass = XposedHelpers.findClass("com.android.systemui.fragments.FragmentHostManager", lpparam.classLoader);
        Class<?> ScrimControllerClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.ScrimController", lpparam.classLoader);
        Class<?> GradientColorsClass = XposedHelpers.findClass("com.android.internal.colorextraction.ColorExtractor.GradientColors", lpparam.classLoader);
        Class<?> StatusbarClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.StatusBar", lpparam.classLoader);
        Class<?> QSPanelControllerClass = XposedHelpers.findClass("com.android.systemui.qs.QSPanelController", lpparam.classLoader);
        Class<?> QuickQSPanelControllerClass = XposedHelpers.findClass("com.android.systemui.qs.QuickQSPanelController", lpparam.classLoader);
        Class<?> InterestingConfigChangesClass = XposedHelpers.findClass("com.android.settingslib.applications.InterestingConfigChanges", lpparam.classLoader);


        Method applyStateMethod = XposedHelpers.findMethodExactIfExists(ScrimControllerClass, "applyStateToAlpha");
        if(applyStateMethod == null)
        {
            applyStateMethod = XposedHelpers.findMethodExact(ScrimControllerClass, "applyState");
        }
        
        try {
            mBehindColors = GradientColorsClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        XposedBridge.hookAllMethods(ScrimControllerClass,
                "onUiModeChanged", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mBehindColors = GradientColorsClass.newInstance();
                    }
                });

        XposedBridge.hookAllConstructors(QuickQSPanelControllerClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                QuickQSPanelController = param.thisObject;
            }
        });


        XposedBridge.hookAllConstructors(QSPanelControllerClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                QSPanelController = param.thisObject;
            }
        });

        XposedBridge.hookAllMethods(ScrimControllerClass,
                "updateScrims", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(!dualToneQSEnabled) return;
                        
                        Object mScrimBehind = XposedHelpers.getObjectField(param.thisObject, "mScrimBehind");
                        boolean mBlankScreen = (boolean) XposedHelpers.getObjectField(param.thisObject, "mBlankScreen");
                        float alpha = (float) XposedHelpers.callMethod(mScrimBehind, "getViewAlpha");
                        boolean animateBehindScrim =  alpha!= 0 && !mBlankScreen;
                        
                        XposedHelpers.callMethod(mScrimBehind, "setColors", mBehindColors, animateBehindScrim);
                    }
                });
        
        XposedBridge.hookAllMethods(ScrimControllerClass,
                "updateThemeColors", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(!dualToneQSEnabled) return;
                        
//                        Object mScrimBehind = XposedHelpers.getObjectField(param.thisObject, "mScrimBehind");
                        
                        ColorStateList states = (ColorStateList) XposedHelpers.callStaticMethod(UtilsClass,
                                "getColorAttr",
                                mContext,
                                mContext.getResources().getIdentifier("android:attr/colorSurfaceHeader", "attr", listenPackage));
                        int surfaceBackground = states.getDefaultColor();
                        
                        ColorStateList accentStates = (ColorStateList) XposedHelpers.callStaticMethod(UtilsClass,
                                "getColorAccent",
                                mContext);
                        int accent = accentStates.getDefaultColor();
                        
                        XposedHelpers.callMethod(mBehindColors, "setMainColor", surfaceBackground);
                        XposedHelpers.callMethod(mBehindColors, "setSecondaryColor", accent);
                        
                        double contrast = ColorUtils.calculateContrast((int) XposedHelpers.callMethod(mBehindColors, "getMainColor"), Color.WHITE);
                        
                        XposedHelpers.callMethod(mBehindColors, "setSupportsDarkText", contrast > 4.5);
                    }
                });
        
        
        XposedHelpers.findAndHookMethod(OngoingPrivacyChipClass,
                "updateResources", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(!lightQSHeaderEnabled) return;
                        Resources res = mContext.getResources();
                        
                        int iconColor = mContext.getColor(res.getIdentifier("android:color/system_neutral1_900", "color", mContext.getPackageName()));
                        XposedHelpers.setObjectField(param.thisObject, "iconColor", iconColor);
                    }
                });
        
        XposedBridge.hookAllConstructors(QSTileViewImplClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if(!lightQSHeaderEnabled) return;
                
                Object colorActive = XposedHelpers.callStaticMethod(UtilsClass, "getColorAttrDefaultColor",
                        mContext,
                        mContext.getResources().getIdentifier("android:attr/colorAccent", "attr", lpparam.packageName));
                
                XposedHelpers.setObjectField(param.thisObject, "colorActive", colorActive);
            }
        });
        
        XposedBridge.hookMethod(applyStateMethod, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!lightQSHeaderEnabled) return;
                
                boolean mClipsQsScrim = (boolean) XposedHelpers.getObjectField(param.thisObject, "mClipsQsScrim");
                if (mClipsQsScrim) {
                    XposedHelpers.setObjectField(param.thisObject, "mBehindTint", Color.TRANSPARENT);
                }
            }
        });
        
        Class<?> ScrimStateEnum = XposedHelpers.findClass("com.android.systemui.statusbar.phone.ScrimState", lpparam.classLoader);
        
        Object[] constants =  ScrimStateEnum.getEnumConstants();
        for (Object constant : constants) {
            String enumVal = constant.toString();
            switch (enumVal) {
                case "KEYGUARD":
                    XposedHelpers.findAndHookMethod(constant.getClass(),
                            "prepare", ScrimStateEnum, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    if (!lightQSHeaderEnabled) return;
                                    
                                    boolean mClipQsScrim = (boolean) XposedHelpers.getObjectField(param.thisObject, "mClipQsScrim");
                                    if (mClipQsScrim) {
                                        XposedHelpers.callMethod(param.thisObject,
                                                "updateScrimColor",
                                                XposedHelpers.getObjectField(param.thisObject,
                                                        "mScrimBehind"),
                                                1f,
                                                Color.TRANSPARENT);
                                    }
                                }
                            });
                    break;
                case "BOUNCER":
                    XposedHelpers.findAndHookMethod(constant.getClass(),
                            "prepare", ScrimStateEnum, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    if (!lightQSHeaderEnabled) return;
                                    
                                    XposedHelpers.setObjectField(param.thisObject, "mBehindTint", Color.TRANSPARENT);
                                }
                            });
                    break;
                case "SHADE_LOCKED":
                    XposedBridge.hookAllMethods(constant.getClass(),
                            "prepare", new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    if (!lightQSHeaderEnabled) return;
                                    
                                    XposedHelpers.setObjectField(param.thisObject, "mBehindTint", Color.TRANSPARENT);
                                    
                                    boolean mClipQsScrim = (boolean) XposedHelpers.getObjectField(param.thisObject, "mClipQsScrim");
                                    if (mClipQsScrim) {
                                        XposedHelpers.callMethod(param.thisObject,
                                                "updateScrimColor",
                                                XposedHelpers.getObjectField(param.thisObject,
                                                        "mScrimBehind"),
                                                1f,
                                                Color.TRANSPARENT);
                                    }
                                }
                            });
                    XposedBridge.hookAllMethods(constant.getClass(),
                            "getBehindTint", new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    if (!lightQSHeaderEnabled) return;
                                    param.setResult(Color.TRANSPARENT);
                                }
                            });
                    break;
                
                case "UNLOCKED":
                    XposedHelpers.findAndHookMethod(constant.getClass(),
                            "prepare", ScrimStateEnum, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    if (!lightQSHeaderEnabled) return;
                                    
                                    XposedHelpers.setObjectField(param.thisObject, "mBehindTint", Color.TRANSPARENT);
                                    
                                    XposedHelpers.callMethod(param.thisObject,
                                            "updateScrimColor",
                                            XposedHelpers.getObjectField(param.thisObject,
                                                    "mScrimBehind"),
                                            1f,
                                            Color.TRANSPARENT);
                                }
                            });
                    break;
            }
        }
        
        XposedBridge.hookAllConstructors(FragmentClass, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedHelpers.setObjectField(param.thisObject,
                        "mConfigChanges",
                        InterestingConfigChangesClass.getDeclaredConstructor(int.class).newInstance(0x40000000 | 0x0004 | 0x0100 | 0x80000000 | 0x0200));
            }
        });

        XposedBridge.hookAllConstructors(StatusbarClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.hookAllMethods(XposedHelpers.getObjectField(param.thisObject,
                        "mOnColorsChangedListener").getClass(),
                        "onColorsChanged", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        applyOverlays();
                    }
                });
            }
        });

        XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.StatusBar", lpparam.classLoader,
                "updateTheme", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if(! lightQSHeaderEnabled) return;
                    new Timer().schedule(new QSLightColorCorrector(), 1500);
                    }
                });
    }

    private void applyOverlays() throws Throwable {
        boolean isDark = getIsDark();

        Helpers.setOverlay("QSLightThemeOverlay", false, true);
        Helpers.setOverlay("QSLightThemeBSTOverlay", false, false);

        Thread.sleep(50);

        if (lightQSHeaderEnabled && !isDark) {

            Helpers.setOverlay("QSLightThemeOverlay", !brightnessThickTrackEnabled, true);
            Helpers.setOverlay("QSLightThemeBSTOverlay", brightnessThickTrackEnabled, false);

            new Timer().schedule(new QSLightColorCorrector(), 1500);
        }
    }

    class QSLightColorCorrector extends TimerTask {

        @SuppressWarnings("rawtypes")
        @Override
        public void run() {
            if(getIsDark()) return;
            Resources res = mContext.getResources();

            int colorUnavailable = res.getColor(
                    res.getIdentifier("android:color/system_neutral1_10", "color", listenPackage),
                    mContext.getTheme());

            int colorInactive = res.getColor(
                    res.getIdentifier("android:color/system_accent1_10", "color", listenPackage),
                    mContext.getTheme());

            try {
                ArrayList<?> mRecords = (ArrayList<?>) XposedHelpers.getObjectField(QSPanelController, "mRecords");
                //noinspection unchecked
                mRecords.addAll((ArrayList) XposedHelpers.getObjectField(QuickQSPanelController, "mRecords"));
                mRecords.forEach((r) -> fixTileColor(r, colorInactive, colorUnavailable));
            }catch (Exception ignored){}
        }
    }

    private void fixTileColor(Object tileRec, int colorInactive, int colorUnavailable)
    {
        Object tile = XposedHelpers.getObjectField(tileRec, "tileView");
        XposedHelpers.setObjectField(tile, "colorInactive", colorInactive);
        XposedHelpers.setObjectField(tile, "colorUnavailable", colorUnavailable);

        XposedHelpers.callMethod(tile,
                "setColor",
                XposedHelpers.callMethod(tile,
                        "getBackgroundColorForState",
                        XposedHelpers.getObjectField(tile,
                                "lastState")));
    }

    private boolean getIsDark() {
        return (mContext.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_YES) == Configuration.UI_MODE_NIGHT_YES;
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
}
