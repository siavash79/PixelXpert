package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.findFieldIfExists;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import android.content.Context;
import android.content.Intent;
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
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nfx.android.rangebarpreference.RangeBarHelper;

import java.util.ArrayList;
import java.util.HashMap;
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
import sh.siava.AOSPMods.Utils.NetworkTraffic;
import sh.siava.AOSPMods.Utils.NotificationIconContainerOverride;
import sh.siava.AOSPMods.Utils.StringFormatter;
import sh.siava.AOSPMods.Utils.SystemUtils;
import sh.siava.AOSPMods.Utils.batteryStyles.BatteryBarView;
import sh.siava.AOSPMods.XPrefs;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings({"RedundantThrows", "ConstantConditions"})

public class StatusbarMods extends XposedModPack
{
	private static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;
	
	//region battery
	private static final int CHARGING_FAST = 2;
	//endregion
	
	//region Clock
	private static final int POSITION_LEFT = 0;
	private static final int POSITION_CENTER = 1;
	private static final int POSITION_RIGHT = 2;
	private static final int POSITION_LEFT_EXTRA_LEVEL = 3;
	
	private static final int AM_PM_STYLE_SMALL = 1;
	private static final int AM_PM_STYLE_GONE = 2;
	//region general use
	private static final ArrayList<ClockVisibilityCallback> clockVisibilityCallbacks = new ArrayList<>();
	//region volte
	private static final int VOLTE_AVAILABLE = 2;
	private static final int VOLTE_NOT_AVAILABLE = 1;
	private static final int VOLTE_UNKNOWN = - 1;
	public static int clockColor = 0;
	private static int clockPosition = POSITION_LEFT;
	private static int mAmPmStyle = AM_PM_STYLE_GONE;
	//endregion
	private static boolean mShowSeconds = false;
	//endregion
	private static String mStringFormatBefore = "", mStringFormatAfter = "";
	private static boolean mBeforeSmall = true, mAfterSmall = true;
	//region vibration icon
	private static boolean showVibrationIcon = false;
	private static boolean networkOnSBEnabled = false;
	private static boolean networkOnQSEnabled = false;
	private static int networkTrafficPosition = POSITION_LEFT;
	//endregion
	//region battery bar
	private static boolean BBarEnabled;
	private static boolean BBarColorful;
	private static boolean BBOnlyWhileCharging;
	private static boolean BBOnBottom;
	private static boolean BBSetCentered;
	private static int BBOpacity = 100;
	private static int BBarHeight = 10;
	private static float[] batteryLevels = new float[]{20f, 40f};
	private static int[] batteryColors = new int[]{Color.RED, Color.YELLOW};
	private static int chargingColor = Color.WHITE;
	private static int fastChargingColor = Color.WHITE;
	private static boolean indicateCharging = false;
	private static boolean indicateFastCharging = false;
	private static boolean BBarTransitColors = false;
	//endregion
	private static boolean VolteIconEnabled = false;
	private final int leftClockPadding, rightClockPadding;
	private final Executor volteExec = Runnable::run;
	private final serverStateCallback volteCallback = new serverStateCallback();
	private final StringFormatter stringFormatter = new StringFormatter();
	private Integer mBeforeClockColor = null, mAfterClockColor = null, mClockColor = null;
	//region network traffic
	private FrameLayout NTQSHolder = null;
	private NetworkTraffic networkTrafficSB = null;
	private NetworkTraffic networkTrafficQS = null;
	private Object mActivityStarter;
	private Object KIC = null;
	private Object QSBH = null;
	private View mStatusBar;
	private Object mCollapsedStatusBarFragment = null;
	private ViewGroup mClockParent = null;
	private ViewGroup mNotificationIconAreaInner = null;
	private View mCenteredIconArea = null;
	//endregion
	private LinearLayout mSystemIconArea = null;
	private FrameLayout fullStatusbar;
	//    private Object STB = null;
	private int centerAreaFineTune = 50;
	private View mClockView;
	private FrameLayout mNotificationIconContainer;
	private LinearLayout mLeftVerticalSplitContainer;
	private LinearLayout mLeftExtraRowContainer;
	private Object mStatusBarIconController;
	private Class<?> StatusBarIcon;
	private Object volteStatusbarIcon;
	private boolean telephonyCallbackRegistered = false;
	//endregion
	private int lastVolteState = VOLTE_UNKNOWN;
	
	public StatusbarMods(Context context)
	{
		super(context);
		rightClockPadding = mContext.getResources().getDimensionPixelSize(mContext.getResources()
		                                                                          .getIdentifier("status_bar_clock_starting_padding", "dimen", mContext.getPackageName()));
		leftClockPadding = mContext.getResources().getDimensionPixelSize(mContext.getResources()
		                                                                         .getIdentifier("status_bar_left_clock_end_padding", "dimen", mContext.getPackageName()));
	}
	
