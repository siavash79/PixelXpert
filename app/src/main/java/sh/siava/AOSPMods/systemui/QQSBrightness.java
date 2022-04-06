package sh.siava.AOSPMods.systemui;

import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.IXposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class QQSBrightness implements IXposedModPack {
    private static final String listenPackage = "com.android.systemui";

    private Object mBrightnessMirrorHandlerController = null;
    private Object brightnessSliderFactory = null;
    private Object brightnessControllerFactory = null;
    private Object QQSBrightnessSliderController = null;
    private Object BrightnessMirrorController = null;
    private View brightnessSliderView = null;
    private Object QQS;

    private static boolean QQSBrightnessHookEnabled = true;
    private static boolean QQSBrightnessEnabled = false;
    @Override
    public void updatePrefs(String... Key) {

        QQSBrightnessHookEnabled = XPrefs.Xprefs.getBoolean("QQSBrightnessHookEnabled", true);
        QQSBrightnessEnabled = XPrefs.Xprefs.getBoolean("QQSBrightnessEnabled", false);

        if(Key.length > 0)
        {
            if(Key[0].equals("QQSBrightnessEnabled"))
            {
                setView();
            }
        }
        else
        {
            setView();
        }
    }

    private void setView() {
        if(brightnessSliderView == null) return;
        if(QQSBrightnessEnabled)
        {
            brightnessSliderView.setVisibility(View.VISIBLE);
        }
        else
        {
            try {
                brightnessSliderView.setVisibility(View.GONE);
            }catch(Exception ignored){}
        }
    }

    @Override
    public String getListenPack() {
        return listenPackage;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!QQSBrightnessHookEnabled) //master switch
            return;

        Class QuickQSPanelClass = XposedHelpers.findClass("com.android.systemui.qs.QuickQSPanel", lpparam.classLoader);
        Class QSPanelControllerClass = XposedHelpers.findClass("com.android.systemui.qs.QSPanelController", lpparam.classLoader);
        Class BrightnessMirrorHandlerClass = XposedHelpers.findClass("com.android.systemui.settings.brightness.BrightnessMirrorHandler", lpparam.classLoader);

        //Stealing info from Main QS
        XposedBridge.hookAllMethods(QSPanelControllerClass, "setBrightnessMirror", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                BrightnessMirrorController = param.args[0];
            }
        });
        
        XposedBridge.hookAllConstructors(QSPanelControllerClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                brightnessControllerFactory = param.args[12];
                brightnessSliderFactory = param.args[13];
            }
        });
        //End Stealing info from Main QS

        //Making new Brightness Slider in QQS using stolen info
        XposedBridge.hookAllConstructors(QuickQSPanelClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                QQS = param.thisObject;
                if(BrightnessMirrorController == null) return;

                Object context = XposedHelpers.callMethod(param.thisObject, "getContext");

                //Create new Slider
                QQSBrightnessSliderController = XposedHelpers.callMethod(brightnessSliderFactory, "create", context, param.thisObject);

                //Place it to QQS
                brightnessSliderView = (View) XposedHelpers.callMethod(QQSBrightnessSliderController, "getRootView");
                XposedHelpers.callMethod(QQS, "setBrightnessView", brightnessSliderView);

                //Creating controller and handler
                Object mBrightnessController = XposedHelpers.callMethod(brightnessControllerFactory, "create", QQSBrightnessSliderController);
                mBrightnessMirrorHandlerController = BrightnessMirrorHandlerClass.getConstructors()[0].newInstance(mBrightnessController);
                XposedHelpers.callMethod(mBrightnessMirrorHandlerController, "setController", BrightnessMirrorController);

                //initialization
                XposedHelpers.callMethod(QQSBrightnessSliderController, "init");
                XposedHelpers.callMethod(mBrightnessController, "registerCallbacks");
                XposedHelpers.callMethod(mBrightnessController, "checkRestrictionAndSetEnabled");
                XposedHelpers.callMethod(mBrightnessMirrorHandlerController, "onQsPanelAttached");
                
                setView();
            }
        });
    }
}
