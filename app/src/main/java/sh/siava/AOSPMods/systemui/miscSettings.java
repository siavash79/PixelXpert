package sh.siava.AOSPMods.systemui;

import com.topjohnwu.superuser.Shell;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.IXposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class miscSettings implements IXposedModPack {

    @Override
    public void updatePrefs(String...Key) {
        if(XPrefs.Xprefs == null) return; //it won't be null. but anyway...

        if(Key.length == 0)
        {
            //we are at startup
            updateSysUITuner();
            updateWifiCell();
        }
        else
        {
            //we know what has changed
            switch(Key[0])
            {
                case "sysui_tuner":
                    updateSysUITuner();
                    break;
                case "wifi_cell":
                    updateWifiCell();
                    break;
            }
        }
    }

    private void updateWifiCell() {
        Boolean WifiCellEnabled = XPrefs.Xprefs.getBoolean("wifi_cell", false);

        try {
            String currentTiles = Shell.su("settings get secure sysui_qs_tiles").exec().getOut().get(0);

            boolean hasWifi = currentTiles.contains("wifi");
            boolean hasCell = currentTiles.contains("cell");
            boolean hasInternet = currentTiles.contains("internet");

            boolean providerModel;
            if (WifiCellEnabled) {
                providerModel = false;
                if (!hasCell) {
                    currentTiles = "cell," + currentTiles;
                }
                if (!hasWifi) {
                    currentTiles = "wifi," + currentTiles;
                }
                currentTiles = currentTiles.replace(",,", ",");
            } else {
                providerModel = true;

                currentTiles = currentTiles.replace("wifi", "").replace(",,", ",").replace("cell", "").replace(",,", ",");
                if (!hasInternet) {
                    currentTiles = "internet," + currentTiles;
                }
                currentTiles = currentTiles.replace(",,", ",");

            }

            if (currentTiles.startsWith(",")) {
                currentTiles = currentTiles.substring(1, currentTiles.length() - 1);
            }
            // sorry I was toooo lazy to go through regex work..... this seems good enough
            XposedBridge.log(currentTiles);

            XposedBridge.log("settings put global settings_provider_model " + providerModel + "; settings put secure sysui_qs_tiles \"" + currentTiles + "\"");
            Shell.su("settings put global settings_provider_model " + providerModel + "; settings put secure sysui_qs_tiles \"" + currentTiles + "\"").exec();
        }catch (Exception ignored){}
    }

    private void updateSysUITuner() {
        Boolean SysUITunerEnabled = XPrefs.Xprefs.getBoolean("sysui_tuner", false);
        String mode = (SysUITunerEnabled) ? "enable" : "disable";

        try {
            Shell.su("pm " + mode + " com.android.systemui/.tuner.TunerActivity").exec();
        }catch(Exception ignored){}
    }


    @Override
    public String getListenPack() {
        return "com.android.systemui";
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        return;
    }
}
