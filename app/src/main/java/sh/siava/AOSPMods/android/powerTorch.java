package sh.siava.AOSPMods.android;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.os.Handler;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;
import java.util.Calendar;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.IXposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class powerTorch implements IXposedModPack {
    public static final String listenPackage = "android";
    private boolean torchOn = false;
    private static boolean replaceAssistantwithTorch = true;
    private CameraManager cameraManager = null;
    private long wakeTime = 0;
    private Context mContext;

    CameraManager.TorchCallback torchCallback = new CameraManager.TorchCallback() {
        @Override
        public void onTorchModeChanged(@NonNull String cameraId, boolean enabled) {
            super.onTorchModeChanged(cameraId, enabled);
            torchOn = enabled;
        }
    };

    @Override
    public void updatePrefs() {
        replaceAssistantwithTorch = XPrefs.Xprefs.getBoolean("replaceAssistantwithTorch", true);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(listenPackage)) return;

        Class PhoneWindowManager = null;
        Method init = null;
        Method powerLongPress = null;
        Method startedWakingUp = null;

        try {
            PhoneWindowManager = XposedHelpers.findClassIfExists("com.android.server.policy.PhoneWindowManager", lpparam.classLoader);
            powerLongPress = XposedHelpers.findMethodExactIfExists(PhoneWindowManager, "powerLongPress", long.class);
            init = XposedHelpers.findMethodExactIfExists(PhoneWindowManager, "init", Context.class, "android.view.IWindowManager", "com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs");
            startedWakingUp = XposedHelpers.findMethodExactIfExists(PhoneWindowManager, "startedWakingUp", int.class);
        }
        catch(Throwable t)
        {
            t.printStackTrace();
            return;
        }
        if(startedWakingUp ==null || powerLongPress == null || init == null)
        {
            XposedBridge.log("SIAPOSED: method not found");
            return;
        }

        XposedBridge.hookMethod(startedWakingUp, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if(!replaceAssistantwithTorch) return;

                int r = (int) param.args[0];

                if(r==1)
                {
                    wakeTime = Calendar.getInstance().getTimeInMillis();
                }
            }
        });

        XposedBridge.hookMethod(init, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try
                {
                    mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    if (mContext == null)
                    {
                        return;
                    }
                    cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
                    cameraManager.registerTorchCallback(torchCallback, new Handler());
                }
                catch (Throwable T)
                {
                    T.printStackTrace();
                }
            }
        });

        XposedBridge.hookMethod(powerLongPress, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if(!replaceAssistantwithTorch) return;
                if(Calendar.getInstance().getTimeInMillis() - wakeTime > 1000) return;
                try {
                    int behavior = (int) XposedHelpers.callMethod(param.thisObject, "getResolvedLongPressOnPowerBehavior");

                    if(behavior == 3) // this is a force shutdown event. never play with it (3=LONG_PRESS_POWER_SHUT_OFF_NO_CONFIRM)
                    {
                        return;
                    }

                    toggleFlash();
                    param.setResult(null);
                    if(mContext != null) {
                        Object pm = mContext.getSystemService(Context.POWER_SERVICE);
                        XposedHelpers.callMethod(pm, "goToSleep", SystemClock.uptimeMillis());
                    }
                }
                catch (Throwable T){
                    T.printStackTrace();
                }
            }
        });
    }

    private void toggleFlash() {
        try {
            if(cameraManager == null)
            {
                XposedBridge.log("camera manager null");
                return;
            }

            String flashID = getFlashID(cameraManager);
            if(flashID == "")
            {
                XposedBridge.log("camera flash null");
                return;
            }

            cameraManager.setTorchMode(flashID, !torchOn);
        }
        catch(Throwable T)
        {
            T.printStackTrace();
        }
    }

    private String getFlashID(CameraManager c) throws CameraAccessException {
        String[] ids = c.getCameraIdList();
        for(String id : ids)
        {
            if(c.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK)
            {
                if(c.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE))
                {
                    return id;
                }
            }
        }
        return "";
    }

    @Override
    public String getListenPack() {
        return listenPackage;
    }
}
