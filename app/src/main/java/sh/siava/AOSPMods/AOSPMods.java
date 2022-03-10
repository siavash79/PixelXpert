package sh.siava.AOSPMods;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.systemui.*;
public class AOSPMods implements IXposedHookLoadPackage{

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {

        if (lpparam.packageName.equals("com.android.systemui")) {
//            XPrefs.Xprefs.edit().putBoolean("SystemUIConncted", true).commit();
        }
    }
}




