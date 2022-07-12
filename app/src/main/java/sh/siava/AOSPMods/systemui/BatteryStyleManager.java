package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedBridge.hookMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.findMethodExactIfExists;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.Utils.batteryStyles.BatteryBarView;
import sh.siava.AOSPMods.Utils.batteryStyles.BatteryDrawable;
import sh.siava.AOSPMods.Utils.batteryStyles.CircleBatteryDrawable;
import sh.siava.AOSPMods.Utils.batteryStyles.CircleFilledBatteryDrawable;
import sh.siava.AOSPMods.Utils.batteryStyles.hiddenBatteryDrawable;
import sh.siava.AOSPMods.XPrefs;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.rangesliderpreference.RangeSliderPreference;

@SuppressWarnings("RedundantThrows")
public class BatteryStyleManager extends XposedModPack {
    public static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;
    
    public static boolean customBatteryEnabled = false;
    
    private int frameColor;
    public static int BatteryStyle = 1;
    public static boolean ShowPercent = false;
    public static int scaleFactor = 100;
    public static boolean scaleWithPercent = false;
    private static boolean isFastCharging = false;
    private static int BatteryIconOpacity = 100;
    private static List<Float> batteryLevels = Arrays.asList(20f, 40f);
    private static final ArrayList<Object> batteryViews = new ArrayList<>();
    private Object mStatusbarView = null;
    private Object mKeyguardView = null;

    public BatteryStyleManager(Context context) { super(context); }
    
    public static void setIsFastCharging(boolean isFastCharging)
    {
        BatteryStyleManager.isFastCharging = isFastCharging;
        for(Object view : batteryViews)
        {
            try {
                BatteryDrawable drawable = (BatteryDrawable) getAdditionalInstanceField(view, "mBatteryDrawable");
                drawable.setFastCharging(isFastCharging);
            }catch(Throwable ignored){} //it's probably default battery. no action needed
        }
    }
    
    public void updatePrefs(String...Key)
    {
        if(XPrefs.Xprefs == null) return;
        String BatteryStyleStr = XPrefs.Xprefs.getString("BatteryStyle", "0");
        scaleFactor = XPrefs.Xprefs.getInt("BatteryIconScaleFactor", 50)*2;
        int batteryStyle = Integer.parseInt(BatteryStyleStr);
        customBatteryEnabled = batteryStyle != 0;
        if(batteryStyle == 99)
        {
            scaleFactor = 0;
        }
    
        if(BatteryStyle != batteryStyle)
        {
            BatteryStyle = batteryStyle;
            for(Object view : batteryViews) //destroy old drawables and make new ones :D
            {
                ImageView mBatteryIconView = (ImageView) getObjectField(view, "mBatteryIconView");
                boolean mCharging = (boolean) getObjectField(view, "mCharging");
                int mLevel = (int) getObjectField(view, "mLevel");

                if(customBatteryEnabled) {
                    BatteryDrawable newDrawable = getNewDrawable(mContext);
                    mBatteryIconView.setImageDrawable(newDrawable);
                    setAdditionalInstanceField(view,"mBatteryDrawable", newDrawable);
                    newDrawable.setBatteryLevel(mLevel);
                    newDrawable.setCharging(mCharging);
                }
                else
                {
                    mBatteryIconView.setImageDrawable(
                            (Drawable) getObjectField(
                                    view,
                                    "mDrawable"));
                }
            }
        }
        
        ShowPercent = XPrefs.Xprefs.getBoolean("BatteryShowPercent", false);
        
        BatteryIconOpacity = XPrefs.Xprefs.getInt("BIconOpacity", 100);
        boolean BIconTransitColors = XPrefs.Xprefs.getBoolean("BIconTransitColors", false);
        boolean BIconColorful = XPrefs.Xprefs.getBoolean("BIconColorful", false);
        boolean BIconIndicateFastCharging = XPrefs.Xprefs.getBoolean("BIconindicateFastCharging", false);
        int batteryIconFastChargingColor = XPrefs.Xprefs.getInt("batteryIconFastChargingColor", Color.BLUE);
        int batteryChargingColor = XPrefs.Xprefs.getInt("batteryIconChargingColor", Color.GREEN);
        boolean BIconIndicateCharging = XPrefs.Xprefs.getBoolean("BIconindicateCharging", false);

        batteryLevels = RangeSliderPreference.getValues(XPrefs.Xprefs, "BIconbatteryWarningRange", 0);

        int[] batteryColors = new int[]{
                XPrefs.Xprefs.getInt("BIconbatteryCriticalColor", Color.RED),
                XPrefs.Xprefs.getInt("BIconbatteryWarningColor", Color.YELLOW)};
    
        BatteryDrawable.setStaticColor(batteryLevels, batteryColors, BIconIndicateCharging, batteryChargingColor, BIconIndicateFastCharging, batteryIconFastChargingColor, BIconTransitColors, BIconColorful);
        
        refreshBatteryIcons();
    }
    
