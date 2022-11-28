package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.utils.StringFormatter;
import sh.siava.rangesliderpreference.RangeSliderPreference;

@SuppressWarnings("RedundantThrows")
public class KeyguardMods extends XposedModPack {
	private static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;

	//region keyguard charging data
	public static final String EXTRA_MAX_CHARGING_CURRENT = "max_charging_current";
	public static final String EXTRA_MAX_CHARGING_VOLTAGE = "max_charging_voltage";
	public static final String EXTRA_TEMPERATURE = "temperature";

	private float max_charging_current = 0;
	private float max_charging_voltage = 0;
	private float temperature = 0;

	private static boolean ShowChargingInfo = false;
	//endregion

	private static boolean customCarrierTextEnabled = false;
	private static String customCarrierText = "";
	private static Object carrierTextController;

	StringFormatter stringFormatter = new StringFormatter();
	private TextView KGMiddleCustomTextView;
	private static String KGMiddleCustomText = "";
	LinearLayout mStatusArea = null;
	private Object KGCS;
	private boolean mDozing = false;

	//region keyguardDimmer
	public static float KeyGuardDimAmount = -1f;
	//endregion

	public KeyguardMods(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		KGMiddleCustomText = Xprefs.getString("KGMiddleCustomText", "");

		customCarrierTextEnabled = Xprefs.getBoolean("carrierTextMod", false);
		customCarrierText = Xprefs.getString("carrierTextValue", "");

		ShowChargingInfo = Xprefs.getBoolean("ShowChargingInfo", false);

		try {
			KeyGuardDimAmount = RangeSliderPreference.getValues(Xprefs, "KeyGuardDimAmount", -1f).get(0) / 100f;
		} catch (Throwable ignored) {
		}

		if (Key.length > 0) {
			switch (Key[0]) {
				case "KGMiddleCustomText":
					setMiddleText();
					break;
				case "carrierTextValue":
				case "carrierTextMod":
					if (customCarrierTextEnabled) {
						setCarrierText();
					} else {
						try {
							callMethod(
									getObjectField(carrierTextController, "mCarrierTextManager"),
									"updateCarrierText");
						} catch (Throwable ignored) {
						} //probably not initiated yet
					}
					break;
			}
		}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

		Class<?> CarrierTextControllerClass = findClass("com.android.keyguard.CarrierTextController", lpparam.classLoader);
		Class<?> KeyguardSliceViewClass = findClass("com.android.keyguard.KeyguardSliceView$Row", lpparam.classLoader);
		Class<?> KeyguardClockSwitchClass = findClass("com.android.keyguard.KeyguardClockSwitch", lpparam.classLoader);
		Class<?> BatteryStatusClass = findClass("com.android.settingslib.fuelgauge.BatteryStatus", lpparam.classLoader);
		Class<?> KeyguardIndicationControllerClass = findClass("com.android.systemui.statusbar.KeyguardIndicationController", lpparam.classLoader);

		//region keyguard battery info
		hookAllMethods(KeyguardIndicationControllerClass, "computePowerIndication", new XC_MethodHook() {
			@SuppressLint("DefaultLocale")
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(ShowChargingInfo) {
					String result = (String) param.getResult();
					param.setResult(String.format("%s\n%.1fW (%.1fV, %.1fA) • %.0fºC", result, max_charging_current * max_charging_voltage, max_charging_voltage, max_charging_current, temperature));
				}
			}
		});

