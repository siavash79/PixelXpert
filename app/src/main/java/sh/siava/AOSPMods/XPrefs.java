package sh.siava.AOSPMods;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.XModuleResources;

import com.crossbowffs.remotepreferences.RemotePreferences;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import sh.siava.AOSPMods.Utils.Overlays;


public class XPrefs implements IXposedHookZygoteInit {

    public static String MOD_PATH = "";
    public static XModuleResources modRes;
    public static SharedPreferences Xprefs;

    static SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> loadEverything(key);

    public static void loadPrefs(Context mContext) {
        Xprefs = new RemotePreferences(mContext, BuildConfig.APPLICATION_ID, BuildConfig.APPLICATION_ID + "_preferences", true);
        XposedBridge.log("size: " + Xprefs.getAll().keySet().size());
        Xprefs.registerOnSharedPreferenceChangeListener(listener);
        loadEverything();
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {

        MOD_PATH = startupParam.modulePath;
        modRes = XModuleResources.createInstance(XPrefs.MOD_PATH, null);
    }

    public static void loadEverything(String...key)
    {
        Overlays.setAll();
        for(IXposedModPack thisMod : AOSPMods.runningMods)
        {
            thisMod.updatePrefs(key);
        }
    }
}
