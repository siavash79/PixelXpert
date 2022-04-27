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
    public static String MagiskRoot = "/data/adb/modules/AOSPMods";
    private static Context mContext;

    static SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> loadEverything(mContext.getPackageName(), key);

    public static void loadPrefs(Context context) {
        mContext = context;
        Xprefs = new RemotePreferences(context, BuildConfig.APPLICATION_ID, BuildConfig.APPLICATION_ID + "_preferences", true);
        XposedBridge.log("AOSPMods Version: " + BuildConfig.VERSION_NAME);
        XposedBridge.log("AOSPMods Records: " + Xprefs.getAll().keySet().size());
        Xprefs.registerOnSharedPreferenceChangeListener(listener);
        loadEverything(context.getPackageName());
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MOD_PATH = startupParam.modulePath;
        modRes = XModuleResources.createInstance(XPrefs.MOD_PATH, null);
    }

    public static void loadEverything(String packageName, String...key)
    {
        if(packageName.equals("com.android.systemui")) {
            Overlays.setAll();
        }
        for(IXposedModPack thisMod : AOSPMods.runningMods)
        {
            thisMod.updatePrefs(key);
        }
    }
}
