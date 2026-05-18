package sh.siava.pixelxpert.xposed.modpacks.systemui;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.widget.LinearLayout.VERTICAL;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.xposed.XPrefs.Xprefs;
import static sh.siava.pixelxpert.xposed.utils.SystemUtils.dimenIdOf;
import static sh.siava.pixelxpert.xposed.utils.SystemUtils.idOf;
import static sh.siava.pixelxpert.xposed.utils.SystemUtils.resourceIdOf;
import static sh.siava.pixelxpert.xposed.utils.toolkit.ObjectTools.getStateFlowImplOf;
import static sh.siava.pixelxpert.xposed.utils.reflection.ReflectionTools.reAddView;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.telephony.ServiceState;
import android.telephony.TelephonyCallback;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.CharacterStyle;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.objenesis.ObjenesisHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;

import javax.security.auth.callback.Callback;

import io.github.libxposed.api.XposedModuleInterface;
import sh.siava.pixelxpert.BuildConfig;
import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.xposed.Constants;
import sh.siava.pixelxpert.xposed.XposedModPack;
import sh.siava.pixelxpert.xposed.annotations.SystemUIModPack;
import sh.siava.pixelxpert.xposed.utils.NetworkTraffic;
import sh.siava.pixelxpert.xposed.utils.ShyLinearLayout;
import sh.siava.pixelxpert.xposed.utils.StringFormatter;
import sh.siava.pixelxpert.xposed.utils.StringFormatter.FormattedStringCallback;
import sh.siava.pixelxpert.xposed.utils.SystemUtils;
import sh.siava.pixelxpert.xposed.utils.batteryStyles.BatteryBarView;
import sh.siava.pixelxpert.xposed.utils.reflection.ReflectedClass;
import sh.siava.pixelxpert.xposed.utils.toolkit.ResourceTools;

/**
 * @noinspection v
 */
@SystemUIModPack
public class StatusbarMods extends XposedModPack {
	//region Clock
	public static final int POSITION_LEFT = 0;
	public static final int POSITION_CENTER = 1;
	public static final int POSITION_RIGHT = 2;
	public static final int POSITION_LEFT_EXTRA_LEVEL = 3;

	private static final int AM_PM_STYLE_SMALL = 1;
	private static final int AM_PM_STYLE_GONE = 2;
	private static final String ORIGINAL_STATUS_BAR_CONTENTS_TOP_PADDING = "px_original_status_bar_contents_top_padding";
	private final int leftClockPadding, rightClockPadding;
	private static int clockPosition = POSITION_LEFT;
	private static int mAmPmStyle = AM_PM_STYLE_GONE;
	private static boolean mShowSeconds = false;
	private static String mStringFormatBefore = "", mStringFormatAfter = "";
	private static boolean mBeforeSmall = true, mAfterSmall = true;
	private Integer mBeforeClockColor = null, mAfterClockColor = null, clockColor = null;
	//endregion

	//region network traffic
	private static boolean networkOnSBEnabled = false;
	private static int networkTrafficPosition = POSITION_LEFT;
	private NetworkTraffic networkTrafficSB = null;
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
	private static int powerSaveColor = Color.parseColor("#FFBF00");
	private static boolean indicateCharging = false;
	private static boolean indicateFastCharging = false;
	private static boolean indicatePowerSave = false;
	private static boolean BBarTransitColors = false;
	private static boolean BBAnimateCharging = false;
	//endregion

	//region privacy chip
	private static boolean HidePrivacyChip = false; //works
	//endregion

	//region general use
	private static final float PADDING_DEFAULT = -0.5f;
	private static final ArrayList<ClockVisibilityCallback> clockVisibilityCallbacks = new ArrayList<>();
	private Object mActivityStarter;
	private static boolean notificationAreaMultiRow = false;
	private static boolean noCutoutEnabled = false;
	private static int statusbarHeightFactor = 100;
	private static int NotificationAODIconLimit = 3;
	private static int NotificationIconLimit = 4;
	private Object AODNIC;
	private Object SBNIC;
	private ViewGroup mStatusbarStartSide = null;
	private View mCenteredIconArea = null;
	private LinearLayout mSystemIconArea = null;
	private static int currentClockColor = 0;
	private static final ArrayList<StatusbarTextColorCallback> mTextColorCallbacks = new ArrayList<>();
	//    private Object STB = null;

	private TextView mClockView;
	private ViewGroup mNotificationIconContainer = null;
	private ViewGroup mNotificationIconArea = null;
	LinearLayout mNotificationContainerContainer;
	private LinearLayout mLeftVerticalSplitContainer;
	private LinearLayout mLeftExtraRowContainer;
	private static float SBPaddingStart = 0, SBPaddingEnd = 0;
	private FrameLayout mPhoneStatusbarView;
	private View mStatusBarContents;
	private final Runnable mSetHeightsRunnable = this::setHeights;

	//endregion

	//region vo_data
	private static final String VO_LTE_SLOT = "volte";
	private static final String VO_WIFI_SLOT = "vowifi";

	private static boolean VolteIconEnabled = false; //works
	private final Executor voDataExec = Runnable::run;

	private Object mStatusBarIconController;

	private ReflectedClass StatusBarIconClass;
	private ReflectedClass StatusBarIconHolderClass;
	private Object volteStatusbarIconHolder;
	private boolean telephonyCallbackRegistered = false;
	private boolean lastVolteAvailable = false;
	private final serverStateCallback voDataCallback = new serverStateCallback();
	//endregion

	private static boolean VowifiIconEnabled = false;
	private Object vowifiStatusbarIconHolder;
	private boolean lastVowifiAvailable = false;
	//endregion

	//region combined signal icons
	private boolean mWifiVisible = false;
	private static boolean CombineSignalIcons = false;
	private static boolean HideRoamingState = false;
	private Object mTunerService;
	public static final String ICON_HIDE_LIST = "icon_blacklist";
	//endregion
	//region app profile switch
	public static final String APP_SWITCH_SLOT = "app_switch";
	private Object mAppSwitchStatusbarIconHolder = null;

	private static boolean StatusbarAppSwitchIconEnabled = false; //works

