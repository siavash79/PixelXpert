package sh.siava.AOSPMods;

import android.content.Context;

import com.topjohnwu.superuser.Shell;

import java.util.regex.Pattern;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class miscSettings extends XposedModPack {
    
    public miscSettings(Context context)
    {
        super(context);
    }
    
    @Override
    public void updatePrefs(String...Key) {
        if(XPrefs.Xprefs == null) return; //it won't be null. but anyway...
        
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
            }
        }
        else
        {
            if(AOSPMods.isSecondProcess) return;
            
            //startup jobs
            updateSysUITuner();
            updateFontsInfrastructure();
        }
    }

    private void updateWifiCell() {
        boolean WifiCellEnabled = XPrefs.Xprefs.getBoolean("wifi_cell", false);

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
            boolean SysUITunerEnabled = XPrefs.Xprefs.getBoolean("sysui_tuner", false);
            String mode = (SysUITunerEnabled) ? "enable" : "disable";
            
            com.topjohnwu.superuser.Shell.cmd("pm " + mode + " com.android.systemui/.tuner.TunerActivity").exec();
        }catch(Exception ignored){}
    }

    private void updateFontsInfrastructure() {
            boolean customFontsEnabled = XPrefs.Xprefs.getBoolean("enableCustomFonts", false);
            boolean GSansOverrideEnabled = XPrefs.Xprefs.getBoolean("gsans_override", false);
            
            new Thread(() -> {
                try {
                    if (customFontsEnabled && GSansOverrideEnabled) {
                        Shell.cmd(String.format("cp %s/data/fontz/GSans/*.ttf %s/system/fonts/ && cp %s/data/productz/etc/fonts_customization.xml.NEW %s/system/product/etc/fonts_customization.xml", XPrefs.MagiskRoot, XPrefs.MagiskRoot, XPrefs.MagiskRoot, XPrefs.MagiskRoot)).exec();
                    } else if (customFontsEnabled) {
                        Shell.cmd(String.format("rm -rf %s/system/fonts/*.ttf && cp %s/data/productz/etc/fonts_customization.xml.OLD %s/system/product/etc/fonts_customization.xml && cp -r %s/data/productz/fonts/* %s/system/product/fonts/", XPrefs.MagiskRoot, XPrefs.MagiskRoot, XPrefs.MagiskRoot, XPrefs.MagiskRoot, XPrefs.MagiskRoot)).exec();
                    } else {
                        Shell.cmd(String.format("rm -rf %s/system/fonts/*.ttf && rm -f %s/system/product/etc/fonts_customization.xml && rm -rf %s/system/product/fonts/*", XPrefs.MagiskRoot, XPrefs.MagiskRoot, XPrefs.MagiskRoot)).exec();
                    }
                }catch (Exception ignored){}
            }).start();
    }

    @Override
    public boolean listensTo(String packageName) { return packageName.equals("com.android.systemui"); }
    
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable { }
}
