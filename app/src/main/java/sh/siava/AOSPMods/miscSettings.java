package sh.siava.AOSPMods;

import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.content.Context;
import android.graphics.Color;

import com.topjohnwu.superuser.Shell;

import java.util.Objects;
import java.util.regex.Pattern;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.utils.ModuleFolderOperations;
import sh.siava.AOSPMods.utils.StringFormatter;
import sh.siava.AOSPMods.utils.SystemUtils;
import sh.siava.rangesliderpreference.RangeSliderPreference;

public class miscSettings extends XposedModPack {
    
    public miscSettings(Context context)
    {
        super(context);
    }
    
    @Override
    public void updatePrefs(String...Key) {
        if(Xprefs == null) return; //it won't be null. but anyway...

        //netstat settings
        try {
            Objects.requireNonNull(SystemUtils.NetworkStats()).setStatus(Xprefs.getBoolean("NetworkStatsEnabled", false));
            Objects.requireNonNull(SystemUtils.NetworkStats()).setSaveInterval(Xprefs.getInt("netstatSaveInterval", 5));
        }catch (Exception ignored){}

        boolean netstatColorful = Xprefs.getBoolean("networkStatsColorful", false);
        StringFormatter.RXColor = (netstatColorful) ? Xprefs.getInt("networkStatDLColor", Color.GREEN) : null;
        StringFormatter.TXColor = (netstatColorful) ? Xprefs.getInt("networkStatULColor", Color.RED) : null;
        StringFormatter.refreshAll();

        if(Key.length > 0)
        {
            //we're not at startup
            //we know what has changed
            switch(Key[0])
            {
                case "sysui_tuner":
                    updateSysUITuner();
                    break;
                case "wifi_cell":
                    updateWifiCell();
                    break;
                case "enableCustomFonts":
                case "gsans_override":
                    updateFontsInfrastructure();
                    break;
                case "volumeStps":
                    setVolumeSteps();
                    break;
            }
        }
        else
        {
            if(AOSPMods.isSecondProcess) return;
            
            //startup jobs
            setDisplayOverride();

            updateSysUITuner();
            updateFontsInfrastructure();

            setVolumeSteps();
        }
    }

    private void setDisplayOverride() {
        float displayOverride = 1f;
        try
        {
            displayOverride = RangeSliderPreference.getValues(Xprefs, "displayOverride",100f).get(0) / 100f;
        }catch (Exception ignored){}

        String sizeResult = Shell.cmd("wm size").exec().getOut().get(0);
        String[] physicalSizes = sizeResult.replace("Physical size: ", "").split("x");
        int w = Integer.parseInt(physicalSizes[0]);
        int h = Integer.parseInt(physicalSizes[1]);

        int overrideW = Math.round(w * displayOverride);
        int overrideH = Math.round(h * displayOverride);

        Shell.cmd(String.format("wm size %sx%s", overrideW, overrideH)).submit();
    }

    private void setVolumeSteps() {
        int volumeStps = Xprefs.getInt("volumeStps", 0);

        ModuleFolderOperations.applyVolumeSteps(volumeStps, XPrefs.MagiskRoot);
    }

    private void updateWifiCell() {
        boolean WifiCellEnabled = Xprefs.getBoolean("wifi_cell", false);

        try {
            String currentTiles = Shell.cmd("settings get secure sysui_qs_tiles").exec().getOut().get(0);
    
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
    
            com.topjohnwu.superuser.Shell.cmd("settings put global settings_provider_model " + providerModel + "; settings put secure sysui_qs_tiles \"" + currentTiles + "\"").exec();
        }catch (Exception ignored){}
    }

    private static String getPattern(String tile) {
        return String.format("^(%s,)(.+)|(.+)(,%s)(,.+|$)",tile,tile);
    }
    
    private void updateSysUITuner() {
        try
        {
            boolean SysUITunerEnabled = Xprefs.getBoolean("sysui_tuner", false);
            String mode = (SysUITunerEnabled) ? "enable" : "disable";
            
            com.topjohnwu.superuser.Shell.cmd("pm " + mode + " com.android.systemui/.tuner.TunerActivity").exec();
        }catch(Exception ignored){}
    }

    private void updateFontsInfrastructure() {
        boolean customFontsEnabled = Xprefs.getBoolean("enableCustomFonts", false);
        boolean GSansOverrideEnabled = Xprefs.getBoolean("gsans_override", false);

        ModuleFolderOperations.applyFontSettings(customFontsEnabled, GSansOverrideEnabled, XPrefs.MagiskRoot);
    }

    @Override
    public boolean listensTo(String packageName) { return packageName.equals(AOSPMods.SYSTEM_UI_PACKAGE); }
    
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable { }
}
