package sh.siava.AOSPMods;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.XModuleResources;

import androidx.preference.PreferenceManager;

import com.crossbowffs.remotepreferences.RemotePreferences;

import de.robv.android.xposed.IXposedHookZygoteInit;
import sh.siava.AOSPMods.Utils.Overlays;


public class XPrefs implements IXposedHookZygoteInit {

    public static String MOD_PATH = "";
    public static XModuleResources modRes;
    public static SharedPreferences Xprefs;

    static SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> loadEverything();

    public static void loadPrefs(Context mContext) {
        if(mContext.getPackageName() == BuildConfig.APPLICATION_ID)
        {
            Xprefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        }
        else {
            Xprefs = new RemotePreferences(mContext, BuildConfig.APPLICATION_ID, BuildConfig.APPLICATION_ID + "_preferences");
        }
        Xprefs.registerOnSharedPreferenceChangeListener(listener);
        loadEverything();
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {

        MOD_PATH = startupParam.modulePath;
        modRes = XModuleResources.createInstance(XPrefs.MOD_PATH, null);
    }

    public static void loadEverything()
    {
        Overlays.setAll();
        for(IXposedModPack thisMod : AOSPMods.runningMods)
        {
            thisMod.updatePrefs();
        }
    }
}
