package sh.siava.AOSPMods;

import android.content.res.XModuleResources;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;

public class ResourceManager implements IXposedHookInitPackageResources, IXposedHookZygoteInit {

    private String MODULE_PATH;
    private XC_InitPackageResources.InitPackageResourcesParam resparam;
    private XModuleResources modRes;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {

        XModuleResources modRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);

        this.resparam = resparam;
        this.modRes = modRes;
        if(resparam.packageName.startsWith(AOSPMods.SYSTEM_Ui_PACKAGE)) {
        }
    }


}
