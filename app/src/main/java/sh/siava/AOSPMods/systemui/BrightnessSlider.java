package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class BrightnessSlider extends XposedModPack {
    private static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;

    private Object brightnessSliderFactory = null;
    private Object brightnessControllerFactory = null;

    private static boolean BrightnessSliderOnBottom = false;
    private static boolean BrightnessHookEnabled = true;
    private static boolean QQSBrightnessEnabled = false;
    private static boolean QSBrightnessDisabled = false;
    private Object QQSPC;
    private static final String QS_SHOW_BRIGHTNESS = "qs_show_brightness";
    private Object mBrightnessMirrorController;
    private Object mTunerService;
    private Object QSPanelController;
    private final ArrayList<Integer> collectedFields = new ArrayList<>();
    private Object mBrightnessController;
    private Object mBrightnessMirrorHandler;

    public BrightnessSlider(Context context) { super(context); }

    private void dataCollected(int id, Class<?> BrightnessMirrorHandlerClass)
    {
        if(!collectedFields.contains(id))
        {
            collectedFields.add(id);
        }
        if(collectedFields.size() == 3)
        {
            try {
                makeQQSBrightness(BrightnessMirrorHandlerClass);
            } catch(Throwable ignored){}
        }
    }

    @Override
    public void updatePrefs(String... Key) {

        BrightnessHookEnabled = Xprefs.getBoolean("BrightnessHookEnabled", true);
        QQSBrightnessEnabled = Xprefs.getBoolean("QQSBrightnessEnabled", false);
        QSBrightnessDisabled = Xprefs.getBoolean("QSBrightnessDisabled", false);
        BrightnessSliderOnBottom = Xprefs.getBoolean("BrightnessSlierOnBottom", false);

        if(QSBrightnessDisabled) QQSBrightnessEnabled = false; //if there's no slider, then .......

        if(Key.length > 0)
        {
            switch (Key[0])
            {
                case "QQSBrightnessEnabled":
                case "QSBrightnessDisabled":
                case "BrightnessSlierOnBottom":
                    setQSVisibility();
                    setQQSVisibility();
                    break;
            }
        }
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }

    private void makeQQSBrightness(Class<?> BrightnessMirrorHandlerClass) throws Throwable {
        ViewGroup quickQSPanel = (ViewGroup) getObjectField(QQSPC, "mView");

        Object mBrightnessSliderController = callMethod(brightnessSliderFactory, "create", mContext, quickQSPanel);

        View QQSBrightnessSliderView = (View) getObjectField(mBrightnessSliderController, "mView");
        setBrightnessView(quickQSPanel, QQSBrightnessSliderView, true);

        //mBrightnessController = BrightnessControllerClass.getConstructors()[0].newInstance(getObjectField(brightnessControllerFactory, "mContext"), mBrightnessSliderController, getObjectField(brightnessControllerFactory, "mBroadcastDispatcher"),getObjectField(brightnessControllerFactory, "mBackgroundHandler"));
        mBrightnessController = callMethod(brightnessControllerFactory, "create", mBrightnessSliderController);

        mBrightnessMirrorHandler = BrightnessMirrorHandlerClass.getConstructors()[0].newInstance(mBrightnessController);

        callMethod(mBrightnessController, "checkRestrictionAndSetEnabled");

        callMethod(mTunerService, "addTunable", quickQSPanel, QS_SHOW_BRIGHTNESS);
        callMethod(mBrightnessMirrorHandler, "onQsPanelAttached");
        callMethod(mBrightnessController, "registerCallbacks");
        callMethod(mBrightnessMirrorHandler, "setController", mBrightnessMirrorController);
        callMethod(mBrightnessSliderController, "init");
    }


    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!BrightnessHookEnabled || !listenPackage.equals(lpparam.packageName)) //master switch
            return;

        Class<?> QSPanelControllerClass = findClass("com.android.systemui.qs.QSPanelController", lpparam.classLoader);
        Class<?> BrightnessMirrorHandlerClass = findClass("com.android.systemui.settings.brightness.BrightnessMirrorHandler", lpparam.classLoader);
        Class<?> QuickQSPanelControllerClass = findClass("com.android.systemui.qs.QuickQSPanelController", lpparam.classLoader);
        Class<?> QSPanelControllerBaseClass = findClass("com.android.systemui.qs.QSPanelControllerBase", lpparam.classLoader);
        Class<?> CentralSurfacesImplClass = findClassIfExists("com.android.systemui.statusbar.phone.CentralSurfacesImpl", lpparam.classLoader);

        if(CentralSurfacesImplClass != null)
        {
            hookAllConstructors(CentralSurfacesImplClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    new Thread(() -> {
                        try {
                            while (mBrightnessMirrorController == null) {
                                Thread.currentThread().wait(500);
                                mBrightnessMirrorController = getObjectField(param.thisObject, "mBrightnessMirrorController");
                            }
                            dataCollected(2, BrightnessMirrorHandlerClass);
                        } catch (Throwable ignored) {
                        }
                    }).start();
                }
            });
        }

        hookAllMethods(QSPanelControllerBaseClass, "setTiles", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                setQSVisibility();
                setQQSVisibility();
            }
        });

        hookAllConstructors(QSPanelControllerClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                QSPanelController = param.thisObject;

                int indexCorrection = (Build.VERSION.SDK_INT == 33)
                        ? 0
                        : 1;

                brightnessControllerFactory = param.args[11 + indexCorrection];
                brightnessSliderFactory = param.args[12 + indexCorrection];
                mTunerService = getObjectField(param.thisObject, "mTunerService");

                dataCollected(0, BrightnessMirrorHandlerClass);
                Object mView = getObjectField(param.thisObject,"mView");
                setBrightnessView((ViewGroup) mView, (View) getObjectField(mView, "mBrightnessView"), false);
            }
        });

        hookAllConstructors(QuickQSPanelControllerClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            }
        });

        hookAllMethods(QuickQSPanelControllerClass, "onInit", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                QQSPC = param.thisObject;

                dataCollected(1, BrightnessMirrorHandlerClass);
            }
        });

        hookAllMethods(QuickQSPanelControllerClass, "onViewAttached", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            }
        });

        hookAllMethods(QuickQSPanelControllerClass, "onViewDetached", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    callMethod(mBrightnessMirrorHandler, "onQsPanelDettached");
                    callMethod(mBrightnessController, "unregisterCallbacks");
                }catch (Throwable ignored){}
            }
        });

        hookAllMethods(QuickQSPanelControllerClass, "setTiles", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            }
        });
    }

    //swapping top and bottom margins of slider
    private void setSliderMargins(View slider) {
        if (slider != null) {
            Resources res = mContext.getResources();
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) slider.getLayoutParams();
            int top = res.getDimensionPixelSize(
                    res.getIdentifier("qs_brightness_margin_top", "dimen", mContext.getPackageName()));
            int bottom = res.getDimensionPixelSize(
                    res.getIdentifier("qs_brightness_margin_bottom", "dimen", mContext.getPackageName()));

            lp.topMargin = (BrightnessSliderOnBottom)
                    ? bottom
                    : top;
            lp.bottomMargin = (BrightnessSliderOnBottom)
                    ? top
                    : bottom;
            slider.setLayoutParams(lp);
        }
    }

    private void setBrightnessView(ViewGroup o, @NonNull View view, boolean isQQS) { //Classloader can't find the method by reflection
        View mBrightnessView = (View) getObjectField(o,"mBrightnessView");
        if (mBrightnessView != null) {
            o.removeView(mBrightnessView);
            setObjectField(o, "mMovableContentStartIndex", getIntField(o, "mMovableContentStartIndex") - 1);
        }
        try{
            ((ViewGroup) view.getParent()).removeView(view);
        }catch(Throwable ignored){}

        o.addView(view, BrightnessSliderOnBottom
                ? isQQS
                    ? 2
                    : 1
                : 0);

        setObjectField(o, "mBrightnessView", view);

        setSliderMargins(view);

        setObjectField(o, "mMovableContentStartIndex", getIntField(o, "mMovableContentStartIndex") + 1);
    }
    private void setQSVisibility(){
        try {
            ViewGroup mView = (ViewGroup) getObjectField(QSPanelController, "mView");
            mView.post(() -> {
                View mBrightnessView = (View) getObjectField(mView, "mBrightnessView");
                try {
                    if (QSBrightnessDisabled) {
                        mView.removeView(mBrightnessView);
                    } else {
                        setBrightnessView(mView, mBrightnessView, false);
                    }
                }catch (Exception ignored){}
            });
        }catch(Throwable ignored){}

    }

    private void setQQSVisibility() {
        if(mBrightnessMirrorHandler == null) return; //Brightness slider isn't made
        ViewGroup QuickQSPanel = (ViewGroup) getObjectField(QQSPC, "mView");
        View QQSBrightnessSliderView = (View) getObjectField(QuickQSPanel, "mBrightnessView");

        if(QuickQSPanel == null || QQSBrightnessSliderView == null) return;

        QuickQSPanel.post(() -> {
            try {
                if (QQSBrightnessEnabled) {
                    setBrightnessView(QuickQSPanel, QQSBrightnessSliderView, true);
                } else {
                    ((ViewGroup) QQSBrightnessSliderView.getParent()).removeView(QQSBrightnessSliderView);
                }
            } catch (Exception ignored) {}
        });
    }
}