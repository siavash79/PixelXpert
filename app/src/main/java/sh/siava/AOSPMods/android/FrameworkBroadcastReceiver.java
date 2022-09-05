package sh.siava.AOSPMods.android;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

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

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class FrameworkBroadcastReceiver extends XposedModPack {
    public static final String listenPackage = AOSPMods.SYSTEM_FRAMEWORK_PACKAGE;

    private static final String KEY_BUFFER = "bitmap_util_buffer";
    private static final String KEY_COLOR_SPACE = "bitmap_util_color_space";

    private Object windowMan = null;
    private static boolean broadcastRegistered = false;
    private Object mDisplayManagerInternal;
    private Display mDefaultDisplay;
    private Object mDefaultDisplayPolicy;
    private Object mHandler;

    public FrameworkBroadcastReceiver(Context context) {
        super(context);
    }

    @Override
    public void updatePrefs(String... Key) {
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
                }
            });

        } catch (Throwable t) {
            t.printStackTrace();
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