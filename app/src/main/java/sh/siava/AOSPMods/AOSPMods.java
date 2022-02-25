package sh.siava.AOSPMods;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XModuleResources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
public class AOSPMods implements IXposedHookLoadPackage{




    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals("com.android.systemui"))
            return;

        XposedBridge.log("SIAPOSED : " + lpparam.packageName);
        XposedHelpers.findAndHookMethod("com.android.systemui.qs.QSFooterView", lpparam.classLoader, "setBuildText", new removeBuildText());

        try {
            aModManager batteryStyle = null;
            batteryStyle = new BatteryStyleManager(lpparam, 2, true);
//            batteryStyle.hookMethods();
        }
        catch(Exception e){}

        try {
            aModManager udfpsManager = new UDFPSManager(lpparam, true);
            udfpsManager.hookMethods();
        }
        catch(Exception e){}

        try {
            aModManager backGestureManager = new BackGestureManager(lpparam);
            backGestureManager.hookMethods();
        }
        catch(Exception e){}

}

/*    private static void modNavBar(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        Class navbarClass = XposedHelpers.findClass("com.android.systemui.navigationbar.NavigationBarInflaterView", lpparam.classLoader);

        XposedBridge.log("SIAPOSED : found nav class");

        String POWER = "power";
        String VOLUME_UP = "volup";
        String VOLUME_DOWN = "voldown";

        XposedHelpers.findAndHookMethod(navbarClass, "createView",
                String.class, ViewGroup.class, LayoutInflater.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        View v = (View) param.getResult();
                        String button = extractButton((String) param.args[0]);
                        LayoutInflater inflater = (LayoutInflater) param.args[2];
                        ViewGroup parent = (ViewGroup) param.args[1];

                        XposedBridge.log("SIAPOSED btn :" + button);
                        if (POWER.equals(button)) {
                            v = inflater.inflate(mappedResource.get(R.layout.power), parent, false);
                        } else if (VOLUME_UP.equals(button)) {
                            v = inflater.inflate(mappedResource.get(R.layout.volume_plus), parent, false);
                        } else if (VOLUME_DOWN.equals(button)) {
                            v = inflater.inflate(mappedResource.get(R.layout.volume_minus), parent, false);
                        }
                        param.setResult(v);
                    }
                    private String SIZE_MOD_START = "[";
                    String extractButton(String buttonSpec) {
                        if (!buttonSpec.contains(SIZE_MOD_START)) {
                            return buttonSpec;
                        }
                        return buttonSpec.substring(0, buttonSpec.indexOf(SIZE_MOD_START));
                    }

                });


        Class ButtonDispatcherClass = XposedHelpers.findClass("com.android.systemui.navigationbar.buttons.ButtonDispatcher", lpparam.classLoader);
        XposedBridge.log("SIAPOSED : found btnclass");

        XposedHelpers.findAndHookConstructor("com.android.systemui.navigationbar.NavigationBarView", lpparam.classLoader, Context.class, AttributeSet.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("SIAPOSED : in const hook");
                Class ButtonDispatcherClass = XposedHelpers.findClass("com.android.systemui.navigationbar.buttons.ButtonDispatcher", lpparam.classLoader);
                Class KeyButtonDrawableClass = XposedHelpers.findClass("com.android.systemui.navigationbar.buttons.KeyButtonDrawable", lpparam.classLoader);

                XposedBridge.log("SIAPOSED : line 94");

                SparseArray mButtonDispatchers = (SparseArray) XposedHelpers.getObjectField(param.thisObject, "mButtonDispatchers");

                Object temp, btnTemp;

                XposedBridge.log("SIAPOSED : line 100");

                temp = ButtonDispatcherClass.getDeclaredConstructor(int.class).newInstance(mappedResource.get(R.id.power));
                XposedBridge.log("SIAPOSED : line 101");
                
                btnTemp = XposedHelpers.callMethod(param.thisObject, "getDrawable", mappedResource.get(R.drawable.ic_sysbar_power));
                XposedBridge.log("SIAPOSED : line 102");
                XposedHelpers.callMethod(temp, "setImageDrawable", btnTemp);
                XposedHelpers.callMethod(temp, "setVisibility", View.VISIBLE);
                mButtonDispatchers.put(mappedResource.get(R.id.power), temp);
                pwrDispatch = temp;

                XposedBridge.log("SIAPOSED : line 108");

                temp = ButtonDispatcherClass.getDeclaredConstructor(int.class).newInstance(mappedResource.get(R.id.volume_minus));
                btnTemp = XposedHelpers.callMethod(param.thisObject, "getDrawable", mappedResource.get(R.drawable.ic_sysbar_volume_minus));
                XposedHelpers.callMethod(temp, "setImageDrawable", btnTemp);
                XposedHelpers.callMethod(temp, "setVisibility", View.VISIBLE);
                mButtonDispatchers.put(mappedResource.get(R.id.volume_minus), temp);
                vmDispatch = temp;

                temp = ButtonDispatcherClass.getDeclaredConstructor(int.class).newInstance(mappedResource.get(R.id.volume_plus));
                btnTemp = XposedHelpers.callMethod(param.thisObject, "getDrawable", mappedResource.get(R.drawable.ic_sysbar_volume_plus));
                XposedHelpers.callMethod(temp, "setImageDrawable", btnTemp);
                XposedHelpers.callMethod(temp, "setVisibility", View.VISIBLE);
                mButtonDispatchers.put(mappedResource.get(R.id.volume_plus), temp);
                vuDispatch = temp;

                XposedBridge.log("SIAPOSED : line 122");
            }
        });

        XposedHelpers.findAndHookMethod("com.android.systemui.navigationbar.NavigationBarView", lpparam.classLoader, "updateNavButtonIcons", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

            }
        });

        XposedHelpers.findAndHookMethod("com.android.systemui.navigationbar.NavigationBarView", lpparam.classLoader, "getButtonLocations", boolean.class, boolean.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Class ButtonDispatcherClass = XposedHelpers.findClass("com.android.systemui.navigationbar.buttons.ButtonDispatcher", lpparam.classLoader);

                XposedHelpers.callMethod(param.thisObject, "updateButtonLocation", pwrDispatch, param.args[1], param.args[2]);
                XposedHelpers.callMethod(param.thisObject, "updateButtonLocation", vuDispatch, param.args[1], param.args[2]);
                XposedHelpers.callMethod(param.thisObject, "updateButtonLocation", vmDispatch, param.args[1], param.args[2]);
                param.setResult(param.getResult());
            }
        });

    }*/

}

class removeBuildText extends XC_MethodHook {
    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        super.afterHookedMethod(param);

        TextView mBuildText = (TextView) XposedHelpers.getObjectField(param.thisObject, "mBuildText");
        boolean mShouldShowBuildText = (boolean) XposedHelpers.getObjectField(param.thisObject, "mShouldShowBuildText");

        mBuildText.setText("Siavash + XPOSED");
        mShouldShowBuildText = true;
        mBuildText.setSelected(true);
    }


}

