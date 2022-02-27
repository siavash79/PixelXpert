package sh.siava.AOSPMods;

import android.content.Context;
import android.widget.ImageView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class UDFPSManager extends aModManager {
    boolean transparentBG;

    UDFPSManager(XC_LoadPackage.LoadPackageParam lpparam, boolean transparentBG) {
        super(lpparam);
        this.transparentBG = transparentBG;
    }

    @Override
    protected void hookMethods() {
        XposedHelpers.findAndHookMethod(" com.android.systemui.biometrics.UdfpsKeyguardView", lpparam.classLoader,
                "updateAlpha", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ImageView mBgProtection = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mBgProtection");
                        mBgProtection.setImageAlpha(0);

//                        ImageView mLockScreenFp = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mLockScreenFp");


                    }
                });

        XposedHelpers.findAndHookMethod("com.android.keyguard.LockIconView", lpparam.classLoader,
                "setUseBackground", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.args[0] = false;

                    }
                });
        XposedHelpers.findAndHookMethod("com.android.systemui.biometrics.UdfpsKeyguardView", lpparam.classLoader,
                "updateColor", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");

                        Object mLockScreenFp = XposedHelpers.getObjectField(param.thisObject, "mLockScreenFp");

                        Class UtilClass = XposedHelpers.findClass("com.android.settingslib.Utils", lpparam.classLoader);

                        int mTextColorPrimary = (int) XposedHelpers.callStaticMethod(UtilClass, "getColorAttrDefaultColor", mContext,
                                mContext.getResources().getIdentifier("wallpaperTextColorAccent", "attr", mContext.getPackageName()));

                        XposedHelpers.setObjectField(param.thisObject, "mTextColorPrimary", mTextColorPrimary);
                           //Xposedbridge.log("SIAPOSED color:" + mTextColorPrimary);

                        XposedHelpers.callMethod(mLockScreenFp, "invalidate");
                        param.setResult(null);
                    }
                });
    }
}