	//endregion
	//region callbacks
	public static void registerClockVisibilityCallback(ClockVisibilityCallback callback)
	{
		clockVisibilityCallbacks.add(callback);
	}
	
	@SuppressWarnings("unused")
	public static void unRegisterClockVisibilityCallback(ClockVisibilityCallback callback)
	{
		clockVisibilityCallbacks.remove(callback);
	}
	
	@Override
	public boolean listensTo(String packageName)
	{
		return listenPackage.equals(packageName);
	}
	//endregion
	
	public void updatePrefs(String... Key)
	{
		if(XPrefs.Xprefs == null)
			return;
		
		NotificationIconContainerOverride.MAX_STATIC_ICONS = Integer.parseInt(XPrefs.Xprefs.getString("NotificationIconLimit", "4"));
		
		centerAreaFineTune = XPrefs.Xprefs.getInt("centerAreaFineTune", 50);
		tuneCenterArea();
		
		//region BatteryBar Settings
		BBarEnabled = XPrefs.Xprefs.getBoolean("BBarEnabled", false);
		BBarColorful = XPrefs.Xprefs.getBoolean("BBarColorful", false);
		BBOnlyWhileCharging = XPrefs.Xprefs.getBoolean("BBOnlyWhileCharging", false);
		BBOnBottom = XPrefs.Xprefs.getBoolean("BBOnBottom", false);
		BBSetCentered = XPrefs.Xprefs.getBoolean("BBSetCentered", false);
		BBOpacity = XPrefs.Xprefs.getInt("BBOpacity", 100);
		BBarHeight = XPrefs.Xprefs.getInt("BBarHeight", 50);
		BBarTransitColors = XPrefs.Xprefs.getBoolean("BBarTransitColors", false);
		
		String jsonString = XPrefs.Xprefs.getString("batteryWarningRange", "");
		if(jsonString.length() > 0) {
			batteryLevels = new float[]{RangeBarHelper.getLowValueFromJsonString(jsonString), RangeBarHelper.getHighValueFromJsonString(jsonString)};
		}
		
		batteryColors = new int[]{XPrefs.Xprefs.getInt("batteryCriticalColor", Color.RED), XPrefs.Xprefs.getInt("batteryWarningColor", Color.YELLOW)};
		
		
		indicateFastCharging = XPrefs.Xprefs.getBoolean("indicateFastCharging", false);
		indicateCharging = XPrefs.Xprefs.getBoolean("indicateCharging", true);
		
		chargingColor = XPrefs.Xprefs.getInt("batteryChargingColor", Color.GREEN);
		fastChargingColor = XPrefs.Xprefs.getInt("batteryFastChargingColor", Color.BLUE);
		
		if(BBarEnabled) {
			placeBatteryBar();
		}
		
		if(BatteryBarView.hasInstance()) {
			refreshBatteryBar(BatteryBarView.getInstance());
		}
		//endregion BatteryBar Settings
		
		
		//region network Traffic settings
		networkOnSBEnabled = XPrefs.Xprefs.getBoolean("networkOnSBEnabled", false);
		networkOnQSEnabled = XPrefs.Xprefs.getBoolean("networkOnQSEnabled", false);
		String networkTrafficModeStr = XPrefs.Xprefs.getString("networkTrafficMode", "0");
		int networkTrafficMode = Integer.parseInt(networkTrafficModeStr);
		
		boolean networkTrafficRXTop = XPrefs.Xprefs.getBoolean("networkTrafficRXTop", true);
		int networkTrafficDLColor = XPrefs.Xprefs.getInt("networkTrafficDLColor", Color.GREEN);
		int networkTrafficULColor = XPrefs.Xprefs.getInt("networkTrafficULColor", Color.RED);
		int networkTrafficOpacity = XPrefs.Xprefs.getInt("networkTrafficOpacity", 100);
		int networkTrafficInterval = XPrefs.Xprefs.getInt("networkTrafficInterval", 1);
		boolean networkTrafficColorful = XPrefs.Xprefs.getBoolean("networkTrafficColorful", false);
		
		
		if(networkOnSBEnabled || networkOnQSEnabled) {
			networkTrafficPosition = - 1; //anyway we have to call placer method
			int newnetworkTrafficPosition = Integer.parseInt(XPrefs.Xprefs.getString("networkTrafficPosition", "2"));
			
			String thresholdText = XPrefs.Xprefs.getString("networkTrafficThreshold", "10");
			
			int networkTrafficThreshold;
			try {
				networkTrafficThreshold = Math.round(Float.parseFloat(thresholdText));
			} catch(Exception e) {
				networkTrafficThreshold = 10;
			}
			if(newnetworkTrafficPosition != networkTrafficPosition) {
				networkTrafficPosition = newnetworkTrafficPosition;
			}
			NetworkTraffic.setConstants(networkTrafficInterval, networkTrafficThreshold, networkTrafficMode, networkTrafficRXTop, networkTrafficColorful, networkTrafficDLColor, networkTrafficULColor, networkTrafficOpacity);
			
		}
		if(networkOnSBEnabled) {
			networkTrafficSB = NetworkTraffic.getInstance(mContext, true);
			networkTrafficSB.update();
		}
		if(networkOnQSEnabled) {
			networkTrafficQS = NetworkTraffic.getInstance(mContext, false);
			networkTrafficQS.update();
		}
		placeNTSB();
		placeNTQS();
		
		//endregion network settings
		
		//region vibration settings
		boolean newshowVibrationIcon = XPrefs.Xprefs.getBoolean("SBshowVibrationIcon", false);
		if(newshowVibrationIcon != showVibrationIcon) {
			showVibrationIcon = newshowVibrationIcon;
			setShowVibrationIcon();
		}
		//endregion
		
		
		//region clock settings
		
		clockPosition = Integer.parseInt(XPrefs.Xprefs.getString("SBClockLoc", String.valueOf(POSITION_LEFT)));
		mShowSeconds = XPrefs.Xprefs.getBoolean("SBCShowSeconds", false);
		mAmPmStyle = Integer.parseInt(XPrefs.Xprefs.getString("SBCAmPmStyle", String.valueOf(AM_PM_STYLE_GONE)));
		
		mStringFormatBefore = XPrefs.Xprefs.getString("DateFormatBeforeSBC", "");
		mStringFormatAfter = XPrefs.Xprefs.getString("DateFormatAfterSBC", "");
		mBeforeSmall = XPrefs.Xprefs.getBoolean("BeforeSBCSmall", true);
		mAfterSmall = XPrefs.Xprefs.getBoolean("AfterSBCSmall", true);
		
		if(XPrefs.Xprefs.getBoolean("SBCClockColorful", false)) {
			mClockColor = XPrefs.Xprefs.getInt("SBCClockColor", Color.WHITE);
			mBeforeClockColor = XPrefs.Xprefs.getInt("SBCBeforeClockColor", Color.WHITE);
			mAfterClockColor = XPrefs.Xprefs.getInt("SBCAfterClockColor", Color.WHITE);
		} else {
			mClockColor = mBeforeClockColor = mAfterClockColor = null;
		}
		
		
		if((mStringFormatBefore + mStringFormatAfter).trim().length() == 0) {
			int SBCDayOfWeekMode = Integer.parseInt(XPrefs.Xprefs.getString("SBCDayOfWeekMode", "0"));
			
			switch(SBCDayOfWeekMode) {
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
		} catch(Throwable ignored) {
		}
		//endregion clock settings
		
		
		//region volte
		VolteIconEnabled = XPrefs.Xprefs.getBoolean("VolteIconEnabled", false);
		if(VolteIconEnabled)
			initVolte();
		else
			removeVolte();
		//endregion
		
	}
	
	private void placeNTQS()
	{
		if(networkTrafficQS == null) {
			return;
		}
		try {
			((ViewGroup) networkTrafficQS.getParent()).removeView(networkTrafficQS);
		} catch(Throwable ignored) {
		}
		if(! networkOnQSEnabled)
			return;
		
		try {
			NTQSHolder.addView(networkTrafficQS);
		} catch(Throwable ignored) {
		}
	}
	
	//region general
	private void tuneCenterArea()
	{
		try {
			int screenWidth = fullStatusbar.getMeasuredWidth();
			int notificationWidth = (screenWidth * centerAreaFineTune / 100);
			((ViewGroup) mNotificationIconAreaInner.getParent().getParent()
			                                       .getParent()).getLayoutParams().width = notificationWidth;
			mSystemIconArea.getLayoutParams().width = screenWidth - notificationWidth;
		} catch(Exception ignored) {
		}
	}
	//end region
	
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable
	{
		if(! lpparam.packageName.equals(listenPackage))
			return;
		
		
		//region needed classes
		Class<?> ActivityStarterClass = findClass("com.android.systemui.plugins.ActivityStarter", lpparam.classLoader);
		Class<?> DependencyClass = findClass("com.android.systemui.Dependency", lpparam.classLoader);
		Class<?> CollapsedStatusBarFragmentClass;
		Class<?> UtilsClass = findClass("com.android.settingslib.Utils", lpparam.classLoader);
		Class<?> KeyguardStatusBarViewControllerClass = findClass("com.android.systemui.statusbar.phone.KeyguardStatusBarViewController", lpparam.classLoader);
		//        Class<?> QuickStatusBarHeaderControllerClass = findClass("com.android.systemui.qs.QuickStatusBarHeaderController", lpparam.classLoader);
		Class<?> QuickStatusBarHeaderClass = findClass("com.android.systemui.qs.QuickStatusBarHeader", lpparam.classLoader);
		Class<?> ClockClass = findClass("com.android.systemui.statusbar.policy.Clock", lpparam.classLoader);
		Class<?> PhoneStatusBarViewClass = findClass("com.android.systemui.statusbar.phone.PhoneStatusBarView", lpparam.classLoader);
		Class<?> KeyGuardIndicationClass = findClass("com.android.systemui.statusbar.KeyguardIndicationController", lpparam.classLoader);
		Class<?> BatteryTrackerClass = findClass("com.android.systemui.statusbar.KeyguardIndicationController$BaseKeyguardCallback", lpparam.classLoader);
		Class<?> notificationIconContainerClass = findClass("com.android.systemui.statusbar.phone.NotificationIconContainer", lpparam.classLoader);
		StatusBarIcon = findClass("com.android.internal.statusbar.StatusBarIcon", lpparam.classLoader);
		NotificationIconContainerOverride.StatusBarIconViewClass = findClass("com.android.systemui.statusbar.StatusBarIconView", lpparam.classLoader);
		
		CollapsedStatusBarFragmentClass = findClassIfExists("com.android.systemui.statusbar.phone.fragment.CollapsedStatusBarFragment", lpparam.classLoader);
		
		if(CollapsedStatusBarFragmentClass == null) {
			CollapsedStatusBarFragmentClass = findClass("com.android.systemui.statusbar.phone.CollapsedStatusBarFragment", lpparam.classLoader);
		}
		//endregion
		
		//region multi row statusbar
		XC_MethodHook mNotificationIconAreaContentCheck = new XC_MethodHook()
		{
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable
			{
				setNotificationVisibility();
			}
		};
		hookAllMethods(notificationIconContainerClass, "onViewAdded", mNotificationIconAreaContentCheck);
		hookAllMethods(notificationIconContainerClass, "onViewRemoved", mNotificationIconAreaContentCheck);
		
		hookAllMethods(notificationIconContainerClass, "calculateIconTranslations", new XC_MethodHook()
		{
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable
			{
				NotificationIconContainerOverride.calculateIconTranslations(param);
				param.setResult(null);
			}
		});
		//endregion
		
		// needed to check fastcharging
		hookAllConstructors(KeyGuardIndicationClass, new XC_MethodHook()
		{
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable
			{
				KIC = param.thisObject;
			}
		});
		
		//setting charing status for batterybar and batteryicon
		hookAllMethods(BatteryTrackerClass, "onRefreshBatteryInfo", new XC_MethodHook()
		{
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable
			{
				int mChargingSpeed = getIntField(KIC, "mChargingSpeed");
				if(mChargingSpeed == CHARGING_FAST) {
					BatteryBarView.setIsFastCharging(true);
					BatteryStyleManager.setIsFastCharging(true);
				} else {
					BatteryBarView.setIsFastCharging(false);
					BatteryStyleManager.setIsFastCharging(false);
				}
			}
		});
		
		//getting statusbar class for further use
		hookAllConstructors(CollapsedStatusBarFragmentClass, new XC_MethodHook()
		{
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable
			{
				mCollapsedStatusBarFragment = param.thisObject;
			}
		});

/*        //getting statusbarview for further use
        hookAllConstructors(PhoneStatusBarViewClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                STB = param.thisObject;
            }
        });*/
		
		//update statusbar
		hookAllMethods(PhoneStatusBarViewClass, "onConfigurationChanged", new XC_MethodHook()
		{
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable
			{
				new Timer().schedule(new TimerTask()
				{
					@Override
					public void run()
					{
						tuneCenterArea();
						if(BatteryBarView.hasInstance()) {
							BatteryBarView.getInstance()
							              .post(() -> refreshBatteryBar(BatteryBarView.getInstance()));
						}
					}
				}, 2000);
			}
		});
		
		//getting activitity starter for further use
		hookAllConstructors(QuickStatusBarHeaderClass, new XC_MethodHook()
		{
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable
			{
				QSBH = param.thisObject;
				NTQSHolder = new FrameLayout(mContext);
				FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				lp.gravity = Gravity.CENTER_HORIZONTAL;
				NTQSHolder.setLayoutParams(lp);
				((FrameLayout) QSBH).addView(NTQSHolder);
				mActivityStarter = callStaticMethod(DependencyClass, "get", ActivityStarterClass);
				placeNTQS();
			}
		});
		
		//marking clock instances for recognition and setting click actions on some icons
		findAndHookMethod(QuickStatusBarHeaderClass, "onFinishInflate", new XC_MethodHook()
		{
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable
			{
				//Clickable icons
				Object mBatteryRemainingIcon = getObjectField(param.thisObject, "mBatteryRemainingIcon");
				Object mDateView = getObjectField(param.thisObject, "mDateView");
				Object mClockView = getObjectField(param.thisObject, "mClockView");
				
				ClickListener clickListener = new ClickListener(param.thisObject);
				
				try {
					callMethod(mBatteryRemainingIcon, "setOnClickListener", clickListener);
					callMethod(mClockView, "setOnClickListener", clickListener);
					callMethod(mClockView, "setOnLongClickListener", clickListener);
					callMethod(mDateView, "setOnClickListener", clickListener);
					callMethod(mDateView, "setOnLongClickListener", clickListener);
				} catch(Exception ignored) {
				}
			}
		});
		
		//show/hide vibration icon from system icons
		hookAllConstructors(KeyguardStatusBarViewControllerClass, new XC_MethodHook()
		{
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable
			{
				//Removing vibration icon from blocked icons in lockscreen
				if(showVibrationIcon && (findFieldIfExists(KeyguardStatusBarViewControllerClass, "mBlockedIcons") != null)) { //Android 12 doesn't have such thing at all
					@SuppressWarnings("unchecked") List<String> OldmBlockedIcons = (List<String>) getObjectField(param.thisObject, "mBlockedIcons");
					
					List<String> NewmBlockedIcons = new ArrayList<>();
					for(String item : OldmBlockedIcons) {
						if(! item.equals("volume")) {
							NewmBlockedIcons.add(item);
						}
					}
					setObjectField(param.thisObject, "mBlockedIcons", NewmBlockedIcons);
				}
			}
		});
		
		//understanding when to hide the battery bar and network traffic: when clock goes to hiding
		hookAllMethods(CollapsedStatusBarFragmentClass, "hideClock", new XC_MethodHook()
		{
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable
			{
				for(ClockVisibilityCallback c : clockVisibilityCallbacks) {
					try {
						c.OnVisibilityChanged(false);
					} catch(Exception ignored) {
					}
				}
			}
		});
		
		//restoring batterybar and network traffic: when clock goes back to life
		findAndHookMethod(CollapsedStatusBarFragmentClass, "showClock", boolean.class, new XC_MethodHook()
		{
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable
			{
				for(ClockVisibilityCallback c : clockVisibilityCallbacks) {
					try {
						c.OnVisibilityChanged(true);
					} catch(Exception ignored) {
					}
				}
			}
		});
		
		//modding clock, adding additional objects,
		findAndHookMethod(CollapsedStatusBarFragmentClass, "onViewCreated", View.class, Bundle.class, new XC_MethodHook()
		{
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable
			{
				
				mStatusBarIconController = getObjectField(param.thisObject, "mStatusBarIconController");
				
				mNotificationIconAreaInner = (ViewGroup) getObjectField(param.thisObject, "mNotificationIconAreaInner");
				
				try {
					mClockView = (View) getObjectField(param.thisObject, "mClockView");
				} catch(Throwable t) { //PE Plus
					Object mClockController = getObjectField(param.thisObject, "mClockController");
					mClockView = (View) callMethod(mClockController, "getClock");
				}
				
				mClockParent = (ViewGroup) mClockView.getParent();
				
				mCenteredIconArea = (View) getObjectField(param.thisObject, "mCenteredIconArea");
				mSystemIconArea = (LinearLayout) getObjectField(param.thisObject, "mSystemIconArea");
				
				mStatusBar = (View) getObjectField(mCollapsedStatusBarFragment, "mStatusBar");
				fullStatusbar = (FrameLayout) mStatusBar.getParent();
				
				makeLeftSplitArea();
				
				tuneCenterArea();
				
				if(BBarEnabled) //in case we got the config but view wasn't ready yet
				{
					placeBatteryBar();
				}
				
				if(VolteIconEnabled) //in case we got the config but context wasn't ready yet
				{
					initVolte();
				}
				
				if(networkOnSBEnabled) {
					networkTrafficSB = NetworkTraffic.getInstance(mContext, true);
					placeNTSB();
				}
				
				//<Showing vibration icon in collapsed statusbar>
				if(showVibrationIcon) {
					setShowVibrationIcon();
				}
				//</Showing vibration icon in collapsed statusbar>
				
				
				//<modding clock>
				placeClock();
			}
		});
		
		//clock mods
		
		findAndHookMethod(ClockClass, "getSmallTime", new XC_MethodHook()
		{
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable
			{
				setObjectField(param.thisObject, "mAmPmStyle", AM_PM_STYLE_GONE);
				setObjectField(param.thisObject, "mShowSeconds", mShowSeconds);
			}
			
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable
			{
				if(param.thisObject != mClockView)
					return; //We don't want custom format in QS header. do we?
				
				SpannableStringBuilder result = new SpannableStringBuilder();
				result.append(getFormattedString(mStringFormatBefore, mBeforeSmall, mBeforeClockColor)); //before clock
				SpannableStringBuilder clockText = SpannableStringBuilder.valueOf((CharSequence) param.getResult()); //THE clock
				if(mClockColor != null) {
					clockText.setSpan(new NetworkTraffic.trafficStyle(mClockColor), 0, (clockText).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				result.append(clockText);
				if(mAmPmStyle != AM_PM_STYLE_GONE) {
					result.append(getFormattedString("$Ga", mAmPmStyle == AM_PM_STYLE_SMALL, mClockColor));
				}
				result.append(getFormattedString(mStringFormatAfter, mAfterSmall, mAfterClockColor)); //after clock
				param.setResult(result);
			}
		});
		
		//Getting QS text color for Network traffic
		hookAllMethods(QuickStatusBarHeaderClass, "onAttach", new XC_MethodHook()
		{
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable
			{
				int fillColor = (int) callStaticMethod(UtilsClass, "getColorAttrDefaultColor", mContext, mContext.getResources()
				                                                                                                 .getIdentifier("@android:attr/textColorPrimary", "attr", mContext.getPackageName()));
				NetworkTraffic.setTintColor(fillColor, false);
			}
		});
		
		//using clock colors for network traffic and battery bar
		hookAllMethods(ClockClass, "onDarkChanged", new XC_MethodHook()
		{
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable
			{
				if(param.thisObject != mClockView)
					return; //We don't want colors of QS header. only statusbar
				
				clockColor = ((TextView) param.thisObject).getTextColors().getDefaultColor();
				NetworkTraffic.setTintColor(clockColor, true);
				if(BatteryBarView.hasInstance()) {
					refreshBatteryBar(BatteryBarView.getInstance());
				}
			}
		});
	}
	
	//region double row left area
	private void setNotificationVisibility()
	{
		try {
			HashMap<?, ?> mIconStates = (HashMap<?, ?>) getObjectField(mNotificationIconContainer, "mIconStates");
			boolean shouldShow = mIconStates.size() > 0;
			
			mNotificationIconContainer.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
			LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mLeftExtraRowContainer.getLayoutParams();
			lp.height = (shouldShow) ? ViewGroup.LayoutParams.WRAP_CONTENT : ViewGroup.LayoutParams.MATCH_PARENT;
		} catch(Throwable ignored) {
		}
	}
	//endregion
	
	private void makeLeftSplitArea()
	{
		mNotificationIconContainer = mNotificationIconAreaInner.findViewById(mContext.getResources()
		                                                                             .getIdentifier("notificationIcons", "id", mContext.getPackageName()));
		
		if(mLeftVerticalSplitContainer == null) {
			mLeftVerticalSplitContainer = new LinearLayout(mContext);
		} else {
			mLeftVerticalSplitContainer.removeAllViews();
			if(mLeftVerticalSplitContainer.getParent() != null)
				((ViewGroup) mLeftVerticalSplitContainer.getParent()).removeView(mLeftVerticalSplitContainer);
		}
		mLeftVerticalSplitContainer.setLayoutParams(mNotificationIconContainer.getLayoutParams());
		mLeftVerticalSplitContainer.setOrientation(LinearLayout.VERTICAL);
		
		mLeftExtraRowContainer = new LinearLayout(mContext);
		mLeftExtraRowContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

/*        LayoutTransition layoutTransition = new LayoutTransition();
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING);
        layoutTransition.setDuration(1000);
        mLeftExtraRowContainer.setLayoutTransition(layoutTransition);*/
		
		ViewGroup parent = ((ViewGroup) mNotificationIconContainer.getParent());
		if(parent != null) {
			parent.removeView(mNotificationIconContainer);
		}
		
		mLeftVerticalSplitContainer.addView(mLeftExtraRowContainer);
		mLeftVerticalSplitContainer.addView(mNotificationIconContainer);
		
		mNotificationIconAreaInner.addView(mLeftVerticalSplitContainer);
		
		setNotificationVisibility();
	}
	
	//region battery bar related
	private void refreshBatteryBar(BatteryBarView instance)
	{
		BatteryBarView.setStaticColor(batteryLevels, batteryColors, indicateCharging, chargingColor, indicateFastCharging, fastChargingColor, BBarTransitColors);
		instance.setVisibility((BBarEnabled) ? View.VISIBLE : View.GONE);
		instance.setColorful(BBarColorful);
		instance.setOnlyWhileCharging(BBOnlyWhileCharging);
		instance.setOnTop(! BBOnBottom);
		instance.setSingleColorTone(clockColor);
		instance.setAlphaPct(BBOpacity);
		instance.setBarHeight(Math.round(BBarHeight / 10f) + 5);
		instance.setCenterBased(BBSetCentered);
		instance.refreshLayout();
	}
	
	private void placeBatteryBar()
	{
		try {
			fullStatusbar.addView(BatteryBarView.getInstance(mContext));
			refreshBatteryBar(BatteryBarView.getInstance());
		} catch(Throwable ignored) {
		}
	}
	
	//region volte related
	private void initVolte()
	{
		
		try {
			if(! telephonyCallbackRegistered) {
				Icon volteIcon = Icon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.ic_volte);
				//noinspection JavaReflectionMemberAccess
				volteStatusbarIcon = StatusBarIcon.getDeclaredConstructor(UserHandle.class, String.class, Icon.class, int.class, int.class, CharSequence.class)
				                                  .newInstance(UserHandle.class.getDeclaredConstructor(int.class)
				                                                               .newInstance(0), BuildConfig.APPLICATION_ID, volteIcon, 0, 0, "volte");
				SystemUtils.TelephonyManager().registerTelephonyCallback(volteExec, volteCallback);
				telephonyCallbackRegistered = true;
			}
		} catch(Exception ignored) {
		}
	}
	
	private void removeVolte()
	{
		try {
			SystemUtils.TelephonyManager().unregisterTelephonyCallback(volteCallback);
			telephonyCallbackRegistered = false;
		} catch(Exception ignored) {
		}
		removeVolteIcon();
	}
	//endregion
	
	private void updateVolte()
	{
		int newVolteState = (Boolean) callMethod(SystemUtils.TelephonyManager(), "isVolteAvailable") ? VOLTE_AVAILABLE : VOLTE_NOT_AVAILABLE;
		if(lastVolteState != newVolteState) {
			lastVolteState = newVolteState;
			switch(newVolteState) {
				case VOLTE_AVAILABLE:
					mStatusBar.post(() -> {
						try {
							callMethod(mStatusBarIconController, "setIcon", "volte", volteStatusbarIcon);
						} catch(Exception ignored) {
						}
					});
					break;
				case VOLTE_NOT_AVAILABLE:
					removeVolteIcon();
					break;
			}
		}
	}
	//endregion
	
	private void removeVolteIcon()
	{
		if(mStatusBar == null)
			return; //probably it's too soon to have a statusbar
		mStatusBar.post(() -> {
			try {
				callMethod(mStatusBarIconController, "removeIcon", "volte");
			} catch(Exception ignored) {
			}
		});
	}
	//endregion
	
	//region vibrationicon related
	private void setShowVibrationIcon()
	{
		try {
			@SuppressWarnings("unchecked") List<String> mBlockedIcons = (List<String>) getObjectField(mCollapsedStatusBarFragment, "mBlockedIcons");
			Object mStatusBarIconController = getObjectField(mCollapsedStatusBarFragment, "mStatusBarIconController");
			Object mDarkIconManager = getObjectField(mCollapsedStatusBarFragment, "mDarkIconManager");
			
			if(showVibrationIcon) {
				mBlockedIcons.remove("volume");
			} else {
				mBlockedIcons.add("volume");
			}
			callMethod(mDarkIconManager, "setBlockList", mBlockedIcons);
			callMethod(mStatusBarIconController, "refreshIconGroups");
		} catch(Throwable ignored) {
		}
	}
	//endregion
	
	//region network traffic related
	private void placeNTSB()
	{
		if(networkTrafficSB == null) {
			return;
		}
		try {
			((ViewGroup) networkTrafficSB.getParent()).removeView(networkTrafficSB);
		} catch(Exception ignored) {
		}
		if(! networkOnSBEnabled)
			return;
		
		try {
			LinearLayout.LayoutParams ntsbLayoutP;
			switch(networkTrafficPosition) {
				case POSITION_RIGHT:
					mSystemIconArea.addView(networkTrafficSB, 0);
					networkTrafficSB.setPadding(rightClockPadding, 0, leftClockPadding, 0);
					break;
				case POSITION_LEFT:
					mClockParent.addView(networkTrafficSB, 0);
					networkTrafficSB.setPadding(0, 0, leftClockPadding, 0);
					break;
				case POSITION_LEFT_EXTRA_LEVEL:
					mLeftExtraRowContainer.addView(networkTrafficSB, 0);
					networkTrafficSB.setPadding(0, 0, leftClockPadding, 0);
					break;
				case POSITION_CENTER:
					mClockParent.addView(networkTrafficSB);
					networkTrafficSB.setPadding(rightClockPadding, 0, leftClockPadding, 0);
					break;
			}
			ntsbLayoutP = (LinearLayout.LayoutParams) networkTrafficSB.getLayoutParams();
			ntsbLayoutP.gravity = Gravity.CENTER_VERTICAL;
			networkTrafficSB.setLayoutParams(ntsbLayoutP);
			//            networkTrafficSB.setPadding(10, 0, 10, 0);
		} catch(Throwable ignored) {
		}
	}
	
	//region clock and date related
	private void placeClock()
	{
		ViewGroup parent = (ViewGroup) mClockView.getParent();
		ViewGroup targetArea = null;
		Integer index = null;
		
		switch(clockPosition) {
			case POSITION_LEFT:
				targetArea = mClockParent;
				index = 0;
				mClockView.setPadding(0, 0, leftClockPadding, 0);
				break;
			case POSITION_LEFT_EXTRA_LEVEL:
				targetArea = mLeftExtraRowContainer;
				mClockView.setPadding(0, 0, leftClockPadding, 0);
				break;
			case POSITION_CENTER:
				targetArea = (ViewGroup) mCenteredIconArea.getParent();
				mClockView.setPadding(rightClockPadding, 0, rightClockPadding, 0);
				break;
			case POSITION_RIGHT:
				mClockView.setPadding(rightClockPadding, 0, 0, 0);
				targetArea = ((ViewGroup) mSystemIconArea.getParent());
				break;
		}
		parent.removeView(mClockView);
		if(index != null) {
			targetArea.addView(mClockView, index);
		} else {
			targetArea.addView(mClockView);
		}
	}
	
	private CharSequence getFormattedString(String dateFormat, boolean small, @Nullable @ColorInt Integer textColor)
	{
		if(dateFormat.length() == 0)
			return "";
		
		//There's some format to work on
		SpannableStringBuilder formatted = new SpannableStringBuilder(stringFormatter.formatString(dateFormat));
		
		if(small) {
			//small size requested
			CharacterStyle style = new RelativeSizeSpan(0.7f);
			formatted.setSpan(style, 0, formatted.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		if(textColor != null) {
			formatted.setSpan(new NetworkTraffic.trafficStyle(textColor), 0, (formatted).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		
		return formatted;
	}
	
	public interface ClockVisibilityCallback extends Callback
	{
		void OnVisibilityChanged(boolean isVisible);
	}
	
	private class serverStateCallback extends TelephonyCallback
			implements TelephonyCallback.ServiceStateListener
	{
		@Override
		public void onServiceStateChanged(@NonNull ServiceState serviceState)
		{
			updateVolte();
		}
	}
	
	//region icon tap related
	class ClickListener implements View.OnClickListener, View.OnLongClickListener
	{
		Object parent;
		
		public ClickListener(Object parent)
		{
			this.parent = parent;
		}
		
		@Override
		public void onClick(View v)
		{
			Object mBatteryRemainingIcon = getObjectField(parent, "mBatteryRemainingIcon");
			Object mDateView = getObjectField(parent, "mDateView");
			Object mClockView = getObjectField(parent, "mClockView");
			boolean mExpanded = (boolean) getObjectField(parent, "mExpanded");
			
			
			if(v.equals(mBatteryRemainingIcon)) {
				callMethod(mActivityStarter, "postStartActivityDismissingKeyguard", new Intent(Intent.ACTION_POWER_USAGE_SUMMARY), 0);
			} else if(mExpanded && v.equals(mClockView)) {
				callMethod(mActivityStarter, "postStartActivityDismissingKeyguard", new Intent(AlarmClock.ACTION_SHOW_ALARMS), 0);
			} else if(v == mDateView || (v == mClockView && ! mExpanded)) {
				Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
				builder.appendPath("time");
				builder.appendPath(Long.toString(java.lang.System.currentTimeMillis()));
				Intent todayIntent = new Intent(Intent.ACTION_VIEW, builder.build());
				callMethod(mActivityStarter, "postStartActivityDismissingKeyguard", todayIntent, 0);
			}
		}
		
		@Override
		public boolean onLongClick(View v)
		{
			Object mDateView = getObjectField(parent, "mDateView");
			Object mClockView = getObjectField(parent, "mClockView");
			
			if(v == mClockView || v == mDateView) {
				Intent mIntent = new Intent(Intent.ACTION_MAIN);
				mIntent.setClassName("com.android.settings", "com.android.settings.Settings$DateTimeSettingsActivity");
				callMethod(mActivityStarter, "startActivity", mIntent, true /* dismissShade */);
				//                mVibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
				return true;
			}
			return false;
		}
	}
	//endregion
}
