package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.nfx.android.rangebarpreference.RangeBarHelper;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.IXposedModPack;
import sh.siava.AOSPMods.Utils.BatteryBarView;
import sh.siava.AOSPMods.Utils.CircleBatteryDrawable;
import sh.siava.AOSPMods.Utils.CircleFilledBatteryDrawable;
import sh.siava.AOSPMods.Utils.BatteryDrawable;
import sh.siava.AOSPMods.XPrefs;


//TODO: unknown battery symbol / percent text beside icon / update shape upon request / other shapes / dual tone

public class BatteryStyleManager implements IXposedModPack {
    public static final String listenPackage = "com.android.systemui";

    public static boolean circleBatteryEnabled = false;

    private int frameColor;
    public static int BatteryStyle = 1;
    public static boolean ShowPercent = false;
    public static int scaleFactor = 100;
    public static boolean scaleWithPercent =false;
    static boolean hideBattery = false;
    public static boolean isFastCharging = false;
    private static int BatteryIconOpacity = 100;
    private static int BIconbatteryWarningColor = Color.GREEN;
    private static int BIconbatteryCriticalColor = Color.RED;
    private static boolean BIconTransitColors = false;
    private static boolean BIconColorful = false;
    private static boolean BIconindicateFastCharging = false;
    private static int batteryIconFastChargingColor = Color.BLUE;
    private static int batteryChargingColor = Color.GREEN;
    private static boolean BIconindicateCharging = false;
    private static float[] batteryLevels = new float[]{20f, 40f};
    private static int[] batteryColors = new int[]{Color.RED, Color.YELLOW};
    
