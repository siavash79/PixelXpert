package sh.siava.AOSPMods.systemui;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.widget.LinearLayout.VERTICAL;
import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.findFieldIfExists;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;
import static sh.siava.AOSPMods.utils.SettingsLibUtils.getColorAttrDefaultColor;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.telephony.ServiceState;
import android.telephony.TelephonyCallback;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.CharacterStyle;
import android.text.style.RelativeSizeSpan;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;

import javax.security.auth.callback.Callback;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.BuildConfig;
import sh.siava.AOSPMods.R;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.utils.NetworkTraffic;
import sh.siava.AOSPMods.utils.NotificationIconContainerOverride;
import sh.siava.AOSPMods.utils.SettingsLibUtils;
import sh.siava.AOSPMods.utils.ShyLinearLayout;
import sh.siava.AOSPMods.utils.StringFormatter;
import sh.siava.AOSPMods.utils.SystemUtils;
import sh.siava.AOSPMods.utils.batteryStyles.BatteryBarView;
import sh.siava.rangesliderpreference.RangeSliderPreference;

@SuppressWarnings({"RedundantThrows", "ConstantConditions"})

public class StatusbarMods extends XposedModPack {
	private static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;

	//region battery
	public static final int CHARGING_FAST = 2;
	//endregion

	//region Clock
	public static final int POSITION_LEFT = 0;
	public static final int POSITION_CENTER = 1;
	public static final int POSITION_RIGHT = 2;
	public static final int POSITION_LEFT_EXTRA_LEVEL = 3;

	private static final int AM_PM_STYLE_SMALL = 1;
	private static final int AM_PM_STYLE_GONE = 2;

	private int leftClockPadding = 0, rightClockPadding = 0;
	private static int clockPosition = POSITION_LEFT;
	private static int mAmPmStyle = AM_PM_STYLE_GONE;
	private static boolean mShowSeconds = false;
	private static String mStringFormatBefore = "", mStringFormatAfter = "";
	private static boolean mBeforeSmall = true, mAfterSmall = true;
	private Integer mBeforeClockColor = null, mAfterClockColor = null, mClockColor = null;
	//endregion

	//region vibration icon
	private static boolean showVibrationIcon = false;
	//endregion

	//region network traffic
	private FrameLayout NTQSHolder = null;
	private static boolean networkOnSBEnabled = false;
	private static boolean networkOnQSEnabled = false;
	private static int networkTrafficPosition = POSITION_LEFT;
	private NetworkTraffic networkTrafficSB = null;
	private NetworkTraffic networkTrafficQS = null;
	//endregion

	//region battery bar
	private static boolean BBarEnabled;
	private static boolean BBarColorful;
	private static boolean BBOnlyWhileCharging;
	private static boolean BBOnBottom;
	private static boolean BBSetCentered;
	private static int BBOpacity = 100;
	private static int BBarHeight = 10;
	private static List<Float> batteryLevels = Arrays.asList(20f, 40f);
	private static int[] batteryColors = new int[]{Color.RED, Color.YELLOW};
	private static int chargingColor = Color.WHITE;
	private static int fastChargingColor = Color.WHITE;
	private static boolean indicateCharging = false;
	private static boolean indicateFastCharging = false;
	private static boolean BBarTransitColors = false;
	//endregion

	//region privacy chip
	private static boolean HidePrivacyChip = false;
	//endregion

	//region general use
	private static final float PADDING_DEFAULT = -0.5f;
	private static final ArrayList<ClockVisibilityCallback> clockVisibilityCallbacks = new ArrayList<>();
	private Object mActivityStarter;
	private Object KIC = null;
	private Object QSBH = null;
	private ViewGroup mStatusBar;
	private static boolean notificationAreaMultiRow = false;

	private Object mCollapsedStatusBarFragment = null;
	private ViewGroup mStatusbarStartSide = null;
	private View mCenteredIconArea = null;
	private LinearLayout mSystemIconArea = null;
	public static int clockColor = 0;
	private FrameLayout fullStatusbar;
	//    private Object STB = null;

	private View mClockView;
	@SuppressWarnings("FieldCanBeLocal")
	private ViewGroup mNotificationIconContainer = null;
	LinearLayout mNotificationContainerContainer;
	private LinearLayout mLeftVerticalSplitContainer;
	private LinearLayout mLeftExtraRowContainer;
	private static float SBPaddingStart = 0, SBPaddingEnd = 0;
	private Object PSBV;

	//endregion

	//region volte
	private static final int VOLTE_AVAILABLE = 2;
	private static final int VOLTE_NOT_AVAILABLE = 1;
	private static final int VOLTE_UNKNOWN = -1;

	private static boolean VolteIconEnabled = false;
	private final Executor volteExec = Runnable::run;

	private Object mStatusBarIconController;
	private Class<?> StatusBarIconClass;
	private Class<?> StatusBarIconHolderClass;
	private Object volteStatusbarIconHolder;
	private boolean telephonyCallbackRegistered = false;
	private int lastVolteState = VOLTE_UNKNOWN;
	private final serverStateCallback volteCallback = new serverStateCallback();
	//endregion

	//region combined signal icons
	private Object NetworkController;
	private boolean mWifiVisble = false;
	private Object StatusBarSignalPolicy;
	private static boolean CombineSignalIcons = false;
	//endregion

	@SuppressLint("DiscouragedApi")
	public StatusbarMods(Context context) {
		super(context);
		if (!listensTo(context.getPackageName())) return;

		rightClockPadding = mContext.getResources().getDimensionPixelSize(mContext.getResources().getIdentifier("status_bar_clock_starting_padding", "dimen", mContext.getPackageName()));
		leftClockPadding = mContext.getResources().getDimensionPixelSize(mContext.getResources().getIdentifier("status_bar_left_clock_end_padding", "dimen", mContext.getPackageName()));
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !AOSPMods.isChildProcess;
	}

