package sh.siava.AOSPMods;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class QSHeaderManager extends aModManager {

    QSHeaderManager(XC_LoadPackage.LoadPackageParam lpparam) {
        super(lpparam);
    }
    public static int resid = 0;
    @Override
    protected void hookMethods() throws InstantiationException, IllegalAccessException {
        XposedHelpers.findAndHookMethod("com.android.systemui.privacy.OngoingPrivacyChip", lpparam.classLoader,
                "updateResources", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        Resources res = context.getResources();

                        int iconColor = res.getColor(res.getIdentifier("android:color/system_neutral1_900", "color", context.getPackageName()));
                        XposedHelpers.setObjectField(param.thisObject, "iconColor", iconColor);
                    }
                });
        Class QSTileViewImplClass = XposedHelpers.findClass("com.android.systemui.qs.tileimpl.QSTileViewImpl", lpparam.classLoader);
        Class UtilsClass = XposedHelpers.findClass("com.android.settingslib.Utils", lpparam.classLoader);

        XposedBridge.hookAllConstructors(QSTileViewImplClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");

                Object colorActive = XposedHelpers.callStaticMethod(UtilsClass, "getColorAttrDefaultColor",
                        context,
                        context.getResources().getIdentifier("anroid:attr/colorAccent", "attr", context.getPackageName()));
                XposedHelpers.setObjectField(param.thisObject, "colorActive", colorActive);
            }
        });

        XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.ScrimController", lpparam.classLoader,
                "applyStateToAlpha", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
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
                    XposedBridge.log("SIAPOSED found keyguard");
                    XposedHelpers.findAndHookMethod(constants[i].getClass(),
                            "prepare", ScrimStateEnum, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    XposedBridge.log("SIAPOSED found keyguard method");

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
                    XposedBridge.log("SIAPOSED found bouncer");
                    XposedHelpers.findAndHookMethod(constants[i].getClass(),
                            "prepare", ScrimStateEnum, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    XposedBridge.log("SIAPOSED found bouncer method");
                                    XposedHelpers.setObjectField(param.thisObject, "mBehindTint", Color.TRANSPARENT);
                                }
                            });
                    break;
                case "SHADE_LOCKED":
                    XposedBridge.log("SIAPOSED found shade lock");
                    XposedHelpers.findAndHookMethod(constants[i].getClass(),
                            "prepare", ScrimStateEnum, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    XposedBridge.log("SIAPOSED found shade lock method");
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
                                    param.setResult(Color.TRANSPARENT);
                                }
                            });
                    break;
                case "UNLOCKED":
                    XposedBridge.log("SIAPOSED found unlock");
                    XposedHelpers.findAndHookMethod(constants[i].getClass(),
                            "prepare", ScrimStateEnum, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    XposedBridge.log("SIAPOSED found unlock method");
                                    XposedHelpers.setObjectField(param.thisObject, "mBehindTint", Color.TRANSPARENT);

                                    Object mScrimBehind = XposedHelpers.getObjectField(param.thisObject, "mScrimBehind");
                                    XposedHelpers.callMethod(param.thisObject, "updateScrimColor", mScrimBehind, 1f, Color.TRANSPARENT);
                                }
                            });
                    break;
            }
        }
    }
}
