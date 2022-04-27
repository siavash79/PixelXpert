package sh.siava.AOSPMods.systemui;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.IXposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class CarrierTextManager implements IXposedModPack {
    public static final String listenPackage = "com.android.systemui";
    public static boolean isEnabled = false;
    public static String customText = "";
    private static Object mCarrierTextManager = null;

    public void updatePrefs(String...Key)
    {
        if(XPrefs.Xprefs == null) return;
        boolean newisEnabled = XPrefs.Xprefs.getBoolean("carrierTextMod", false);
        String newcustomText = XPrefs.Xprefs.getString("carrierTextValue", "");

        if(newisEnabled != isEnabled || !newcustomText.equals(customText))
        {
            isEnabled = newisEnabled;
            customText = newcustomText;
            if(mCarrierTextManager != null)
            {
                XposedHelpers.callMethod(mCarrierTextManager, "updateCarrierText");
            }
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if(!lpparam.packageName.equals(listenPackage)) return;

        Class<?> CarrierTextManagerClass = XposedHelpers.findClass("com.android.keyguard.CarrierTextManager", lpparam.classLoader);
        Class<?> CarrierTextCallbackInfo = XposedHelpers.findClass("com.android.keyguard.CarrierTextManager$CarrierTextCallbackInfo", lpparam.classLoader);

        XposedBridge.hookAllConstructors(CarrierTextManagerClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mCarrierTextManager = param.thisObject;
            }
        });

        XposedBridge.hookAllConstructors(CarrierTextCallbackInfo, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if(isEnabled)
                {
                    XposedHelpers.setObjectField(param.thisObject, "carrierText", customText);
                    XposedHelpers.setObjectField(param.thisObject, "listOfCarriers", new CharSequence[0]);
                    XposedHelpers.setObjectField(param.thisObject, "anySimReady", true);
                    XposedHelpers.setObjectField(param.thisObject, "subscriptionIds", new int[0]);
                    XposedHelpers.setObjectField(param.thisObject, "airplaneMode", false);
                    param.setResult(null);
                }
            }
        });

    }
    
    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
    
    
}
