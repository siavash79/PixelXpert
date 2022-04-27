package sh.siava.AOSPMods.android;

import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.media.AudioManager;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;
import java.util.Calendar;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.IXposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class screenOffKeys implements IXposedModPack {
    public static final String listenPackage = "android";
    private boolean torchOn = false;
    private static boolean replaceAssistantwithTorch = false;
    private static boolean holdVolumeToSkip = false;
    private CameraManager cameraManager = null;
    private long wakeTime = 0;
    private Context mContext = null;
    private AudioManager audioManager = null;
//    private boolean isVolumeLongPress = false;
    private boolean isVolDown = false;
    private Handler mHandler = null;
    private PowerManager powerManager = null;

    CameraManager.TorchCallback torchCallback = new CameraManager.TorchCallback() {
        @Override
        public void onTorchModeChanged(@NonNull String cameraId, boolean enabled) {
            super.onTorchModeChanged(cameraId, enabled);
            torchOn = enabled;
        }
    };

    @Override
    public void updatePrefs(String...Key) {
        holdVolumeToSkip = XPrefs.Xprefs.getBoolean("holdVolumeToSkip", false);
        replaceAssistantwithTorch = XPrefs.Xprefs.getBoolean("replaceAssistantwithTorch", false);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(listenPackage)) return;

        Class<?> PhoneWindowManager;
        Method init;
        Method powerLongPress;
        Method startedWakingUp;
        Method interceptKeyBeforeQueueing;

        try {
            PhoneWindowManager = XposedHelpers.findClass("com.android.server.policy.PhoneWindowManager", lpparam.classLoader);
            powerLongPress = XposedHelpers.findMethodExact(PhoneWindowManager, "powerLongPress", long.class);
            init = XposedHelpers.findMethodExact(PhoneWindowManager, "init", Context.class, "android.view.IWindowManager", "com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs");
            startedWakingUp = XposedHelpers.findMethodExact(PhoneWindowManager, "startedWakingUp", int.class);
            interceptKeyBeforeQueueing = XposedHelpers.findMethodExact(PhoneWindowManager, "interceptKeyBeforeQueueing", KeyEvent.class, int.class);
        }
        catch(Throwable t)
        {
            t.printStackTrace();
            return;
        }

        Runnable mVolumeLongPress = () -> {
            Intent keyIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
            KeyEvent keyEvent = new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, (isVolDown) ? KeyEvent.KEYCODE_MEDIA_PREVIOUS : KeyEvent.KEYCODE_MEDIA_NEXT, 0);
            keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
            audioManager.dispatchMediaKeyEvent(keyEvent);

            keyEvent = KeyEvent.changeAction(keyEvent, KeyEvent.ACTION_UP);
            keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
            audioManager.dispatchMediaKeyEvent(keyEvent);
        };

        XposedBridge.hookMethod(interceptKeyBeforeQueueing, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if(!holdVolumeToSkip) return;

                try {
                    KeyEvent e = (KeyEvent) param.args[0];
                    int Keycode = e.getKeyCode();

                    switch (e.getAction()) {
                        case KeyEvent.ACTION_UP:
                            if (mHandler.hasCallbacks(mVolumeLongPress)) {
                                mHandler.removeCallbacks(mVolumeLongPress);
                            }
                            return;
                        case KeyEvent.ACTION_DOWN:
                            if (!powerManager.isInteractive() && (Keycode == KeyEvent.KEYCODE_VOLUME_DOWN || Keycode == KeyEvent.KEYCODE_VOLUME_UP) && audioManager.isMusicActive()) {
                                isVolDown = (Keycode == KeyEvent.KEYCODE_VOLUME_DOWN);
                                mHandler.postDelayed(mVolumeLongPress, ViewConfiguration.getLongPressTimeout());
                            }
                    }
                }catch (Throwable e){e.printStackTrace();}
            }
        });

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
                    if (mContext == null) return;

                    mHandler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");
                    audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                    cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
                    cameraManager.registerTorchCallback(torchCallback, new Handler());
                    powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
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
                        XposedHelpers.callMethod(powerManager, "goToSleep", SystemClock.uptimeMillis());
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
                return;
            }

            String flashID = getFlashID(cameraManager);
            if(flashID.equals(""))
            {
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
        try {
            for (String id : ids) {
                if (c.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK) {
                    if (c.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE)) {
                        return id;
                    }
                }
            }
        }catch (Throwable e) {e.printStackTrace();}
        return "";
    }
    
    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
    
}
