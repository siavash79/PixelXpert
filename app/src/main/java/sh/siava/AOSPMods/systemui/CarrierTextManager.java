package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.XPrefs;

@SuppressWarnings("RedundantThrows")
public class CarrierTextManager extends XposedModPack {
    private static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;
    private static boolean isEnabled = false;
    private static String customText = "";
    private static Object carrierTextController;

    public CarrierTextManager(Context context) { super(context); }
    
    @Override
    public void updatePrefs(String...Key)
    {
        if(XPrefs.Xprefs == null) return;
        boolean newisEnabled = XPrefs.Xprefs.getBoolean("carrierTextMod", false);
        String newcustomText = XPrefs.Xprefs.getString("carrierTextValue", "");

        if(newisEnabled != isEnabled || !newcustomText.equals(customText))
        {
            isEnabled = newisEnabled;
            customText = newcustomText;
            if(isEnabled)
            {
                setCarrierText();
            }
            else
            {
                try {
                    XposedHelpers.callMethod(
                            XposedHelpers.getObjectField(carrierTextController, "mCarrierTextManager"),
                            "updateCarrierText");
                }catch (Throwable ignored){} //probably not initiated yet
            }
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if(!lpparam.packageName.equals(listenPackage)) return;

        Class<?> CarrierTextControllerClass = XposedHelpers.findClass("com.android.keyguard.CarrierTextController", lpparam.classLoader);

        XposedBridge.hookAllConstructors(CarrierTextControllerClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                carrierTextController = param.thisObject;
                Object carrierTextCallback = XposedHelpers.getObjectField(carrierTextController, "mCarrierTextCallback");

                XposedBridge.hookAllMethods(carrierTextCallback.getClass(),
                        "updateCarrierInfo", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if(!isEnabled) return; //nothing to do

                        setCarrierText();
                        param.setResult(null);
                    }
                });
            }
        });
    }

    private void setCarrierText() {
        try {
            TextView mView = (TextView) XposedHelpers.getObjectField(carrierTextController, "mView");
            mView.setText(customText);
        } catch (Throwable ignored){} //probably not initiated yet
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
}