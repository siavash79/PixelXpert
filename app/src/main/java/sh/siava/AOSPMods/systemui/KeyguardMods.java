package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
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
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.utils.StringFormatter;
import sh.siava.AOSPMods.utils.SystemUtils;
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
	private static boolean TemperatureUnitF = false;
	//endregion

	//region keyguard bottom area shortcuts and transparency
	private static boolean transparentBGcolor = false;
	private static String leftShortcut = "";
	private static String rightShortcut = "";

	private ImageView mWalletButton;
	private ImageView mControlsButton;

	private Object thisObject = null;
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
		TemperatureUnitF = Xprefs.getBoolean("TemperatureUnitF", false);

		try {
			KeyGuardDimAmount = RangeSliderPreference.getValues(Xprefs, "KeyGuardDimAmount", -1f).get(0) / 100f;
		} catch (Throwable ignored) {
		}

		leftShortcut = Xprefs.getString("leftKeyguardShortcut", "");
		rightShortcut = Xprefs.getString("rightKeyguardShortcut", "");

		transparentBGcolor = Xprefs.getBoolean("KeyguardBottomButtonsTransparent", false);


		if (Key.length > 0) {
			switch (Key[0]) {
				case "leftKeyguardShortcut":
					if (thisObject != null)
						convertShortcut(mControlsButton, leftShortcut, thisObject);
					break;
				case "rightKeyguardShortcut":
					if (thisObject != null)
						convertShortcut(mWalletButton, rightShortcut, thisObject);
					break;
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
		Class<?> KeyguardbottomAreaViewClass = findClass("com.android.systemui.statusbar.phone.KeyguardBottomAreaView", lpparam.classLoader);
		Class<?> UtilClass = findClass("com.android.settingslib.Utils", lpparam.classLoader);
		Class<?> ScrimControllerClass = findClass("com.android.systemui.statusbar.phone.ScrimController", lpparam.classLoader);
		Class<?> ScrimStateEnum = findClass("com.android.systemui.statusbar.phone.ScrimState", lpparam.classLoader);

		//region keyguard bottom area shortcuts and transparency
		//convert wallet button to camera button
		findAndHookMethod(KeyguardbottomAreaViewClass,
				"onFinishInflate", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						mWalletButton = (ImageView) getObjectField(param.thisObject, "mWalletButton");
						mControlsButton = (ImageView) getObjectField(param.thisObject, "mControlsButton");
						thisObject = param.thisObject;

						if (leftShortcut.length() > 0) {
							convertShortcut(mControlsButton, leftShortcut, param.thisObject);
						}
						if (rightShortcut.length() > 0) {
							convertShortcut(mWalletButton, rightShortcut, param.thisObject);
						}
					}
				});

		//make sure system won't play with our button
		findAndHookMethod(KeyguardbottomAreaViewClass.getName() + "$" + "WalletCardRetriever", lpparam.classLoader,
				"onWalletCardsRetrieved", "android.service.quickaccesswallet.GetWalletCardsResponse", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						if (rightShortcut.length() == 0) return;
						param.setResult(null);
					}
				});

		//make sure system won't play with our button
		hookAllMethods(KeyguardbottomAreaViewClass,
				"updateControlsVisibility", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						if (leftShortcut.length() == 0 || mControlsButton == null) return;
						updateVisibility(mControlsButton, param.thisObject);
						param.setResult(null);
					}
				});

		//make sure system won't play with our button
		findAndHookMethod(KeyguardbottomAreaViewClass,
				"updateWalletVisibility", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						if (rightShortcut.length() == 0 || mWalletButton == null) return;
						updateVisibility(mWalletButton, param.thisObject);
						param.setResult(null);
					}
				});

		//Transparent background
		findAndHookMethod(KeyguardbottomAreaViewClass,
				"updateAffordanceColors", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						if (!transparentBGcolor) return;
						ImageView mWalletButton = (ImageView) getObjectField(param.thisObject, "mWalletButton");
						ImageView mControlsButton = (ImageView) getObjectField(param.thisObject, "mControlsButton");

						@SuppressLint("DiscouragedApi") int mTextColorPrimary = (int) callStaticMethod(UtilClass, "getColorAttrDefaultColor", mContext,
								mContext.getResources().getIdentifier("wallpaperTextColorAccent", "attr", mContext.getPackageName()));

						mControlsButton.setBackgroundColor(Color.TRANSPARENT);
						mControlsButton.setColorFilter(mTextColorPrimary);

						mWalletButton.setBackgroundColor(Color.TRANSPARENT);
						mWalletButton.setColorFilter(mTextColorPrimary);
					}
				});

		//Set camera intent to be always secure when launchd from keyguard screen
		findAndHookMethod(KeyguardbottomAreaViewClass.getName() + "$DefaultRightButton", lpparam.classLoader,
				"getIntent", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						if ((!leftShortcut.equals("camera") && !rightShortcut.equals("camera")))
							return;
						param.setResult(getCameraIntent(mContext));
					}
				});
		//endregion

		//region keyguard battery info
		hookAllMethods(KeyguardIndicationControllerClass, "computePowerIndication", new XC_MethodHook() {
			@SuppressLint("DefaultLocale")
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(ShowChargingInfo) {
					String result = (String) param.getResult();

					Float shownTemperature = (TemperatureUnitF)
							? (temperature*1.8f) + 32f
							: temperature;

					param.setResult(
							String.format(
									"%s\n%.1fW (%.1fV, %.1fA) • %.0fº%s"
									, result
									, max_charging_current * max_charging_voltage
									, max_charging_voltage
									, max_charging_current
									, shownTemperature
									, TemperatureUnitF
											? "F"
											: "C"));
				}
			}
		});

		hookAllConstructors(BatteryStatusClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				try {
					Intent batteryInfoIntent = (Intent) param.args[0];

					max_charging_current = batteryInfoIntent.getIntExtra(EXTRA_MAX_CHARGING_CURRENT, 0) / 1000000f;
					max_charging_voltage = batteryInfoIntent.getIntExtra(EXTRA_MAX_CHARGING_VOLTAGE, 0) / 1000000f;
					temperature = batteryInfoIntent.getIntExtra(EXTRA_TEMPERATURE, 0) / 10f;
				} catch (Throwable ignored){}
			}
		});
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

	//region keyguard bottom area shortcuts and transparency
	@SuppressLint("DiscouragedApi")
	private Intent getCameraIntent(Context context) {
		Resources res = context.getResources();

		Intent cameraIntent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE);
		cameraIntent.setPackage(res.getString(res.getIdentifier("config_cameraGesturePackage", "string", context.getPackageName())));

		return cameraIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
	}

	private void updateVisibility(ImageView Button, Object thisObject) {
		boolean mDozing = (boolean) getObjectField(thisObject, "mDozing");

		if (mDozing) // AOD is showing
		{
			Button.setVisibility(View.GONE);
		} else {
			Button.setVisibility(View.VISIBLE);
		}
	}

	@SuppressLint("DiscouragedApi")
	private void convertShortcut(ImageView Button, String type, Object thisObject) {
		View.OnClickListener listener = null;
		Drawable drawable = null;
		switch (type) {
			case "camera":
				listener = v -> callMethod(thisObject, "launchCamera", "lockscreen_affordance");
				drawable = ResourcesCompat.getDrawable(mContext.getResources(), mContext.getResources().getIdentifier("ic_camera_alt_24dp", "drawable", mContext.getPackageName()), mContext.getTheme());
				break;
			case "assistant":
				listener = v -> callMethod(thisObject, "launchVoiceAssist");
				drawable = ResourcesCompat.getDrawable(mContext.getResources(), mContext.getResources().getIdentifier("ic_mic_26dp", "drawable", mContext.getPackageName()), mContext.getTheme());
				break;
			case "torch":
				listener = v -> SystemUtils.ToggleFlash();
				drawable = ResourcesCompat.getDrawable(mContext.getResources(), mContext.getResources().getIdentifier("@android:drawable/ic_qs_flashlight", "drawable", mContext.getPackageName()), mContext.getTheme());
				break;
		}
		if (type.length() > 0) {
			Button.setImageDrawable(drawable);
			Button.setOnClickListener(listener);
			Button.setClickable(true);
			Button.setVisibility(View.VISIBLE);
		} else {
			Button.setVisibility(View.GONE);
		}
	}
	//endregion

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