package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;

import com.topjohnwu.superuser.Shell;

import java.lang.reflect.Method;

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

    public void updatePrefs()
    {
        if(XPrefs.Xprefs == null) return;
        setLightQSHeader(XPrefs.Xprefs.getBoolean("LightQSPanel", false));
    }

    private static Context context;
    public static void setLightQSHeader(boolean state)
    {
        if(lightQSHeaderEnabled != state) {
            lightQSHeaderEnabled = state;

            if(context != null) {

                switch (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
                    case Configuration.UI_MODE_NIGHT_YES:
                        break;
                    case Configuration.UI_MODE_NIGHT_NO:

                        try {
                            Shell.su("cmd uimode night yes").exec();
                            Thread.sleep(1000);
                            Shell.su("cmd uimode night no").exec();
                        } catch (Exception e) {}

                        break;
                }
            }
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if(!lpparam.packageName.equals(listenPackage)) return;


        Class QSTileViewImplClass = XposedHelpers.findClass("com.android.systemui.qs.tileimpl.QSTileViewImpl", lpparam.classLoader);
        Class UtilsClass = XposedHelpers.findClass("com.android.settingslib.Utils", lpparam.classLoader);
        Class OngoingPrivacyChipClass = XposedHelpers.findClass("com.android.systemui.privacy.OngoingPrivacyChip", lpparam.classLoader);
        Class FragmentClass = XposedHelpers.findClass("com.android.systemui.fragments.FragmentHostManager", lpparam.classLoader);

        Method ScrimControllerMethod = XposedHelpers.findMethodExactIfExists("com.android.systemui.statusbar.phone.ScrimController", lpparam.classLoader,
                "applyStateToAlpha");
        if(ScrimControllerMethod == null)
        {
            ScrimControllerMethod = XposedHelpers.findMethodExact("com.android.systemui.statusbar.phone.ScrimController", lpparam.classLoader,
                    "applyState");
        }

        XposedHelpers.findAndHookMethod(OngoingPrivacyChipClass,
                "updateResources", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(!lightQSHeaderEnabled) return;
                        Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        Resources res = context.getResources();

                        int iconColor = res.getColor(res.getIdentifier("android:color/system_neutral1_900", "color", context.getPackageName()));
                        //XposedBridge.log("iconcolor :" + iconColor);
                        XposedHelpers.setObjectField(param.thisObject, "iconColor", iconColor);
                    }
                });

        XposedBridge.hookAllConstructors(QSTileViewImplClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                //we need the context anyway
                if(!lightQSHeaderEnabled) return;

                Object colorActive = XposedHelpers.callStaticMethod(UtilsClass, "getColorAttrDefaultColor",
                        context,
                        context.getResources().getIdentifier("android:attr/colorAccent", "attr", "com.android.systemui"));

//                XposedBridge.log("active :" + colorActive);
                XposedHelpers.setObjectField(param.thisObject, "colorActive", colorActive);
            }
        });


        XposedBridge.hookMethod(ScrimControllerMethod, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!lightQSHeaderEnabled) return;

                boolean mClipsQsScrim = (boolean) XposedHelpers.getObjectField(param.thisObject, "mClipsQsScrim");
                if (mClipsQsScrim) {
                    XposedHelpers.setObjectField(param.thisObject, "mBehindTint", Color.TRANSPARENT);
                }
            }
        });

        Class ScrimStateEnum = XposedHelpers.findClass("com.android.systemui.statusbar.phone.ScrimState", lpparam.classLoader);

        Object[] constants =  ScrimStateEnum.getEnumConstants();
        for(int i = 0; i< constants.length; i++)
        {
            String enumVal = constants[i].toString();
            switch(enumVal)
            {
                case "KEYGUARD":
                    //Xposedbridge.log("SIAPOSED found keyguard");
                    XposedHelpers.findAndHookMethod(constants[i].getClass(),
                            "prepare", ScrimStateEnum, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    //Xposedbridge.log("SIAPOSED found keyguard method");
                                    if(!lightQSHeaderEnabled) return;

                                    boolean mClipQsScrim = (boolean) XposedHelpers.getObjectField(param.thisObject, "mClipQsScrim");
                                    if(mClipQsScrim)
                                    {
                                        Object mScrimBehind = XposedHelpers.getObjectField(param.thisObject, "mScrimBehind");
                                        XposedHelpers.callMethod(param.thisObject, "updateScrimColor", mScrimBehind, 1f, Color.TRANSPARENT);
                                    }
                                }
                           });
                    break;
                case "BOUNCER":
                    //Xposedbridge.log("SIAPOSED found bouncer");
                    XposedHelpers.findAndHookMethod(constants[i].getClass(),
                            "prepare", ScrimStateEnum, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    //Xposedbridge.log("SIAPOSED found bouncer method");
                                    if(!lightQSHeaderEnabled) return;

                                    XposedHelpers.setObjectField(param.thisObject, "mBehindTint", Color.TRANSPARENT);
                                }
                            });
                    break;
                case "SHADE_LOCKED":
                    XposedHelpers.findAndHookMethod(constants[i].getClass(),
                            "prepare", ScrimStateEnum, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    if(!lightQSHeaderEnabled) return;

                                    XposedHelpers.setObjectField(param.thisObject, "mBehindTint", Color.TRANSPARENT);

                                    boolean mClipQsScrim = (boolean) XposedHelpers.getObjectField(param.thisObject, "mClipQsScrim");
                                    if(mClipQsScrim)
                                    {
                                        Object mScrimBehind = XposedHelpers.getObjectField(param.thisObject, "mScrimBehind");
                                        XposedHelpers.callMethod(param.thisObject, "updateScrimColor", mScrimBehind, 1f, Color.TRANSPARENT);
                                    }
                                }
                            });
                    XposedHelpers.findAndHookMethod(constants[i].getClass(),
                            "getBehindTint", new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    if(!lightQSHeaderEnabled) return;

                                    param.setResult(Color.TRANSPARENT);
                                }
                            });
                    break;
                case "UNLOCKED":

                    XposedHelpers.findAndHookMethod(constants[i].getClass(),
                            "prepare", ScrimStateEnum, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    if(!lightQSHeaderEnabled) return;

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
               Class InterestingClass = XposedHelpers.findClass("com.android.settingslib.applications.InterestingConfigChanges", lpparam.classLoader);

                Object o = InterestingClass.getDeclaredConstructor(int.class).newInstance(0x40000000 | 0x0004 | 0x0100 | 0x80000000 | 0x0200);
//                Class ActivityClass = Helpers.findClass("android.content.pm.ActivityInfo", lpparam.classLoader);
                XposedHelpers.setObjectField(param.thisObject, "mConfigChanges", o);
            }
        });

        XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.StatusBar", lpparam.classLoader,
                "updateTheme", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Helpers.setOverlay("QSLightTheme", false);
                        Thread.sleep(50);

                        if(lightQSHeaderEnabled) {
                            Helpers.setOverlay("QSLightTheme", true);
                        }
                    }
                });
    }

    @Override
    public String getListenPack() {
        return listenPackage;
    }


}
