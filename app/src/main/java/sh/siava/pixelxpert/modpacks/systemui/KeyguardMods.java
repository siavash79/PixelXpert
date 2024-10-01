package sh.siava.pixelxpert.modpacks.systemui;

import static android.graphics.Color.TRANSPARENT;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;
import static sh.siava.pixelxpert.modpacks.systemui.BatteryDataProvider.isCharging;
import static sh.siava.pixelxpert.modpacks.utils.toolkit.ColorUtils.getColorAttr;
import static sh.siava.pixelxpert.modpacks.utils.toolkit.ColorUtils.getColorAttrDefaultColor;
import static sh.siava.pixelxpert.modpacks.utils.toolkit.ReflectionTools.hookAllMethodsMatchPattern;
import static sh.siava.pixelxpert.modpacks.utils.toolkit.ReflectionTools.tryHookAllMethods;

import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import java.lang.ref.WeakReference;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.ResourceManager;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.pixelxpert.modpacks.utils.StringFormatter;
import sh.siava.pixelxpert.modpacks.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class KeyguardMods extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	//region keyguard charging data
	public static final String EXTRA_MAX_CHARGING_CURRENT = "max_charging_current";
	public static final String EXTRA_MAX_CHARGING_VOLTAGE = "max_charging_voltage";
	public static final String EXTRA_TEMPERATURE = "temperature";

	public static final String SHORTCUT_TV_REMOTE = "tvremote";
	public static final String SHORTCUT_CAMERA = "camera";
	public static final String SHORTCUT_ASSISTANT = "assistant";
	public static final String SHORTCUT_TORCH = "torch";
	public static final String SHORTCUT_ZEN = "zen";
	public static final String SHORTCUT_QR_SCANNER = "qrscanner";
	private static final Object WALLPAPER_DIM_AMOUNT_DIMMED = 0.6F; //DefaultDeviceEffectsApplier
	private static KeyguardMods instance = null;

	private float max_charging_current = 0;
	private float max_charging_voltage = 0;
	private float temperature = 0;

	private static boolean ShowChargingInfo = false;
	//endregion

	private static boolean customCarrierTextEnabled = false;
	private static String customCarrierText = "";
	private static Object carrierTextController;

	final StringFormatter carrierStringFormatter = new StringFormatter();
	final StringFormatter clockStringFormatter = new StringFormatter();
	private TextView KGMiddleCustomTextView, mComposeKGMiddleCustomTextView;
	private static String KGMiddleCustomText = "";
	LinearLayout mStatusArea = null;
	private Object KGCS;
	private Object mColorExtractor;
	private boolean mDozing = false;
	private boolean mSupportsDarkText = false;

	private static boolean DisableUnlockHintAnimation = false;

	//region keyguardDimmer
	public static float KeyGuardDimAmount = -1f;
	private static boolean TemperatureUnitF = false;
	//endregion

	//region keyguard bottom area shortcuts and transparency
	public static final int ZEN_MODE_OFF = 0;
	public static final int ZEN_MODE_IMPORTANT_INTERRUPTIONS = 1;

	private Object ZenController;
	private Object CommandQueue;
	private Object QRScannerController;
	private Object ActivityStarter;
	private Object KeyguardBottomAreaView;
	private Object mAssistUtils;
	private static boolean transparentBGcolor = false;
	private static String leftShortcutClick = "";
	private static String rightShortcutClick = "";
	private static String leftShortcutLongClick = "";
	private static String rightShortcutLongClick = "";
	private int cameraResID = 0;
	//endregion

	//region hide user avatar
	private boolean HideLockScreenUserAvatar = false;
	private static boolean ForceAODwCharging = false;
	private Object KeyguardIndicationController;
	private LinearLayout mComposeSmartSpaceContainer;
	//endregion


	public KeyguardMods(Context context) {
		super(context);

		instance = this;
	}

	@Override
	public void updatePrefs(String... Key) {
		DisableUnlockHintAnimation = Xprefs.getBoolean("DisableUnlockHintAnimation", false);

		KGMiddleCustomText = Xprefs.getString("KGMiddleCustomText", "");

		customCarrierTextEnabled = Xprefs.getBoolean("carrierTextMod", false);
		customCarrierText = Xprefs.getString("carrierTextValue", "");

		ShowChargingInfo = Xprefs.getBoolean("ShowChargingInfo", false);
		TemperatureUnitF = Xprefs.getBoolean("TemperatureUnitF", false);

		HideLockScreenUserAvatar = Xprefs.getBoolean("HideLockScreenUserAvatar", false);

		ForceAODwCharging = Xprefs.getBoolean("ForceAODwCharging", false);

		KeyGuardDimAmount = Xprefs.getSliderFloat( "KeyGuardDimAmount", -1f) / 100f;

		leftShortcutClick = Xprefs.getString("leftKeyguardShortcut", "");
		rightShortcutClick = Xprefs.getString("rightKeyguardShortcut", "");

		leftShortcutLongClick = Xprefs.getString("leftKeyguardShortcutLongClick", "");
		rightShortcutLongClick = Xprefs.getString("rightKeyguardShortcutLongClick", "");

		transparentBGcolor = Xprefs.getBoolean("KeyguardBottomButtonsTransparent", false);


		if (Key.length > 0) {
			switch (Key[0]) {
				case "KGMiddleCustomText":
					updateMiddleTexts();
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
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		Class<?> CarrierTextControllerClass = findClass("com.android.keyguard.CarrierTextController", lpParam.classLoader);
		Class<?> KeyguardClockSwitchClass = findClass("com.android.keyguard.KeyguardClockSwitch", lpParam.classLoader);
		Class<?> KeyguardIndicationControllerClass = findClass("com.android.systemui.statusbar.KeyguardIndicationController", lpParam.classLoader);
		Class<?> ScrimControllerClass = findClass("com.android.systemui.statusbar.phone.ScrimController", lpParam.classLoader);
		Class<?> ScrimStateEnum = findClass("com.android.systemui.statusbar.phone.ScrimState", lpParam.classLoader);
		Class<?> KeyguardStatusBarViewClass = findClass("com.android.systemui.statusbar.phone.KeyguardStatusBarView", lpParam.classLoader);
		Class<?> CentralSurfacesImplClass = findClass("com.android.systemui.statusbar.phone.CentralSurfacesImpl", lpParam.classLoader);
		Class<?> KeyguardBottomAreaViewBinderClass = findClassIfExists("com.android.systemui.keyguard.ui.binder.KeyguardBottomAreaViewBinder", lpParam.classLoader);
		Class<?> NotificationPanelViewControllerClass = findClass("com.android.systemui.shade.NotificationPanelViewController", lpParam.classLoader); //used to launch camera
		Class<?> QRCodeScannerControllerClass = findClass("com.android.systemui.qrcodescanner.controller.QRCodeScannerController", lpParam.classLoader);
//		Class<?> ActivityStarterDelegateClass = findClass("com.android.systemui.ActivityStarterDelegate", lpParam.classLoader);
		Class<?> ZenModeControllerImplClass = findClass("com.android.systemui.statusbar.policy.ZenModeControllerImpl", lpParam.classLoader);
		Class<?> FooterActionsInteractorImplClass = findClass("com.android.systemui.qs.footer.domain.interactor.FooterActionsInteractorImpl", lpParam.classLoader);
		Class<?> CommandQueueClass = findClass("com.android.systemui.statusbar.CommandQueue", lpParam.classLoader);
		Class<?> AmbientDisplayConfigurationClass = findClass("android.hardware.display.AmbientDisplayConfiguration", lpParam.classLoader);
		Class<?> AssistManagerClass = findClassIfExists("com.android.systemui.assist.AssistManager", lpParam.classLoader);
		Class<?> DefaultShortcutsSectionClass = findClassIfExists("com.android.systemui.keyguard.ui.view.layout.sections.DefaultShortcutsSection", lpParam.classLoader);
		if(AssistManagerClass == null)
		{
			AssistManagerClass = findClass("com.google.android.systemui.assist.AssistManagerGoogle", lpParam.classLoader);
		}

		tryHookAllMethods(DefaultShortcutsSectionClass, "addViews", new XC_MethodHook() {
			@SuppressLint("DiscouragedApi")
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				Resources res = mContext.getResources();

				ControlledLaunchableImageViewBackgroundDrawable.captureDrawable(
						(ImageView) callMethod(param.args[0], "findViewById", res.getIdentifier(
								"end_button",
								"id",
								mContext.getPackageName())));

				ControlledLaunchableImageViewBackgroundDrawable.captureDrawable(
						(ImageView) callMethod(param.args[0], "findViewById", res.getIdentifier(
								"start_button",
								"id",
								mContext.getPackageName())));

			}
		});

		Class<?> SmartspaceSectionClass = findClassIfExists("com.android.systemui.keyguard.ui.view.layout.sections.SmartspaceSection", lpParam.classLoader);
		tryHookAllMethods(SmartspaceSectionClass, "addViews", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				try {
					mComposeKGMiddleCustomTextView = new TextView(mContext);
					mComposeKGMiddleCustomTextView.setMaxLines(2);
					mComposeKGMiddleCustomTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
					mComposeKGMiddleCustomTextView.setLetterSpacing(.03f);
					mComposeKGMiddleCustomTextView.setId(View.generateViewId());

					mComposeSmartSpaceContainer = (LinearLayout) getObjectField(param.thisObject, "dateWeatherView");
					mComposeSmartSpaceContainer.setOrientation(LinearLayout.VERTICAL);

					LinearLayout l = new LinearLayout(mContext);
					l.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));
					l.setId(View.generateViewId());
					while(mComposeSmartSpaceContainer.getChildCount() > 0)
					{
						View c = mComposeSmartSpaceContainer.getChildAt(0);
						mComposeSmartSpaceContainer.removeView(c);
						l.addView(c);
					}
					mComposeSmartSpaceContainer.addView(l);

					mComposeSmartSpaceContainer.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
						ViewGroup.LayoutParams lp = v.getLayoutParams();
						if(lp.width != -1)
						{
							lp.width = -1;
							setObjectField(lp, "rightMargin", getObjectField(lp, "leftMargin"));
						}
					});

					updateMiddleTexts();
					setMiddleColor();
				}
				catch (Throwable ignored){}
			}
		});

		hookAllMethods(AmbientDisplayConfigurationClass, "alwaysOnEnabled", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(ForceAODwCharging) {
					param.setResult((boolean) param.getResult() || isCharging());
				}
			}
		});

		hookAllConstructors(CommandQueueClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				CommandQueue = param.thisObject;
			}
		});

		hookAllMethods(NotificationPanelViewControllerClass, "startUnlockHintAnimation", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if(DisableUnlockHintAnimation) param.setResult(null);
			}
		});


		hookAllConstructors(FooterActionsInteractorImplClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				ActivityStarter = getObjectField(param.thisObject, "activityStarter");
			}
		});

		hookAllConstructors(QRCodeScannerControllerClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				QRScannerController = param.thisObject;
			}
		});

		hookAllConstructors(ZenModeControllerImplClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				ZenController = param.thisObject;
			}
		});

		hookAllConstructors(AssistManagerClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				mAssistUtils = getObjectField(param.thisObject, "mAssistUtils");
			}
		});

		//needed to extract wallpaper colors and capabilities. This is a SysUIColorExtractor
		hookAllConstructors(CentralSurfacesImplClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				mColorExtractor = getObjectField(param.thisObject, "mColorExtractor");
			}
		});

		//region hide user avatar
		hookAllMethods(KeyguardStatusBarViewClass, "updateVisibilities", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				View mMultiUserAvatar = (View) getObjectField(param.thisObject, "mMultiUserAvatar");
				boolean mIsUserSwitcherEnabled = getBooleanField(param.thisObject, "mIsUserSwitcherEnabled");
				mMultiUserAvatar.setVisibility(!HideLockScreenUserAvatar && mIsUserSwitcherEnabled
						? VISIBLE
						: GONE);
			}
		});
		//endregion

		//region keyguard bottom area shortcuts and transparency


		if(KeyguardBottomAreaViewBinderClass != null)
		{
			hookAllMethods(KeyguardBottomAreaViewBinderClass, "bind", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					KeyguardBottomAreaView = param.args[0];
				}
			});

			hookAllMethodsMatchPattern(KeyguardBottomAreaViewBinderClass, ".*updateButton", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					ImageView v = (ImageView) param.args[0];

					try {
						if(Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { //feature deprecated for Android 14
							String shortcutID = mContext.getResources().getResourceName(v.getId());

							if (shortcutID.contains("start")) {
								convertShortcut(v, leftShortcutClick);
								if (isShortcutSet(v)) {
									setLongPress(v, leftShortcutLongClick);
								}
							} else if (shortcutID.contains("end")) {
								convertShortcut(v, rightShortcutClick);
								if (isShortcutSet(v)) {
									setLongPress(v, rightShortcutLongClick);
								}
							}
						}

						if (transparentBGcolor) {
							@SuppressLint("DiscouragedApi") int wallpaperTextColorAccent = getColorAttrDefaultColor(
									mContext,
									mContext.getResources().getIdentifier("wallpaperTextColorAccent", "attr", mContext.getPackageName()));

							try {
								v.getDrawable().setTintList(ColorStateList.valueOf(wallpaperTextColorAccent));
								v.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
							} catch (Throwable ignored) {}
						} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE){
							@SuppressLint("DiscouragedApi")
							int mTextColorPrimary = getColorAttrDefaultColor(
									mContext,
									mContext.getResources().getIdentifier("textColorPrimary", "attr", "android"));

							@SuppressLint("DiscouragedApi")
							ColorStateList colorSurface = getColorAttr(
									mContext,
									mContext.getResources().getIdentifier("colorSurface", "attr", "android"));

							v.getDrawable().setTint(mTextColorPrimary);

							v.setBackgroundTintList(colorSurface);
						}
					} catch (Throwable ignored) {
					}
				}
			});
		}

		//endregion

		//region keyguard battery info
		XC_MethodHook powerIndicationHook = new XC_MethodHook() {
			@SuppressLint("DefaultLocale")
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (ShowChargingInfo) {
					String result = (String) param.getResult();

					Float shownTemperature = (TemperatureUnitF)
							? (temperature * 1.8f) + 32f
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
		};

		XC_MethodHook keyguardIndicatorFinder = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				KeyguardIndicationController = param.thisObject;
			}
		};

		try { //A14
			Class<?> KeyguardIndicationControllerGoogleClass = findClass("com.google.android.systemui.statusbar.KeyguardIndicationControllerGoogle", lpParam.classLoader);
			hookAllConstructors(KeyguardIndicationControllerGoogleClass, keyguardIndicatorFinder);
			hookAllMethods(KeyguardIndicationControllerGoogleClass, "computePowerIndication", powerIndicationHook);
		}
		catch (Throwable ignored)
		{ //A13 and maybe 14 custom
			hookAllConstructors(KeyguardIndicationControllerClass, keyguardIndicatorFinder);
			hookAllMethods(KeyguardIndicationControllerClass, "computePowerIndication", powerIndicationHook);
		}

		BatteryDataProvider.registerStatusCallback((batteryStatus, batteryStatusIntent) -> {
			max_charging_current = batteryStatusIntent.getIntExtra(EXTRA_MAX_CHARGING_CURRENT, 0) / 1000000f;
			max_charging_voltage = batteryStatusIntent.getIntExtra(EXTRA_MAX_CHARGING_VOLTAGE, 0) / 1000000f;
			temperature = batteryStatusIntent.getIntExtra(EXTRA_TEMPERATURE, 0) / 10f;
		});
		//endregion

		//region keyguardDimmer
		//A13 - A14
		hookAllMethodsMatchPattern(ScrimControllerClass, "scheduleUpdate.*", new XC_MethodHook() {
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

		//A15
		hookAllMethods(WallpaperManager.class, "getWallpaperDimAmount", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						if ((KeyGuardDimAmount < 0 || KeyGuardDimAmount > 1)
								|| param.getResult().equals(WALLPAPER_DIM_AMOUNT_DIMMED))
							return;

						//ref ColorUtils.compositeAlpha - Since KEYGUARD_SCRIM_ALPHA = .2f, we need to range the result between -70 and 255 to get the correct value when composed with 20% - we use 60 to cover float inaccuracies and never see a negative final result
						param.setResult((325 * KeyGuardDimAmount - 60) / 255);
					}
				});
		//endregion

		carrierStringFormatter.registerCallback(this::setCarrierText);

		clockStringFormatter.registerCallback(this::updateMiddleTexts);

		Resources res = mContext.getResources();

		hookAllMethods(CarrierTextControllerClass, "onInit", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {

				carrierTextController = param.thisObject;
				Object carrierTextCallback = getObjectField(carrierTextController, "mCarrierTextCallback");
				setCarrierText();
				hookAllMethods(carrierTextCallback.getClass(),
						"updateCarrierInfo", new XC_MethodHook() {
							@Override
							protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
								if (customCarrierTextEnabled)
									param.setResult(null);
							}
						});
			}
		}).size();

		//a way to know when the device goes to AOD/dozing
		hookAllMethods(KeyguardIndicationControllerClass, "updateDeviceEntryIndication", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (mDozing != (boolean) getObjectField(param.thisObject, "mDozing")) {
					mDozing = !mDozing;
					setMiddleColor();
					//setShortcutVisibility();
				}
			}
		});

		hookAllMethods(KeyguardClockSwitchClass, "onFinishInflate", new XC_MethodHook() {
			@SuppressLint("DiscouragedApi")
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				try {
					KGCS = param.thisObject;
					KGMiddleCustomTextView = new TextView(mContext);
					KGMiddleCustomTextView.setMaxLines(2);
					KGMiddleCustomTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
					KGMiddleCustomTextView.setLetterSpacing(.03f);
					KGMiddleCustomTextView.setId(View.generateViewId());

					@SuppressLint("DiscouragedApi") int sidePadding = res.getDimensionPixelSize(
							res.getIdentifier(
									"clock_padding_start",
									"dimen",
									mContext.getPackageName()));

					KGMiddleCustomTextView.setPadding(sidePadding,
							(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, mContext.getResources().getDisplayMetrics()),
							sidePadding,
							0);

					mStatusArea = ((View)param.thisObject).findViewById(mContext.getResources().getIdentifier("keyguard_status_area", "id", lpParam.packageName));

					updateMiddleTexts();
					setMiddleColor();

				} catch (Exception ignored) {}
			}
		});
	}


	private void setLongPress(ImageView button, String type) {
		if(type.isEmpty())
		{
			button.setLongClickable(false);
			return;
		}
		button.setOnLongClickListener(v -> {
			launchAction(type);
			return true;
		});
	}

	private boolean isShortcutSet(ImageView v) {
		Object info = getObjectField(v, "mListenerInfo");
		return info != null && getObjectField(info, "mOnClickListener") != null;
	}

	//region keyguard bottom area shortcuts and transparency
	@SuppressLint("DiscouragedApi")
	private void setShortcutVisibility() {
		int visibility = mDozing ? GONE : VISIBLE;

		Resources res = mContext.getResources();

		if(!leftShortcutClick.isEmpty()) {
			((View) KeyguardBottomAreaView)
					.findViewById(res.getIdentifier("start_button",
							"id",
							mContext.getPackageName()))
					.setVisibility(visibility);
		}

		if(!rightShortcutClick.isEmpty()) {
			((View) KeyguardBottomAreaView)
					.findViewById(res.getIdentifier("end_button",
							"id",
							mContext.getPackageName()))
					.setVisibility(visibility);
		}
	}

	@SuppressLint("DiscouragedApi")
	private void convertShortcut(ImageView button, String type) {
		if(type.isEmpty()) return;

		Resources res = mContext.getResources();

		if(cameraResID == 0)
		{
			cameraResID = res.getIdentifier("ic_camera_alt_24dp", "drawable", mContext.getPackageName()); //13

			if(cameraResID == 0)
			{
				cameraResID = res.getIdentifier("ic_camera", "drawable", mContext.getPackageName()); //14
			}
		}

		Drawable drawable = null;
		switch (type) {
			case SHORTCUT_TV_REMOTE:
				drawable = ResourcesCompat.getDrawable(ResourceManager.modRes, R.drawable.ic_remote, mContext.getTheme());
				break;
			case SHORTCUT_CAMERA:
				drawable = ResourcesCompat.getDrawable(res, cameraResID, mContext.getTheme());
				break;
			case SHORTCUT_ASSISTANT:
				drawable = ResourcesCompat.getDrawable(res, res.getIdentifier("ic_mic_26dp", "drawable", mContext.getPackageName()), mContext.getTheme());
				break;
			case SHORTCUT_TORCH:
				drawable = ResourcesCompat.getDrawable(res, res.getIdentifier("@android:drawable/ic_qs_flashlight", "drawable", mContext.getPackageName()), mContext.getTheme());
				break;
			case SHORTCUT_ZEN:
				drawable = ResourcesCompat.getDrawable(res, res.getIdentifier("@android:drawable/ic_zen_24dp", "drawable", mContext.getPackageName()), mContext.getTheme());
				break;
			case SHORTCUT_QR_SCANNER:
				drawable = ResourcesCompat.getDrawable(res, res.getIdentifier("ic_qr_code_scanner", "drawable", mContext.getPackageName()), mContext.getTheme());
				break;
		}

		button.setOnClickListener(v -> launchAction(type));
		button.setImageDrawable(drawable);

		button.setVisibility(mDozing
				? GONE
				: VISIBLE);
	}

	private void launchAction(String type) {
		switch (type) {
			case SHORTCUT_TV_REMOTE:
				launchTVRemote();
				break;
			case SHORTCUT_CAMERA:
				launchCamera();
				break;
			case SHORTCUT_ASSISTANT:
				launchAssistant();
				break;
			case SHORTCUT_TORCH:
				toggleFlash();
				break;
			case SHORTCUT_ZEN:
				toggleZen();
				break;
			case SHORTCUT_QR_SCANNER:
				try {
					callMethod(ActivityStarter, "startActivity", getObjectField(QRScannerController, "mIntent"), true);
					break;
				}
				catch (Throwable ignored){}
		}
	}

	private void toggleFlash() {
		SystemUtils.toggleFlash();
	}

	private void toggleZen()
	{
		if(ZenController == null) return;

		int zenMode = (int) callMethod(ZenController, "getZen");

		int newZenMode = (zenMode == ZEN_MODE_OFF) ? ZEN_MODE_IMPORTANT_INTERRUPTIONS : ZEN_MODE_OFF;

		callMethod(ZenController, "setZen", newZenMode, null, "lockscreen Shortcut");
	}
	private void launchAssistant() {
		callMethod(mAssistUtils, "launchVoiceAssistFromKeyguard");
	}


	private void launchTVRemote() {
		XPLauncher.enqueueProxyCommand(proxy -> {
			try {
				proxy.runCommand("pm enable com.google.android.videos; am start -n com.google.android.videos/com.google.android.apps.play.movies.common.remote.RemoteDevicesListActivity"); //enabling it if disabled, and start remote activity on older versions
				proxy.runCommand("am start -a com.google.android.apps.googletv.ACTION_VIRTUAL_REMOTE"); //start activity on the updated TV app
			} catch (Throwable ignored) {}
		});
	}

	private void launchCamera() {
		if(CommandQueue != null)
		{
			callMethod(CommandQueue, "onCameraLaunchGestureDetected", 0);
		}
	}
	//endregion

	private void setMiddleColor() {
		if(mColorExtractor != null) {
			Object colors = callMethod(mColorExtractor, "getColors", WallpaperManager.FLAG_LOCK);
			mSupportsDarkText = (boolean) callMethod(colors, "supportsDarkText");
		}
		int color = (mDozing || !mSupportsDarkText) ? Color.WHITE : Color.BLACK;

		try {
			mComposeKGMiddleCustomTextView.setShadowLayer(1, 1, 1, color == Color.BLACK ? Color.TRANSPARENT : Color.BLACK); //shadow only for white color
			mComposeKGMiddleCustomTextView.setTextColor(color);
		}
		catch (Throwable ignored) {}

		try {
			KGMiddleCustomTextView.setShadowLayer(1, 1, 1, color == Color.BLACK ? Color.TRANSPARENT : Color.BLACK); //shadow only for white color
			KGMiddleCustomTextView.setTextColor(color);
		}
		catch (Throwable ignored) {}
	}

	private void setCarrierText() {
		if(!customCarrierTextEnabled) return;

		try {
			TextView mView = (TextView) getObjectField(carrierTextController, "mView");
			mView.post(() -> mView.setText(carrierStringFormatter.formatString(customCarrierText)));
		} catch (Throwable ignored) {} //probably not initiated yet
	}

	private void updateMiddleTexts()
	{
		CharSequence text = null;
		if(!KGMiddleCustomText.isEmpty())
		{
			text = clockStringFormatter.formatString(KGMiddleCustomText);
		}

		try {
			setMiddleText(text);
		}
		catch (Throwable ignored){}

		try {
			setMiddleTextCompose(text);
		} catch (Throwable ignored) {}
	}

	private void setMiddleText(CharSequence text) {
		if (KGCS == null) return;

		mStatusArea.post(() -> {
			if (text == null) {
				mStatusArea.removeView(KGMiddleCustomTextView);
			} else {
				try {
					ViewGroup parent = (ViewGroup) KGMiddleCustomTextView.getParent();
					if (parent != null) {
						((ViewGroup) KGMiddleCustomTextView.getParent()).removeView(KGMiddleCustomTextView);
					}
					mStatusArea.addView(KGMiddleCustomTextView, 0);
					KGMiddleCustomTextView.setText(text);
				} catch (Exception ignored) {
				}
			}
		});
	}

	private void setMiddleTextCompose(CharSequence text) {
		mComposeSmartSpaceContainer.post(() -> {
			if (text == null) {
				mComposeSmartSpaceContainer.removeView(mComposeKGMiddleCustomTextView);
			} else {
				if(mComposeKGMiddleCustomTextView.getParent() == null) {
					mComposeSmartSpaceContainer.addView(mComposeKGMiddleCustomTextView);
				}
				mComposeKGMiddleCustomTextView.setText(text);
			}
		});
	}


	public static String getPowerIndicationString()
	{
		try {
			return (String) callMethod(instance.KeyguardIndicationController, "computePowerIndication");
		}
		catch (Throwable ignored)
		{
			return ResourceManager.modRes.getString(R.string.power_indication_error);
		}
	}

	public static class ControlledLaunchableImageViewBackgroundDrawable extends Drawable
	{
		Context mContext;
		Drawable mDrawable;
		WeakReference<ImageView> parentImageViewReference;

		public static void captureDrawable(ImageView imageView)
		{
			try {
				Drawable background = imageView.getBackground();

				background = new ControlledLaunchableImageViewBackgroundDrawable(background.getCurrent().mutate(), imageView);
				imageView.setBackground(background);
			} catch (Throwable ignored) {}
		}
		@Override
		public Drawable mutate()
		{
			return new ControlledLaunchableImageViewBackgroundDrawable(mDrawable.mutate(), parentImageViewReference.get());
		}
		public ControlledLaunchableImageViewBackgroundDrawable(Drawable drawable, ImageView parentView)
		{
			parentImageViewReference = new WeakReference<>(parentView);
			mContext = parentView.getContext();
			mDrawable = drawable;
		}
		@Override
		public void setTint(int tintColor)
		{
			if(!transparentBGcolor)
			{
				mDrawable.setTint(tintColor);
			}
			else
			{
				mDrawable.setTint(TRANSPARENT);
			}
		}

		@Override
		public void setTintList(ColorStateList tintList) {
			if(transparentBGcolor)
			{
				mDrawable.setTint(TRANSPARENT);

				ImageView parentView = parentImageViewReference.get();
				if (parentView != null && parentView.getDrawable() != null) {
					@SuppressLint("DiscouragedApi") int wallpaperTextColorAccent = getColorAttrDefaultColor(
							mContext,
							mContext.getResources().getIdentifier("wallpaperTextColorAccent", "attr", mContext.getPackageName()));

					parentView.getDrawable().setTint(wallpaperTextColorAccent);
				}
			}
			else
			{
				mDrawable.setTintList(tintList);
			}
		}

		@Override
		public void draw(@NonNull Canvas canvas) {
			mDrawable.draw(canvas);
		}

		@Override
		public void jumpToCurrentState()
		{
			mDrawable.jumpToCurrentState();
		}

		@Override
		public void setAlpha(int alpha) {
			mDrawable.setAlpha(alpha);
		}

		@Override
		public void setColorFilter(@Nullable ColorFilter colorFilter) {
			if(!transparentBGcolor)
			{
				mDrawable.setColorFilter(colorFilter);
			}
		}

		@Override
		public int getOpacity() {
			//noinspection deprecation
			return mDrawable.getOpacity();
		}

		@Override
		public boolean getPadding(Rect padding)
		{
			return mDrawable.getPadding(padding);
		}

		@Override
		public int getMinimumHeight()
		{
			return mDrawable.getMinimumHeight();
		}

		@Override
		public int getMinimumWidth()
		{
			return mDrawable.getMinimumWidth();
		}

		@Override
		public boolean isStateful()
		{
			return mDrawable.isStateful();
		}

		@Override
		public boolean setVisible(boolean visible, boolean restart)
		{
			return mDrawable.setVisible(visible, restart);
		}

		@Override
		public void getOutline(Outline outline)
		{
			mDrawable.getOutline(outline);
		}

		@Override
		public boolean isProjected()
		{
			return mDrawable.isProjected();
		}

		@Override
		public void setBounds(Rect bounds)
		{
			mDrawable.setBounds(bounds);
		}

		@Override
		public void setBounds(int l, int t, int r, int b)
		{
			mDrawable.setBounds(l,t,r,b);
		}

		@Override
		public Drawable getCurrent()
		{
			return mDrawable.getCurrent();
		}

		@Override
		public boolean setState(int[] stateSet)
		{
			return mDrawable.setState(stateSet);
		}

		@Override
		public int getIntrinsicWidth()
		{
			return mDrawable.getIntrinsicWidth();
		}

		@Override
		public int getIntrinsicHeight()
		{
			return mDrawable.getIntrinsicHeight();
		}
	}
}