package sh.siava.AOSPMods;

import static sh.siava.AOSPMods.systemui.BackGestureManager.backGestureHeightFraction;

import android.widget.TextView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.android.HideGoogle2021;
import sh.siava.AOSPMods.systemui.*;
public class AOSPMods implements IXposedHookLoadPackage{

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        BackGestureManager.backGestureHeightFraction = 2;
        DoubleTapSleepLS.doubleTapToSleepEnabled = true;
        UDFPSManager.transparentBG = true;
        QSHeaderManager.setLightQSHeader(true);

        BatteryStyleManager.circleBatteryEnabled = true;
        BatteryStyleManager.BatteryStyle = 2;
        BatteryStyleManager.ShowPercent = true;


        if (lpparam.packageName.equals("com.android.systemui")) {

            //Xposedbridge.log("SIAPOSED : " + lpparam.packageName);
            XposedHelpers.findAndHookMethod("com.android.systemui.qs.QSFooterView", lpparam.classLoader, "setBuildText", new removeBuildText());



        }

}

/*    private static void modNavBar(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        Class navbarClass = XposedHelpers.findClass("com.android.systemui.navigationbar.NavigationBarInflaterView", lpparam.classLoader);

        //Xposedbridge.log("SIAPOSED : found nav class");

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

                        //Xposedbridge.log("SIAPOSED btn :" + button);
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
        //Xposedbridge.log("SIAPOSED : found btnclass");

        XposedHelpers.findAndHookConstructor("com.android.systemui.navigationbar.NavigationBarView", lpparam.classLoader, Context.class, AttributeSet.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                //Xposedbridge.log("SIAPOSED : in const hook");
                Class ButtonDispatcherClass = XposedHelpers.findClass("com.android.systemui.navigationbar.buttons.ButtonDispatcher", lpparam.classLoader);
                Class KeyButtonDrawableClass = XposedHelpers.findClass("com.android.systemui.navigationbar.buttons.KeyButtonDrawable", lpparam.classLoader);

                //Xposedbridge.log("SIAPOSED : line 94");

                SparseArray mButtonDispatchers = (SparseArray) XposedHelpers.getObjectField(param.thisObject, "mButtonDispatchers");

                Object temp, btnTemp;

                //Xposedbridge.log("SIAPOSED : line 100");

                temp = ButtonDispatcherClass.getDeclaredConstructor(int.class).newInstance(mappedResource.get(R.id.power));
                //Xposedbridge.log("SIAPOSED : line 101");
                
                btnTemp = XposedHelpers.callMethod(param.thisObject, "getDrawable", mappedResource.get(R.drawable.ic_sysbar_power));
                //Xposedbridge.log("SIAPOSED : line 102");
                XposedHelpers.callMethod(temp, "setImageDrawable", btnTemp);
                XposedHelpers.callMethod(temp, "setVisibility", View.VISIBLE);
                mButtonDispatchers.put(mappedResource.get(R.id.power), temp);
                pwrDispatch = temp;

                //Xposedbridge.log("SIAPOSED : line 108");

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

                //Xposedbridge.log("SIAPOSED : line 122");
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

