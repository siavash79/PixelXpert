package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.view.View;

import androidx.core.graphics.ColorUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.IXposedModPack;
import sh.siava.AOSPMods.Utils.Helpers;
import sh.siava.AOSPMods.XPrefs;

public class QSHeaderManager implements IXposedModPack {
    public static final String listenPackage = "com.android.systemui";
    
    private static boolean lightQSHeaderEnabled = false;
    private static boolean dualToneQSEnabled = false;
    private static boolean brightnessThickTrackEnabled = false;
    
    private static final ArrayList<View> tiles = new ArrayList<>();
    
    public void updatePrefs(String...Key)
    {
        if(XPrefs.Xprefs == null) return;
        
        dualToneQSEnabled = XPrefs.Xprefs.getBoolean("dualToneQSEnabled", false);
        Helpers.setOverlay("QSDualToneOverlay", dualToneQSEnabled, true);
        
        setLightQSHeader(XPrefs.Xprefs.getBoolean("LightQSPanel", false));
    
        boolean newbrightnessThickTrackEnabled = XPrefs.Xprefs.getBoolean("BSThickTrackOverlay", false);
        if(newbrightnessThickTrackEnabled != brightnessThickTrackEnabled)
        {
            brightnessThickTrackEnabled = newbrightnessThickTrackEnabled;
            try {
                onStatChanged();
            } catch (Throwable ignored){}
        }
    }
    
    private Context context;
    Object mBehindColors;
    
    public void setLightQSHeader(boolean state)
    {
        if(lightQSHeaderEnabled != state) {
            lightQSHeaderEnabled = state;
            
            try {
                onStatChanged();
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
                        
                        Object mScrimBehind = XposedHelpers.getObjectField(param.thisObject, "mScrimBehind");
                        
                        context = (Context) XposedHelpers.callMethod(mScrimBehind, "getContext");
                        ColorStateList states = (ColorStateList) XposedHelpers.callStaticMethod(UtilsClass,
                                "getColorAttr",
                                context,
                                context.getResources().getIdentifier("android:attr/colorSurfaceHeader", "attr", listenPackage));
                        int surfaceBackground = states.getDefaultColor();
                        
                        ColorStateList accentStates = (ColorStateList) XposedHelpers.callStaticMethod(UtilsClass,
                                "getColorAccent",
                                context);
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
                        Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        Resources res = context.getResources();
                        
                        int iconColor = context.getColor(res.getIdentifier("android:color/system_neutral1_900", "color", context.getPackageName()));
                        XposedHelpers.setObjectField(param.thisObject, "iconColor", iconColor);
                    }
                });
        
        XposedBridge.hookAllConstructors(QSTileViewImplClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                //we need the context anyway
                tiles.add((View) param.thisObject);
                if(!lightQSHeaderEnabled) return;
                
                Object colorActive = XposedHelpers.callStaticMethod(UtilsClass, "getColorAttrDefaultColor",
                        context,
                        context.getResources().getIdentifier("android:attr/colorAccent", "attr", "com.android.systemui"));
                
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
                                        Object mScrimBehind = XposedHelpers.getObjectField(param.thisObject, "mScrimBehind");
                                        XposedHelpers.callMethod(param.thisObject, "updateScrimColor", mScrimBehind, 1f, Color.TRANSPARENT);
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
                                        Object mScrimBehind = XposedHelpers.getObjectField(param.thisObject, "mScrimBehind");
                                        XposedHelpers.callMethod(param.thisObject, "updateScrimColor", mScrimBehind, 1f, Color.TRANSPARENT);
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
                                    
                                    Object mScrimBehind = XposedHelpers.getObjectField(param.thisObject, "mScrimBehind");
                                    XposedHelpers.callMethod(param.thisObject, "updateScrimColor", mScrimBehind, 1f, Color.TRANSPARENT);
                                }
                            });
                    break;
            }
        }
        
        XposedBridge.hookAllConstructors(FragmentClass, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Class<?> InterestingClass = XposedHelpers.findClass("com.android.settingslib.applications.InterestingConfigChanges", lpparam.classLoader);
                
                Object o = InterestingClass.getDeclaredConstructor(int.class).newInstance(0x40000000 | 0x0004 | 0x0100 | 0x80000000 | 0x0200);
                XposedHelpers.setObjectField(param.thisObject, "mConfigChanges", o);
            }
        });
        
        XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.StatusBar", lpparam.classLoader,
                "updateTheme", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        onStatChanged();
                    }
                });
    }
    
    private void onStatChanged() throws Throwable {
        if (context == null) return;
        Resources res = context.getResources();
        
        boolean isDark = getIsDark();
        
        Helpers.setOverlay("QSLightThemeOverlay", false, true);
        Helpers.setOverlay("QSLightThemeBSTOverlay", false, false);
        
        Thread.sleep(50);
        
        if (lightQSHeaderEnabled && !isDark) {
            
            Helpers.setOverlay("QSLightThemeOverlay", !brightnessThickTrackEnabled, true);
            Helpers.setOverlay("QSLightThemeBSTOverlay", brightnessThickTrackEnabled, false);

            Thread t = new Thread(() -> {
                try {
                    Thread.sleep(1500); //wait for animation to finish
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
                int colorUnavailable = res.getColor(
                        res.getIdentifier("android:color/system_neutral1_10", "color", listenPackage),
                        context.getTheme());
                
                int colorInactive = res.getColor(
                        res.getIdentifier("android:color/system_accent1_10", "color", listenPackage),
                        context.getTheme());
                
                
                for (View v : tiles) {
                    XposedHelpers.setObjectField(v, "colorInactive", colorInactive);
                    XposedHelpers.setObjectField(v, "colorUnavailable", colorUnavailable);
                    
                    Object lastState = XposedHelpers.getObjectField(v, "lastState");
                    Object o = XposedHelpers.callMethod(v, "getBackgroundColorForState", lastState);
                    XposedHelpers.callMethod(v, "setColor", o);
                    
                }
            });
            t.start();
        }
    }
    
    private boolean getIsDark() {
        return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_YES) == Configuration.UI_MODE_NIGHT_YES;
    }
    
    
    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
}
