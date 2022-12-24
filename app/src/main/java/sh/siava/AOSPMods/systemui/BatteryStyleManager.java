// Starting Android 12.1, we hook to the callback used in battery view controller


package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.annotation.SuppressLint;
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
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.utils.batteryStyles.BatteryBarView;
import sh.siava.AOSPMods.utils.batteryStyles.BatteryDrawable;
import sh.siava.AOSPMods.utils.batteryStyles.CircleBatteryDrawable;
import sh.siava.AOSPMods.utils.batteryStyles.CircleFilledBatteryDrawable;
import sh.siava.AOSPMods.utils.batteryStyles.HiddenBatteryDrawable;
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
	private Object BatteryController;

	public BatteryStyleManager(Context context) {
		super(context);
	}

	public static void setIsFastCharging(boolean isFastCharging) {
		BatteryStyleManager.isFastCharging = isFastCharging;
		for (Object view : batteryViews) {
			try {
				BatteryDrawable drawable = (BatteryDrawable) getAdditionalInstanceField(view, "mBatteryDrawable");
				drawable.setFastCharging(isFastCharging);
			} catch (Throwable ignored) {
			} //it's probably default battery. no action needed
		}
	}

	public void updatePrefs(String... Key) {
		if (Xprefs == null) return;
		String BatteryStyleStr = Xprefs.getString("BatteryStyle", "0");
		scaleFactor = Xprefs.getInt("BatteryIconScaleFactor", 50) * 2;
		int batteryStyle = Integer.parseInt(BatteryStyleStr);
		customBatteryEnabled = batteryStyle != 0;
		if (batteryStyle == 99) {
			scaleFactor = 0;
		}

		if (BatteryStyle != batteryStyle) {
			BatteryStyle = batteryStyle;
			for (Object view : batteryViews) //destroy old drawables and make new ones :D
			{
				ImageView mBatteryIconView = (ImageView) getObjectField(view, "mBatteryIconView");
				boolean mCharging = (boolean) getObjectField(view, "mCharging");
				int mLevel = (int) getObjectField(view, "mLevel");

				if (customBatteryEnabled) {
					BatteryDrawable newDrawable = getNewDrawable(mContext);
					mBatteryIconView.setImageDrawable(newDrawable);
					setAdditionalInstanceField(view, "mBatteryDrawable", newDrawable);
					newDrawable.setBatteryLevel(mLevel);
					newDrawable.setCharging(mCharging);
					BatteryBarView.setStaticLevel(mLevel, mCharging);
				} else {
					try {
						mBatteryIconView.setImageDrawable(
								(Drawable) getObjectField(
										view,
										"mDrawable"));
					} catch (Throwable ignored) { //PE+ !
						mBatteryIconView.setImageDrawable(
								(Drawable) getObjectField(
										view,
										"mThemedDrawable"));
					}
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
				drawable.setAlpha(Math.round(BatteryIconOpacity * 2.55f));
				drawable.refresh();
			} catch (Throwable ignored) {
			} //it's probably the default battery. no action needed
		}
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
		if (!lpparam.packageName.equals(listenPackage)) return;

		findAndHookConstructor("com.android.settingslib.graph.ThemedBatteryDrawable", lpparam.classLoader, Context.class, int.class, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				frameColor = (int) param.args[1];
			}
		});

		Class<?> BatteryControllerImplClass = findClass("com.android.systemui.statusbar.policy.BatteryControllerImpl", lpparam.classLoader);
