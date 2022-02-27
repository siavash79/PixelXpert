package sh.siava.AOSPMods;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public abstract class aModManager {
    protected XC_LoadPackage.LoadPackageParam lpparam;

    public aModManager(XC_LoadPackage.LoadPackageParam lpparam) {
//        this.getClass().getConstructor(String.class);
        this.lpparam = lpparam;
    }

    protected abstract void hookMethods() throws InstantiationException, IllegalAccessException;
}