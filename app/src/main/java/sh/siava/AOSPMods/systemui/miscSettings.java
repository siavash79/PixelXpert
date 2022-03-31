package sh.siava.AOSPMods.systemui;

import com.topjohnwu.superuser.Shell;

import java.util.regex.Pattern;

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
            updateGSansOverride();
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
                case "gsans_override":
                    updateGSansOverride();
            }
        }
    }

    private void updateWifiCell() {
        Boolean WifiCellEnabled = XPrefs.Xprefs.getBoolean("wifi_cell", false);

        try {
            String currentTiles = Shell.su("settings get secure sysui_qs_tiles").exec().getOut().get(0);

            boolean hasWifi = Pattern.matches(getPattern("wifi"), currentTiles);
            boolean hasCell = Pattern.matches(getPattern("cell"), currentTiles);
            boolean hasInternet = Pattern.matches(getPattern("internet"), currentTiles);

            boolean providerModel;

            if (WifiCellEnabled) {
                providerModel = false;
                if (!hasCell) {
                    currentTiles = String.format("cell,%s", currentTiles);
                }
                if (!hasWifi) {
                    currentTiles = String.format("wifi,%s", currentTiles);
                }
                currentTiles = currentTiles.replaceAll(getPattern("internet"), "$2$3$5"); //remove intrnet

            } else {
                providerModel = true;

                currentTiles = currentTiles.replaceAll(getPattern("cell"), "$2$3$5"); //remove cell
                currentTiles = currentTiles.replaceAll(getPattern("wifi"), "$2$3$5"); //remove wifi

                if (!hasInternet) {
                    currentTiles = "internet," + currentTiles;
                }
            }

            Shell.su("settings put global settings_provider_model " + providerModel + "; settings put secure sysui_qs_tiles \"" + currentTiles + "\"").exec();
        }catch (Exception ignored){}
    }

    private static String getPattern(String tile)
    {
        return String.format("^(%s,)(.+)|(.+)(,%s)(,.+|$)",tile,tile);
    }

    private void updateSysUITuner() {
        Boolean SysUITunerEnabled = XPrefs.Xprefs.getBoolean("sysui_tuner", false);
        String mode = (SysUITunerEnabled) ? "enable" : "disable";

        try {
            Shell.su("pm " + mode + " com.android.systemui/.tuner.TunerActivity").exec();
        }catch(Exception ignored){}
    }

    private void updateGSansOverride() {
        Boolean GSansOverrideEnabled = XPrefs.Xprefs.getBoolean("gsans_override", false);
        try {
            if (GSansOverrideEnabled){
                /* To do
                * replace module path name on the command
                * Shell.su ("cp /data/adb/modules/AOSPMods/data/fontz/GSans/*.ttf /data/adb/modules/AOSPMods/system/fonts/ && cp /data/adb/modules/AOSPMods/data/productz/etc/fonts_customization.xml.NEW /data/adb/modules/AOSPMods/system/product/etc/fonts_customization.xml").exec();*/
                Shell.su ("cp /data/adb/modules/AddonFeaturesForPixel/data/fontz/GSans/*.ttf /data/adb/modules/AddonFeaturesForPixel/system/fonts/ && cp /data/adb/modules/AddonFeaturesForPixel/data/productz/etc/fonts_customization.xml.NEW /data/adb/modules/AddonFeaturesForPixel/system/product/etc/fonts_customization.xml").exec();
            } else {
                /* To do
                 * replace module path name on the command
                 * Shell.su ("rm -rf /data/adb/modules/AOSPMods/system/fonts/*.ttf && cp /data/adb/modules/AOSPMods/data/productz/etc/fonts_customization.xml.OLD /data/adb/modules/AOSPMods/system/product/etc/fonts_customization.xml").exec();*/
                Shell.su ("rm -rf /data/adb/modules/AddonFeaturesForPixel/system/fonts/*.ttf && cp /data/adb/modules/AddonFeaturesForPixel/data/productz/etc/fonts_customization.xml.OLD /data/adb/modules/AddonFeaturesForPixel/system/product/etc/fonts_customization.xml").exec();
            }
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