//		Class<?> BatteryMeterViewControllerClass = findClassIfExists("com.android.systemui.battery.BatteryMeterViewController", lpparam.classLoader);
		Class<?> BatteryMeterViewClass = findClassIfExists("com.android.systemui.battery.BatteryMeterView", lpparam.classLoader);

		hookAllConstructors(BatteryControllerImplClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				BatteryController = param.thisObject;
			}
		});

		hookAllMethods(BatteryControllerImplClass, "fireBatteryUnknownStateChanged", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (!customBatteryEnabled) return;

				for(Object view : batteryViews)
				{
					BatteryDrawable mBatteryDrawable = (BatteryDrawable) getAdditionalInstanceField(view, "mBatteryDrawable");
					mBatteryDrawable.setMeterStyle(BatteryStyle);
					callMethod(view, "setImageDrawable", mBatteryDrawable);
				}
			}
		});

		XC_MethodHook batteryDataRefreshHook = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				int level = getIntField(param.thisObject, "mLevel");
				boolean charging = getBooleanField(param.thisObject, "mPluggedIn")
						|| getBooleanField(param.thisObject, "mCharging")
						|| getBooleanField(param.thisObject, "mWirelessCharging");

				boolean powerSave = getBooleanField(param.thisObject, "mPowerSave");

				//Feeding battery bar
				BatteryBarView.setStaticLevel(level, charging);

				if (!customBatteryEnabled) return;
				for(Object view : batteryViews)
				{
					BatteryDrawable drawable = (BatteryDrawable) getAdditionalInstanceField(view, "mBatteryDrawable");
					drawable.setBatteryLevel(level);
					drawable.setCharging(charging);
					drawable.setPowerSaveEnabled(powerSave);
					scale(view);
				}
			}
		};

		hookAllMethods(BatteryControllerImplClass, "fireBatteryLevelChanged", batteryDataRefreshHook);
		hookAllMethods(BatteryControllerImplClass, "firePowerSaveChanged", batteryDataRefreshHook);

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
				((View) param.thisObject).addOnAttachStateChangeListener(listener);

				if (!customBatteryEnabled) return;

				ImageView mBatteryIconView = (ImageView) getObjectField(param.thisObject, "mBatteryIconView");

				BatteryDrawable mBatteryDrawable = getNewDrawable(mContext);
				setAdditionalInstanceField(param.thisObject, "mBatteryDrawable", mBatteryDrawable);

				mBatteryIconView.setImageDrawable(mBatteryDrawable);
				setObjectField(param.thisObject, "mBatteryIconView", mBatteryIconView);

				callMethod(BatteryController, "fireBatteryLevelChanged"); //force refresh icons during systemui start
			}
		});

		findAndHookMethod(BatteryMeterViewClass,
				"updateColors", int.class, int.class, int.class, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						if (!customBatteryEnabled) return;

						BatteryDrawable mBatteryDrawable = (BatteryDrawable) getAdditionalInstanceField(param.thisObject, "mBatteryDrawable");
						if (mBatteryDrawable == null) return;
						mBatteryDrawable.setColors((int) param.args[0], (int) param.args[1], (int) param.args[2]);
					}
				});
	}

	private BatteryDrawable getNewDrawable(Context context) {
		BatteryDrawable mBatteryDrawable = null;
		switch (BatteryStyle) {
			case 1:
			case 2:
				mBatteryDrawable = new CircleBatteryDrawable(context, frameColor);
				break;
			case 3:
				mBatteryDrawable = new CircleFilledBatteryDrawable(context, frameColor);
				break;
			case 99:
				mBatteryDrawable = new HiddenBatteryDrawable();
		}
		if (mBatteryDrawable != null) {
			mBatteryDrawable.setShowPercent(ShowPercent);
			mBatteryDrawable.setMeterStyle(BatteryStyle);
			mBatteryDrawable.setFastCharging(isFastCharging);
			mBatteryDrawable.setAlpha(Math.round(BatteryIconOpacity * 2.55f));
		}
		return mBatteryDrawable;
	}

	public static void scale(Object thisObject) {
		ImageView mBatteryIconView = (ImageView) getObjectField(thisObject, "mBatteryIconView");
		scale(mBatteryIconView);
	}

	@SuppressLint("DiscouragedApi")
	public static void scale(ImageView mBatteryIconView) {
		if (mBatteryIconView == null) {
			return;
		}

		Context context = mBatteryIconView.getContext();
		Resources res = context.getResources();

		TypedValue typedValue = new TypedValue();

		res.getValue(res.getIdentifier("status_bar_icon_scale_factor", "dimen", context.getPackageName()), typedValue, true);
		float iconScaleFactor = typedValue.getFloat() * (scaleFactor / 100f);

		int batteryHeight = res.getDimensionPixelSize(res.getIdentifier("status_bar_battery_icon_height", "dimen", context.getPackageName()));
		int batteryWidth = res.getDimensionPixelSize(res.getIdentifier((customBatteryEnabled) ? "status_bar_battery_icon_height" : "status_bar_battery_icon_width", "dimen", context.getPackageName()));

		ViewGroup.LayoutParams scaledLayoutParams = mBatteryIconView.getLayoutParams();
		scaledLayoutParams.height = (int) (batteryHeight * iconScaleFactor);
		scaledLayoutParams.width = (int) (batteryWidth * iconScaleFactor);

		mBatteryIconView.setLayoutParams(scaledLayoutParams);
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}
}
