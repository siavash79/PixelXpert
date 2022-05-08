package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import androidx.core.content.res.ResourcesCompat;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.Utils.SystemUtils;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class KeyguardBottomArea extends XposedModPack {
    public static final String listenPackage = "com.android.systemui";
    public static boolean transparentBGcolor = false;
    public static String leftShortcut = "";
    public static String rightShortcut = "";
    
    private ImageView mWalletButton;
    private ImageView mControlsButton;

    private Object thisObject = null;
    
    public KeyguardBottomArea(Context context) { super(context); }
    
    @Override
    public void updatePrefs(String...Key)
    {
        if(XPrefs.Xprefs == null) return;
        leftShortcut = XPrefs.Xprefs.getString("leftKeyguardShortcut", "");
        rightShortcut = XPrefs.Xprefs.getString("rightKeyguardShortcut", "");

        if(Key.length > 0) {
            switch (Key[0]) {
                case "leftKeyguardShortcut":
                    if (thisObject != null)
                        convertShortcut(mControlsButton, leftShortcut, thisObject);
                    break;
                case "rightKeyguardShortcut":
                    if (thisObject != null)
                        convertShortcut(mWalletButton, rightShortcut, thisObject);
                    break;
            }
        }
        transparentBGcolor = XPrefs.Xprefs.getBoolean("KeyguardBottomButtonsTransparent", false);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;
        

        
        Class<?> KeyguardbottomAreaViewClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.KeyguardBottomAreaView", lpparam.classLoader);
        Class<?> UtilClass = XposedHelpers.findClass("com.android.settingslib.Utils", lpparam.classLoader);
        Class<?> CameraIntentsClass = XposedHelpers.findClass("com.android.systemui.camera.CameraIntents", lpparam.classLoader);

        //convert wallet button to camera button
        XposedHelpers.findAndHookMethod(KeyguardbottomAreaViewClass,
                "onFinishInflate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mWalletButton = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mWalletButton");
                        mControlsButton = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mControlsButton");
                        thisObject = param.thisObject;

                        if(leftShortcut.length() > 0)
                        {
                            convertShortcut(mControlsButton, leftShortcut, param.thisObject);
                        }
                        if(rightShortcut.length() > 0)
                        {
                            convertShortcut(mWalletButton, rightShortcut, param.thisObject);
                        }
                    }
                });

        //make sure system won't play with our button
        XposedHelpers.findAndHookMethod(KeyguardbottomAreaViewClass.getName() + "$" + "WalletCardRetriever", lpparam.classLoader,
                "onWalletCardsRetrieved", "android.service.quickaccesswallet.GetWalletCardsResponse", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (rightShortcut.length() == 0) return;
                        param.setResult(null);
                    }
                });

        //make sure system won't play with our button
        XposedBridge.hookAllMethods(KeyguardbottomAreaViewClass,
                "updateControlsVisibility", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if(leftShortcut.length() == 0 || mControlsButton == null) return;
                        updateVisibility(mControlsButton, param.thisObject);
                        param.setResult(null);
                    }
                });

        //make sure system won't play with our button
        XposedHelpers.findAndHookMethod(KeyguardbottomAreaViewClass,
                "updateWalletVisibility", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if(rightShortcut.length() == 0 || mWalletButton == null) return;
                        updateVisibility(mWalletButton, param.thisObject);
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

                        int mTextColorPrimary = (int) XposedHelpers.callStaticMethod(UtilClass, "getColorAttrDefaultColor", mContext,
                                mContext.getResources().getIdentifier("wallpaperTextColorAccent", "attr", mContext.getPackageName()));

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
                        if((!leftShortcut.equals("camera") && !rightShortcut.equals("camera"))) return;
                        param.setResult(XposedHelpers.callStaticMethod(CameraIntentsClass, "getSecureCameraIntent", mContext));
                    }
                });
    }

    private void updateVisibility(ImageView Button, Object thisObject) {
        boolean mDozing = (boolean) XposedHelpers.getObjectField(thisObject, "mDozing");

        if (mDozing) // AOD is showing
        {
            Button.setVisibility(View.GONE);
        } else {
            Button.setVisibility(View.VISIBLE);
        }
    }

    private void convertShortcut(ImageView Button, String type, Object thisObject) {
        View.OnClickListener listener = null;
        Drawable drawable = null;
        switch(type)
        {
            case "camera":
                listener = v -> XposedHelpers.callMethod(thisObject, "launchCamera", "lockscreen_affordance");
                drawable = ResourcesCompat.getDrawable(mContext.getResources(), mContext.getResources().getIdentifier("ic_camera_alt_24dp", "drawable", mContext.getPackageName()), mContext.getTheme());
                break;
            case "assistant":
                listener = v -> XposedHelpers.callMethod(thisObject, "launchVoiceAssist");
                drawable = ResourcesCompat.getDrawable(mContext.getResources(), mContext.getResources().getIdentifier("ic_mic_26dp", "drawable", mContext.getPackageName()), mContext.getTheme());
                break;
            case "torch":
                listener = v -> SystemUtils.ToggleFlash();
                drawable = ResourcesCompat.getDrawable(mContext.getResources(), mContext.getResources().getIdentifier("@android:drawable/ic_qs_flashlight", "drawable", mContext.getPackageName()), mContext.getTheme());
                break;
        }
        if(type.length() > 0) {
            Button.setImageDrawable(drawable);
            Button.setOnClickListener(listener);
            Button.setClickable(true);
            Button.setVisibility(View.VISIBLE);
        }
        else
        {
            Button.setVisibility(View.GONE);
        }
    }


    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }


}