	private final BroadcastReceiver mAppProfileSwitchReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (Constants.ACTION_PROFILE_SWITCH_AVAILABLE.equals(intent.getAction())) {
				boolean isAvailable = intent.getBooleanExtra("available", false);
				if (isAvailable
						&& StatusbarAppSwitchIconEnabled
						&& mStatusBarIconController != null) {
					callMethod(mStatusBarIconController, "setIcon", APP_SWITCH_SLOT, mAppSwitchStatusbarIconHolder);
				} else {
					removeSBIconSlot(APP_SWITCH_SLOT);
				}
			}
		}
	};
	//endregion

	@SuppressLint("DiscouragedApi")
	public StatusbarMods(Context context) {
		super(context);
		rightClockPadding = mContext.getResources().getDimensionPixelSize(dimenIdOf("status_bar_clock_starting_padding"));
		leftClockPadding = mContext.getResources().getDimensionPixelSize(dimenIdOf("status_bar_left_clock_end_padding"));
	}

	private void initSwitchIcon() {
		try {
			Icon appSwitchIcon = Icon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.ic_app_switch);

			Object appSwitchStatusbarIcon = getStatusbarIconFor(appSwitchIcon, APP_SWITCH_SLOT);

			mAppSwitchStatusbarIconHolder = getStatusbarIconHolderFor(appSwitchStatusbarIcon);
		} catch (Throwable ignored) {
		}
	}

	public void onPreferenceUpdated(String... Key) {
		if (Xprefs == null) return;

		StatusbarAppSwitchIconEnabled = Xprefs.getBoolean("StatusbarAppSwitchIconEnabled", false);

		HidePrivacyChip = Xprefs.getBoolean("HidePrivacyChip", false);

		HideRoamingState = Xprefs.getBoolean("HideRoamingState", false);

		CombineSignalIcons = Xprefs.getBoolean("combinedSignalEnabled", false);
		wifiVisibleChanged();

		if (Key.length > 0 && Key[0].equals("notificationAreaMultiRow")) { //WHY we check the old value? because if prefs is empty it will fill it up and count an unwanted change
			boolean newnotificationAreaMultiRow = Xprefs.getBoolean("notificationAreaMultiRow", false);
			if (newnotificationAreaMultiRow != notificationAreaMultiRow) {
				SystemUtils.killSelf();
			}
		}
		notificationAreaMultiRow = Xprefs.getBoolean("notificationAreaMultiRow", false);
		noCutoutEnabled = Xprefs.getBoolean("noCutoutEnabled", false);
		statusbarHeightFactor = Xprefs.getSliderInt("statusbarHeightFactor", 100);

		try {
			NotificationIconLimit = Integer.parseInt(Xprefs.getString("NotificationIconLimit", "").trim());
		} catch (Throwable ignored) {
			NotificationIconLimit = getIntegerResource("max_notif_static_icons", 4);
		}


		try {
			NotificationAODIconLimit = Integer.parseInt(Xprefs.getString("NotificationAODIconLimit", "").trim());
		} catch (Throwable ignored) {
			NotificationAODIconLimit = getIntegerResource("max_notif_icons_on_aod", 3);
		}

		if (AODNIC != null) {
			setObjectField(AODNIC, "maxIcons", NotificationAODIconLimit);
			setObjectField(SBNIC, "maxIcons", NotificationIconLimit);
		}

		List<Float> paddings = Xprefs.getSliderValues("statusbarPaddings", 0);

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
		BBOpacity = Xprefs.getSliderInt("BBOpacity", 100);
		BBarHeight = Xprefs.getSliderInt("BBarHeight", 50);
		BBarTransitColors = Xprefs.getBoolean("BBarTransitColors", false);
		BBAnimateCharging = Xprefs.getBoolean("BBAnimateCharging", false);

		batteryLevels = Xprefs.getSliderValues("batteryWarningRange", 0);

		batteryColors = new int[]{
				Xprefs.getInt("batteryCriticalColor", Color.RED),
				Xprefs.getInt("batteryWarningColor", Color.YELLOW)};


		indicateFastCharging = Xprefs.getBoolean("indicateFastCharging", false);
		indicatePowerSave = Xprefs.getBoolean("indicatePowerSave", false);
		indicateCharging = Xprefs.getBoolean("indicateCharging", true);

		chargingColor = Xprefs.getInt("batteryChargingColor", Color.GREEN);
		fastChargingColor = Xprefs.getInt("batteryFastChargingColor", Color.BLUE);
		powerSaveColor = Xprefs.getInt("batteryPowerSaveColor", Color.parseColor("#FFBF00"));

		if (BBarEnabled) {
			placeBatteryBar();
		}

		if (BatteryBarView.hasInstance()) {
			refreshBatteryBar(BatteryBarView.getInstance());
		}
		//endregion BatteryBar Settings


		//region network Traffic settings
		networkOnSBEnabled = Xprefs.getBoolean("networkOnSBEnabled", false);
		String networkTrafficModeStr = Xprefs.getString("networkTrafficMode", "0");
		int networkTrafficMode = Integer.parseInt(networkTrafficModeStr);

		boolean networkTrafficRXTop = Xprefs.getBoolean("networkTrafficRXTop", true);
		int networkTrafficDLColor = Xprefs.getInt("networkTrafficDLColor", Color.GREEN);
		int networkTrafficULColor = Xprefs.getInt("networkTrafficULColor", Color.RED);
		int networkTrafficOpacity = Xprefs.getSliderInt("networkTrafficOpacity", 100);
		int networkTrafficInterval = Xprefs.getSliderInt("networkTrafficInterval", 1);
		boolean networkTrafficColorful = Xprefs.getBoolean("networkTrafficColorful", false);
		boolean networkTrafficShowIcons = Xprefs.getBoolean("networkTrafficShowIcons", true);
		boolean networkTrafficShowInBits = Xprefs.getBoolean("networkTrafficShowInBits", false);

		if (networkOnSBEnabled) {
			networkTrafficPosition = Integer.parseInt(Xprefs.getString("networkTrafficPosition", String.valueOf(POSITION_RIGHT)));
			if (networkTrafficPosition == POSITION_LEFT_EXTRA_LEVEL) {
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
			NetworkTraffic.setConstants(networkTrafficInterval, networkTrafficThreshold, networkTrafficMode, networkTrafficRXTop, networkTrafficColorful, networkTrafficDLColor, networkTrafficULColor, networkTrafficOpacity, networkTrafficShowIcons, networkTrafficShowInBits);

		}
		if (networkOnSBEnabled) {
			networkTrafficSB = NetworkTraffic.getInstance(mContext, true);
			networkTrafficSB.update();
		}
		placeNTSB();

		//endregion network settings

		//region clock settings
		clockPosition = Integer.parseInt(Xprefs.getString("SBClockLoc", String.valueOf(POSITION_LEFT)));
		if (clockPosition == POSITION_LEFT_EXTRA_LEVEL) {
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
			clockColor = Xprefs.getInt("SBCClockColor", Color.WHITE);
			mBeforeClockColor = Xprefs.getInt("SBCBeforeClockColor", Color.WHITE);
			mAfterClockColor = Xprefs.getInt("SBCAfterClockColor", Color.WHITE);
		} else {
			clockColor
					= mBeforeClockColor
					= mAfterClockColor
					= null;
		}

		if ((mStringFormatBefore + mStringFormatAfter).trim().isEmpty()) {
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
			updateClock();
		} catch (Throwable ignored) {}
		//endregion clock settings

		//region vo_data
		VolteIconEnabled = Xprefs.getBoolean("VolteIconEnabled", false);
		VowifiIconEnabled = Xprefs.getBoolean("VowifiIconEnabled", false);
		//endregion

		if (Key.length > 0) {
			switch (Key[0]) {
				case "statusbarPaddings":
					updateStatusbarHeight();
					break;
				case "NotificationIconLimit":
					applyNotificationIconLimit();
					break;
				case "VolteIconEnabled":
				case "VowifiIconEnabled":
					if (VolteIconEnabled || VowifiIconEnabled) {
						initVoData();

						if (!VolteIconEnabled) removeSBIconSlot(VO_LTE_SLOT);
						if (!VowifiIconEnabled) removeSBIconSlot(VO_WIFI_SLOT);
					} else
						removeVoDataCallback();
					break;
			}
		}
	}

	@SuppressLint("DiscouragedApi")
	private int getIntegerResource(String resourceName, int defaultValue) {
		try {
			return mContext.getResources().getInteger(resourceIdOf(resourceName, "integer"));
		} catch (Throwable ignored) {
			return defaultValue;
		}
	}

	private void updateClock() {
		try {
			mClockView.post(() -> { //the builtin update method doesn't care about the format. Just the text sadly
				callMethod(getObjectField(mClockView, "mCalendar"), "setTimeInMillis", System.currentTimeMillis());

				mClockView.setText((CharSequence) callMethod(mClockView, "getSmallTime"));
			});
		} catch (Throwable ignored) {
		}
	}

	private void applyStatusBarContentPadding(View sbContentsView) {
		if (sbContentsView == null) return;

		Object originalTopPadding = getAdditionalInstanceField(sbContentsView, ORIGINAL_STATUS_BAR_CONTENTS_TOP_PADDING);
		if (!(originalTopPadding instanceof Integer)) {
			originalTopPadding = sbContentsView.getPaddingTop();
			setAdditionalInstanceField(sbContentsView, ORIGINAL_STATUS_BAR_CONTENTS_TOP_PADDING, originalTopPadding);
		}

		int paddingTop = (Integer) originalTopPadding;

		int screenWidth = mContext.getResources().getDisplayMetrics().widthPixels;

		int paddingStart = SBPaddingStart == PADDING_DEFAULT
				? sbContentsView.getPaddingStart()
				: Math.round(SBPaddingStart * screenWidth / 100f);

		int paddingEnd = SBPaddingEnd == PADDING_DEFAULT
				? sbContentsView.getPaddingEnd()
				: Math.round(SBPaddingEnd * screenWidth / 100f);

		sbContentsView.setPaddingRelative(paddingStart, paddingTop, paddingEnd, sbContentsView.getPaddingBottom());
	}

	private int getStatusBarContentTopInset() {
		if (!noCutoutEnabled) return 0;
		if (notificationAreaMultiRow) return 0;

		return ResourceTools.dpToPx(mContext, 6);
	}

	private int getStatusBarIconRowHeight() {
		return getAndroidDimensionPixelSize("status_bar_icon_size_sp",
				getAndroidDimensionPixelSize("status_bar_icon_size", ResourceTools.dpToPx(mContext, 22)));
	}

	private int getStatusBarIconSlotWidth() {
		int iconSize = getStatusBarIconRowHeight();
		int horizontalMargin = getSystemUiDimensionPixelSize("status_bar_icon_horizontal_margin", 0);
		return iconSize + (2 * horizontalMargin);
	}

	@SuppressLint("DiscouragedApi")
	private int getSystemUiDimensionPixelSize(String resourceName, int defaultValue) {
		try {
			int resId = dimenIdOf(resourceName);
			return resId == 0 ? defaultValue : mContext.getResources().getDimensionPixelSize(resId);
		} catch (Throwable ignored) {
			return defaultValue;
		}
	}

	@SuppressLint("DiscouragedApi")
	private int getAndroidDimensionPixelSize(String resourceName, int defaultValue) {
		try {
			int resId = mContext.getResources().getIdentifier(resourceName, "dimen", "android");
			return resId == 0 ? defaultValue : mContext.getResources().getDimensionPixelSize(resId);
		} catch (Throwable ignored) {
			return defaultValue;
		}
	}

	@SuppressLint("DiscouragedApi")
	@Override
	public void onPackageLoaded(XposedModuleInterface.PackageReadyParam PRParam) throws Throwable {
		IntentFilter filter = new IntentFilter();
		filter.addAction(Constants.ACTION_PROFILE_SWITCH_AVAILABLE);
		mContext.registerReceiver(mAppProfileSwitchReceiver, filter, Context.RECEIVER_EXPORTED);

		//region needed classes
		ReflectedClass ClockClass = ReflectedClass.of("com.android.systemui.statusbar.policy.Clock");
		ReflectedClass PhoneStatusBarViewClass = ReflectedClass.of("com.android.systemui.statusbar.phone.PhoneStatusBarView");
		ReflectedClass NotificationIconContainerClass = ReflectedClass.of("com.android.systemui.statusbar.phone.NotificationIconContainer");
		ReflectedClass TunerServiceImplClass = ReflectedClass.of("com.android.systemui.tuner.TunerServiceImpl");
		ReflectedClass ConnectivityCallbackHandlerClass = ReflectedClass.of("com.android.systemui.statusbar.connectivity.CallbackHandler");
		ReflectedClass NotificationIconContainerAlwaysOnDisplayViewModelClass = ReflectedClass.ofIfPossible("com.android.systemui.statusbar.notification.icon.ui.viewmodel.NotificationIconContainerAlwaysOnDisplayViewModel");
		ReflectedClass NotificationIconContainerStatusBarViewModelClass = ReflectedClass.ofIfPossible("com.android.systemui.statusbar.notification.icon.ui.viewmodel.NotificationIconContainerStatusBarViewModel");
		StatusBarIconClass = ReflectedClass.of("com.android.internal.statusbar.StatusBarIcon");
		StatusBarIconHolderClass = ReflectedClass.of("com.android.systemui.statusbar.phone.StatusBarIconHolder");
		ReflectedClass PrivacyItemClass = ReflectedClass.of("com.android.systemui.privacy.PrivacyItem");
		ReflectedClass PhoneStatusBarViewControllerClass = ReflectedClass.of("com.android.systemui.statusbar.phone.PhoneStatusBarViewController");
		ReflectedClass KeyguardStateControllerImplClass = ReflectedClass.of("com.android.systemui.statusbar.policy.KeyguardStateControllerImpl");
		ReflectedClass StatusBarIconControllerImplClass = ReflectedClass.of("com.android.systemui.statusbar.phone.ui.StatusBarIconControllerImpl");
		ReflectedClass ShadeHeaderControllerClass = ReflectedClass.of("com.android.systemui.shade.ShadeHeaderController");
		ReflectedClass ActivityStarterImplClass = ReflectedClass.of("com.android.systemui.statusbar.phone.ActivityStarterImpl");
		//endregion


		KeyguardStateControllerImplClass
				.after("notifyKeyguardState")
				.run(param -> {
					Object mKeyguardUpdateMonitor = getObjectField(param.thisObject, "mKeyguardUpdateMonitor");
					boolean keyguardShowing = (boolean) getObjectField(mKeyguardUpdateMonitor, "mKeyguardShowing");
					for (ClockVisibilityCallback c : clockVisibilityCallbacks)
					{
						try {
							c.OnVisibilityChanged(!keyguardShowing);
						} catch (Throwable ignored) {}
					}
				});

		StatusBarIconControllerImplClass
				.afterConstruction()
				.run(param -> mStatusBarIconController = param.thisObject);


		if (NotificationIconContainerAlwaysOnDisplayViewModelClass.getClazz() != null) //Viewbinder implementation of the notification icon container
		{
			NotificationIconContainerAlwaysOnDisplayViewModelClass
					.afterConstruction()
					.run(param -> {
						AODNIC = param.thisObject;
						setObjectField(AODNIC, "maxIcons", NotificationAODIconLimit);
					});

			NotificationIconContainerStatusBarViewModelClass
					.afterConstruction()
					.run(param -> {
						SBNIC = param.thisObject;
						setObjectField(SBNIC, "maxIcons", NotificationIconLimit);
					});
		}

		initSwitchIcon();

		//forcing a refresh on statusbar once the charging chip goes away to avoid layout issues
		//only needed if chip is shown on lockscreen and device is unlocked quickly afterwards


		//region combined signal icons
		TunerServiceImplClass
				.afterConstruction()
				.run(param -> {
					mTunerService = param.thisObject;
					ReflectedClass.of(getObjectField(param.thisObject, "mObserver").getClass())
							.after("onChange")
							.run(param2 -> wifiVisibleChanged());
				});

		TunerServiceImplClass
				.after("addTunable")
				.run(param -> {
					if (param.args[1].getClass().equals(String[].class)
							&& Arrays.asList((String[]) param.args[1]).contains(ICON_HIDE_LIST)) {
						wifiVisibleChanged();
					} else if (ICON_HIDE_LIST.equals(param.args[1])) {
						wifiVisibleChanged();
					}
				});

		ConnectivityCallbackHandlerClass
				.after("setWifiIndicators")
				.run(param -> {
					boolean wifiVisible = getBooleanField(getObjectField(param.args[0], "statusIcon"), "visible");
					if (wifiVisible != mWifiVisible) {
						mWifiVisible = wifiVisible;
						if (CombineSignalIcons) {
							wifiVisibleChanged();
						}
					}
				});
		//endregion

		//region privacy chip
		PrivacyItemClass //A16 //qpr2b2 has removed the constructor, but it does do a distinct thing before using them. (Sunglass + roll emoji goes here)
				.before("hashCode")
				.run(param -> {
					if(HidePrivacyChip)
					{
						setObjectField(param.thisObject, "paused", true);
					}
				});
		//endregion

			//region SB Padding
			PhoneStatusBarViewClass
					.afterConstruction()
					.run(param -> mPhoneStatusbarView = (FrameLayout) param.thisObject);

			PhoneStatusBarViewClass
					.after("onFinishInflate")
					.run(param -> {
						mPhoneStatusbarView = (FrameLayout) param.thisObject;
						scheduleHeightsUpdate();
					});

			PhoneStatusBarViewClass
					.after("onAttachedToWindow")
					.run(param -> {
						mPhoneStatusbarView = (FrameLayout) param.thisObject;
						scheduleHeightsUpdate();
					});

			PhoneStatusBarViewClass
					.after("updateStatusBarHeight")
					.run(param -> {
					@SuppressLint("DiscouragedApi")
					View sbContentsView = ((View) param.thisObject).findViewById(idOf("status_bar_contents"));
					mStatusBarContents = sbContentsView;

						applyPhoneStatusBarViewHeight();
						applyStatusBarContentPadding(sbContentsView);
						applyStatusBarContainerLayout();
					});
		//endregion

		//region multi row statusbar
		//bypassing the max icon limit during measurement
			NotificationIconContainerClass
					.before("onMeasure")
					.run(param -> {
						if (param.thisObject == mNotificationIconContainer) {
							setObjectField(param.thisObject, "mMaxIcons", NotificationIconLimit);
						}
						setObjectField(param.thisObject, "mIsStaticLayout", false);
					});

			NotificationIconContainerClass
					.after("onMeasure")
					.run(param -> {
						setObjectField(param.thisObject, "mIsStaticLayout", true);
						if (param.thisObject == mNotificationIconContainer) {
							applyNotificationIconLayoutWidth();
						} else if (((View) param.thisObject).getId() == idOf("notificationIcons")) {
							mNotificationIconContainer = (ViewGroup) param.thisObject;
						}
					});

		NotificationIconContainerClass
				.before("setMaxIconsAmount")
				.run(param -> {
					if (param.thisObject == mNotificationIconContainer && param.args.length > 0) {
						param.args[0] = NotificationIconLimit;
					}
				});

			NotificationIconContainerClass
					.after("setMaxIconsAmount")
					.run(param -> {
						if (param.thisObject == mNotificationIconContainer) {
							applyNotificationIconLayoutWidth();
							scheduleHeightsUpdate();
						}
					});

		//endregion

		//update statusbar
		PhoneStatusBarViewClass
				.after("onConfigurationChanged")
				.run(param -> new Timer().schedule(new TimerTask() {
					@Override
					public void run() {
						if (BatteryBarView.hasInstance()) {
							BatteryBarView.getInstance().post(() -> refreshBatteryBar(BatteryBarView.getInstance()));
						}
					}
				}, 2000));

		//stealing a working activity starter
		ActivityStarterImplClass
				.afterConstruction()
				.run(param -> {
					if(mActivityStarter == null)
						mActivityStarter = param.thisObject;
				});

		final ClickListener clickListener = new ClickListener();

		ShadeHeaderControllerClass
				.after("onInit")
				.run(param -> {
					View mView = (View) getObjectField(param.thisObject, "mView");

					mView.findViewById(idOf("clock")).setOnClickListener(clickListener);
					mView.findViewById(idOf("clock")).setOnLongClickListener(clickListener);

					mView.findViewById(idOf("date")).setOnClickListener(clickListener);
					mView.findViewById(idOf("date")).setOnLongClickListener(clickListener);
				});

		//modding clock, adding additional objects,
		PhoneStatusBarViewControllerClass
				.after("onViewAttached")
				.run(param -> {
					mClockView = mPhoneStatusbarView.findViewById(idOf("clock"));
					updateClockColor();

					mPhoneStatusbarView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> scheduleHeightsUpdate());
					applyPhoneStatusBarViewHeight();

					mStatusbarStartSide = mPhoneStatusbarView.findViewById(idOf("status_bar_start_side_except_heads_up"));
					mStatusBarContents = mPhoneStatusbarView.findViewById(idOf("status_bar_contents"));
					applyStatusBarContentPadding(mStatusBarContents);

					mSystemIconArea = mPhoneStatusbarView.findViewById(idOf("statusIcons"));

					createCenterIconArea();

					makeLeftSplitArea();

					if (BBarEnabled) //in case we got the config but view wasn't ready yet
					{
						placeBatteryBar();
					}

					if (VolteIconEnabled || VowifiIconEnabled) //in case we got the config but context wasn't ready yet
					{
						initVoData();
					}

					if (networkOnSBEnabled) {
						networkTrafficSB = NetworkTraffic.getInstance(mContext, true);
						placeNTSB();
					}


						setHeights();

						placeClock();
						setHeights();
					});

		//clock mods
		ClockClass
				.before("getSmallTime")
				.run(param -> {
					setObjectField(param.thisObject, "mAmPmStyle", AM_PM_STYLE_GONE);
					setObjectField(param.thisObject, "mShowSeconds", mShowSeconds);
				});

		ClockClass
				.after("getSmallTime")
				.run(param -> {
					if (param.thisObject != mClockView)
						return; //We don't want custom format in QS header. do we?

					SpannableStringBuilder result = new SpannableStringBuilder();
					result.append(getFormattedString(mStringFormatBefore, mBeforeSmall, mBeforeClockColor)); //before clock
					SpannableStringBuilder clockText = SpannableStringBuilder.valueOf((CharSequence) param.getResult()); //THE clock
					if (clockColor != null) {
						clockText.setSpan(new NetworkTraffic.TrafficStyle(clockColor), 0, (clockText).length(),
								Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
					result.append(clockText);
					if (mAmPmStyle != AM_PM_STYLE_GONE) {
						result.append(getFormattedString("$Ga", mAmPmStyle == AM_PM_STYLE_SMALL, clockColor));
					}
					result.append(getFormattedString(mStringFormatAfter, mAfterSmall, mAfterClockColor)); //after clock

					if (getAdditionalInstanceField(param.thisObject, "stringFormatCallBack") == null) {
						FormattedStringCallback callback = () -> {
							if (!mShowSeconds) //don't update again if it's going to do it every second anyway
								updateClock();
						};

						stringFormatter.registerCallback(callback);
						setAdditionalInstanceField(param.thisObject, "stringFormatCallBack", callback);
					}
					param.setResult(result);
				});

		//using clock colors for network traffic and battery bar
		ClockClass
				.after("onDarkChanged")
				.run(param -> {
					if (param.thisObject != mClockView)
						return; //We don't want colors of QS header. only statusbar

					updateClockColor();
					if (BatteryBarView.hasInstance()) {
						refreshBatteryBar(BatteryBarView.getInstance());
					}
				});

		//region mobile roaming
		ReflectedClass MobileIconsInteractorImplClass = ReflectedClass.of("com.android.systemui.statusbar.pipeline.mobile.domain.interactor.MobileIconsInteractorImpl");

		//we must use the classes defined in the apk. using our own will fail
		ReflectedClass ReadonlyStateFlowClass = ReflectedClass.of("kotlinx.coroutines.flow.ReadonlyStateFlow");

		MobileIconsInteractorImplClass
				.after("getMobileConnectionInteractorForSubId")
				.run(param -> {
					if (HideRoamingState) {
						setObjectField(param.getResult(), "isRoaming", ReadonlyStateFlowClass.getClazz().getConstructors()[0].newInstance(getStateFlowImplOf(false)));
					}
				});
		//endregion
	}

	private void createCenterIconArea() {
		mCenteredIconArea = new LinearLayout(mContext);
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT);
		lp.gravity = Gravity.CENTER;
		mCenteredIconArea.setLayoutParams(lp);
		mPhoneStatusbarView.addView(mCenteredIconArea);
	}

	private void updateClockColor() {
		if(mClockView == null) return;

		currentClockColor = mClockView.getTextColors().getDefaultColor();

		for (StatusbarTextColorCallback callback : mTextColorCallbacks) {
			callback.onTextColorChanged(currentClockColor);
		}
	}

	public static @ColorInt int getCurrentClockColor() {
		return currentClockColor;
	}

	public static int registerTextColorCallback(StatusbarTextColorCallback callback) {
		mTextColorCallbacks.add(callback);
		return currentClockColor;
	}

	private void updateStatusbarHeight() {
		try {
			callMethod(mPhoneStatusbarView, "updateStatusBarHeight");
		} catch (Throwable ignored) {
			

		}
	}

	private void applyNotificationIconLimit() {
		if (mNotificationIconContainer == null) return;

		try {
			callMethod(mNotificationIconContainer, "setMaxIconsAmount", NotificationIconLimit);
			applyNotificationIconLayoutWidth();
			scheduleHeightsUpdate();
		} catch (Throwable ignored) {
		}
	}

	//region double row left area
	@SuppressLint("DiscouragedApi")
	private void makeLeftSplitArea() {
		mNotificationIconContainer = mPhoneStatusbarView.findViewById(idOf("notificationIcons"));

		mNotificationContainerContainer = new LinearLayout(mContext);
		mNotificationContainerContainer.setBaselineAligned(false);
		mNotificationContainerContainer.setGravity(Gravity.CENTER_VERTICAL);
		mNotificationContainerContainer.setClipChildren(false); //allowing headsup icon to go beyond

		if (mLeftVerticalSplitContainer == null) {
			mLeftVerticalSplitContainer = new LinearLayout(mContext);
			mLeftVerticalSplitContainer.setClipChildren(false); //allowing headsup icon to go beyond
		} else {
			mLeftVerticalSplitContainer.removeAllViews();
			if (mLeftVerticalSplitContainer.getParent() != null)
				((ViewGroup) mLeftVerticalSplitContainer.getParent()).removeView(mLeftVerticalSplitContainer);
		}

		mLeftVerticalSplitContainer.setOrientation(VERTICAL);
		mLeftVerticalSplitContainer.setLayoutTransition(null);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
		setLeftSplitAreaMargins(lp);

		mLeftVerticalSplitContainer.setLayoutParams(lp);
		mLeftVerticalSplitContainer.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> scheduleHeightsUpdate());

		mLeftExtraRowContainer = new ShyLinearLayout(mContext);
		mLeftExtraRowContainer.setBaselineAligned(false);
		mLeftExtraRowContainer.setGravity(Gravity.CENTER_VERTICAL);
		mLeftExtraRowContainer.setClipChildren(false);
		mLeftVerticalSplitContainer.addView(mLeftExtraRowContainer, 0);

		ViewGroup parent = (ViewGroup) mNotificationIconContainer.getParent();
		mNotificationIconArea = parent;
		mNotificationIconArea.setClipChildren(false);
		mNotificationIconArea.setClipToPadding(false);

		parent.addView(mLeftVerticalSplitContainer, parent.indexOfChild(mNotificationIconContainer));
		parent.removeView(mNotificationIconContainer);
		mLeftVerticalSplitContainer.addView(mNotificationContainerContainer);

		repositionOngoingChip();

		mNotificationContainerContainer.addView(mNotificationIconContainer);

		LinearLayout.LayoutParams notificationIconParams = (LinearLayout.LayoutParams) mNotificationIconContainer.getLayoutParams();
		notificationIconParams.weight = 100;
		notificationIconParams.gravity = Gravity.CENTER_VERTICAL;
		mNotificationIconContainer.setLayoutParams(notificationIconParams);
		applyNotificationIconLimit();
		mNotificationIconContainer.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
			@Override
			public void onChildViewAdded(View parent, View child) {
				scheduleHeightsUpdate();
			}

			@Override
			public void onChildViewRemoved(View parent, View child) {
				scheduleHeightsUpdate();
			}
		});

		((View) mStatusbarStartSide.getParent()).getLayoutParams().height = MATCH_PARENT;
		mStatusbarStartSide.getLayoutParams().height = MATCH_PARENT;
		mLeftVerticalSplitContainer.getLayoutParams().height = MATCH_PARENT;
		applyStatusBarContainerLayout();
	}

	private void repositionOngoingChip() {
		View ongoingChipComposeView = findComposeView(mPhoneStatusbarView.findViewById(idOf("status_bar_start_side_except_heads_up")));
		reAddView(mNotificationContainerContainer, ongoingChipComposeView);
	}

	private View findComposeView(ViewGroup parent) {
		for(int i = 0; i < parent.getChildCount(); i++)
		{
			View child = parent.getChildAt(i);
			if(child.getClass().getName().endsWith("ComposeView"))
				return child;
		}
		return null;
	}


	private void setHeights() {
		if (mPhoneStatusbarView == null
				|| mNotificationContainerContainer == null
				|| mLeftExtraRowContainer == null
				|| mNotificationIconContainer == null)
			return;

		applyPhoneStatusBarViewHeight();
		int statusbarHeight = getStatusbarContentHeight();
		if (statusbarHeight <= 0) return;

		applyStatusBarContainerLayout();
		setLeftSplitAreaMargins(mLeftVerticalSplitContainer);

		int splitHeight = Math.max(1, statusbarHeight / 2);
		boolean hasExtraRow = mLeftExtraRowContainer.getVisibility() == VISIBLE;

		if (mNotificationContainerContainer.getVisibility() != VISIBLE) {
			mNotificationContainerContainer.setVisibility(VISIBLE);
		}
		setHeight(mNotificationContainerContainer, hasExtraRow ? splitHeight : MATCH_PARENT);
		setHeight(mLeftExtraRowContainer, hasExtraRow ? splitHeight : MATCH_PARENT);
		setHeight(mNotificationIconContainer, MATCH_PARENT);
		applyNotificationIconLayoutWidth();
		if (networkOnSBEnabled) {
			setHeight(networkTrafficSB, statusbarHeight / ((networkTrafficPosition == POSITION_LEFT && notificationAreaMultiRow) ? 2 : 1));
		}
	}

	private void applyNotificationIconLayoutWidth() {
		if (!notificationAreaMultiRow || mNotificationIconContainer == null) return;

		int desiredWidth = getDesiredNotificationIconLayoutWidth();
		if (desiredWidth <= 0) return;

		try {
			Object currentWidth = getObjectField(mNotificationIconContainer, "mActualLayoutWidth");
			if (currentWidth instanceof Integer && (Integer) currentWidth == desiredWidth) return;

			callMethod(mNotificationIconContainer, "setActualLayoutWidth", desiredWidth);
			mNotificationIconContainer.requestLayout();
			mNotificationIconContainer.invalidate();
		} catch (Throwable ignored) {
		}
	}

	private int getDesiredNotificationIconLayoutWidth() {
		if (mNotificationIconContainer == null) return 0;

		int childCount = mNotificationIconContainer.getChildCount();
		if (childCount <= 0 || NotificationIconLimit <= 0) return 0;

		int iconSlots = Math.min(childCount, NotificationIconLimit + 1);
		try {
			Object calculatedWidth = callMethod(mNotificationIconContainer, "calculateWidthFor", (float) iconSlots);
			if (calculatedWidth instanceof Number) {
				int width = (int) Math.ceil(((Number) calculatedWidth).doubleValue());
				if (width > 0) return width;
			}
		} catch (Throwable ignored) {
		}

		int slotWidth = getStatusBarIconSlotWidth();
		return slotWidth <= 0 ? 0 : iconSlots * slotWidth;
	}

	private void scheduleHeightsUpdate() {
		if (mPhoneStatusbarView == null) return;

		mPhoneStatusbarView.removeCallbacks(mSetHeightsRunnable);
		mPhoneStatusbarView.post(mSetHeightsRunnable);
	}

	private int getStatusbarContentHeight() {
		return Math.max(0, getPhoneStatusBarHeight() - getStatusBarContentsTopPadding() - getStatusBarContentTopInset());
	}

	private int getPhoneStatusBarHeight() {
		int height = Math.max(getTargetPhoneStatusBarHeight(), getStatusBarInsetsHeight());
		height = Math.max(height, getViewHeight(mPhoneStatusbarView));
		height = Math.max(height, getViewHeight(mStatusBarContents));
		return height;
	}

	private int getStatusBarInsetsHeight() {
		try {
			if (mPhoneStatusbarView == null || mPhoneStatusbarView.getRootWindowInsets() == null) return 0;

			return mPhoneStatusbarView
					.getRootWindowInsets()
					.getInsetsIgnoringVisibility(android.view.WindowInsets.Type.statusBars())
					.top;
		} catch (Throwable ignored) {
			return 0;
		}
	}

	private int getTargetPhoneStatusBarHeight() {
		try {
			Context portraitContext = getPortraitContext();
			int height = getAndroidDimensionPixelSize(
					portraitContext,
					"status_bar_height_portrait",
					getAndroidDimensionPixelSize(portraitContext, "status_bar_height", 0));
			return Math.round(height * statusbarHeightFactor / 100f);
		} catch (Throwable ignored) {
			return 0;
		}
	}

	private Context getPortraitContext() {
		android.content.res.Configuration config = new android.content.res.Configuration(mContext.getResources().getConfiguration());
		config.orientation = android.content.res.Configuration.ORIENTATION_PORTRAIT;
		return mContext.createConfigurationContext(config);
	}

	private int getViewHeight(View view) {
		if (view == null) return 0;

		int height = view.getHeight();
		if (height <= 0) {
			height = view.getMeasuredHeight();
		}
		if (height <= 0 && view.getLayoutParams() != null && view.getLayoutParams().height > 0) {
			height = view.getLayoutParams().height;
		}
		return height;
	}

	private int getStatusBarContentsTopPadding() {
		if (mStatusBarContents != null) {
			Object originalTopPadding = getAdditionalInstanceField(mStatusBarContents, ORIGINAL_STATUS_BAR_CONTENTS_TOP_PADDING);
			if (originalTopPadding instanceof Integer) {
				return (Integer) originalTopPadding;
			}
			return mStatusBarContents.getPaddingTop();
		}

		try {
			return mContext.getResources().getDimensionPixelSize(dimenIdOf("status_bar_padding_top"));
		} catch (Throwable ignored) {
			return 0;
		}
	}

	@SuppressLint("DiscouragedApi")
	private int getAndroidDimensionPixelSize(Context context, String resourceName, int defaultValue) {
		try {
			int resId = context.getResources().getIdentifier(resourceName, "dimen", "android");
			return resId == 0 ? defaultValue : context.getResources().getDimensionPixelSize(resId);
		} catch (Throwable ignored) {
			return defaultValue;
		}
	}

	private void setLeftSplitAreaMargins(ViewGroup.MarginLayoutParams layoutParams) {
		if (notificationAreaMultiRow) {
			layoutParams.topMargin = 0;
			layoutParams.bottomMargin = 0;
		} else {
			int margin = ResourceTools.dpToPx(mContext, 4);
			layoutParams.topMargin = margin;
			layoutParams.bottomMargin = margin;
		}
	}

	private void setLeftSplitAreaMargins(View view) {
		if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams layoutParams) {
			int oldTopMargin = layoutParams.topMargin;
			int oldBottomMargin = layoutParams.bottomMargin;
			setLeftSplitAreaMargins(layoutParams);
			if (layoutParams.topMargin != oldTopMargin || layoutParams.bottomMargin != oldBottomMargin) {
				view.setLayoutParams(layoutParams);
			}
		}
	}

	private void setHeight(View view, int height) {
		if (view == null) return;

		ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
		if (layoutParams != null && layoutParams.height != height) {
			layoutParams.height = height;
			view.setLayoutParams(layoutParams);
		}
	}

	private void applyStatusBarContainerLayout() {
		int topInset = getStatusBarContentTopInset();
		int contentHeight = getStatusbarContentHeight();

		applyPhoneStatusBarViewHeight();
		applyMatchParentHeight(mStatusBarContents);
		applyMatchParentHeight(mStatusbarStartSide);
		applyMatchParentHeight(mNotificationIconArea);
		if (mStatusbarStartSide != null && mStatusbarStartSide.getParent() instanceof View) {
			applyMatchParentHeight((View) mStatusbarStartSide.getParent());
		}

		applyVerticalContentBounds(mCenteredIconArea, 0, contentHeight);
		setStatusBarTranslationY(mCenteredIconArea, getSingleRowStatusBarTranslationY(mCenteredIconArea));
		if (mSystemIconArea != null) {
			if (mSystemIconArea.getParent() instanceof View) {
				View systemIconParent = (View) mSystemIconArea.getParent();
				applyVerticalContentBounds(systemIconParent, 0, contentHeight);
				setStatusBarTranslationY(systemIconParent, getSingleRowStatusBarTranslationY(systemIconParent));
				applyContentChildBounds(mSystemIconArea);
				setStatusBarTranslationY(mSystemIconArea, 0);
			} else {
				applyVerticalContentBounds(mSystemIconArea, 0, contentHeight);
				setStatusBarTranslationY(mSystemIconArea, getSingleRowStatusBarTranslationY(mSystemIconArea));
			}
		}
		if (mLeftVerticalSplitContainer != null) {
			setStatusBarTranslationY(mLeftVerticalSplitContainer, notificationAreaMultiRow
					? topInset
					: getSingleRowStatusBarTranslationY(mLeftVerticalSplitContainer));
		}
		applyLinearLayoutGravity(mLeftExtraRowContainer, Gravity.CENTER_VERTICAL);
		applyLinearLayoutGravity(mNotificationContainerContainer, Gravity.CENTER_VERTICAL);
		applyLinearLayoutGravity(mSystemIconArea, Gravity.CENTER_VERTICAL);

		if (mClockView != null) {
			mClockView.setGravity(Gravity.CENTER_VERTICAL);
			applyLayoutGravity(mClockView, Gravity.CENTER_VERTICAL);
		}
	}

	private float getSingleRowStatusBarTranslationY(View view) {
		if (view == null) return 0;

		int contentHeight = getStatusbarContentHeight();
		int viewHeight = getViewHeight(view);
		if (contentHeight <= 0 || viewHeight <= 0) return getStatusBarContentTopInset();

		return getStatusBarContentTopInset() + Math.max(0, (contentHeight - viewHeight) / 2f);
	}

	private void setStatusBarTranslationY(View view, float translationY) {
		if (view == null) return;

		if (view.getTranslationY() != translationY) {
			view.setTranslationY(translationY);
		}
	}

	private void applyMatchParentHeight(View view) {
		if (view == null || view.getLayoutParams() == null) return;

		ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
		if (layoutParams.height != MATCH_PARENT) {
			layoutParams.height = MATCH_PARENT;
			view.setLayoutParams(layoutParams);
		}
		if (view instanceof ViewGroup viewGroup) {
			viewGroup.setClipChildren(false);
			viewGroup.setClipToPadding(false);
		}
	}

	private void applyPhoneStatusBarViewHeight() {
		int targetHeight = getPhoneStatusBarHeight();
		if (mPhoneStatusbarView == null || targetHeight <= 0) return;

		applyStatusBarHeight(mPhoneStatusbarView, targetHeight);
		applyStatusBarAncestorHeights(targetHeight);
	}

	private void applyStatusBarAncestorHeights(int targetHeight) {
		View rootView = mPhoneStatusbarView.getRootView();
		ViewParent parent = mPhoneStatusbarView.getParent();
		while (parent instanceof View parentView && parentView != rootView) {
			if (parentView.getHeight() > 0 && parentView.getHeight() < targetHeight) {
				applyStatusBarHeight(parentView, targetHeight);
			}
			if (parentView instanceof ViewGroup viewGroup) {
				viewGroup.setClipChildren(false);
				viewGroup.setClipToPadding(false);
			}
			parent = parentView.getParent();
		}
	}

	private void applyStatusBarHeight(View view, int targetHeight) {
		ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
		if (layoutParams != null && layoutParams.height != targetHeight) {
			layoutParams.height = targetHeight;
			view.setLayoutParams(layoutParams);
			view.requestLayout();
		}
	}

	private void applyVerticalContentBounds(View view, int topMargin, int height) {
		if (view == null || view.getLayoutParams() == null || height <= 0) return;

		ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
		boolean changed = false;
		if (layoutParams.height != height) {
			layoutParams.height = height;
			changed = true;
		}
		if (layoutParams instanceof ViewGroup.MarginLayoutParams marginLayoutParams) {
			if (marginLayoutParams.topMargin != topMargin) {
				marginLayoutParams.topMargin = topMargin;
				changed = true;
			}
			if (marginLayoutParams.bottomMargin != 0) {
				marginLayoutParams.bottomMargin = 0;
				changed = true;
			}
		}
		if (changed) {
			view.setLayoutParams(layoutParams);
		}
		applyLayoutGravity(view, Gravity.CENTER_VERTICAL);
		if (view instanceof ViewGroup viewGroup) {
			viewGroup.setClipChildren(false);
			viewGroup.setClipToPadding(false);
		}
	}

	private void applyContentChildBounds(View view) {
		if (view == null || view.getLayoutParams() == null) return;

		ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
		boolean changed = false;
		if (layoutParams.height != MATCH_PARENT) {
			layoutParams.height = MATCH_PARENT;
			changed = true;
		}
		if (layoutParams instanceof ViewGroup.MarginLayoutParams marginLayoutParams) {
			if (marginLayoutParams.topMargin != 0) {
				marginLayoutParams.topMargin = 0;
				changed = true;
			}
			if (marginLayoutParams.bottomMargin != 0) {
				marginLayoutParams.bottomMargin = 0;
				changed = true;
			}
		}
		if (changed) {
			view.setLayoutParams(layoutParams);
		}
		applyLayoutGravity(view, Gravity.CENTER_VERTICAL);
	}

	private void applyLayoutGravity(View view, int gravity) {
		if (view == null || view.getLayoutParams() == null) return;

		ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
		if (layoutParams instanceof LinearLayout.LayoutParams linearLayoutParams) {
			int updatedGravity = mergeVerticalGravity(linearLayoutParams.gravity, gravity);
			if (linearLayoutParams.gravity != updatedGravity) {
				linearLayoutParams.gravity = updatedGravity;
				view.setLayoutParams(linearLayoutParams);
			}
		} else if (layoutParams instanceof FrameLayout.LayoutParams frameLayoutParams) {
			int updatedGravity = mergeVerticalGravity(frameLayoutParams.gravity, gravity);
			if (frameLayoutParams.gravity != updatedGravity) {
				frameLayoutParams.gravity = updatedGravity;
				view.setLayoutParams(frameLayoutParams);
			}
		}
	}

	private void applyLinearLayoutGravity(LinearLayout view, int gravity) {
		if (view == null) return;

		int updatedGravity = mergeVerticalGravity(view.getGravity(), gravity);
		if (view.getGravity() != updatedGravity) {
			view.setGravity(updatedGravity);
		}
	}

	private int mergeVerticalGravity(int originalGravity, int verticalGravity) {
		if (originalGravity < 0) originalGravity = 0;

		return (originalGravity & ~Gravity.VERTICAL_GRAVITY_MASK) | verticalGravity;
	}
	//endregion

	//region battery bar related
	private void refreshBatteryBar(BatteryBarView instance) {
		BatteryBarView.setStaticColor(batteryLevels, batteryColors, indicateCharging, chargingColor, indicateFastCharging, fastChargingColor, indicatePowerSave, powerSaveColor, BBarTransitColors, BBAnimateCharging);
		instance.setVisibility((BBarEnabled) ? VISIBLE : GONE);
		instance.setColorful(BBarColorful);
		instance.setOnlyWhileCharging(BBOnlyWhileCharging);
		instance.setOnTop(!BBOnBottom);
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
			} catch (Throwable ignored) {}
			mPhoneStatusbarView.addView(batteryBarView);
			refreshBatteryBar(BatteryBarView.getInstance());
		} catch (Throwable ignored) {}
	}
	//endregion

	//region statusbar icon holder
	private Object getStatusbarIconFor(Icon icon, String slotName) {
		try {
			Object statusbarIcon = ObjenesisHelper.newInstance(StatusBarIconClass.getClazz());

			setObjectField(statusbarIcon, "visible", true);

			//noinspection JavaReflectionMemberAccess
			setObjectField(statusbarIcon, "user", UserHandle.class.getDeclaredConstructor(int.class).newInstance(0));
			setObjectField(statusbarIcon, "pkg", BuildConfig.APPLICATION_ID);
			setObjectField(statusbarIcon, "icon", icon);
			setObjectField(statusbarIcon, "iconLevel", 0);
			setObjectField(statusbarIcon, "number", 0);
			setObjectField(statusbarIcon, "contentDescription", slotName);

			return statusbarIcon;
		} catch (Throwable ignored) {
			return null;
		}
	}

	private Object getStatusbarIconHolderFor(Object statusbarIcon) {
		Object holder = ObjenesisHelper.newInstance(StatusBarIconHolderClass.getClazz());
		String[] iconFiled = new String[1];
		Arrays.stream(StatusBarIconHolderClass.getClazz().getFields()).forEach(field ->
		{
			if (field.getName().toLowerCase().contains("icon"))
				iconFiled[0] = field.getName();
		});

		setObjectField(holder, iconFiled[0], statusbarIcon);

		return holder;
	}

	//endregion

	//region vo_data related
	private void initVoData() {
		try {
			if (!telephonyCallbackRegistered) {

				Icon volteIcon = Icon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.ic_volte);
				Object volteStatusbarIcon = getStatusbarIconFor(volteIcon, VO_LTE_SLOT);
				volteStatusbarIconHolder = getStatusbarIconHolderFor(volteStatusbarIcon);

				Icon vowifiIcon = Icon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.ic_vowifi);
				Object vowifiStatusbarIcon = getStatusbarIconFor(vowifiIcon, VO_WIFI_SLOT);
				vowifiStatusbarIconHolder = getStatusbarIconHolderFor(vowifiStatusbarIcon);

				//noinspection DataFlowIssue
				SystemUtils.TelephonyManager().registerTelephonyCallback(voDataExec, voDataCallback);
				telephonyCallbackRegistered = true;
			}
		} catch (Exception ignored) {						

		}

		updateVoData(true);
	}

	private void removeVoDataCallback() {
		try {
			//noinspection DataFlowIssue
			SystemUtils.TelephonyManager().unregisterTelephonyCallback(voDataCallback);
			telephonyCallbackRegistered = false;
		} catch (Exception ignored) {						

		}
		removeSBIconSlot(VO_LTE_SLOT);
		removeSBIconSlot(VO_WIFI_SLOT);
	}

	private class serverStateCallback extends TelephonyCallback implements
			TelephonyCallback.ServiceStateListener {
		@Override
		public void onServiceStateChanged(@NonNull ServiceState serviceState) {
			updateVoData(false);
		}
	}

	private void updateVoData(boolean force) {
		boolean voWifiAvailable = (Boolean) callMethod(SystemUtils.TelephonyManager(), "isWifiCallingAvailable");
		boolean volteStateAvailable = (Boolean) callMethod(SystemUtils.TelephonyManager(), "isVolteAvailable");

		if (lastVolteAvailable != volteStateAvailable || force) {
			lastVolteAvailable = volteStateAvailable;
			if (volteStateAvailable && VolteIconEnabled) {
				mPhoneStatusbarView.post(() -> {
					try {
						callMethod(mStatusBarIconController, "setIcon", VO_LTE_SLOT, volteStatusbarIconHolder);
					} catch (Exception ignored) {}
				});
			} else {
				removeSBIconSlot(VO_LTE_SLOT);
			}
		}

		if (lastVowifiAvailable != voWifiAvailable || force) {
			lastVowifiAvailable = voWifiAvailable;
			if (voWifiAvailable && VowifiIconEnabled) {
				mPhoneStatusbarView.post(() -> {
					try {
						callMethod(mStatusBarIconController, "setIcon", VO_WIFI_SLOT, vowifiStatusbarIconHolder);
					} catch (Exception ignored) {						

					}
				});
			} else {
				removeSBIconSlot(VO_WIFI_SLOT);
			}
		}
	}

	private void removeSBIconSlot(String slot) {
		if (mPhoneStatusbarView == null) return; //probably it's too soon to have a statusbar

		mPhoneStatusbarView.post(() -> {
			try {
				callMethod(mStatusBarIconController, "removeAllIconsForSlot", slot, false);
			} catch (Throwable ignored) {						

			}
		});
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
					((ViewGroup) mSystemIconArea.getParent()).addView(networkTrafficSB, 0);
					networkTrafficSB.setPadding(rightClockPadding, 0, leftClockPadding, 0);
					break;
				case POSITION_LEFT:
					if (notificationAreaMultiRow) {
						mLeftExtraRowContainer.addView(networkTrafficSB, mLeftExtraRowContainer.getChildCount());
					} else {
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
		} catch (Throwable ignored) {}
	}
	//endregion

	//region icon tap related
	class ClickListener implements View.OnClickListener, View.OnLongClickListener {
		@Override
		public void onClick(View v) {
			String name = mContext.getResources().getResourceName(v.getId());

			if (name.endsWith("clock")) {
				callMethod(mActivityStarter, "postStartActivityDismissingKeyguard", new Intent(AlarmClock.ACTION_SHOW_ALARMS), 0);
			} else if (name.endsWith("date")) {
				Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
				builder.appendPath("time");
				builder.appendPath(Long.toString(System.currentTimeMillis()));
				Intent todayIntent = new Intent(Intent.ACTION_VIEW, builder.build());
				callMethod(mActivityStarter, "postStartActivityDismissingKeyguard", todayIntent, 0);
			}
		}

		@Override
		public boolean onLongClick(View v) {
			String name = mContext.getResources().getResourceName(v.getId());

			if (name.endsWith("clock") || name.endsWith("date")) {
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
				if (notificationAreaMultiRow) {
					targetArea = mLeftExtraRowContainer;
					index = 0;
				} else {
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
			//noinspection DataFlowIssue
			targetArea.addView(mClockView);
		}
	}

	private final StringFormatter stringFormatter = new StringFormatter();

	private CharSequence getFormattedString(String dateFormat, boolean small, @Nullable @ColorInt Integer textColor) {
		if (dateFormat.isEmpty()) return "";

		//There's some format to work on
		SpannableStringBuilder formatted = new SpannableStringBuilder(stringFormatter.formatString(dateFormat));

		if (small) {
			//small size requested
			CharacterStyle style = new RelativeSizeSpan(0.7f);
			formatted.setSpan(style, 0, formatted.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		if (textColor != null) {
			formatted.setSpan(new NetworkTraffic.TrafficStyle(textColor), 0, (formatted).length(),
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
	private void wifiVisibleChanged() {
		try { //don't crash the system if failed
			//inspired from from TunerServiceImpl#reloadAll
			String hideListString = Settings.Secure.getString(
					(ContentResolver) getObjectField(mTunerService, "mContentResolver")
					, ICON_HIDE_LIST);

			if (CombineSignalIcons && mWifiVisible) {
				if (hideListString == null || hideListString.isEmpty()) {
					hideListString = "mobile";
				} else if (!hideListString.contains("mobile")) {
					hideListString = hideListString + ",mobile";
				}
			}
			@SuppressWarnings("unchecked")
			Set<Object> tunables = (Set<Object>) callMethod(getObjectField(mTunerService, "mTunableLookup"), "get", ICON_HIDE_LIST);

			String finalHideListString = hideListString;
			mPhoneStatusbarView.post(() -> {
				for (Object tunable : tunables) {
					callMethod(tunable, "onTuningChanged", ICON_HIDE_LIST, finalHideListString);
				}
			});
		} catch (Throwable ignored) {}
	}
	//endregion

	public interface StatusbarTextColorCallback {
		void onTextColorChanged(int textColor);
	}
}
