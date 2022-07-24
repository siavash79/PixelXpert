// Starting Android 12.1, we hook to the callback used in battery view controller


package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.Utils.Helpers.tryHookAllConstructors;
import static sh.siava.AOSPMods.Utils.Helpers.tryHookAllMethods;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.Utils.batteryStyles.BatteryBarView;
import sh.siava.AOSPMods.Utils.batteryStyles.BatteryDrawable;
import sh.siava.AOSPMods.Utils.batteryStyles.CircleBatteryDrawable;
import sh.siava.AOSPMods.Utils.batteryStyles.CircleFilledBatteryDrawable;
import sh.siava.AOSPMods.Utils.batteryStyles.hiddenBatteryDrawable;
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
    private static boolean isFastCharging = false;
    private static int BatteryIconOpacity = 100;
    private static final ArrayList<Object> batteryViews = new ArrayList<>();

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
        if(Xprefs == null) return;
        String BatteryStyleStr = Xprefs.getString("BatteryStyle", "0");
        scaleFactor = Xprefs.getInt("BatteryIconScaleFactor", 50)*2;
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

        ShowPercent = Xprefs.getBoolean("BatteryShowPercent", false);

        BatteryIconOpacity = Xprefs.getInt("BIconOpacity", 100);
        boolean BIconTransitColors = Xprefs.getBoolean("BIconTransitColors", false);
        boolean BIconColorful = Xprefs.getBoolean("BIconColorful", false);
        boolean BIconIndicateFastCharging = Xprefs.getBoolean("BIconindicateFastCharging", false);
        int batteryIconFastChargingColor = Xprefs.getInt("batteryIconFastChargingColor", Color.BLUE);
        int batteryChargingColor = Xprefs.getInt("batteryIconChargingColor", Color.GREEN);
        boolean BIconIndicateCharging = Xprefs.getBoolean("BIconindicateCharging", false);

        List<Float> batteryLevels = RangeSliderPreference.getValues(Xprefs, "BIconbatteryWarningRange", 0);

        int[] batteryColors = new int[]{
                Xprefs.getInt("BIconbatteryCriticalColor", Color.RED),
                Xprefs.getInt("BIconbatteryWarningColor", Color.YELLOW)};

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

        Class<?> BatteryMeterViewClass = findClassIfExists("com.android.systemui.battery.BatteryMeterView", lpparam.classLoader);
        if(BatteryMeterViewClass == null)
        {
            // This Android 12.0 - we hook to old methods
            BatteryMeterViewClass = findClassIfExists("com.android.systemui.BatteryMeterView", lpparam.classLoader);

            tryHookAllMethods(BatteryMeterViewClass, "onBatteryLevelChanged", new XC_MethodHook() {
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

            tryHookAllMethods(BatteryMeterViewClass, "scaleBatteryMeterViews", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    scale(param.thisObject);
                    param.setResult(null);
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

        Class<?> BatteryMeterViewControllerClass = findClassIfExists ("com.android.systemui.battery.BatteryMeterViewController", lpparam.classLoader);

        tryHookAllConstructors(BatteryMeterViewControllerClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param0) throws Throwable {
                Object mView = getObjectField(param0.thisObject, "mView");

                Class<?> mBatteryStateChangeCallbackClass = getObjectField(param0.thisObject, "mBatteryStateChangeCallback").getClass();
                tryHookAllMethods(mBatteryStateChangeCallbackClass, "onBatteryLevelChanged", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        boolean mCharging = (boolean) param.args[2];
                        int mLevel = (int) param.args[0];

                        //Feeding battery bar
                        BatteryBarView.setStaticLevel(mLevel, mCharging);

                        if (!customBatteryEnabled) return;

                        BatteryDrawable mBatteryDrawable = (BatteryDrawable) getAdditionalInstanceField(mView, "mBatteryDrawable");
                        if (mBatteryDrawable == null) return;

                        mBatteryDrawable.setCharging(mCharging);
                        mBatteryDrawable.setBatteryLevel(mLevel);

                        scale(mView);
                    }
                });

                tryHookAllMethods(mBatteryStateChangeCallbackClass, "onPowerSaveChanged", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(!customBatteryEnabled) return;

                        BatteryDrawable mBatteryDrawable = (BatteryDrawable) getAdditionalInstanceField(mView, "mBatteryDrawable");
                        mBatteryDrawable.setPowerSaveEnabled((boolean) param.args[0]);
                    }
                });

                tryHookAllMethods(mBatteryStateChangeCallbackClass, "onBatteryUnknownStateChanged", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(!customBatteryEnabled) return;

                        ImageView mBatteryIconView = (ImageView) getObjectField(mView, "mBatteryIconView");
                        BatteryDrawable mBatteryDrawable = (BatteryDrawable) getAdditionalInstanceField(mView, "mBatteryDrawable");

                        mBatteryDrawable.setMeterStyle(BatteryStyle);

                        mBatteryIconView.setImageDrawable(mBatteryDrawable);
                    }
                });
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

    public static void scale(Object thisObject)
    {
        ImageView mBatteryIconView = (ImageView) getObjectField(thisObject, "mBatteryIconView");
        scale(mBatteryIconView);
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
