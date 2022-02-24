package sh.siava.AOSPMods;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class UDFPSManager extends aModManager {
    boolean transparentBG;

    UDFPSManager(XC_LoadPackage.LoadPackageParam lpparam, boolean transparentBG) {
        super(lpparam);
        this.transparentBG = transparentBG;

        
    }

    @Override
    protected void hookMethods() {
    }
}
