package sh.siava.AOSPMods.systemui;

import android.media.MediaActionSound;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.IXposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class ScreenshotController implements IXposedModPack {
    public static final String listenPackage = "com.android.systemui";
    public static boolean disableScreenshotSound = false;

    public void updatePrefs(String...Key)
    {
        if(XPrefs.Xprefs == null) return;
        disableScreenshotSound = XPrefs.Xprefs.getBoolean("disableScreenshotSound", false);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;


        Class<?> ScreenshotControllerClass = XposedHelpers.findClass("com.android.systemui.screenshot.ScreenshotController", lpparam.classLoader);

        XposedBridge.hookAllConstructors(ScreenshotControllerClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if(!disableScreenshotSound) return;

                //We can't prevent methods from playing sound! So let's break the sound player :D
                XposedHelpers.setObjectField(param.thisObject, "mCameraSound", new NothingPlayer());
            }
        });
    }

    //A Media player that does nothing at all
    static class NothingPlayer extends MediaActionSound
    {
        @Override
        public void play(int o) {}
        @Override
        public void load(int o) {}
        @Override
        public void release(){}
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }

}
