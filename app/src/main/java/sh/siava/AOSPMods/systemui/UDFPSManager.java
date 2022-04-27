package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.widget.ImageView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.IXposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class UDFPSManager implements IXposedModPack {
    private static final String listenPackage = "com.android.systemui";
    public static boolean transparentBG = false;

    public static String UDFPS_hide_key = "fingerprint_circle_hide";

    public void updatePrefs(String...Key)
    {
        if(XPrefs.Xprefs == null) return;
        transparentBG = XPrefs.Xprefs.getBoolean(UDFPSManager.UDFPS_hide_key, false);
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if(!lpparam.packageName.equals(listenPackage)) return;

        Class<?> UtilClass = XposedHelpers.findClass("com.android.settingslib.Utils", lpparam.classLoader);


        XposedHelpers.findAndHookMethod("com.android.systemui.biometrics.UdfpsKeyguardView", lpparam.classLoader,
                "updateAlpha", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(!transparentBG) return;

                        ImageView mBgProtection = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mBgProtection");
                        mBgProtection.setImageAlpha(0);
//                        ImageView mLockScreenFp = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mLockScreenFp");
                    }
                });

        XposedHelpers.findAndHookMethod("com.android.keyguard.LockIconView", lpparam.classLoader,
                "setUseBackground", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if(!transparentBG) return;
                        param.args[0] = false;
                    }
                });
        XposedHelpers.findAndHookMethod("com.android.systemui.biometrics.UdfpsKeyguardView", lpparam.classLoader,
                "updateColor", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if(!transparentBG) return;
                        Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");

                        Object mLockScreenFp = XposedHelpers.getObjectField(param.thisObject, "mLockScreenFp");


                        int mTextColorPrimary = (int) XposedHelpers.callStaticMethod(UtilClass, "getColorAttrDefaultColor", mContext,
                                mContext.getResources().getIdentifier("wallpaperTextColorAccent", "attr", mContext.getPackageName()));

                        XposedHelpers.setObjectField(param.thisObject, "mTextColorPrimary", mTextColorPrimary);

                        XposedHelpers.callMethod(mLockScreenFp, "invalidate");
                        param.setResult(null);
                    }
                });
    }
}
