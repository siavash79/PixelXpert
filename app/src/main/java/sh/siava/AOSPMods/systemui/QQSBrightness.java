package sh.siava.AOSPMods.systemui;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

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
    private View QSbrightnessSliderView = null;
    private Object BrightnessMirrorController = null;
    private View QQSbrightnessSliderView = null;
    private Object QS, QQS;
    private ViewGroup QSParent;

    private static boolean BrightnessHookEnabled = true;
    private static boolean QQSBrightnessEnabled = false;
    private static boolean QSBrightnessDisabled = false;
    @Override
    public void updatePrefs(String... Key) {

        BrightnessHookEnabled = XPrefs.Xprefs.getBoolean("BrightnessHookEnabled", true);
        QQSBrightnessEnabled = XPrefs.Xprefs.getBoolean("QQSBrightnessEnabled", false);
        QSBrightnessDisabled = XPrefs.Xprefs.getBoolean("QSBrightnessDisabled", false);

        if(QSBrightnessDisabled) QQSBrightnessEnabled = false; //if there's no slider, then .......

        if(Key.length > 0)
        {
            switch (Key[0])
            {
                case "QQSBrightnessEnabled":
                    setQQSVisibility();
                    break;
                case "QSBrightnessDisabled":
                    setQSVisibility();
                    setQQSVisibility();
                    break;
            }
        }
        else
        {
            setQQSVisibility();
        }
    }
    
    private void setQSVisibility() {
        try {
            if (QSBrightnessDisabled) {
                QSParent.removeView(QSbrightnessSliderView);
            } else {
                Object mView = XposedHelpers.getObjectField(QS, "mView");
                XposedHelpers.callMethod(mView, "setBrightnessView", QSbrightnessSliderView);
            }
        } catch (Exception ignored){}
    }
    
    private void setQQSVisibility() {
        if (QQSbrightnessSliderView == null) return;
        try{
            if (QQSBrightnessEnabled) {
                XposedHelpers.callMethod(QQS, "setBrightnessView", QQSbrightnessSliderView);
            } else {
                ((FrameLayout) QQSbrightnessSliderView.getParent()).removeView(QQSbrightnessSliderView);
            }
        } catch(Exception ignored){}
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!BrightnessHookEnabled) //master switch
            return;

        Class<?> QuickQSPanelClass = XposedHelpers.findClass("com.android.systemui.qs.QuickQSPanel", lpparam.classLoader);
        Class<?> QSPanelControllerClass = XposedHelpers.findClass("com.android.systemui.qs.QSPanelController", lpparam.classLoader);
        Class<?> BrightnessMirrorHandlerClass = XposedHelpers.findClass("com.android.systemui.settings.brightness.BrightnessMirrorHandler", lpparam.classLoader);

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
                QS = param.thisObject;
                brightnessControllerFactory = param.args[12];
                brightnessSliderFactory = param.args[13];
                Object mBrightnessSliderController = XposedHelpers.getObjectField(param.thisObject, "mBrightnessSliderController");
                QSbrightnessSliderView = (View) XposedHelpers.callMethod(mBrightnessSliderController, "getRootView");
                QSParent = (ViewGroup) QSbrightnessSliderView.getParent();
    
                setQSVisibility();
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
                QQSbrightnessSliderView = (View) XposedHelpers.callMethod(QQSBrightnessSliderController, "getRootView");

                //Creating controller and handler
                Object mBrightnessController;
                try {
                    mBrightnessController = XposedHelpers.callMethod(brightnessControllerFactory, "create", QQSBrightnessSliderController);
                }
                catch(Throwable e) //some custom roms added icon into signature. like ArrowOS
                {
                    ImageView icon = (ImageView) XposedHelpers.callMethod(QQSBrightnessSliderController, "getIconView");
                    mBrightnessController = XposedHelpers.callMethod(brightnessControllerFactory, "create", icon, QQSBrightnessSliderController);
                }
                mBrightnessMirrorHandlerController = BrightnessMirrorHandlerClass.getConstructors()[0].newInstance(mBrightnessController);
                XposedHelpers.callMethod(mBrightnessMirrorHandlerController, "setController", BrightnessMirrorController);

                //initialization
                XposedHelpers.callMethod(QQSBrightnessSliderController, "init");
                XposedHelpers.callMethod(mBrightnessController, "registerCallbacks");
                XposedHelpers.callMethod(mBrightnessController, "checkRestrictionAndSetEnabled");
                XposedHelpers.callMethod(mBrightnessMirrorHandlerController, "onQsPanelAttached");
                
                setQQSVisibility();
            }
        });
    }
}
