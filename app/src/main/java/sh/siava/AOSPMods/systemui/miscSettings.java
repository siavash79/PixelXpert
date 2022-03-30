package sh.siava.AOSPMods.systemui;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.IXposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class miscSettings implements IXposedModPack {

    @Override
    public void updatePrefs() {
        // her you can ask for preferences and do stuff based on them.
        //preferences are located here: XPrefs.Xprefs
        XPrefs.Xprefs.getBoolean("something", false);
        if(true/*something*/)
        {

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
