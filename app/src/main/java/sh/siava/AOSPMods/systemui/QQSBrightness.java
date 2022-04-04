package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.IXposedModPack;

public class QQSBrightness implements IXposedModPack {
    @Override
    public void updatePrefs(String... Key) {

    }

    @Override
    public String getListenPack() {
        return null;
    }

    private View mBrightnessView = null;
    private Context mContext = null;
    private Resources res = null;
    private XC_LoadPackage.LoadPackageParam lpparam = null;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        this.lpparam = lpparam;

        Class QuickQSPanelClass = XposedHelpers.findClass("com.android.systemui.qs.QuickQSPanel", lpparam.classLoader);
        Class QuickQSPanelControllerClass = XposedHelpers.findClass("com.android.systemui.qs.QuickQSPanelController", lpparam.classLoader);
        Class TunerServiceClass = XposedHelpers.findClass("com.android.systemui.tuner.TunerService", lpparam.classLoader);

        Method setBrightnessViewMethod = XposedHelpers.findMethodExact(QuickQSPanelClass, "setBrightnessView", View.class);
        Method onTuningChangedMethod = XposedHelpers.findMethodExact(QuickQSPanelClass, "onTuningChanged", String.class, String.class);

        XposedBridge.hookMethod(setBrightnessViewMethod
                , new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        mBrightnessView = (View) XposedHelpers.getObjectField(param.thisObject, "mBrightnessView");
                        View view = (View) param.args[0];
                        mContext = view.getContext();
                        res = mContext.getResources();
                        if(mBrightnessView != null)
                        {
                            XposedHelpers.callMethod(param.thisObject, "removeView", mBrightnessView);
                        }

                        mBrightnessView = view;

                        XposedHelpers.setObjectField(param.thisObject, "mBrightnessView", mBrightnessView);


                        Object mAutoBrightnessIcon = view.findViewById(res.getIdentifier("brightness_icon", "id", lpparam.packageName));
                        XposedHelpers.setObjectField(param.thisObject, "mAutoBrightnessIcon", mAutoBrightnessIcon);

                        XposedHelpers.callMethod(param.thisObject, "setBrightnessViewMargin", true);

                        if(mBrightnessView != null)
                        {
                            XposedHelpers.callMethod(param.thisObject, "addView", mBrightnessView);
                        }
                        param.setResult(null);
                    }
                });

        XposedBridge.hookMethod(onTuningChangedMethod, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String key = (String) param.args[0];
                String newValue = (String) param.args[1];

                switch (key)
                {
                    case QS_SHOW_BRIGHTNESS_SLIDER:
                        boolean value =
                                (int) XposedHelpers.callStaticMethod(TunerServiceClass, "parseInteger", newValue, 1) > 1;
                        if (mBrightnessView != null) {
                            mBrightnessView.setVisibility(value ? View.VISIBLE : View.GONE);
                        }
                        break;
                    case QS_BRIGHTNESS_SLIDER_POSITION:
                        break;
                    case QS_SHOW_AUTO_BRIGHTNESS:
                        break;
                    default:
                        break;
                }


                param.setResult(null);
            }
        });


    }

    View getBrightnessView() {
        return mBrightnessView;
    }

    public void setBrightnessViewMargin(boolean top) {
        if (mBrightnessView != null) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mBrightnessView.getLayoutParams();
            if (top) {
                lp.topMargin = mContext.getResources()
                        .getDimensionPixelSize(res.getIdentifier("qs_brightness_margin_top", "dimen", lpparam.packageName)) / 2;
                lp.bottomMargin = mContext.getResources()
                        .getDimensionPixelSize(res.getIdentifier("qs_brightness_margin_bottom", "dimen", lpparam.packageName)) / 2;
            } else {
                lp.topMargin = mContext.getResources()
                        .getDimensionPixelSize(res.getIdentifier("qs_tile_margin_vertical", "dimen", lpparam.packageName));
                lp.bottomMargin = 0;
            }
            mBrightnessView.setLayoutParams(lp);
        }
    }
}
