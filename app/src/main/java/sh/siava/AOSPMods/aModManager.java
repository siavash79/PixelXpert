package sh.siava.AOSPMods;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

abstract class aModManager {
    protected XC_LoadPackage.LoadPackageParam lpparam;

    aModManager(XC_LoadPackage.LoadPackageParam lpparam) {
//        this.getClass().getConstructor(String.class);
        this.lpparam = lpparam;
    }

    protected abstract void hookMethods() throws InstantiationException, IllegalAccessException;
}