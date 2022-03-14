package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.XPrefs;


//TODO: unknown battery symbol / percent text beside icon / update shape upon request / other shapes / dual tone

public class BatteryStyleManager implements IXposedHookLoadPackage {
    public static final String listenPackage = "com.android.systemui";
    public static boolean circleBatteryEnabled = false;

    private int frameColor;
    public static int BatteryStyle = 1;
    public static boolean ShowPercent = false;
    //private CircleBatteryDrawable mCircleDrawable;

    public static void updatePrefs()
    {
        String BatteryStyleStr = XPrefs.Xprefs.getString("BatteryStyle", "0");
        int batteryStyle = Integer.parseInt(BatteryStyleStr);

        if(batteryStyle == 0)
        {
            return;
        }
        circleBatteryEnabled = true;

        BatteryStyle = batteryStyle;
        ShowPercent = XPrefs.Xprefs.getBoolean("BatteryShowPercent", false);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if(!lpparam.packageName.equals(listenPackage)) return;

        XposedHelpers.findAndHookConstructor("com.android.settingslib.graph.ThemedBatteryDrawable", lpparam.classLoader, Context.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                frameColor = (int) param.args[1];
            }
        });

        Class BatteryMeterViewClass;
        if(Build.VERSION.SDK_INT == 31) { //Andriod 12
            BatteryMeterViewClass = XposedHelpers.findClass("com.android.systemui.BatteryMeterView", lpparam.classLoader);
        }
        else if (Build.VERSION.SDK_INT == 32) //Android 12L-13
        {
            BatteryMeterViewClass = XposedHelpers.findClass("com.android.systemui.battery.BatteryMeterView", lpparam.classLoader);
        }
        else //not compatible
        {
            return;
        }

        XposedHelpers.findAndHookConstructor(BatteryMeterViewClass,
                    Context.class, AttributeSet.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (!circleBatteryEnabled) return;

                            CircleBatteryDrawable mCircleDrawable = new CircleBatteryDrawable((Context) param.args[0], frameColor);
                            mCircleDrawable.setShowPercent(ShowPercent);
                            mCircleDrawable.setMeterStyle(BatteryStyle);

                            XposedHelpers.setAdditionalInstanceField(param.thisObject, "mCircleDrawable", mCircleDrawable);

                            ImageView mBatteryIconView = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mBatteryIconView");

//                            Drawable mCircleDrawable = (Drawable) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mCircleDrawable");
                            mBatteryIconView.setImageDrawable(mCircleDrawable);
                            XposedHelpers.setObjectField(param.thisObject, "mBatteryIconView", mBatteryIconView);
                        }
                    });


        XposedHelpers.findAndHookMethod(BatteryMeterViewClass,
                "onBatteryUnknownStateChanged", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(!circleBatteryEnabled) return;

                        ImageView mBatteryIconView = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mBatteryIconView");
                        CircleBatteryDrawable mCircleDrawable = (CircleBatteryDrawable) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mCircleDrawable");

                        mCircleDrawable.setMeterStyle(BatteryStyle);

                        mBatteryIconView.setImageDrawable(mCircleDrawable);
//                        XposedHelpers.setObjectField(param.thisObject, "mBatteryIconView", mBatteryIconView);

                    }
                });


        XposedHelpers.findAndHookMethod(BatteryMeterViewClass,
                "scaleBatteryMeterViews", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if(!circleBatteryEnabled) return;

                        ImageView mBatteryIconView = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mBatteryIconView");
                        if (mBatteryIconView == null)
                            param.setResult(null);

                        Context context = (Context) XposedHelpers.callMethod(param.thisObject, "getContext");
                        Resources res = context.getResources();

                        TypedValue typedValue = new TypedValue();

                        res.getValue(res.getIdentifier("status_bar_icon_scale_factor", "dimen", context.getPackageName()), typedValue, true);
                        float iconScaleFactor = typedValue.getFloat();

                        int batteryHeight = res.getDimensionPixelSize(res.getIdentifier("status_bar_battery_icon_height", "dimen", context.getPackageName()));
                        int batteryWidth = res.getDimensionPixelSize(res.getIdentifier("status_bar_battery_icon_height", "dimen", context.getPackageName()));
                        int marginBottom = res.getDimensionPixelSize(res.getIdentifier("battery_margin_bottom", "dimen", context.getPackageName()));

                        LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                                (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));

                        scaledLayoutParams.setMargins(0, 0, 0, marginBottom);

                        mBatteryIconView.setLayoutParams(scaledLayoutParams);

                        param.setResult(null);
                    }
                });

        if(Build.VERSION.SDK_INT == 31) { //Android 12

            XposedHelpers.findAndHookMethod(BatteryMeterViewClass,
                    "onBatteryLevelChanged", int.class, boolean.class, boolean.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (!circleBatteryEnabled) return;

                            CircleBatteryDrawable mCircleDrawable = (CircleBatteryDrawable) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mCircleDrawable");
                            mCircleDrawable.setCharging((boolean) param.args[1]);
                            mCircleDrawable.setBatteryLevel((int) param.args[0]);
                        }
                    });
        }
        else
        { //Probably SDK 32-33
            XposedHelpers.findAndHookMethod(BatteryMeterViewClass,
                    "onBatteryLevelChanged", int.class, boolean.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (!circleBatteryEnabled) return;

                            CircleBatteryDrawable mCircleDrawable = (CircleBatteryDrawable) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mCircleDrawable");
                            mCircleDrawable.setCharging((boolean) param.args[1]);
                            mCircleDrawable.setBatteryLevel((int) param.args[0]);
                        }
                    });
        }

        XposedHelpers.findAndHookMethod(BatteryMeterViewClass,
                "onPowerSaveChanged", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if(!circleBatteryEnabled) return;

                        CircleBatteryDrawable mCircleDrawable = (CircleBatteryDrawable) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mCircleDrawable");
                        mCircleDrawable.setPowerSaveEnabled((boolean) param.args[0]);
                    }
                });


        XposedHelpers.findAndHookMethod(BatteryMeterViewClass,
                "updateColors", int.class, int.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(!circleBatteryEnabled) return;

                        CircleBatteryDrawable mCircleDrawable = (CircleBatteryDrawable) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mCircleDrawable");
                        if(mCircleDrawable == null) return;
                        mCircleDrawable.setColors((int) param.args[0], (int) param.args[1], (int) param.args[2]);
                    }
                });
    }
}
