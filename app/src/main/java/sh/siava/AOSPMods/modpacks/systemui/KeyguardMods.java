package sh.siava.AOSPMods.modpacks.systemui;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedBridge.hookMethod;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.modpacks.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.topjohnwu.superuser.Shell;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.R;
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.ResourceManager;
import sh.siava.AOSPMods.modpacks.XPLauncher;
import sh.siava.AOSPMods.modpacks.XposedModPack;
import sh.siava.AOSPMods.modpacks.utils.StringFormatter;
import sh.siava.AOSPMods.modpacks.utils.SystemUtils;
import sh.siava.rangesliderpreference.RangeSliderPreference;

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

	private float max_charging_current = 0;
	private float max_charging_voltage = 0;
	private float temperature = 0;

	private static boolean ShowChargingInfo = false;
	//endregion

	private static boolean customCarrierTextEnabled = false;
	private static String customCarrierText = "";
	private static Object carrierTextController;

	StringFormatter carrierStringFormatter = new StringFormatter();
	StringFormatter clockStringFormatter = new StringFormatter();
	private TextView KGMiddleCustomTextView;
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
	//endregion

	public KeyguardMods(Context context) {
		super(context);
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

		try {
			KeyGuardDimAmount = RangeSliderPreference.getValues(Xprefs, "KeyGuardDimAmount", -1f).get(0) / 100f;
		} catch (Throwable ignored) {
		}

		leftShortcutClick = Xprefs.getString("leftKeyguardShortcut", "");
		rightShortcutClick = Xprefs.getString("rightKeyguardShortcut", "");

		leftShortcutLongClick = Xprefs.getString("leftKeyguardShortcutLongClick", "");
		rightShortcutLongClick = Xprefs.getString("rightKeyguardShortcutLongClick", "");

		transparentBGcolor = Xprefs.getBoolean("KeyguardBottomButtonsTransparent", false);


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
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

		Class<?> CarrierTextControllerClass = findClass("com.android.keyguard.CarrierTextController", lpparam.classLoader);
		Class<?> KeyguardClockSwitchClass = findClass("com.android.keyguard.KeyguardClockSwitch", lpparam.classLoader);
		Class<?> BatteryStatusClass = findClass("com.android.settingslib.fuelgauge.BatteryStatus", lpparam.classLoader);
		Class<?> KeyguardIndicationControllerClass = findClass("com.android.systemui.statusbar.KeyguardIndicationController", lpparam.classLoader);
		Class<?> ScrimControllerClass = findClass("com.android.systemui.statusbar.phone.ScrimController", lpparam.classLoader);
		Class<?> ScrimStateEnum = findClass("com.android.systemui.statusbar.phone.ScrimState", lpparam.classLoader);
		Class<?> KeyguardStatusBarViewClass = findClass("com.android.systemui.statusbar.phone.KeyguardStatusBarView", lpparam.classLoader);
		Class<?> CentralSurfacesImplClass = findClass("com.android.systemui.statusbar.phone.CentralSurfacesImpl", lpparam.classLoader);
		Class<?> KeyguardBottomAreaViewBinderClass = findClass("com.android.systemui.keyguard.ui.binder.KeyguardBottomAreaViewBinder", lpparam.classLoader);
		Class<?> AssistManager = findClass("com.android.systemui.assist.AssistManager", lpparam.classLoader);
		Class<?> NotificationPanelViewControllerClass = findClass("com.android.systemui.shade.NotificationPanelViewController", lpparam.classLoader); //used to launch camera
		Class<?> QRCodeScannerControllerClass = findClass("com.android.systemui.qrcodescanner.controller.QRCodeScannerController", lpparam.classLoader);
//		Class<?> ActivityStarterDelegateClass = findClass("com.android.systemui.ActivityStarterDelegate", lpparam.classLoader);
		Class<?> ZenModeControllerImplClass = findClass("com.android.systemui.statusbar.policy.ZenModeControllerImpl", lpparam.classLoader);
		Class<?> FooterActionsInteractorImplClass = findClass("com.android.systemui.qs.footer.domain.interactor.FooterActionsInteractorImpl", lpparam.classLoader);
		Class<?> CommandQueueClass = findClass("com.android.systemui.statusbar.CommandQueue", lpparam.classLoader);

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

		hookAllConstructors(AssistManager, new XC_MethodHook() {
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
		hookAllMethods(KeyguardBottomAreaViewBinderClass, "bind", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				KeyguardBottomAreaView = param.args[0];
			}
		});

		Method updateMethod = null;
		Method[] methods = KeyguardBottomAreaViewBinderClass.getMethods();
		for (Method m : methods) {
			if (m.getName().contains("updateButton")) {
				updateMethod = m;
				break;
			}
		}
		if (updateMethod != null) {
			hookMethod(updateMethod, new XC_MethodHook() {
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
							@SuppressLint("DiscouragedApi") int wallpaperTextColorAccent = SettingsLibUtilsProvider.getColorAttrDefaultColor(
									mContext.getResources().getIdentifier("wallpaperTextColorAccent", "attr", mContext.getPackageName()), mContext);

							try {
								v.getDrawable().setTintList(ColorStateList.valueOf(wallpaperTextColorAccent));
								v.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
							} catch (Throwable ignored) {
							}
						} else {
							@SuppressLint("DiscouragedApi") int mTextColorPrimary = SettingsLibUtilsProvider.getColorAttrDefaultColor(
									mContext.getResources().getIdentifier("textColorPrimary", "attr", "android"), mContext);

							@SuppressLint("DiscouragedApi") ColorStateList colorSurface = SettingsLibUtilsProvider.getColorAttr(
									mContext.getResources().getIdentifier("colorSurface", "attr", "android"), mContext);

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

		try { //A14
			Class<?> KeyguardIndicationControllerGoogleClass = findClass("com.google.android.systemui.statusbar.KeyguardIndicationControllerGoogle", lpparam.classLoader);
			hookAllMethods(KeyguardIndicationControllerGoogleClass, "computePowerIndication", powerIndicationHook);
		}
		catch (Throwable ignored)
		{ //A13 and maybe 14 custom
			hookAllMethods(KeyguardIndicationControllerClass, "computePowerIndication", powerIndicationHook);
		}

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

		carrierStringFormatter.registerCallback(this::setCarrierText);

		clockStringFormatter.registerCallback(this::setMiddleText);

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

		//a way to know when the device goes to AOD/dozing
		hookAllMethods(KeyguardIndicationControllerClass, "updateDeviceEntryIndication", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (mDozing != (boolean) getObjectField(param.thisObject, "mDozing")) {
					mDozing = !mDozing;
					setMiddleColor();
					setShortcutVisibility();
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

	private void setLongPress(ImageView button, String type) {
		if(type.length() == 0)
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

		if(leftShortcutClick.length() > 0) {
			((View) KeyguardBottomAreaView)
					.findViewById(res.getIdentifier("start_button",
							"id",
							mContext.getPackageName()))
					.setVisibility(visibility);
		}

		if(rightShortcutClick.length() > 0) {
			((View) KeyguardBottomAreaView)
					.findViewById(res.getIdentifier("end_button",
							"id",
							mContext.getPackageName()))
					.setVisibility(visibility);
		}
	}

	@SuppressLint("DiscouragedApi")
	private void convertShortcut(ImageView button, String type) {
		if(type.length() == 0) return;

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
		SystemUtils.ToggleFlash();
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
		Shell.cmd("pm enable com.google.android.videos; am start -n com.google.android.videos/com.google.android.apps.play.movies.common.remote.RemoteDevicesListActivity").exec();
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

		KGMiddleCustomTextView.setShadowLayer(1, 1, 1, color == Color.BLACK ? Color.TRANSPARENT : Color.BLACK); //shadow only for white color
		KGMiddleCustomTextView.setTextColor(color);
	}

	private void setCarrierText() {
		try {
			TextView mView = (TextView) getObjectField(carrierTextController, "mView");
			mView.setText(carrierStringFormatter.formatString(customCarrierText));
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
				KGMiddleCustomTextView.setText(clockStringFormatter.formatString(KGMiddleCustomText));

			} catch (Exception ignored) {
			}
		}
	}
}