	public void updatePrefs(String... Key) {
		if (Xprefs == null) return;

		HidePrivacyChip = Xprefs.getBoolean("HidePrivacyChip", false);

		CombineSignalIcons = Xprefs.getBoolean("combinedSignalEnabled", false);
		wifiVisibleChanged();

		if (Key.length > 0 && Key[0].equals("notificationAreaMultiRow")) { //WHY we check the old value? because if prefs is empty it will fill it up and count an unwanted change
			boolean newnotificationAreaMultiRow = Xprefs.getBoolean("notificationAreaMultiRow", false);
			if (newnotificationAreaMultiRow != notificationAreaMultiRow) {
				SystemUtils.RestartSystemUI();
			}
		}
		notificationAreaMultiRow = Xprefs.getBoolean("notificationAreaMultiRow", false);

		try {
			NotificationIconContainerOverride.MAX_STATIC_ICONS = Integer.parseInt(Xprefs.getString("NotificationIconLimit", "").trim());
		} catch (Throwable ignored) {
			NotificationIconContainerOverride.MAX_STATIC_ICONS = 4;
		}

		try {
			NotificationIconContainerOverride.MAX_ICONS_ON_AOD = Integer.parseInt(Xprefs.getString("NotificationAODIconLimit", "").trim());
		} catch (Throwable ignored) {
			NotificationIconContainerOverride.MAX_ICONS_ON_AOD = 3;
		}

		List<Float> paddings = RangeSliderPreference.getValues(Xprefs, "statusbarPaddings", 0);

		if (paddings.size() > 1) {
			SBPaddingStart = paddings.get(0);
			SBPaddingEnd = 100f - paddings.get(1);
		}

		//region BatteryBar Settings
		BBarEnabled = Xprefs.getBoolean("BBarEnabled", false);
		BBarColorful = Xprefs.getBoolean("BBarColorful", false);
		BBOnlyWhileCharging = Xprefs.getBoolean("BBOnlyWhileCharging", false);
		BBOnBottom = Xprefs.getBoolean("BBOnBottom", false);
		BBSetCentered = Xprefs.getBoolean("BBSetCentered", false);
		BBOpacity = Xprefs.getInt("BBOpacity", 100);
		BBarHeight = Xprefs.getInt("BBarHeight", 50);
		BBarTransitColors = Xprefs.getBoolean("BBarTransitColors", false);

		batteryLevels = RangeSliderPreference.getValues(Xprefs, "batteryWarningRange", 0);

		batteryColors = new int[]{
				Xprefs.getInt("batteryCriticalColor", Color.RED),
				Xprefs.getInt("batteryWarningColor", Color.YELLOW)};


		indicateFastCharging = Xprefs.getBoolean("indicateFastCharging", false);
		indicateCharging = Xprefs.getBoolean("indicateCharging", true);

		chargingColor = Xprefs.getInt("batteryChargingColor", Color.GREEN);
		fastChargingColor = Xprefs.getInt("batteryFastChargingColor", Color.BLUE);

		if (BBarEnabled) {
			placeBatteryBar();
		}

		if (BatteryBarView.hasInstance()) {
			refreshBatteryBar(BatteryBarView.getInstance());
		}
		//endregion BatteryBar Settings


		//region network Traffic settings
		networkOnSBEnabled = Xprefs.getBoolean("networkOnSBEnabled", false);
		networkOnQSEnabled = Xprefs.getBoolean("networkOnQSEnabled", false);
		String networkTrafficModeStr = Xprefs.getString("networkTrafficMode", "0");
		int networkTrafficMode = Integer.parseInt(networkTrafficModeStr);

		boolean networkTrafficRXTop = Xprefs.getBoolean("networkTrafficRXTop", true);
		int networkTrafficDLColor = Xprefs.getInt("networkTrafficDLColor", Color.GREEN);
		int networkTrafficULColor = Xprefs.getInt("networkTrafficULColor", Color.RED);
		int networkTrafficOpacity = Xprefs.getInt("networkTrafficOpacity", 100);
		int networkTrafficInterval = Xprefs.getInt("networkTrafficInterval", 1);
		boolean networkTrafficColorful = Xprefs.getBoolean("networkTrafficColorful", false);
		boolean networkTrafficShowIcons = Xprefs.getBoolean("networkTrafficShowIcons", true);

		if (networkOnSBEnabled || networkOnQSEnabled) {
			networkTrafficPosition = Integer.parseInt(Xprefs.getString("networkTrafficPosition", String.valueOf(POSITION_RIGHT)));
			if(networkTrafficPosition == POSITION_LEFT_EXTRA_LEVEL)
			{
				Xprefs.edit().putString("networkTrafficPosition", String.valueOf(POSITION_LEFT)).apply();
				networkTrafficPosition = POSITION_LEFT;
			}

			String thresholdText = Xprefs.getString("networkTrafficThreshold", "10");

			int networkTrafficThreshold;
			try {
				networkTrafficThreshold = Math.round(Float.parseFloat(thresholdText));
			} catch (Exception ignored) {
				networkTrafficThreshold = 10;
			}
			NetworkTraffic.setConstants(networkTrafficInterval, networkTrafficThreshold, networkTrafficMode, networkTrafficRXTop, networkTrafficColorful, networkTrafficDLColor, networkTrafficULColor, networkTrafficOpacity, networkTrafficShowIcons);

		}
		if (networkOnSBEnabled) {
			networkTrafficSB = NetworkTraffic.getInstance(mContext, true);
			networkTrafficSB.update();
		}
		if (networkOnQSEnabled) {
			networkTrafficQS = NetworkTraffic.getInstance(mContext, false);
			networkTrafficQS.update();
		}
		placeNTSB();
		placeNTQS();

		//endregion network settings

		//region vibration settings
		boolean newshowVibrationIcon = Xprefs.getBoolean("SBshowVibrationIcon", false);
		if (newshowVibrationIcon != showVibrationIcon) {
			showVibrationIcon = newshowVibrationIcon;
			setShowVibrationIcon();
		}
		//endregion


		//region clock settings

		clockPosition = Integer.parseInt(Xprefs.getString("SBClockLoc", String.valueOf(POSITION_LEFT)));
		if(clockPosition == POSITION_LEFT_EXTRA_LEVEL)
		{
			Xprefs.edit().putString("SBClockLoc", String.valueOf(POSITION_LEFT)).apply();
			clockPosition = POSITION_LEFT;
		}

		mShowSeconds = Xprefs.getBoolean("SBCShowSeconds", false);
		mAmPmStyle = Integer.parseInt(Xprefs.getString("SBCAmPmStyle", String.valueOf(AM_PM_STYLE_GONE)));

		mStringFormatBefore = Xprefs.getString("DateFormatBeforeSBC", "");
		mStringFormatAfter = Xprefs.getString("DateFormatAfterSBC", "");
		mBeforeSmall = Xprefs.getBoolean("BeforeSBCSmall", true);
		mAfterSmall = Xprefs.getBoolean("AfterSBCSmall", true);

		if (Xprefs.getBoolean("SBCClockColorful", false)) {
			mClockColor = Xprefs.getInt("SBCClockColor", Color.WHITE);
			mBeforeClockColor = Xprefs.getInt("SBCBeforeClockColor", Color.WHITE);
			mAfterClockColor = Xprefs.getInt("SBCAfterClockColor", Color.WHITE);
		} else {
			mClockColor
					= mBeforeClockColor
					= mAfterClockColor
					= null;
		}


		if ((mStringFormatBefore + mStringFormatAfter).trim().length() == 0) {
			int SBCDayOfWeekMode = Integer.parseInt(Xprefs.getString("SBCDayOfWeekMode", "0"));

			switch (SBCDayOfWeekMode) {
				case 0:
					mStringFormatAfter = mStringFormatBefore = "";
					break;
				case 1:
					mStringFormatBefore = "$GEEE ";
					mStringFormatAfter = "";
					mBeforeSmall = false;
					break;
				case 2:
					mStringFormatBefore = "$GEEE ";
					mStringFormatAfter = "";
					mBeforeSmall = true;
					break;
				case 3:
					mStringFormatBefore = "";
					mStringFormatAfter = " $GEEE";
					mAfterSmall = false;
					break;
				case 4:
					mStringFormatBefore = "";
					mStringFormatAfter = " $GEEE";
					mAfterSmall = true;
					break;
			}
		}

		try {
			placeClock();
			callMethod(mClockView, "getSmallTime");
		} catch (Throwable ignored) {
		}
		//endregion clock settings


		//region volte
		VolteIconEnabled = Xprefs.getBoolean("VolteIconEnabled", false);
		//endregion

		if (Key.length > 0) {
			switch (Key[0]) {
				case "statusbarPaddings":
					updateStatusbarHeight();
					break;
				case "VolteIconEnabled":
					if (VolteIconEnabled)
						initVolte();
					else
						removeVolte();
					break;
			}
		}

	}

