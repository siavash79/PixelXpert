package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class QSHeaderManager implements IXposedHookLoadPackage {
    public static final String listenPackage = "com.android.systemui";

    private static boolean lightQSHeaderEnabled = false;

    private static Context context;
    public static void setLightQSHeader(boolean state)
    {
        XposedBridge.log("SIA QS set to " + state);
        if(lightQSHeaderEnabled != state) {
            lightQSHeaderEnabled = state;

            if(context != null) {
                switch (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
                    case Configuration.UI_MODE_NIGHT_YES:
                        break;
                    case Configuration.UI_MODE_NIGHT_NO:
                        try {
                            Runtime.getRuntime().exec("settings get secure ui_night_mode 2");
                            Thread.sleep(500);
                            Runtime.getRuntime().exec("settings get secure ui_night_mode 1");
                        } catch (Exception e) {
                        }
                        break;
                }
            }

        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if(!lpparam.packageName.equals(listenPackage)) return;

        XposedHelpers.findAndHookMethod("com.android.systemui.privacy.OngoingPrivacyChip", lpparam.classLoader,
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
        Class QSTileViewImplClass = XposedHelpers.findClass("com.android.systemui.qs.tileimpl.QSTileViewImpl", lpparam.classLoader);
        Class UtilsClass = XposedHelpers.findClass("com.android.settingslib.Utils", lpparam.classLoader);

        XposedBridge.hookAllConstructors(QSTileViewImplClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if(!lightQSHeaderEnabled) return;

                context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");

                Object colorActive = XposedHelpers.callStaticMethod(UtilsClass, "getColorAttrDefaultColor",
                        context,
                        context.getResources().getIdentifier("android:attr/colorAccent", "attr", "com.android.systemui"));

//                XposedBridge.log("active :" + colorActive);
                XposedHelpers.setObjectField(param.thisObject, "colorActive", colorActive);
            }
        });

        XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.ScrimController", lpparam.classLoader,
                "applyStateToAlpha", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(!lightQSHeaderEnabled) return;

                        boolean mClipsQsScrim = (boolean) XposedHelpers.getObjectField(param.thisObject, "mClipsQsScrim");
                        if(mClipsQsScrim)
                        {
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
                    //Xposedbridge.log("SIAPOSED found shade lock");
                    XposedHelpers.findAndHookMethod(constants[i].getClass(),
                            "prepare", ScrimStateEnum, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    //Xposedbridge.log("SIAPOSED found shade lock method");
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
                    //Xposedbridge.log("SIAPOSED found unlock");
                    XposedHelpers.findAndHookMethod(constants[i].getClass(),
                            "prepare", ScrimStateEnum, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    if(!lightQSHeaderEnabled) return;

                                    //Xposedbridge.log("SIAPOSED found unlock method");
                                    XposedHelpers.setObjectField(param.thisObject, "mBehindTint", Color.TRANSPARENT);

                                    Object mScrimBehind = XposedHelpers.getObjectField(param.thisObject, "mScrimBehind");
                                    XposedHelpers.callMethod(param.thisObject, "updateScrimColor", mScrimBehind, 1f, Color.TRANSPARENT);
                                }
                            });
                    break;
            }
        }

/*        Class FragmentClass = XposedHelpers.findClass("com.android.systemui.fragments.FragmentHostManager", lpparam.classLoader);
        XposedBridge.hookAllConstructors(FragmentClass, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
               Class InterestingClass = XposedHelpers.findClass("com.android.settingslib.applications.InterestingConfigChanges", lpparam.classLoader);
                Object o = InterestingClass.getDeclaredConstructor(int.class).newInstance(0x40000000 | 0x0004 | 0x0100 | 0x80000000 | 0x0200);
//                Class ActivityClass = XposedHelpers.findClass("android.content.pm.ActivityInfo", lpparam.classLoader);
                XposedHelpers.setObjectField(param.thisObject, "mConfigChanges", o);
            }
        });
*/
        XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.StatusBar", lpparam.classLoader,
                "updateTheme", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Runtime.getRuntime().exec("cmd overlay disable sh.siava.AOSPMods_QSTheme");
                        Thread.sleep(50);

                        if(lightQSHeaderEnabled) {
                            Runtime.getRuntime().exec("cmd overlay enable sh.siava.AOSPMods_QSTheme");
                            Thread.sleep(50);
                            Runtime.getRuntime().exec("cmd overlay set-priority sh.siava.AOSPMods_QSTheme lowest");
                        }
                    }
                });


    }
}
