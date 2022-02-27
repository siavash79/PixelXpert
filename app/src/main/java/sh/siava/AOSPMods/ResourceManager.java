package sh.siava.AOSPMods;

import android.content.res.XModuleResources;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import sh.siava.AOSPMods.systemui.QSHeaderResManager;

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

        //map drawables
//        mapResource(R.drawable.ic_sysbar_power);
//        mapResource(R.drawable.ic_sysbar_volume_plus);
//        mapResource(R.drawable.ic_sysbar_volume_minus);

        //map ids
//        mapResource(R.id.power);
//        mapResource(R.id.volume_plus);
//        mapResource(R.id.volume_minus);

        //map layouts
//        mapResource(R.layout.power);
//        mapResource(R.layout.volume_plus);
//        mapResource(R.layout.volume_minus);

//        resparam.res.setReplacement("com.android.systemui", "raw", "udfps_lockscreen_fp", modRes.fwd(R.raw.udfps_lockscreen_fp));

        if(resparam.packageName.startsWith("com.android.systemui")) {
/*            UDFPSResources udfpsResources = new UDFPSResources(MODULE_PATH, resparam, modRes);
            udfpsResources.hookResources();
            udfpsResources.setTransparent(true);*/

            aResManager QSHeader = new QSHeaderResManager(MODULE_PATH, resparam, modRes);
            QSHeader.hookResources();
        }
    }


}