		hookAllConstructors(BatteryStatusClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				Intent batteryInfoIntent = (Intent) param.args[0];

				max_charging_current = batteryInfoIntent.getIntExtra(EXTRA_MAX_CHARGING_CURRENT, 0) / 1000000f;
				max_charging_voltage = batteryInfoIntent.getIntExtra(EXTRA_MAX_CHARGING_VOLTAGE, 0) / 1000000f;
				temperature = batteryInfoIntent.getIntExtra(EXTRA_TEMPERATURE, 0) / 10f;
			}
		});
		//endregion

		//region keyguardDimmer
		Class<?> ScrimControllerClass = findClass("com.android.systemui.statusbar.phone.ScrimController", lpparam.classLoader);
		Class<?> ScrimStateEnum = findClass("com.android.systemui.statusbar.phone.ScrimState", lpparam.classLoader);
		//endregion

        /* might be useful later
        Class<?> AnimatableClockControllerClass = findClass("com.android.keyguard.AnimatableClockController", lpparam.classLoader);
        Class<?> KeyguardClockSwitchControllerClass = findClass("com.android.keyguard.KeyguardClockSwitchController", lpparam.classLoader);
        Class<?> DefaultClockControllerClass = findClass("com.android.keyguard.clock.DefaultClockController", lpparam.classLoader);
        Class<?> AvailableClocksClass = findClass("com.android.keyguard.clock.ClockManager$AvailableClocks", lpparam.classLoader);*/

		//region keyguardDimmer
		hookAllMethods(ScrimControllerClass, "scheduleUpdate", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (KeyGuardDimAmount < 0 || KeyGuardDimAmount > 1) return;

				setObjectField(param.thisObject, "mScrimBehindAlphaKeyguard", KeyGuardDimAmount);
				Object[] constants = ScrimStateEnum.getEnumConstants();
				for (Object constant : constants) {
					setObjectField(constant, "mScrimBehindAlphaKeyguard", KeyGuardDimAmount);
				}
			}
		});
		//endregion

		stringFormatter.registerCallback(() -> {
			setMiddleText();
			setCarrierText();
		});

		Resources res = mContext.getResources();

		hookAllMethods(CarrierTextControllerClass, "onInit", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {

				carrierTextController = param.thisObject;
				Object carrierTextCallback = getObjectField(carrierTextController, "mCarrierTextCallback");
				hookAllMethods(carrierTextCallback.getClass(),
						"updateCarrierInfo", new XC_MethodHook() {
							@Override
							protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
								if (!customCarrierTextEnabled) return; //nothing to do
								setCarrierText();
								param.setResult(null);
							}
						});
			}
		}).size();

		hookAllMethods(KeyguardSliceViewClass, "setDarkAmount", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				boolean isDozing = ((float) param.args[0]) > .5f;
				if (mDozing != isDozing) {
					mDozing = isDozing;
					setMiddleColor();
				}
			}
		});

		hookAllMethods(KeyguardClockSwitchClass, "onFinishInflate", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				try {
					KGCS = param.thisObject;
					KGMiddleCustomTextView = new TextView(mContext);
					KGMiddleCustomTextView.setMaxLines(2);
					KGMiddleCustomTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
					KGMiddleCustomTextView.setLetterSpacing(.03f);

					@SuppressLint("DiscouragedApi") int padding = res.getDimensionPixelSize(
							res.getIdentifier(
									"clock_padding_start",
									"dimen",
									mContext.getPackageName()));

					KGMiddleCustomTextView.setPadding(padding, 0, padding, 0);

					mStatusArea = ((LinearLayout) getObjectField(param.thisObject, "mStatusArea"));

					setMiddleText();
					setMiddleColor();
				} catch (Exception ignored) {
				}
			}
		});
	}

	private void setMiddleColor() {
		boolean mSupportsDarkText = getBooleanField(KGCS, "mSupportsDarkText");
		int color = (mDozing || !mSupportsDarkText) ? Color.WHITE : Color.BLACK;

		KGMiddleCustomTextView.setShadowLayer(1, 1, 1, color == Color.BLACK ? Color.TRANSPARENT : Color.BLACK); //shadow only for white color
		KGMiddleCustomTextView.setTextColor(color);
	}

	private void setCarrierText() {
		try {
			TextView mView = (TextView) getObjectField(carrierTextController, "mView");
			mView.setText(stringFormatter.formatString(customCarrierText));
		} catch (Throwable ignored) {
		} //probably not initiated yet
	}

	private void setMiddleText() {
		if (KGCS == null) return;

		if (KGMiddleCustomText.length() == 0) {
			mStatusArea.removeView(KGMiddleCustomTextView);
		} else {
			try {
				ViewGroup parent = (ViewGroup) KGMiddleCustomTextView.getParent();
				if (parent != null) {
					((ViewGroup) KGMiddleCustomTextView.getParent()).removeView(KGMiddleCustomTextView);
				}
				mStatusArea.addView(KGMiddleCustomTextView, 0);
				KGMiddleCustomTextView.setText(stringFormatter.formatString(KGMiddleCustomText));

			} catch (Exception ignored) {
			}
		}
	}
}