	private void placeNTQS() {
		if (networkTrafficQS == null) {
			return;
		}
		try {
			((ViewGroup) networkTrafficQS.getParent()).removeView(networkTrafficQS);
		} catch (Throwable ignored) {
		}
		if (!networkOnQSEnabled) return;

		try {
			NTQSHolder.addView(networkTrafficQS);
		} catch (Throwable ignored) {
		}
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(listenPackage)) return;

		//region needed classes
		Class<?> ActivityStarterClass = findClass("com.android.systemui.plugins.ActivityStarter", lpparam.classLoader);
		Class<?> DependencyClass = findClass("com.android.systemui.Dependency", lpparam.classLoader);
		Class<?> KeyguardStatusBarViewControllerClass = findClass("com.android.systemui.statusbar.phone.KeyguardStatusBarViewController", lpparam.classLoader);
//        Class<?> QuickStatusBarHeaderControllerClass = findClass("com.android.systemui.qs.QuickStatusBarHeaderController", lpparam.classLoader);
		Class<?> QuickStatusBarHeaderClass = findClass("com.android.systemui.qs.QuickStatusBarHeader", lpparam.classLoader);
		Class<?> ClockClass = findClass("com.android.systemui.statusbar.policy.Clock", lpparam.classLoader);
		Class<?> PhoneStatusBarViewClass = findClass("com.android.systemui.statusbar.phone.PhoneStatusBarView", lpparam.classLoader);
		Class<?> KeyGuardIndicationClass = findClass("com.android.systemui.statusbar.KeyguardIndicationController", lpparam.classLoader);
		Class<?> BatteryTrackerClass = findClass("com.android.systemui.statusbar.KeyguardIndicationController$BaseKeyguardCallback", lpparam.classLoader);
		Class<?> NotificationIconContainerClass = findClass("com.android.systemui.statusbar.phone.NotificationIconContainer", lpparam.classLoader);
		NotificationIconContainerOverride.StatusBarIconViewClass = findClass("com.android.systemui.statusbar.StatusBarIconView", lpparam.classLoader);
		Class<?> CollapsedStatusBarFragmentClass = findClassIfExists("com.android.systemui.statusbar.phone.fragment.CollapsedStatusBarFragment", lpparam.classLoader);
		Class<?> StatusBarSignalPolicyClass = findClass("com.android.systemui.statusbar.phone.StatusBarSignalPolicy", lpparam.classLoader);
		Class<?> NetworkControllerImplClass = findClass("com.android.systemui.statusbar.connectivity.NetworkControllerImpl", lpparam.classLoader);
		Class<?> PrivacyItemControllerClass = findClass("com.android.systemui.privacy.PrivacyItemController", lpparam.classLoader);
		StatusBarIconClass = findClass("com.android.internal.statusbar.StatusBarIcon", lpparam.classLoader);
		StatusBarIconHolderClass = findClass("com.android.systemui.statusbar.phone.StatusBarIconHolder", lpparam.classLoader);
		SettingsLibUtils.init(lpparam.classLoader);
//		Method setMeasuredDimensionMethod = findMethodExact(View.class, "setMeasuredDimension", int.class, int.class);
		//endregion

