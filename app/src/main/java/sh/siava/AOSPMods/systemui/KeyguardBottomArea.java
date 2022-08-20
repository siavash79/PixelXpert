package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;

import androidx.core.content.res.ResourcesCompat;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.Utils.SystemUtils;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class KeyguardBottomArea extends XposedModPack {
    private static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;
    private static boolean transparentBGcolor = false;
    private static String leftShortcut = "";
    private static String rightShortcut = "";
    
    private ImageView mWalletButton;
    private ImageView mControlsButton;

    private Object thisObject = null;
    
    public KeyguardBottomArea(Context context) { super(context); }
    
    @Override
    public void updatePrefs(String...Key)
    {
        if(Xprefs == null) return;
        leftShortcut = Xprefs.getString("leftKeyguardShortcut", "");
        rightShortcut = Xprefs.getString("rightKeyguardShortcut", "");

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
        transparentBGcolor = Xprefs.getBoolean("KeyguardBottomButtonsTransparent", false);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;

        Class<?> KeyguardbottomAreaViewClass = findClass("com.android.systemui.statusbar.phone.KeyguardBottomAreaView", lpparam.classLoader);
        Class<?> UtilClass = findClass("com.android.settingslib.Utils", lpparam.classLoader);

        //convert wallet button to camera button
        findAndHookMethod(KeyguardbottomAreaViewClass,
                "onFinishInflate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mWalletButton = (ImageView) getObjectField(param.thisObject, "mWalletButton");
                        mControlsButton = (ImageView) getObjectField(param.thisObject, "mControlsButton");
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
        findAndHookMethod(KeyguardbottomAreaViewClass.getName() + "$" + "WalletCardRetriever", lpparam.classLoader,
                "onWalletCardsRetrieved", "android.service.quickaccesswallet.GetWalletCardsResponse", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (rightShortcut.length() == 0) return;
                        param.setResult(null);
                    }
                });

        //make sure system won't play with our button
        hookAllMethods(KeyguardbottomAreaViewClass,
                "updateControlsVisibility", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if(leftShortcut.length() == 0 || mControlsButton == null) return;
                        updateVisibility(mControlsButton, param.thisObject);
                        param.setResult(null);
                    }
                });

        //make sure system won't play with our button
        findAndHookMethod(KeyguardbottomAreaViewClass,
                "updateWalletVisibility", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if(rightShortcut.length() == 0 || mWalletButton == null) return;
                        updateVisibility(mWalletButton, param.thisObject);
                        param.setResult(null);
                    }
                });

        //Transparent background
        findAndHookMethod(KeyguardbottomAreaViewClass,
                "updateAffordanceColors", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(!transparentBGcolor) return;
                        ImageView mWalletButton = (ImageView) getObjectField(param.thisObject, "mWalletButton");
                        ImageView mControlsButton = (ImageView) getObjectField(param.thisObject, "mControlsButton");

                        int mTextColorPrimary = (int) callStaticMethod(UtilClass, "getColorAttrDefaultColor", mContext,
                                mContext.getResources().getIdentifier("wallpaperTextColorAccent", "attr", mContext.getPackageName()));

                        mControlsButton.setBackgroundColor(Color.TRANSPARENT);
                        mControlsButton.setColorFilter(mTextColorPrimary);

                        mWalletButton.setBackgroundColor(Color.TRANSPARENT);
                        mWalletButton.setColorFilter(mTextColorPrimary);
                    }
                });

        //Set camera intent to be always secure when launchd from keyguard screen
        findAndHookMethod(KeyguardbottomAreaViewClass.getName()+"$DefaultRightButton", lpparam.classLoader,
                "getIntent", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if((!leftShortcut.equals("camera") && !rightShortcut.equals("camera"))) return;
                        param.setResult(getCameraIntent(mContext));
                    }
                });
    }

    private Intent getCameraIntent(Context context)
    {
        Resources res = context.getResources();

        Intent cameraIntent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE);
        cameraIntent.setPackage(res.getString(res.getIdentifier("config_cameraGesturePackage", "string", context.getPackageName())));

        return cameraIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
    }

    private void updateVisibility(ImageView Button, Object thisObject) {
        boolean mDozing = (boolean) getObjectField(thisObject, "mDozing");

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
                listener = v -> callMethod(thisObject, "launchCamera", "lockscreen_affordance");
                drawable = ResourcesCompat.getDrawable(mContext.getResources(), mContext.getResources().getIdentifier("ic_camera_alt_24dp", "drawable", mContext.getPackageName()), mContext.getTheme());
                break;
            case "assistant":
                listener = v -> callMethod(thisObject, "launchVoiceAssist");
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
