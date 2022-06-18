package sh.siava.AOSPMods;

import android.content.res.Configuration;
import android.content.res.XModuleResources;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;

public class ResourceManager implements IXposedHookInitPackageResources, IXposedHookZygoteInit {

    private String MODULE_PATH;
    public static XC_InitPackageResources.InitPackageResourcesParam SysUIresparam;
    public static XModuleResources modRes;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {

        XModuleResources modRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);

        if(resparam.packageName.equals(AOSPMods.SYSTEM_UI_PACKAGE)) {
            this.SysUIresparam = resparam;
            this.modRes = modRes;
        }

    }


}
