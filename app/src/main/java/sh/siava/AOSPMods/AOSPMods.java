package sh.siava.AOSPMods;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.systemui.*;
public class AOSPMods implements IXposedHookLoadPackage{

    // THIS is used to detect either the module is enabled correctly or not
    public static boolean systemUIconnected = false;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
//        BackGestureManager.backGestureHeightFraction = 2;
        DoubleTapSleepLS.doubleTapToSleepEnabled = true;
        QSHeaderManager.setLightQSHeader(true);

        BatteryStyleManager.circleBatteryEnabled = true;
        BatteryStyleManager.BatteryStyle = 2;
        BatteryStyleManager.ShowPercent = true;
        CarrierTextManager.isEnabled = true;
        CarrierTextManager.customText = "Siavash";

        if (lpparam.packageName.equals("com.android.systemui")) {
            systemUIconnected = true;
        }
    }
}




