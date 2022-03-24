package sh.siava.AOSPMods;

import android.content.SharedPreferences;
import android.content.res.XModuleResources;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;


public class XPrefs implements IXposedHookZygoteInit {

    public static XSharedPreferences Xprefs;
    public static String MOD_PATH = "";
    public static XModuleResources modRes;


    private static XSharedPreferences getPref(String path) {
        XSharedPreferences pref = new XSharedPreferences(BuildConfig.APPLICATION_ID, path);

        if(pref == null)
        {
            XposedBridge.log("SIAPOS null pref");
            return null;
        }

        XposedBridge.log("SIAPOS pref not null");
        XposedBridge.log("SIAPOS pref readable: " + pref.getFile().canRead());

        return pref.getFile().canRead() ? pref : null;
    }

    Thread configLoader = new Thread()
    {
        @Override
        public void run()
        {
            while (true) {
                Xprefs = getPref("sh.siava.AOSPMods_preferences");
                if (Xprefs != null) break;
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {}
            }

            loadEverything();
            Xprefs.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    loadEverything();
                }
            });
        }
    };

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {

        MOD_PATH = startupParam.modulePath;
        modRes = XModuleResources.createInstance(XPrefs.MOD_PATH, null);

        loadEverything();
    }

    private void loadEverything()
    {
        if(Xprefs == null || !Xprefs.getFile().canRead())
        {
            configLoader.start();
            return;
        }

        Xprefs.reload();

        for(IXposedModPack thisMod : AOSPMods.runningMods)
        {
            thisMod.updatePrefs();
        }
    }
}
