package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class CarrierTextManager extends XposedModPack {
    public static final String listenPackage = "com.android.systemui";
    public static boolean isEnabled = false;
    public static String customText = "";
    private static Object carrierTextController;
    private static Object carrierTextCallback;

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
            if(isEnabled && carrierTextController != null)
            {
                setCarrierText();
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
                carrierTextCallback = XposedHelpers.getObjectField(carrierTextController, "mCarrierTextCallback");

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
        TextView mView = (TextView) XposedHelpers.getObjectField(carrierTextController, "mView");
        mView.setText(customText);
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
    
    
}
