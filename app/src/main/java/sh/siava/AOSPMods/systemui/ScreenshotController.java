package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.content.Context;
import android.media.MediaActionSound;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class ScreenshotController extends XposedModPack {
    private static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;

    private static final NothingPlayer nothingPlayer = new NothingPlayer();

    private static boolean disableScreenshotSound = false;

    public ScreenshotController(Context context) { super(context); }
    
    @Override
    public void updatePrefs(String...Key)
    {
        if(Xprefs == null) return;
        disableScreenshotSound = Xprefs.getBoolean("disableScreenshotSound", false);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;

        Class<?> ScreenshotControllerClass = findClass("com.android.systemui.screenshot.ScreenshotController", lpparam.classLoader);

        hookAllConstructors(ScreenshotControllerClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if(!disableScreenshotSound) return;

                //We can't prevent methods from playing sound! So let's break the sound player :D
                setObjectField(param.thisObject, "mCameraSound", nothingPlayer);
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
