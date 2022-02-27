package sh.siava.AOSPMods;

import android.content.res.XModuleResources;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;

public class QSHeaderResManager extends aResManager {
    public QSHeaderResManager(String MODULE_PATH, XC_InitPackageResources.InitPackageResourcesParam resparam, XModuleResources modRes) {
        super(MODULE_PATH, resparam, modRes);
    }

    @Override
    public void hookResources() {
        resparam.res.setReplacement(resparam.packageName, "drawable", "brightness_progress_full_drawable", modRes.fwd(R.drawable.brightness_progress_full_drawable));
        resparam.res.setReplacement(resparam.packageName, "drawable", "qs_footer_action_chip_background", modRes.fwd(R.drawable.qs_footer_action_chip_background));
        resparam.res.setReplacement(resparam.packageName, "drawable", "qs_security_footer_background", modRes.fwd(R.drawable.qs_security_footer_background));
        mapResource(R.style.Theme_SystemUI_QuickSettings);
        resparam.res.setReplacement(resparam.packageName, "style", "QSCustomizeToolbar", modRes.fwd(R.style.QSCustomizeToolbar));
//        resparam.res.setReplacement(resparam.packageName, "style", "Theme_SystemUI_QuickSettings", modRes.fwd(R.style.Theme_SystemUI_QuickSettings));
        XposedBridge.log("resource added");

//        resparam.res.setReplacement("com.android.systemui", "attr", "underSurfaceColor", resparam.res.getColor(resparam.res.getIdentifier("@android:color/system_neutral1_100", "attr", "com.android.systemui")));

    }
}
