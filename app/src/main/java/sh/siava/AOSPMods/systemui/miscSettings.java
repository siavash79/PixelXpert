package sh.siava.AOSPMods.systemui;

import com.topjohnwu.superuser.Shell;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.IXposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class miscSettings implements IXposedModPack {

    @Override
    public void updatePrefs(String...Key) {
        if(Key == null)
        {
            //we are at startup
        }
        else
        {
            //we know what has changed
        }
        // here you can ask for preferences and do stuff based on them.
        //preferences are located here: XPrefs.Xprefs
        Boolean SysUITunerEnabled = XPrefs.Xprefs.getBoolean("sysui_tuner", false);
        if(SysUITunerEnabled)
        {
            Shell.su("pm enable com.android.systemui/.tuner.TunerActivity").exec();
        } else {
            Shell.su("pm disable com.android.systemui/.tuner.TunerActivity").exec();
        }

        Boolean WifiCellEnabled = XPrefs.Xprefs.getBoolean("wifi_cell", false);
        if(SysUITunerEnabled)
        {
            Shell.su("settings put global settings_provider_model false && settings list secure | grep sysui_qs_tiles > /sdcard/current_qs_tiles.txt && sed 's/wifi,cell,//g' /sdcard/current_qs_tiles.txt > /sdcard/nowificell_qs_tiles.txt && sed 's/=/ /g' /sdcard/nowificell_qs_tiles.txt > /sdcard/new_qs_tiles.txt && sed 's/sysui_qs_tiles/settings put secure sysui_qs_tiles/g' /sdcard/new_qs_tiles.txt > /sdcard/new2_qs_tiles.txt && sed -re 's/\\(/\"\\(/g' /sdcard/new2_qs_tiles.txt > /sdcard/new3_qs_tiles.txt && sed -re 's/\\)/\\)\"/g' /sdcard/new3_qs_tiles.txt > /sdcard/new4_qs_tiles.txt && sed -re 's/internet,/wifi,cell,/g' /sdcard/new4_qs_tiles.txt > /sdcard/final_qs_tiles.txt && sh /sdcard/final_qs_tiles.txt && rm -rf /sdcard/current_qs_tiles.txt && rm -rf /sdcard/new_qs_tiles.txt && rm -rf /sdcard/new2_qs_tiles.txt && rm -rf /sdcard/new3_qs_tiles.txt && rm -rf /sdcard/new4_qs_tiles.txt && rm -rf /sdcard/final_qs_tiles.txt && rm -rf /sdcard/nowificell_qs_tiles.txt").exec();
        } else {
            Shell.su("settings list secure | grep sysui_qs_tiles > /sdcard/current_qs_tiles.txt && sed 's/internet,//g' /sdcard/current_qs_tiles.txt > /sdcard/nointernet_qs_tiles.txt && sed 's/=/ /g' /sdcard/nointernet_qs_tiles.txt > /sdcard/new_qs_tiles.txt && sed 's/sysui_qs_tiles/settings put secure sysui_qs_tiles/g' /sdcard/new_qs_tiles.txt > /sdcard/new2_qs_tiles.txt && sed -re 's/\\(/\"\\(/g' /sdcard/new2_qs_tiles.txt > /sdcard/new3_qs_tiles.txt && sed -re 's/\\)/\\)\"/g' /sdcard/new3_qs_tiles.txt > /sdcard/new4_qs_tiles.txt && sed -re 's/wifi,cell,/internet,/g' /sdcard/new4_qs_tiles.txt > /sdcard/final_qs_tiles.txt && sh /sdcard/final_qs_tiles.txt && rm -rf /sdcard/current_qs_tiles.txt && rm -rf /sdcard/new_qs_tiles.txt && rm -rf /sdcard/new2_qs_tiles.txt && rm -rf /sdcard/new3_qs_tiles.txt && rm -rf /sdcard/new4_qs_tiles.txt && rm -rf /sdcard/final_qs_tiles.txt && rm -rf /sdcard/nointernet_qs_tiles.txt && settings put global settings_provider_model true").exec();
        }
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