		//region combined signal icons
		hookAllConstructors(NetworkControllerImplClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				NetworkController = param.thisObject;
			}
		});

		hookAllConstructors(StatusBarSignalPolicyClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				StatusBarSignalPolicy = param.thisObject;
			}
		});
		hookAllMethods(StatusBarSignalPolicyClass, "setWifiIndicators", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				boolean wifiVisible = getBooleanField(getObjectField(param.args[0], "statusIcon"), "visible");
				if(wifiVisible != mWifiVisble)
				{
					mWifiVisble = wifiVisible;
					if(CombineSignalIcons) {
						wifiVisibleChanged();
					}
				}
			}
		});
		//endregion

		//region privacy chip
		hookAllConstructors(PrivacyItemControllerClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				hookAllMethods(getObjectField(param.thisObject, "notifyChanges").getClass(), "run", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param1) throws Throwable {
						if(HidePrivacyChip)
						{
							try { //It's sometimes a readonly collection
								((List<?>) getObjectField(param.thisObject, "privacyList"))
										.clear();
							}
							catch (Throwable ignored){}
						}
					}
				});
			}
		});
		//endregion

		//region SB Padding
		hookAllConstructors(PhoneStatusBarViewClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				PSBV = param.thisObject;
			}
		});

		hookAllMethods(PhoneStatusBarViewClass, "updateStatusBarHeight", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				@SuppressLint("DiscouragedApi") View sbContentsView = ((View) param.thisObject).findViewById(mContext.getResources().getIdentifier("status_bar_contents", "id", listenPackage));

				if (SBPaddingStart == PADDING_DEFAULT && SBPaddingEnd == PADDING_DEFAULT)
					return;

				int screenWidth = mContext.getResources().getDisplayMetrics().widthPixels;

				int paddingStart = SBPaddingStart == PADDING_DEFAULT
						? sbContentsView.getPaddingStart()
						: Math.round(SBPaddingStart * screenWidth / 100f);

				int paddingEnd = SBPaddingEnd == PADDING_DEFAULT
						? sbContentsView.getPaddingEnd()
						: Math.round(SBPaddingEnd * screenWidth / 100f);

				sbContentsView.setPaddingRelative(paddingStart, sbContentsView.getPaddingTop(), paddingEnd, sbContentsView.getPaddingBottom());
			}
		});
		//endregion

		//region multi row statusbar
		//bypassing the max icon limit during measurement
		hookAllMethods(NotificationIconContainerClass, "onMeasure", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				setObjectField(param.thisObject, "mIsStaticLayout", false);
			}
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				setObjectField(param.thisObject, "mIsStaticLayout", true);
			}
		});

		hookAllMethods(NotificationIconContainerClass, "calculateIconXTranslations", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				NotificationIconContainerOverride.calculateIconXTranslations(param);
				param.setResult(null);
			}
		});

		//endregion

		// needed to check fastcharging
		hookAllConstructors(KeyGuardIndicationClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				KIC = param.thisObject;
			}
		});

		//setting charing status for batterybar and batteryicon
		hookAllMethods(BatteryTrackerClass, "onRefreshBatteryInfo", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				int mChargingSpeed = getIntField(KIC, "mChargingSpeed");
				if (mChargingSpeed == CHARGING_FAST) {
					BatteryBarView.setIsFastCharging(true);
					BatteryStyleManager.setIsFastCharging(true);
				} else {
					BatteryBarView.setIsFastCharging(false);
					BatteryStyleManager.setIsFastCharging(false);
				}
			}
		});

		//getting statusbar class for further use
		hookAllConstructors(CollapsedStatusBarFragmentClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				mCollapsedStatusBarFragment = param.thisObject;
			}
		});

		//update statusbar
		hookAllMethods(PhoneStatusBarViewClass, "onConfigurationChanged", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				new Timer().schedule(new TimerTask() {
					@Override
					public void run() {
						if (BatteryBarView.hasInstance()) {
							BatteryBarView.getInstance().post(() -> refreshBatteryBar(BatteryBarView.getInstance()));
						}
					}
				}, 2000);
			}
		});

		//getting activitity starter for further use
		hookAllConstructors(QuickStatusBarHeaderClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				QSBH = param.thisObject;
				NTQSHolder = new FrameLayout(mContext);
				FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
				lp.gravity = Gravity.CENTER_HORIZONTAL;
				NTQSHolder.setLayoutParams(lp);
				((FrameLayout) QSBH).addView(NTQSHolder);
				mActivityStarter = callStaticMethod(DependencyClass, "get", ActivityStarterClass);
				placeNTQS();
			}
		});

		final ClickListener clickListener = new ClickListener();

		//marking clock instances for recognition and setting click actions on some icons
		hookAllMethods(QuickStatusBarHeaderClass,
				"onFinishInflate", new XC_MethodHook() {
					@SuppressLint("DiscouragedApi")
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						//Getting QS text color for Network traffic
						int fillColor = getColorAttrDefaultColor(
									mContext.getResources().getIdentifier("@android:attr/textColorPrimary", "attr", mContext.getPackageName()),
									mContext);

						NetworkTraffic.setTintColor(fillColor, false);

						try {
							//Clickable icons
							Object mBatteryRemainingIcon = getObjectField(param.thisObject, "mBatteryRemainingIcon");
							Object mDateView = getObjectField(param.thisObject, "mDateView");
							Object mClockViewQS = getObjectField(param.thisObject, "mClockView");

							callMethod(mBatteryRemainingIcon, "setOnClickListener", clickListener);
							callMethod(mClockViewQS, "setOnClickListener", clickListener);
							callMethod(mClockViewQS, "setOnLongClickListener", clickListener);
							callMethod(mDateView, "setOnClickListener", clickListener);
							callMethod(mDateView, "setOnLongClickListener", clickListener);
						} catch (Throwable ignored) {}
					}
				});

		try
		{
			//QPR3
			Class<?> ShadeHeaderControllerClass = findClassIfExists("com.android.systemui.shade.ShadeHeaderController", lpparam.classLoader);

			if(ShadeHeaderControllerClass == null) //QPR2
			{
				ShadeHeaderControllerClass = findClass("com.android.systemui.shade.LargeScreenShadeHeaderController", lpparam.classLoader);
			}

			hookAllMethods(ShadeHeaderControllerClass, "onInit", new XC_MethodHook() {
				@SuppressLint("DiscouragedApi")
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					View mView = (View) getObjectField(param.thisObject, "mView");

					mView.findViewById(mContext.getResources().getIdentifier("clock", "id", mContext.getPackageName())).setOnClickListener(clickListener);
					mView.findViewById(mContext.getResources().getIdentifier("clock", "id", mContext.getPackageName())).setOnLongClickListener(clickListener);
					mView.findViewById(mContext.getResources().getIdentifier("date", "id", mContext.getPackageName())).setOnClickListener(clickListener);
					mView.findViewById(mContext.getResources().getIdentifier("batteryRemainingIcon", "id", mContext.getPackageName())).setOnClickListener(clickListener);
				}
			});

		}
		catch (Throwable ignored){}

		//show/hide vibration icon from system icons
		hookAllConstructors(KeyguardStatusBarViewControllerClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				//Removing vibration icon from blocked icons in lockscreen
				if (showVibrationIcon && (findFieldIfExists(KeyguardStatusBarViewControllerClass, "mBlockedIcons") != null)) { //Android 12 doesn't have such thing at all
					@SuppressWarnings("unchecked") List<String> OldmBlockedIcons = (List<String>) getObjectField(param.thisObject, "mBlockedIcons");

					List<String> NewmBlockedIcons = new ArrayList<>();
					for (String item : OldmBlockedIcons) {
						if (!item.equals("volume")) {
							NewmBlockedIcons.add(item);
						}
					}
					setObjectField(param.thisObject, "mBlockedIcons", NewmBlockedIcons);
				}
			}
		});

		//restoring batterybar and network traffic: when clock goes back to life
		hookAllMethods(CollapsedStatusBarFragmentClass,
				"animateShow", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						if (param.args[0] != mClockView) return;
						for (ClockVisibilityCallback c : clockVisibilityCallbacks) {
							try {
								c.OnVisibilityChanged(true);
							} catch (Exception ignored) {
							}
						}
					}
				});

		hookAllMethods(CollapsedStatusBarFragmentClass,
				"animateHiddenState", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						if (param.args[0] != mClockView) return;
						for (ClockVisibilityCallback c : clockVisibilityCallbacks) {
							try {
								c.OnVisibilityChanged(false);
							} catch (Exception ignored) {
							}
						}

					}
				});

		//modding clock, adding additional objects,
		findAndHookMethod(CollapsedStatusBarFragmentClass,
				"onViewCreated", View.class, Bundle.class, new XC_MethodHook() {
					@SuppressLint("DiscouragedApi")
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {

						mStatusBarIconController = getObjectField(param.thisObject, "mStatusBarIconController");

						try {
							mClockView = (View) getObjectField(param.thisObject, "mClockView");
						} catch (Throwable t) { //PE Plus
							Object mClockController = getObjectField(param.thisObject, "mClockController");
							mClockView = (View) callMethod(mClockController, "getClock");
						}

						mStatusBar = (ViewGroup) getObjectField(mCollapsedStatusBarFragment, "mStatusBar");

						mStatusBar.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> setHeights());

						mStatusbarStartSide = mStatusBar.findViewById(mContext.getResources().getIdentifier("status_bar_start_side_except_heads_up", "id", mContext.getPackageName()));

						mSystemIconArea = mStatusBar.findViewById(mContext.getResources().getIdentifier("statusIcons", "id", mContext.getPackageName()));

						fullStatusbar = (FrameLayout) mStatusBar.getParent();

						try {
							mCenteredIconArea = (View) ((View) getObjectField(param.thisObject, "mCenteredIconArea")).getParent();
						} catch (Throwable ignored) {
							mCenteredIconArea = new LinearLayout(mContext);
							FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT);
							lp.gravity = Gravity.CENTER;
							mCenteredIconArea.setLayoutParams(lp);
							mStatusBar.addView(mCenteredIconArea);
						}

						makeLeftSplitArea();

						if (BBarEnabled) //in case we got the config but view wasn't ready yet
						{
							placeBatteryBar();
						}

						if (VolteIconEnabled) //in case we got the config but context wasn't ready yet
						{
							initVolte();
						}

						if (networkOnSBEnabled) {
							networkTrafficSB = NetworkTraffic.getInstance(mContext, true);
							placeNTSB();
						}

						//<Showing vibration icon in collapsed statusbar>
						if (showVibrationIcon) {
							setShowVibrationIcon();
						}
						//</Showing vibration icon in collapsed statusbar>

						//<modding clock>
						placeClock();

						if(mNotificationIconContainer.getChildCount() == 0)
						{
							mNotificationContainerContainer.setVisibility(GONE);
						}
						setHeights();
					}
				});

		//clock mods

		findAndHookMethod(ClockClass,
				"getSmallTime", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						setObjectField(param.thisObject, "mAmPmStyle", AM_PM_STYLE_GONE);
						setObjectField(param.thisObject, "mShowSeconds", mShowSeconds);
					}

					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						if (param.thisObject != mClockView)
							return; //We don't want custom format in QS header. do we?

						SpannableStringBuilder result = new SpannableStringBuilder();
						result.append(getFormattedString(mStringFormatBefore, mBeforeSmall, mBeforeClockColor)); //before clock
						SpannableStringBuilder clockText = SpannableStringBuilder.valueOf((CharSequence) param.getResult()); //THE clock
						if (mClockColor != null) {
							clockText.setSpan(new NetworkTraffic.trafficStyle(mClockColor), 0, (clockText).length(),
									Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
						}
						result.append(clockText);
						if (mAmPmStyle != AM_PM_STYLE_GONE) {
							result.append(getFormattedString("$Ga", mAmPmStyle == AM_PM_STYLE_SMALL, mClockColor));
						}
						result.append(getFormattedString(mStringFormatAfter, mAfterSmall, mAfterClockColor)); //after clock
						param.setResult(result);
					}
				});

		//using clock colors for network traffic and battery bar
		hookAllMethods(ClockClass,
				"onDarkChanged", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						if (param.thisObject != mClockView)
							return; //We don't want colors of QS header. only statusbar

						clockColor = ((TextView) param.thisObject).getTextColors().getDefaultColor();
						NetworkTraffic.setTintColor(clockColor, true);
						if (BatteryBarView.hasInstance()) {
							refreshBatteryBar(BatteryBarView.getInstance());
						}
					}
				});
	}

	private void updateStatusbarHeight() {
		try {
			callMethod(PSBV, "updateStatusBarHeight");
		} catch (Throwable ignored) {}
	}

	//region double row left area
	@SuppressLint("DiscouragedApi")
	private void makeLeftSplitArea() {
		mNotificationIconContainer = mStatusBar.findViewById(mContext.getResources().getIdentifier("notificationIcons", "id", mContext.getPackageName()));

		mNotificationContainerContainer = new LinearLayout(mContext);

		if (mLeftVerticalSplitContainer == null) {
			mLeftVerticalSplitContainer = new LinearLayout(mContext);
		} else {
			mLeftVerticalSplitContainer.removeAllViews();
			if (mLeftVerticalSplitContainer.getParent() != null)
				((ViewGroup) mLeftVerticalSplitContainer.getParent()).removeView(mLeftVerticalSplitContainer);
		}

		mLeftVerticalSplitContainer.setOrientation(VERTICAL);
		mLeftVerticalSplitContainer.setLayoutParams(new LinearLayoutCompat.LayoutParams(MATCH_PARENT, MATCH_PARENT));
		mLeftVerticalSplitContainer.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> setHeights());

		LayoutTransition layoutTransition = new LayoutTransition();
		layoutTransition.enableTransitionType(LayoutTransition.CHANGING);
		layoutTransition.setDuration(200);
		mLeftVerticalSplitContainer.setLayoutTransition(layoutTransition);

		mLeftExtraRowContainer = new ShyLinearLayout(mContext);
		mLeftVerticalSplitContainer.addView(mLeftExtraRowContainer, 0);

		ViewGroup parent = (ViewGroup) mNotificationIconContainer.getParent();

		parent.addView(mLeftVerticalSplitContainer, parent.indexOfChild(mNotificationIconContainer));
		parent.removeView(mNotificationIconContainer);
		mLeftVerticalSplitContainer.addView(mNotificationContainerContainer);
		mNotificationContainerContainer.addView(mNotificationIconContainer);

		mNotificationIconContainer.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
			@Override
			public void onChildViewAdded(View parent, View child) {
				mNotificationContainerContainer.setVisibility(VISIBLE);
				setHeights();
			}

			@Override
			public void onChildViewRemoved(View parent, View child) {
				if(mNotificationIconContainer.getChildCount() == 0)
				{
					mNotificationContainerContainer.setVisibility(GONE);
					setHeights();
				}
			}
		});

		((View)mStatusbarStartSide.getParent()).getLayoutParams().height = MATCH_PARENT;
		mStatusbarStartSide.getLayoutParams().height = MATCH_PARENT;
		mLeftVerticalSplitContainer.getLayoutParams().height = MATCH_PARENT;
	}

	private void setHeights() {
		Resources res = mContext.getResources();
		@SuppressLint("DiscouragedApi") int statusbarHeight = mStatusBar.getLayoutParams().height - res.getDimensionPixelSize(res.getIdentifier("status_bar_padding_top", "dimen", mContext.getPackageName()));

		mNotificationContainerContainer.getLayoutParams().height = (mLeftExtraRowContainer.getVisibility() == VISIBLE) ? statusbarHeight / 2 : MATCH_PARENT;
		mLeftExtraRowContainer.getLayoutParams().height = ((mNotificationContainerContainer.getVisibility() == VISIBLE) ? statusbarHeight / 2 : MATCH_PARENT);
		if (networkOnSBEnabled) {
			networkTrafficSB.getLayoutParams().height = statusbarHeight / ((networkTrafficPosition == POSITION_LEFT && notificationAreaMultiRow) ?  2 : 1);
		}
	}
	//end region

	//region battery bar related
	private void refreshBatteryBar(BatteryBarView instance) {
		BatteryBarView.setStaticColor(batteryLevels, batteryColors, indicateCharging, chargingColor, indicateFastCharging, fastChargingColor, BBarTransitColors);
		instance.setVisibility((BBarEnabled) ? VISIBLE : GONE);
		instance.setColorful(BBarColorful);
		instance.setOnlyWhileCharging(BBOnlyWhileCharging);
		instance.setOnTop(!BBOnBottom);
		instance.setSingleColorTone(clockColor);
		instance.setAlphaPct(BBOpacity);
		instance.setBarHeight(Math.round(BBarHeight / 10f) + 5);
		instance.setCenterBased(BBSetCentered);
		instance.refreshLayout();
	}

	private void placeBatteryBar() {
		try {
			BatteryBarView batteryBarView = BatteryBarView.getInstance(mContext);
			try {
				((ViewGroup) batteryBarView.getParent()).removeView(batteryBarView);
			} catch (Throwable ignored) {
			}
			fullStatusbar.addView(batteryBarView);
			refreshBatteryBar(BatteryBarView.getInstance());
		} catch (Throwable ignored) {
		}
	}
	//endregion

	//region volte related
	private void initVolte() {
		try {
			if (!telephonyCallbackRegistered) {
				Icon volteIcon = Icon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.ic_volte);
				//noinspection JavaReflectionMemberAccess
				Object volteStatusbarIcon = StatusBarIconClass.getDeclaredConstructor(UserHandle.class, String.class, Icon.class, int.class, int.class, CharSequence.class).newInstance(UserHandle.class.getDeclaredConstructor(int.class).newInstance(0), BuildConfig.APPLICATION_ID, volteIcon, 0, 0, "volte");
				volteStatusbarIconHolder = StatusBarIconHolderClass.newInstance();
				setObjectField(volteStatusbarIconHolder, "mIcon", volteStatusbarIcon);
				SystemUtils.TelephonyManager().registerTelephonyCallback(volteExec, volteCallback);
				telephonyCallbackRegistered = true;
			}
		} catch (Exception ignored) {
		}

		updateVolte(true);
	}

	private void removeVolte() {
		try {
			SystemUtils.TelephonyManager().unregisterTelephonyCallback(volteCallback);
			telephonyCallbackRegistered = false;
		} catch (Exception ignored) {
		}
		removeVolteIcon();
	}

	private class serverStateCallback extends TelephonyCallback implements
			TelephonyCallback.ServiceStateListener {
		@Override
		public void onServiceStateChanged(@NonNull ServiceState serviceState) {
			updateVolte(false);
		}
	}

	private void updateVolte(boolean force) {
		int newVolteState = (Boolean) callMethod(SystemUtils.TelephonyManager(), "isVolteAvailable") ? VOLTE_AVAILABLE : VOLTE_NOT_AVAILABLE;
		if (lastVolteState != newVolteState || force) {
			lastVolteState = newVolteState;
			switch (newVolteState) {
				case VOLTE_AVAILABLE:
					mStatusBar.post(() -> {
						try {
							callMethod(mStatusBarIconController, "setIcon", "volte", volteStatusbarIconHolder);
						} catch (Exception ignored) {}
					});
					break;
				case VOLTE_NOT_AVAILABLE:
					removeVolteIcon();
					break;
			}
		}
	}

	private void removeVolteIcon() {
		if (mStatusBar == null) return; //probably it's too soon to have a statusbar
		mStatusBar.post(() -> {
			try {
				callMethod(mStatusBarIconController, "removeAllIconsForSlot", "volte");
			} catch (Throwable ignored) {}
		});
	}
	//endregion

	//region vibrationicon related
	private void setShowVibrationIcon() {
		try {
			@SuppressWarnings("unchecked") List<String> mBlockedIcons = (List<String>) getObjectField(mCollapsedStatusBarFragment, "mBlockedIcons");
			Object mStatusBarIconController = getObjectField(mCollapsedStatusBarFragment, "mStatusBarIconController");
			Object mDarkIconManager = getObjectField(mCollapsedStatusBarFragment, "mDarkIconManager");

			if (showVibrationIcon) {
				mBlockedIcons.remove("volume");
			} else {
				mBlockedIcons.add("volume");
			}
			callMethod(mDarkIconManager, "setBlockList", mBlockedIcons);
			callMethod(mStatusBarIconController, "refreshIconGroups");
		} catch (Throwable ignored) {
		}
	}
	//endregion

	//region network traffic related
	private void placeNTSB() {
		if (networkTrafficSB == null) {
			return;
		}
		try {
			((ViewGroup) networkTrafficSB.getParent()).removeView(networkTrafficSB);
		} catch (Exception ignored) {
		}
		if (!networkOnSBEnabled) return;

		try {
			LinearLayout.LayoutParams ntsbLayoutP;
			switch (networkTrafficPosition) {
				case POSITION_RIGHT:
					((ViewGroup)mSystemIconArea.getParent()).addView(networkTrafficSB, 0);
					networkTrafficSB.setPadding(rightClockPadding, 0, leftClockPadding, 0);
					break;
				case POSITION_LEFT:
					if(notificationAreaMultiRow)
					{
						mLeftExtraRowContainer.addView(networkTrafficSB, mLeftExtraRowContainer.getChildCount());
					}
					else
					{
						mStatusbarStartSide.addView(networkTrafficSB, 1);
					}
					networkTrafficSB.setPadding(0, 0, leftClockPadding, 0);
					break;
				case POSITION_CENTER:
					mStatusbarStartSide.addView(networkTrafficSB);
					networkTrafficSB.setPadding(rightClockPadding, 0, leftClockPadding, 0);
					break;
			}
			ntsbLayoutP = (LinearLayout.LayoutParams) networkTrafficSB.getLayoutParams();
			ntsbLayoutP.gravity = Gravity.CENTER_VERTICAL;
			networkTrafficSB.setLayoutParams(ntsbLayoutP);
		} catch (Throwable ignored) {
		}
	}
	//endregion

	//region icon tap related
	class ClickListener implements View.OnClickListener, View.OnLongClickListener {
		public ClickListener() {}

		@Override
		public void onClick(View v) {
			String name = mContext.getResources().getResourceName(v.getId());

			if(name.endsWith("clock"))
			{
				callMethod(mActivityStarter, "postStartActivityDismissingKeyguard", new Intent(AlarmClock.ACTION_SHOW_ALARMS), 0);
			}
			else if (name.endsWith("date"))
			{
				Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
				builder.appendPath("time");
				builder.appendPath(Long.toString(java.lang.System.currentTimeMillis()));
				Intent todayIntent = new Intent(Intent.ACTION_VIEW, builder.build());
				callMethod(mActivityStarter, "postStartActivityDismissingKeyguard", todayIntent, 0);
			}
			else if (name.endsWith("batteryRemainingIcon")) {
				callMethod(mActivityStarter, "postStartActivityDismissingKeyguard", new Intent(Intent.ACTION_POWER_USAGE_SUMMARY), 0);
			}
		}

		@Override
		public boolean onLongClick(View v) {
			String name = mContext.getResources().getResourceName(v.getId());

			if(name.endsWith("clock") || name.endsWith("date"))
			{
				Intent mIntent = new Intent(Intent.ACTION_MAIN);
				mIntent.setClassName("com.android.settings",
						"com.android.settings.Settings$DateTimeSettingsActivity");
				callMethod(mActivityStarter, "startActivity", mIntent, true /* dismissShade */);
				return true;
			}
			return false;
		}
	}
	//endregion

	//region clock and date related
	private void placeClock() {
		ViewGroup parent = (ViewGroup) mClockView.getParent();
		ViewGroup targetArea = null;
		Integer index = null;

		switch (clockPosition) {
			case POSITION_LEFT:
				if(notificationAreaMultiRow)
				{
					targetArea = mLeftExtraRowContainer;
					index = 0;
				}
				else
				{
					targetArea = mStatusbarStartSide;
					index = 1;
				}
				mClockView.setPadding(0, 0, leftClockPadding, 0);
				break;
			case POSITION_CENTER:
				targetArea = (ViewGroup) mCenteredIconArea;
				mClockView.setPadding(rightClockPadding, 0, rightClockPadding, 0);
				break;
			case POSITION_RIGHT:
				mClockView.setPadding(rightClockPadding, 0, 0, 0);
				targetArea = ((ViewGroup) mSystemIconArea.getParent());
				break;
		}
		parent.removeView(mClockView);
		if (index != null) {
			targetArea.addView(mClockView, index);
		} else {
			targetArea.addView(mClockView);
		}
	}

	private final StringFormatter stringFormatter = new StringFormatter();

	private CharSequence getFormattedString(String dateFormat, boolean small, @Nullable @ColorInt Integer textColor) {
		if (dateFormat.length() == 0) return "";

		//There's some format to work on
		SpannableStringBuilder formatted = new SpannableStringBuilder(stringFormatter.formatString(dateFormat));

		if (small) {
			//small size requested
			CharacterStyle style = new RelativeSizeSpan(0.7f);
			formatted.setSpan(style, 0, formatted.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		if (textColor != null) {
			formatted.setSpan(new NetworkTraffic.trafficStyle(textColor), 0, (formatted).length(),
					Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		return formatted;
	}

	//endregion
	//region callbacks
	public static void registerClockVisibilityCallback(ClockVisibilityCallback callback) {
		clockVisibilityCallbacks.add(callback);
	}

	@SuppressWarnings("unused")
	public static void unRegisterClockVisibilityCallback(ClockVisibilityCallback callback) {
		clockVisibilityCallbacks.remove(callback);
	}

	public interface ClockVisibilityCallback extends Callback {
		void OnVisibilityChanged(boolean isVisible);
	}
	//endregion

	//region combined signal icons
	private void wifiVisibleChanged()
	{
		try
		{
			setObjectField(StatusBarSignalPolicy, "mHideMobile", mWifiVisble && CombineSignalIcons);

			SparseArray<?> mMobileSignalControllers = (SparseArray<?>) getObjectField(NetworkController, "mMobileSignalControllers");

			for (int i = 0; i < mMobileSignalControllers.size(); i++) {
				callMethod(mMobileSignalControllers.get(mMobileSignalControllers.keyAt(i)), "notifyListeners", StatusBarSignalPolicy);
			}
		}
		catch (Throwable ignored){}
	}
	//endregion
}
