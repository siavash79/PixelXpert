package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;

import java.security.Key;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.IXposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class KeyguardBottomArea implements IXposedModPack {
    public static final String listenPackage = "com.android.systemui";
    public static boolean showCameraOnLockscreen = false;
    public static boolean transparentBGcolor = false;
    final Context[] mContext = new Context[1];

    public void updatePrefs()
    {
        if(XPrefs.Xprefs == null) return;
        showCameraOnLockscreen = XPrefs.Xprefs.getBoolean("KeyguardCameraEnabled", false);
        transparentBGcolor = XPrefs.Xprefs.getBoolean("KeyguardBottomButtonsTransparent", false);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;

        Class KeyguardbottomAreaViewClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.KeyguardBottomAreaView", lpparam.classLoader);
        Class UtilClass = XposedHelpers.findClass("com.android.settingslib.Utils", lpparam.classLoader);
        Class CameraIntentsClass = XposedHelpers.findClass("com.android.systemui.camera.CameraIntents", lpparam.classLoader);

        //convert wallet button to camera button
        XposedHelpers.findAndHookMethod(KeyguardbottomAreaViewClass,
                "onFinishInflate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(!showCameraOnLockscreen) return;

                        View.OnClickListener cameraListener = new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                XposedHelpers.callMethod(param.thisObject, "launchCamera", "lockscreen_affordance");
                            }
                        };

                        ImageView mWalletButton = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mWalletButton");
                        Context mContext = mWalletButton.getContext();
                        mWalletButton.setImageDrawable(((Context) mContext).getResources().getDrawable(mContext.getResources().getIdentifier("ic_camera_alt_24dp", "drawable", mContext.getPackageName())));
                        mWalletButton.setOnClickListener(cameraListener);
                        mWalletButton.setClickable(true);
                    }
                });

        //make sure system won't play with our camera button
        XposedHelpers.findAndHookMethod(KeyguardbottomAreaViewClass, "onWalletCardsRetrieved", "android.service.quickaccesswallet.GetWalletCardsResponse", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if(!showCameraOnLockscreen) return;
                param.setResult(null);
            }
        });


        //make sure system won't play with our camera button
        XposedHelpers.findAndHookMethod(KeyguardbottomAreaViewClass,
                "updateWalletVisibility", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if(!showCameraOnLockscreen) return;

                        ImageView mWalletButton = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mWalletButton");
                        boolean mDozing = (boolean) XposedHelpers.getObjectField(param.thisObject, "mDozing");

                        if(mDozing) // AOD is showing
                        {
                            mWalletButton.setVisibility(View.GONE);
                        }
                        else
                        {
                            mWalletButton.setVisibility(View.VISIBLE);
                        }
                        param.setResult(null);
                    }
                });

        //Transparent background
        XposedHelpers.findAndHookMethod(KeyguardbottomAreaViewClass,
                "updateAffordanceColors", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(!transparentBGcolor) return;
                        ImageView mWalletButton = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mWalletButton");
                        ImageView mControlsButton = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mControlsButton");

                        mContext[0] = mControlsButton.getContext();

                        int mTextColorPrimary = (int) XposedHelpers.callStaticMethod(UtilClass, "getColorAttrDefaultColor", mContext[0],
                                mContext[0].getResources().getIdentifier("wallpaperTextColorAccent", "attr", mContext[0].getPackageName()));

                        mControlsButton.setBackgroundColor(Color.TRANSPARENT);
                        mControlsButton.setColorFilter(mTextColorPrimary);

                        mWalletButton.setBackgroundColor(Color.TRANSPARENT);
                        mWalletButton.setColorFilter(mTextColorPrimary);
                    }
                });

        //Set camera intent to be always secure when launchd from keyguard screen

        XposedHelpers.findAndHookMethod(KeyguardbottomAreaViewClass.getName()+"$DefaultRightButton", lpparam.classLoader,
                "getIntent", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if(!showCameraOnLockscreen || mContext[0] == null) return;
                        param.setResult(XposedHelpers.callStaticMethod(CameraIntentsClass, "getSecureCameraIntent", mContext[0]));
                    }
                });
    }
}