    public void updatePrefs(String...Key)
    {
        if(XPrefs.Xprefs == null) return;
        String BatteryStyleStr = XPrefs.Xprefs.getString("BatteryStyle", "0");
        scaleFactor = XPrefs.Xprefs.getInt("BatteryIconScaleFactor", 50)*2;
        int batteryStyle = Integer.parseInt(BatteryStyleStr);
        
        BatteryIconOpacity = XPrefs.Xprefs.getInt("BatteryIconOpacity", 100);
    
        if(batteryStyle == 0)
        {
            circleBatteryEnabled = false;
            return;
        }
        else if(batteryStyle == 99)
        {
            hideBattery = true;
            circleBatteryEnabled = false;
            return;
        }
        circleBatteryEnabled = true;

        BatteryStyle = batteryStyle;
        ShowPercent = XPrefs.Xprefs.getBoolean("BatteryShowPercent", false);
    
        BIconTransitColors = XPrefs.Xprefs.getBoolean("BIconTransitColors", false);
        BIconColorful = XPrefs.Xprefs.getBoolean("BIconColorful", false);
        BIconindicateFastCharging = XPrefs.Xprefs.getBoolean("BIconindicateFastCharging", false);
        batteryIconFastChargingColor = XPrefs.Xprefs.getInt("batteryIconFastChargingColor", Color.BLUE);
        batteryChargingColor = XPrefs.Xprefs.getInt("batteryChargingColor", Color.GREEN);
        BIconindicateCharging = XPrefs.Xprefs.getBoolean("BIconindicateCharging", false);
    
        String jsonString = XPrefs.Xprefs.getString("BIconbatteryWarningRange", "");
        if(jsonString.length() > 0)
        {
            batteryLevels = new float[]{
                    RangeBarHelper.getLowValueFromJsonString(jsonString),
                    RangeBarHelper.getHighValueFromJsonString(jsonString)};
        }
    
        batteryColors = new int[]{
                XPrefs.Xprefs.getInt("BIconbatteryCriticalColor", Color.RED),
                XPrefs.Xprefs.getInt("BIconbatteryWarningColor", Color.YELLOW)};
    
    
        BatteryDrawable.setStaticColor(batteryLevels, batteryColors, BIconindicateCharging, batteryChargingColor, BIconindicateFastCharging, batteryIconFastChargingColor, BIconTransitColors, BIconColorful);
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

        Class<?> BatteryMeterViewClass = XposedHelpers.findClassIfExists("com.android.systemui.battery.BatteryMeterView", lpparam.classLoader);

        if(BatteryMeterViewClass == null)
        {
            BatteryMeterViewClass = XposedHelpers.findClass("com.android.systemui.BatteryMeterView", lpparam.classLoader);
        }

        //Android 12 June beta
        Method updatePercentTextMethod = XposedHelpers.findMethodExactIfExists(BatteryMeterViewClass, "updatePercentText");
        if(updatePercentTextMethod != null) {
            XposedBridge.hookMethod(updatePercentTextMethod, new batteryUpdater());
        }
        else {
            XposedBridge.hookAllMethods(BatteryMeterViewClass, "onBatteryLevelChanged", new batteryUpdater());
        }
        
        XposedHelpers.findAndHookConstructor(BatteryMeterViewClass,
                    Context.class, AttributeSet.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            ImageView mBatteryIconView = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mBatteryIconView");
                            
                            if(hideBattery)
                            {
                                ViewGroup batteryParent = (ViewGroup) mBatteryIconView.getParent();
                                batteryParent.removeView(mBatteryIconView);
                                return;
                            }

                            if (!circleBatteryEnabled) return;
                            
                            BatteryDrawable mBatteryDrawable = null;
                            switch (BatteryStyle)
                            {
                                case 1:
                                case 2:
                                    mBatteryDrawable = new CircleBatteryDrawable((Context) param.args[0], frameColor);
                                    break;
                                case 3:
                                    mBatteryDrawable = new CircleFilledBatteryDrawable((Context) param.args[0], frameColor);
                                    break;
                            }
                            mBatteryDrawable.setShowPercent(ShowPercent);
                            mBatteryDrawable.setMeterStyle(BatteryStyle);
                            XposedHelpers.setAdditionalInstanceField(param.thisObject, "mBatteryDrawable", mBatteryDrawable);

                            mBatteryIconView.setImageDrawable(mBatteryDrawable);
                            XposedHelpers.setObjectField(param.thisObject, "mBatteryIconView", mBatteryIconView);
                        }
                    });


        XposedHelpers.findAndHookMethod(BatteryMeterViewClass,
                "onBatteryUnknownStateChanged", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(!circleBatteryEnabled) return;

                        ImageView mBatteryIconView = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mBatteryIconView");
                        BatteryDrawable mBatteryDrawable = (BatteryDrawable) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mBatteryDrawable");

                        mBatteryDrawable.setMeterStyle(BatteryStyle);

                        mBatteryIconView.setImageDrawable(mBatteryDrawable);
                    }
                });

        Method scaleBatteryMeterViewsMethod = XposedHelpers.findMethodExactIfExists(BatteryMeterViewClass, "scaleBatteryMeterViews");

        if(scaleBatteryMeterViewsMethod != null)
        {
            XposedBridge.hookMethod(scaleBatteryMeterViewsMethod, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            scale(param);
                        }
                    });

            XposedHelpers.findAndHookMethod(BatteryMeterViewClass,
                    "onPowerSaveChanged", boolean.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if(!circleBatteryEnabled) return;

                            BatteryDrawable mBatteryDrawable = (BatteryDrawable) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mBatteryDrawable");
                            mBatteryDrawable.setPowerSaveEnabled((boolean) param.args[0]);
                        }
                    });
        }
        else
        {
            scaleWithPercent = true;
        }

        XposedHelpers.findAndHookMethod(BatteryMeterViewClass,
                "updateColors", int.class, int.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(!circleBatteryEnabled) return;

                        BatteryDrawable mBatteryDrawable = (BatteryDrawable) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mBatteryDrawable");
                        if(mBatteryDrawable == null) return;
                        mBatteryDrawable.setColors((int) param.args[0], (int) param.args[1], (int) param.args[2]);
                    }
                });
    }

    static class batteryUpdater extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            boolean mCharging = (boolean) XposedHelpers.getObjectField(param.thisObject, "mCharging");
            int mLevel = (int) XposedHelpers.getObjectField(param.thisObject, "mLevel");

            //Feeding battery bar
            BatteryBarView.setStaticLevel(mLevel, mCharging);

            if (!circleBatteryEnabled) return;

            BatteryDrawable mBatteryDrawable = (BatteryDrawable) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mBatteryDrawable");
            if (mBatteryDrawable == null) return;
            
            mBatteryDrawable.setFastCharging(isFastCharging);
            mBatteryDrawable.setCharging(mCharging);
            mBatteryDrawable.setBatteryLevel(mLevel);
            

            if(scaleWithPercent) scale(param);
        }
    }

    public static void scale(XC_MethodHook.MethodHookParam param)
    {
        ImageView mBatteryIconView = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mBatteryIconView");
        
        param.setResult(null);
        if (mBatteryIconView == null) {
            return;
        }

        Context context = (Context) XposedHelpers.callMethod(param.thisObject, "getContext");
        Resources res = context.getResources();

        TypedValue typedValue = new TypedValue();

        res.getValue(res.getIdentifier("status_bar_icon_scale_factor", "dimen", context.getPackageName()), typedValue, true);
        float iconScaleFactor = typedValue.getFloat() * (scaleFactor/100f);

        int batteryHeight = res.getDimensionPixelSize(res.getIdentifier("status_bar_battery_icon_height", "dimen", context.getPackageName()));
        int batteryWidth = res.getDimensionPixelSize(res.getIdentifier("status_bar_battery_icon_height", "dimen", context.getPackageName()));
        int marginBottom = res.getDimensionPixelSize(res.getIdentifier("battery_margin_bottom", "dimen", context.getPackageName()));

        LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));

        scaledLayoutParams.setMargins(0, 0, 0, marginBottom);

        mBatteryIconView.setLayoutParams(scaledLayoutParams);
    }

    @Override
    public String getListenPack() {
        return listenPackage;
    }

}
