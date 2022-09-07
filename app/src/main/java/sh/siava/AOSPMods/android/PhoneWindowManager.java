package sh.siava.AOSPMods.android;

import static android.view.KeyEvent.KEYCODE_POWER;
import static android.view.KeyEvent.KEYCODE_VOLUME_DOWN;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.ColorSpace;
import android.graphics.Insets;
import android.graphics.ParcelableColorSpace;
import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import android.os.Bundle;
import android.view.Display;

import java.util.ArrayList;
import java.util.Collections;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class PhoneWindowManager extends XposedModPack {
    public static final String listenPackage = AOSPMods.SYSTEM_FRAMEWORK_PACKAGE;

    private static final String KEY_BUFFER = "bitmap_util_buffer";
    private static final String KEY_COLOR_SPACE = "bitmap_util_color_space";

    private Object windowMan = null;
    private static boolean broadcastRegistered = false;
    private Object mDisplayManagerInternal;
    private Display mDefaultDisplay;
    private Object mDefaultDisplayPolicy;
    private Object mHandler;
    private boolean ScreenshotChordInsecure;
    private final ArrayList<Object> screenshotChords = new ArrayList<>();

    public PhoneWindowManager(Context context) {
        super(context);
    }

    @Override
    public void updatePrefs(String... Key) {
        ScreenshotChordInsecure = Xprefs.getBoolean("ScreenshotChordInsecure", false);
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                switch (action)
                {
                    case AOSPMods.ACTION_INSECURE_SCREENSHOT:
                        takeInsecureScreenshot();
                        break;
                    case AOSPMods.ACTION_SCREENSHOT:
                        callMethod(windowMan, "handleScreenShot", 1, 1);
                        break;
                    case AOSPMods.ACTION_BACK:
                        callMethod(windowMan, "backKeyPress");
                        break;
                }
            }catch (Throwable ignored){}
        }
    };

    IntentFilter intentFilter = new IntentFilter();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(listenPackage)) return;

        Collections.addAll(screenshotChords, KEYCODE_POWER, KEYCODE_VOLUME_DOWN);

        if(!broadcastRegistered) {
            broadcastRegistered = true;
            intentFilter.addAction(AOSPMods.ACTION_SCREENSHOT);
            intentFilter.addAction(AOSPMods.ACTION_BACK);
            intentFilter.addAction(AOSPMods.ACTION_INSECURE_SCREENSHOT);
            mContext.registerReceiver(broadcastReceiver, intentFilter);
        }

        try {
            Class<?> PhoneWindowManagerClass = findClass("com.android.server.policy.PhoneWindowManager", lpparam.classLoader);

            hookAllMethods(PhoneWindowManagerClass, "enableScreen", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    windowMan = param.thisObject;

                    //Apparently some stuff init before Xposed. Will have to find and hack them...
                    Object mKeyCombinationManager = getObjectField(param.thisObject, "mKeyCombinationManager");
                    ArrayList<?> mRules = (ArrayList<?>) getObjectField(mKeyCombinationManager, "mRules");
                    for(Object mRule : mRules)
                    {
                        if(screenshotChords.contains(getObjectField(mRule, "mKeyCode1"))
                            && screenshotChords.contains(getObjectField(mRule, "mKeyCode2")))
                        {
                            hookAllMethods(mRule.getClass(), "execute", new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    if(ScreenshotChordInsecure) {
                                        try {
                                            takeInsecureScreenshot();
                                            param.setResult(null);
                                        } catch(Throwable ignored){}
                                    }
                                }
                            });
                            break;
                        }
                    }
                }
            });

        } catch (Throwable ignored) {
        }
    }

    private void takeInsecureScreenshot()
    {
        if(mDisplayManagerInternal == null)
        {
            initVars();
        }

        Object SCBuffer = callMethod(mDisplayManagerInternal, "systemScreenshot", mDefaultDisplay.getDisplayId());

        HardwareBuffer mHardwareBuffer = (HardwareBuffer) getObjectField(SCBuffer, "mHardwareBuffer");
        ParcelableColorSpace colorSpace = new ParcelableColorSpace((ColorSpace) getObjectField(SCBuffer, "mColorSpace"));

        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_BUFFER, mHardwareBuffer);
        bundle.putParcelable(KEY_COLOR_SPACE, colorSpace);

        callMethod(getObjectField(mDefaultDisplayPolicy, "mScreenshotHelper"), "provideScreenshot", bundle, new Rect(0, 0,mHardwareBuffer.getWidth(), mHardwareBuffer.getHeight()), Insets.of(0,0,0,0), 1, 1, new ComponentName("", ""), 0, mHandler, null);
    }

    private void initVars() {
        mDisplayManagerInternal = getObjectField(windowMan, "mDisplayManagerInternal");
        mDefaultDisplay = (Display) getObjectField(windowMan, "mDefaultDisplay");
        mDefaultDisplayPolicy = getObjectField(windowMan, "mDefaultDisplayPolicy");
        mHandler = getObjectField(windowMan, "mHandler");
    }

    @Override
    public boolean listensTo(String packageName) {
        return listenPackage.equals(packageName);
    }
}