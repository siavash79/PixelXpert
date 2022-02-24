package sh.siava.AOSPMods;

import android.content.res.XModuleResources;

import de.robv.android.xposed.callbacks.XC_InitPackageResources;

public abstract class aResManager {
    protected String MODULE_PATH;
    protected XC_InitPackageResources.InitPackageResourcesParam resparam;
    protected XModuleResources modRes;

    public aResManager(String MODULE_PATH, XC_InitPackageResources.InitPackageResourcesParam resparam, XModuleResources modRes)
    {
        this.MODULE_PATH = MODULE_PATH;
        this.resparam = resparam;
        this.modRes = modRes;
    }

    public abstract void hookResources();
}