    private static void refreshBatteryIcons() {
        for (Object view : batteryViews) {
            ImageView mBatteryIconView = (ImageView) getObjectField(view, "mBatteryIconView");
            scale(mBatteryIconView);
            try {
                BatteryDrawable drawable = (BatteryDrawable) getAdditionalInstanceField(view, "mBatteryDrawable");
                drawable.setShowPercent(ShowPercent);
                drawable.setAlpha(Math.round(BatteryIconOpacity*2.55f));
                drawable.refresh();
            }catch(Throwable ignored){} //it's probably the default battery. no action needed
        }
    }
    
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if(!lpparam.packageName.equals(listenPackage)) return;

        
        findAndHookConstructor("com.android.settingslib.graph.ThemedBatteryDrawable", lpparam.classLoader, Context.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                frameColor = (int) param.args[1];
            }
        });

        Class<?> PhoneStatusBarViewClass = findClass("com.android.systemui.statusbar.phone.PhoneStatusBarView", lpparam.classLoader);
        Class<?> BatteryMeterViewClass = findClassIfExists("com.android.systemui.battery.BatteryMeterView", lpparam.classLoader);

        if(BatteryMeterViewClass == null)
        {
            BatteryMeterViewClass = findClass("com.android.systemui.BatteryMeterView", lpparam.classLoader);
        }

        hookAllConstructors(PhoneStatusBarViewClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mStatusbarView = param.thisObject;
            }
        });

        hookAllMethods(BatteryMeterViewClass, "onBatteryLevelChanged", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                boolean mCharging = (boolean) getObjectField(param.thisObject, "mCharging");
                int mLevel = (int) getObjectField(param.thisObject, "mLevel");

                //Feeding battery bar
                BatteryBarView.setStaticLevel(mLevel, mCharging);

                if (!customBatteryEnabled) return;

                BatteryDrawable mBatteryDrawable = (BatteryDrawable) getAdditionalInstanceField(param.thisObject, "mBatteryDrawable");
                if (mBatteryDrawable == null) return;

                mBatteryDrawable.setCharging(mCharging);
                mBatteryDrawable.setBatteryLevel(mLevel);

                if(scaleWithPercent) scale(param);
            }
        });

        View.OnAttachStateChangeListener listener = new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                batteryViews.add(v);
            }
    
            @Override
            public void onViewDetachedFromWindow(View v) {
                batteryViews.remove(v);
            }
        };

        hookAllConstructors(BatteryMeterViewClass, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            ((View)param.thisObject).addOnAttachStateChangeListener(listener);

                            if (!customBatteryEnabled) return;

                            ImageView mBatteryIconView = (ImageView) getObjectField(param.thisObject, "mBatteryIconView");

                            BatteryDrawable mBatteryDrawable = getNewDrawable(mContext);
                            setAdditionalInstanceField(param.thisObject, "mBatteryDrawable", mBatteryDrawable);

                            mBatteryIconView.setImageDrawable(mBatteryDrawable);
                            setObjectField(param.thisObject, "mBatteryIconView", mBatteryIconView);
                        }
                    });


        findAndHookMethod(BatteryMeterViewClass,
                "onBatteryUnknownStateChanged", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(!customBatteryEnabled) return;

                        ImageView mBatteryIconView = (ImageView) getObjectField(param.thisObject, "mBatteryIconView");
                        BatteryDrawable mBatteryDrawable = (BatteryDrawable) getAdditionalInstanceField(param.thisObject, "mBatteryDrawable");

                        mBatteryDrawable.setMeterStyle(BatteryStyle);

                        mBatteryIconView.setImageDrawable(mBatteryDrawable);
                    }
                });

        Method scaleBatteryMeterViewsMethod = findMethodExactIfExists(BatteryMeterViewClass, "scaleBatteryMeterViews");

        if(scaleBatteryMeterViewsMethod != null)
        {
            hookMethod(scaleBatteryMeterViewsMethod, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            scale(param);
                        }
                    });

            findAndHookMethod(BatteryMeterViewClass,
                    "onPowerSaveChanged", boolean.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if(!customBatteryEnabled) return;

                            BatteryDrawable mBatteryDrawable = (BatteryDrawable) getAdditionalInstanceField(param.thisObject, "mBatteryDrawable");
                            mBatteryDrawable.setPowerSaveEnabled((boolean) param.args[0]);
                        }
                    });
        }
        else
        {
            scaleWithPercent = true;
        }

        findAndHookMethod(BatteryMeterViewClass,
                "updateColors", int.class, int.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(!customBatteryEnabled) return;

                        BatteryDrawable mBatteryDrawable = (BatteryDrawable) getAdditionalInstanceField(param.thisObject, "mBatteryDrawable");
                        if(mBatteryDrawable == null) return;
                        mBatteryDrawable.setColors((int) param.args[0], (int) param.args[1], (int) param.args[2]);
                    }
                });
    }


    private BatteryDrawable getNewDrawable(Context context) {
        BatteryDrawable mBatteryDrawable = null;
        switch (BatteryStyle)
        {
            case 1:
            case 2:
                mBatteryDrawable = new CircleBatteryDrawable(context, frameColor);
                break;
            case 3:
                mBatteryDrawable = new CircleFilledBatteryDrawable(context, frameColor);
                break;
            case 99:
                mBatteryDrawable = new hiddenBatteryDrawable();
        }
        if(mBatteryDrawable != null) {
            mBatteryDrawable.setShowPercent(ShowPercent);
            mBatteryDrawable.setMeterStyle(BatteryStyle);
            mBatteryDrawable.setFastCharging(isFastCharging);
            mBatteryDrawable.setAlpha(Math.round(BatteryIconOpacity * 2.55f));
        }
        return mBatteryDrawable;
    }
    
    public static void scale(XC_MethodHook.MethodHookParam param)
    {
        ImageView mBatteryIconView = (ImageView) getObjectField(param.thisObject, "mBatteryIconView");
        scale(mBatteryIconView);
        param.setResult(null);
    }

    public static void scale(ImageView mBatteryIconView)
    {
        if (mBatteryIconView == null) {
            return;
        }

        Context context = mBatteryIconView.getContext();
        Resources res = context.getResources();

        TypedValue typedValue = new TypedValue();

        res.getValue(res.getIdentifier("status_bar_icon_scale_factor", "dimen", context.getPackageName()), typedValue, true);
        float iconScaleFactor = typedValue.getFloat() * (scaleFactor/100f);

        int batteryHeight = res.getDimensionPixelSize(res.getIdentifier("status_bar_battery_icon_height", "dimen", context.getPackageName()));
        int batteryWidth = res.getDimensionPixelSize(res.getIdentifier((customBatteryEnabled) ? "status_bar_battery_icon_height" : "status_bar_battery_icon_width", "dimen", context.getPackageName()));

        ViewGroup.LayoutParams scaledLayoutParams = mBatteryIconView.getLayoutParams();
        scaledLayoutParams.height = (int) (batteryHeight * iconScaleFactor);
        scaledLayoutParams.width = (int) (batteryWidth * iconScaleFactor);

        mBatteryIconView.setLayoutParams(scaledLayoutParams);
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
}
