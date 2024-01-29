package sh.siava.pixelxpert.modpacks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;
import static sh.siava.pixelxpert.modpacks.utils.toolkit.ObjectTools.isColorDark;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.pixelxpert.modpacks.utils.batteryStyles.BatteryDrawable;
import sh.siava.pixelxpert.modpacks.utils.batteryStyles.CircleBatteryDrawable;
import sh.siava.pixelxpert.modpacks.utils.batteryStyles.CircleFilledBatteryDrawable;
import sh.siava.pixelxpert.modpacks.utils.batteryStyles.HiddenBatteryDrawable;

@SuppressWarnings("RedundantThrows")
public class BatteryStyleManager extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	public static boolean customBatteryEnabled = false;
	private int frameColor;
	public static int mBatteryStyle = 1;
	public static boolean ShowPercent = false;
	public static int scaleFactor = 100;
	private static int BatteryIconOpacity = 100;
	private static boolean BatteryChargingAnimationEnabled = true;
	private final ArrayList<Object> batteryViews = new ArrayList<>();
	private static boolean mShouldScale = false;
	private static boolean BatteryPercentIndicateCharging = false;

	public BatteryStyleManager(Context context) {
		super(context);
	}

	public void updatePrefs(String... Key) {
		if (Xprefs == null) return;

		String BatteryStyleStr = Xprefs.getString("BatteryStyle", "0");
		scaleFactor = Xprefs.getSliderInt( "BatteryIconScaleFactor", 50) * 2;
		int batteryStyle = Integer.parseInt(BatteryStyleStr);

		customBatteryEnabled = batteryStyle != 0;
		mShouldScale = mShouldScale || customBatteryEnabled || scaleFactor != 100;
		BatteryPercentIndicateCharging = Xprefs.getBoolean("BatteryPercentIndicateCharging", false);

		if (batteryStyle == 99) {
			scaleFactor = 0;
		}

		if (mBatteryStyle != batteryStyle) {
			mBatteryStyle = batteryStyle;
			for (Object view : batteryViews) //destroy old drawables and make new ones :D
			{
				ImageView mBatteryIconView = (ImageView) getObjectField(view, "mBatteryIconView");

				if (customBatteryEnabled) {
					BatteryDrawable newDrawable = getNewDrawable(mContext);
					mBatteryIconView.setImageDrawable(newDrawable);
					setAdditionalInstanceField(view, "mBatteryDrawable", newDrawable);
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
		BatteryChargingAnimationEnabled = Xprefs.getBoolean("BatteryChargingAnimationEnabled", true);

		BatteryIconOpacity = Xprefs.getSliderInt( "BIconOpacity", 100);
		boolean BIconTransitColors = Xprefs.getBoolean("BIconTransitColors", false);
		boolean BIconColorful = Xprefs.getBoolean("BIconColorful", false);
		boolean BIconIndicateFastCharging = Xprefs.getBoolean("BIconindicateFastCharging", false);
		int batteryIconFastChargingColor = Xprefs.getInt("batteryIconFastChargingColor", Color.BLUE);
		int batteryChargingColor = Xprefs.getInt("batteryIconChargingColor", Color.GREEN);
		boolean BIconIndicateCharging = Xprefs.getBoolean("BIconindicateCharging", false);

		List<Float> batteryLevels = Xprefs.getSliderValues("BIconbatteryWarningRange", 0);

		int[] batteryColors = new int[]{
				Xprefs.getInt("BIconbatteryCriticalColor", Color.RED),
				Xprefs.getInt("BIconbatteryWarningColor", Color.YELLOW)};

		BatteryDrawable.setStaticColor(batteryLevels, batteryColors, BIconIndicateCharging, batteryChargingColor, BIconIndicateFastCharging, batteryIconFastChargingColor, BIconTransitColors, BIconColorful);

		refreshAllBatteryIcons(true);
	}

	private void refreshAllBatteryIcons(boolean forcePercentageColor) {
		for (Object view : batteryViews) {
			updateBatteryViewValues((View) view, forcePercentageColor);
		}
	}

	private static void updateBatteryViewValues(View view, boolean forcePercentageColor)
	{
		setPercentViewColor(view, forcePercentageColor);

		ImageView mBatteryIconView = (ImageView) getObjectField(view, "mBatteryIconView");
		scale(mBatteryIconView);
		try {
			BatteryDrawable drawable = (BatteryDrawable) getAdditionalInstanceField(view, "mBatteryDrawable");
			drawable.setShowPercent(ShowPercent);
			drawable.setAlpha(Math.round(BatteryIconOpacity * 2.55f));
			drawable.setChargingAnimationEnabled(BatteryChargingAnimationEnabled);
			drawable.invalidateSelf();
		} catch (Throwable ignored) {} //it's probably the default battery. no action needed
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
		BatteryDataProvider.registerInfoCallback(() -> refreshAllBatteryIcons(false));

		findAndHookConstructor("com.android.settingslib.graph.ThemedBatteryDrawable", lpparam.classLoader, Context.class, int.class, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				frameColor = (int) param.args[1];
			}
		});

		Class<?> BatteryMeterViewClass = findClassIfExists("com.android.systemui.battery.BatteryMeterView", lpparam.classLoader);

		View.OnAttachStateChangeListener listener = new View.OnAttachStateChangeListener() {
			@Override
			public void onViewAttachedToWindow(View view) {
				batteryViews.add(view);
				updateBatteryViewValues(view, true);
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
			}
		});

		hookAllMethods(BatteryMeterViewClass, "updateColors", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						setPercentViewColor(param.thisObject);
						if(customBatteryEnabled)
						{
							BatteryDrawable mBatteryDrawable = (BatteryDrawable) getAdditionalInstanceField(param.thisObject, "mBatteryDrawable");
							if (mBatteryDrawable == null) return;
							mBatteryDrawable.setColors((int) param.args[0], (int) param.args[1], (int) param.args[2]);
						}
					}
				});

		hookAllMethods(BatteryMeterViewClass, "updatePercentText", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				setPercentViewColor(param.thisObject);
			}
		});
	}

	private static void setPercentViewColor(Object meterView)
	{
		setPercentViewColor(meterView, false);
	}
	private static void setPercentViewColor(Object meterView, boolean force) {
		if(BatteryPercentIndicateCharging || force) {
			TextView mBatteryPercentView = (TextView) getObjectField(meterView, "mBatteryPercentView");
			if (mBatteryPercentView != null) {
				int mTextColor = (int) getObjectField(meterView, "mTextColor");

				int color = BatteryPercentIndicateCharging && BatteryDataProvider.isCharging()
					? isColorDark(mTextColor)
						? 0xFF048800 //dark green
						: Color.GREEN
					: mTextColor;

				mBatteryPercentView.post(() -> mBatteryPercentView.setTextColor(color));
			}
		}
	}

	private BatteryDrawable getNewDrawable(Context context) {
		BatteryDrawable batteryDrawable = null;
		switch (mBatteryStyle) {
			case 1:
			case 2:
				batteryDrawable = new CircleBatteryDrawable(context, frameColor);
				break;
			case 3:
				batteryDrawable = new CircleFilledBatteryDrawable(context, frameColor);
				break;
			case 99:
				batteryDrawable = new HiddenBatteryDrawable();
		}
		if (batteryDrawable != null) {
			batteryDrawable.setShowPercent(ShowPercent);
			batteryDrawable.setMeterStyle(mBatteryStyle);
			batteryDrawable.setAlpha(Math.round(BatteryIconOpacity * 2.55f));

			batteryDrawable.invalidateSelf();
		}
		return batteryDrawable;
	}

	@SuppressLint("DiscouragedApi")
	public static void scale(ImageView mBatteryIconView) {
		if (mBatteryIconView == null || !mShouldScale) {
			return;
		}

		Context context = mBatteryIconView.getContext();
		Resources res = context.getResources();

		TypedValue typedValue = new TypedValue();

		res.getValue(res.getIdentifier("status_bar_icon_scale_factor", "dimen", context.getPackageName()), typedValue, true);
		float iconScaleFactor = typedValue.getFloat() * (scaleFactor / 100f);

		int height = Math.round(res.getDimensionPixelSize(res.getIdentifier("status_bar_battery_icon_height", "dimen", context.getPackageName())) * iconScaleFactor);
		int width = Math.round(res.getDimensionPixelSize(res.getIdentifier((customBatteryEnabled) ? "status_bar_battery_icon_height" : "status_bar_battery_icon_width", "dimen", context.getPackageName())) * iconScaleFactor);

		mBatteryIconView.post(() -> {
			ViewGroup.LayoutParams scaledLayoutParams = mBatteryIconView.getLayoutParams();
			scaledLayoutParams.height = height;
			scaledLayoutParams.width = width;

			mBatteryIconView.setLayoutParams(scaledLayoutParams);
		});
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}
}
