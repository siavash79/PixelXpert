package sh.siava.AOSPMods.systemui;

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
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;

import javax.security.auth.callback.Callback;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.BuildConfig;
import sh.siava.AOSPMods.R;
import sh.siava.AOSPMods.Utils.NetworkTraffic;
import sh.siava.AOSPMods.Utils.StringFormatter;
import sh.siava.AOSPMods.Utils.SystemUtils;
import sh.siava.AOSPMods.Utils.batteryStyles.BatteryBarView;
import sh.siava.AOSPMods.XPrefs;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings({"RedundantThrows", "ConstantConditions"})

public class StatusbarMods extends XposedModPack {
    private static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;

    //region battery
    private static final int CHARGING_FAST = 2;
    //endregion
    
    //region Clock
    private static final int POSITION_LEFT = 0;
    private static final int POSITION_CENTER = 1;
    private static final int POSITION_RIGHT = 2;

//    private static final int AM_PM_STYLE_NORMAL  = 0;
//    private static final intAM_PM_STYLE_SMALL   = 1;
    private static final int AM_PM_STYLE_GONE    = 2;

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
    private static float[] batteryLevels = new float[]{20f, 40f};
    private static int[] batteryColors = new int[]{Color.RED, Color.YELLOW};
    private static int chargingColor = Color.WHITE;
    private static int fastChargingColor = Color.WHITE;
    private static boolean indicateCharging = false;
    private static boolean indicateFastCharging = false;
    private static boolean BBarTransitColors = false;
    //endregion
    
    //region general use
    private static final ArrayList<ClockVisibilityCallback> clockVisibilityCallbacks = new ArrayList<>();
    private Object mActivityStarter;
    private Object KIC = null;
    private Object QSBH = null;
    private View mStatusBar;

    private Object mCollapsedStatusBarFragment = null;
    private ViewGroup mClockParent = null;
    private View mNotificationIconAreaInner = null;
    private View mCenteredIconArea = null;
    private LinearLayout mSystemIconArea = null;
    public static int clockColor = 0;
    private FrameLayout fullStatusbar;
//    private Object STB = null;
    private int centerAreaFineTune = 50;
    //endregion
    
    //region volte
    private static final int VOLTE_AVAILABLE = 2;
    private static final int VOLTE_NOT_AVAILABLE = 1;
    private static final int VOLTE_UNKNOWN = -1;
    
    private static boolean VolteIconEnabled = false;
    private final Executor volteExec = Runnable::run;
    
    private Object mStatusBarIconController;
    private Class<?> StatusBarIcon;
    private Object volteStatusbarIcon;
    private boolean telephonyCallbackRegistered = false;
    private int lastVolteState = VOLTE_UNKNOWN;
    private final serverStateCallback volteCallback = new serverStateCallback();
    private View mClockView;
    //endregion
    
    public StatusbarMods(Context context) { super(context); }
    
    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
    
    public void updatePrefs(String...Key)
    {
        if(XPrefs.Xprefs == null) return;
        
        centerAreaFineTune = XPrefs.Xprefs.getInt("centerAreaFineTune", 50);
        tuneCenterArea();
        
        //region BatteryBar Settings
        BBarEnabled = XPrefs.Xprefs.getBoolean("BBarEnabled", false);
        BBarColorful = XPrefs.Xprefs.getBoolean("BBarColorful", false);
        BBOnlyWhileCharging = XPrefs.Xprefs.getBoolean("BBOnlyWhileCharging", false);
        BBOnBottom = XPrefs.Xprefs.getBoolean("BBOnBottom", false);
        BBSetCentered = XPrefs.Xprefs.getBoolean("BBSetCentered", false);
        BBOpacity = XPrefs.Xprefs.getInt("BBOpacity" , 100);
        BBarHeight = XPrefs.Xprefs.getInt("BBarHeight" , 50);
        BBarTransitColors = XPrefs.Xprefs.getBoolean("BBarTransitColors", false);
        
        String jsonString = XPrefs.Xprefs.getString("batteryWarningRange", "");
        if(jsonString.length() > 0)
        {
            batteryLevels = new float[]{
                    RangeBarHelper.getLowValueFromJsonString(jsonString),
                    RangeBarHelper.getHighValueFromJsonString(jsonString)};
        }
    
        batteryColors = new int[]{
                XPrefs.Xprefs.getInt("batteryCriticalColor", Color.RED),
                XPrefs.Xprefs.getInt("batteryWarningColor", Color.YELLOW)};
        
    
        indicateFastCharging = XPrefs.Xprefs.getBoolean("indicateFastCharging", false);
        indicateCharging = XPrefs.Xprefs.getBoolean("indicateCharging", true);
    
        chargingColor = XPrefs.Xprefs.getInt("batteryChargingColor", Color.GREEN);
        fastChargingColor = XPrefs.Xprefs.getInt("batteryFastChargingColor", Color.BLUE);
        
        if(BBarEnabled)
        {
            placeBatteryBar();
        }
        
        if(BatteryBarView.hasInstance())
        {
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


        if(networkOnSBEnabled || networkOnQSEnabled)
        {
            networkTrafficPosition = -1; //anyway we have to call placer method
            int newnetworkTrafficPosition = Integer.parseInt(XPrefs.Xprefs.getString("networkTrafficPosition", "2"));

            String thresholdText = XPrefs.Xprefs.getString("networkTrafficThreshold", "10");

            int networkTrafficThreshold;
            try {
                networkTrafficThreshold = Math.round(Float.parseFloat(thresholdText));
            }
            catch (Exception e) {
                networkTrafficThreshold = 10;
            }
            if(newnetworkTrafficPosition != networkTrafficPosition)
            {
                networkTrafficPosition = newnetworkTrafficPosition;
            }
            NetworkTraffic.setConstants(networkTrafficInterval, networkTrafficThreshold, networkTrafficMode, networkTrafficRXTop, networkTrafficColorful, networkTrafficDLColor, networkTrafficULColor, networkTrafficOpacity);

        }
        if(networkOnSBEnabled)
        {
            networkTrafficSB = NetworkTraffic.getInstance(mContext, true);
            networkTrafficSB.update();
        }
        if(networkOnQSEnabled)
        {
            networkTrafficQS = NetworkTraffic.getInstance(mContext, false);
            networkTrafficQS.update();
        }
        placeNTSB();
        placeNTQS();

        //endregion network settings

        //region vibration settings
        boolean newshowVibrationIcon = XPrefs.Xprefs.getBoolean("SBshowVibrationIcon", false);
        if(newshowVibrationIcon != showVibrationIcon)
        {
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

        if(XPrefs.Xprefs.getBoolean("SBCClockColorful", false))
        {
            mClockColor = XPrefs.Xprefs.getInt("SBCClockColor", Color.WHITE);
            mBeforeClockColor = XPrefs.Xprefs.getInt("SBCBeforeClockColor", Color.WHITE);
            mAfterClockColor = XPrefs.Xprefs.getInt("SBCAfterClockColor", Color.WHITE);
        }
        else
        {
            mClockColor
                    = mBeforeClockColor
                    = mAfterClockColor
                    = null;
        }
        

        if((mStringFormatBefore + mStringFormatAfter).trim().length() == 0) {
            int SBCDayOfWeekMode = Integer.parseInt(XPrefs.Xprefs.getString("SBCDayOfWeekMode", "0"));

            switch (SBCDayOfWeekMode)
            {
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
            XposedHelpers.callMethod(mClockView, "getSmallTime");
        }catch(Throwable ignored){}
        //endregion clock settings
        
    
        //region volte
        VolteIconEnabled = XPrefs.Xprefs.getBoolean("VolteIconEnabled", false);
        if(VolteIconEnabled)
            initVolte();
        else
            removeVolte();
        //endregion
        
    }

    private void placeNTQS() {
        if(networkTrafficQS == null)
        {
            return;
        }
        try
        {
            ((ViewGroup) networkTrafficQS.getParent()).removeView(networkTrafficQS);
        }
        catch(Throwable ignored){}
        if(!networkOnQSEnabled) return;

        try {
            NTQSHolder.addView(networkTrafficQS);
        }
        catch (Throwable ignored){}
    }

    //region general
    private void tuneCenterArea() {
        try {
            int screenWidth = fullStatusbar.getMeasuredWidth();
            int notificationWidth = (screenWidth * centerAreaFineTune / 100);
            ((ViewGroup) mNotificationIconAreaInner.getParent().getParent().getParent()).getLayoutParams().width = notificationWidth;
            mSystemIconArea.getLayoutParams().width = screenWidth - notificationWidth;
        } catch (Exception ignored) { }
    }
    //endregion
    
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;
        

        //region needed classes
        Class<?> ActivityStarterClass = XposedHelpers.findClass("com.android.systemui.plugins.ActivityStarter", lpparam.classLoader);
        Class<?> DependencyClass = XposedHelpers.findClass("com.android.systemui.Dependency", lpparam.classLoader);
        Class<?> CollapsedStatusBarFragmentClass;
        Class<?> UtilsClass = XposedHelpers.findClass("com.android.settingslib.Utils", lpparam.classLoader);
        Class<?> KeyguardStatusBarViewControllerClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.KeyguardStatusBarViewController", lpparam.classLoader);
//        Class<?> QuickStatusBarHeaderControllerClass = XposedHelpers.findClass("com.android.systemui.qs.QuickStatusBarHeaderController", lpparam.classLoader);
        Class<?> QuickStatusBarHeaderClass = XposedHelpers.findClass("com.android.systemui.qs.QuickStatusBarHeader", lpparam.classLoader);
        Class<?> ClockClass = XposedHelpers.findClass("com.android.systemui.statusbar.policy.Clock", lpparam.classLoader);
        Class<?> PhoneStatusBarViewClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.PhoneStatusBarView", lpparam.classLoader);
        Class<?> KeyGuardIndicationClass = XposedHelpers.findClass("com.android.systemui.statusbar.KeyguardIndicationController", lpparam.classLoader);
        Class<?> BatteryTrackerClass = XposedHelpers.findClass("com.android.systemui.statusbar.KeyguardIndicationController$BaseKeyguardCallback", lpparam.classLoader);
        StatusBarIcon = XposedHelpers.findClass("com.android.internal.statusbar.StatusBarIcon", lpparam.classLoader);
        
        CollapsedStatusBarFragmentClass = XposedHelpers.findClassIfExists("com.android.systemui.statusbar.phone.fragment.CollapsedStatusBarFragment", lpparam.classLoader);

        if(CollapsedStatusBarFragmentClass == null)
        {
            CollapsedStatusBarFragmentClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.CollapsedStatusBarFragment", lpparam.classLoader);
        }
        //endregion
        
        // needed to check fastcharging
        XposedBridge.hookAllConstructors(KeyGuardIndicationClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                KIC = param.thisObject;
            }
        });
        
        //setting charing status for batterybar and batteryicon
        XposedBridge.hookAllMethods(BatteryTrackerClass, "onRefreshBatteryInfo", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                int mChargingSpeed = XposedHelpers.getIntField(KIC, "mChargingSpeed");
                if(mChargingSpeed == CHARGING_FAST)
                {
                    BatteryBarView.setIsFastCharging(true);
                    BatteryStyleManager.setIsFastCharging(true);
                }
                else
                {
                    BatteryBarView.setIsFastCharging(false);
                    BatteryStyleManager.setIsFastCharging(false);
                }
            }
        });
        
        //getting statusbar class for further use
        XposedBridge.hookAllConstructors(CollapsedStatusBarFragmentClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mCollapsedStatusBarFragment = param.thisObject;
            }
        });

/*        //getting statusbarview for further use
        XposedBridge.hookAllConstructors(PhoneStatusBarViewClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                STB = param.thisObject;
            }
        });*/
        
        //update statusbar
        XposedBridge.hookAllMethods(PhoneStatusBarViewClass, "onConfigurationChanged", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        tuneCenterArea();
                        if(BatteryBarView.hasInstance())
                        {
                            BatteryBarView.getInstance().post(() -> refreshBatteryBar(BatteryBarView.getInstance()));
                        }
                    }
                }, 200);
            }
        });
        
        //getting activitity starter for further use
        XposedBridge.hookAllConstructors(QuickStatusBarHeaderClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                QSBH = param.thisObject;
                NTQSHolder = new FrameLayout(mContext);
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.gravity = Gravity.CENTER_HORIZONTAL;
                NTQSHolder.setLayoutParams(lp);
                ((FrameLayout)QSBH).addView(NTQSHolder);
                mActivityStarter = XposedHelpers.callStaticMethod(DependencyClass, "get", ActivityStarterClass);
                placeNTQS();
            }
        });

        //marking clock instances for recognition and setting click actions on some icons
        XposedHelpers.findAndHookMethod(QuickStatusBarHeaderClass,
                "onFinishInflate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        //Clickable icons
                        Object mBatteryRemainingIcon = XposedHelpers.getObjectField(param.thisObject, "mBatteryRemainingIcon");
                        Object mDateView = XposedHelpers.getObjectField(param.thisObject, "mDateView");
                        Object mClockView = XposedHelpers.getObjectField(param.thisObject, "mClockView");

                        ClickListener clickListener = new ClickListener(param.thisObject);

                        try {
                            XposedHelpers.callMethod(mBatteryRemainingIcon, "setOnClickListener", clickListener);
                            XposedHelpers.callMethod(mClockView, "setOnClickListener", clickListener);
                            XposedHelpers.callMethod(mClockView, "setOnLongClickListener", clickListener);
                            XposedHelpers.callMethod(mDateView, "setOnClickListener", clickListener);
                            XposedHelpers.callMethod(mDateView, "setOnLongClickListener", clickListener);
                        }catch(Exception ignored) {}
                    }
                });

        //show/hide vibration icon from system icons
        XposedBridge.hookAllConstructors(KeyguardStatusBarViewControllerClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                //Removing vibration icon from blocked icons in lockscreen
                if(showVibrationIcon && (XposedHelpers.findFieldIfExists(KeyguardStatusBarViewControllerClass, "mBlockedIcons") != null)) { //Android 12 doesn't have such thing at all
                    @SuppressWarnings("unchecked") List<String> OldmBlockedIcons = (List<String>) XposedHelpers.getObjectField(param.thisObject, "mBlockedIcons");

                    List<String> NewmBlockedIcons = new ArrayList<>();
                    for (String item : OldmBlockedIcons) {
                        if (!item.equals("volume")) {
                            NewmBlockedIcons.add(item);
                        }
                    }
                    XposedHelpers.setObjectField(param.thisObject, "mBlockedIcons", NewmBlockedIcons);
                }
            }
        });

        //understanding when to hide the battery bar and network traffic: when clock goes to hiding
        XposedBridge.hookAllMethods(CollapsedStatusBarFragmentClass,
                "hideClock", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        for(ClockVisibilityCallback c : clockVisibilityCallbacks)
                        {
                            try
                            {
                                c.OnVisibilityChanged(false);
                            } catch (Exception ignored){}
                        }
                    }
                });

        //restoring batterybar and network traffic: when clock goes back to life
        XposedHelpers.findAndHookMethod(CollapsedStatusBarFragmentClass,
                "showClock", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        for(ClockVisibilityCallback c : clockVisibilityCallbacks)
                        {
                            try {
                                c.OnVisibilityChanged(true);
                            }catch (Exception ignored){}
                        }
                    }
                });


        //modding clock, adding additional objects,
        XposedHelpers.findAndHookMethod(CollapsedStatusBarFragmentClass,
                "onViewCreated", View.class, Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                        mStatusBarIconController = XposedHelpers.getObjectField(param.thisObject, "mStatusBarIconController");
                        
                        mNotificationIconAreaInner = (View) XposedHelpers.getObjectField(param.thisObject, "mNotificationIconAreaInner");
                        
                        try
                        {
                            mClockView = (View) XposedHelpers.getObjectField(param.thisObject, "mClockView");
                        }
                        catch (Throwable t)
                        { //PE Plus
                            Object mClockController = XposedHelpers.getObjectField(param.thisObject, "mClockController");
                            mClockView = (View) XposedHelpers.callMethod(mClockController, "getClock");
                        }

                        mClockParent = (ViewGroup) mClockView.getParent();
    
                        mCenteredIconArea = (View) XposedHelpers.getObjectField(param.thisObject, "mCenteredIconArea");
                        mSystemIconArea = (LinearLayout) XposedHelpers.getObjectField(param.thisObject, "mSystemIconArea");
    
                        mStatusBar = (View) XposedHelpers.getObjectField(mCollapsedStatusBarFragment, "mStatusBar");
                        fullStatusbar = (FrameLayout) mStatusBar.getParent();

                        tuneCenterArea();

                        if(BBarEnabled) //in case we got the config but view wasn't ready yet
                        {
                            placeBatteryBar();
                        }
                        
                        if(VolteIconEnabled) //in case we got the config but context wasn't ready yet
                        {
                            initVolte();
                        }
                        
                        if(networkOnSBEnabled)
                        {
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

        XposedHelpers.findAndHookMethod(ClockClass,
                "getSmallTime", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedHelpers.setObjectField(param.thisObject, "mAmPmStyle", mAmPmStyle);
                        XposedHelpers.setObjectField(param.thisObject, "mShowSeconds", mShowSeconds);
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(param.thisObject != mClockView) return; //We don't want custom format in QS header. do we?

                        Calendar mCalendar = (Calendar) XposedHelpers.getObjectField(param.thisObject, "mCalendar");

                        SpannableStringBuilder result = new SpannableStringBuilder();
                        result.append(getFormattedString(mStringFormatBefore, mCalendar, mBeforeSmall, mBeforeClockColor)); //before clock
                        SpannableStringBuilder clockText = SpannableStringBuilder.valueOf((CharSequence) param.getResult()); //THE clock
                        if(mClockColor != null)
                        {
                            clockText.setSpan(new NetworkTraffic.trafficStyle(mClockColor), 0 , (clockText).length(),
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        result.append(clockText);
                        result.append(getFormattedString(mStringFormatAfter, mCalendar, mAfterSmall, mAfterClockColor)); //after clock
                        param.setResult(result);
                    }
                });

        //Getting QS text color for Network traffic
        XposedBridge.hookAllMethods(QuickStatusBarHeaderClass,
                "onAttach", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        int fillColor = (int) XposedHelpers.callStaticMethod(UtilsClass, "getColorAttrDefaultColor",
                                mContext,
                                mContext.getResources().getIdentifier("@android:attr/textColorPrimary", "attr", mContext.getPackageName()));
                        NetworkTraffic.setTintColor(fillColor, false);
                    }
                });
    
        //using clock colors for network traffic and battery bar
        XposedBridge.hookAllMethods(ClockClass,
                "onDarkChanged", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(param.thisObject != mClockView) return; //We don't want colors of QS header. only statusbar

                        clockColor = ((TextView) param.thisObject).getTextColors().getDefaultColor();
                        NetworkTraffic.setTintColor(clockColor, true);
                        if(BatteryBarView.hasInstance())
                        {
                            refreshBatteryBar(BatteryBarView.getInstance());
                        }
                    }
                });
    }


    //region battery bar related
    private void refreshBatteryBar(BatteryBarView instance) {
        BatteryBarView.setStaticColor(batteryLevels, batteryColors, indicateCharging, chargingColor, indicateFastCharging, fastChargingColor, BBarTransitColors);
        instance.setVisibility((BBarEnabled) ? View.VISIBLE : View.GONE);
        instance.setColorful(BBarColorful);
        instance.setOnlyWhileCharging(BBOnlyWhileCharging);
        instance.setOnTop(!BBOnBottom);
        instance.setSingleColorTone(clockColor);
        instance.setAlphaPct(BBOpacity);
        instance.setBarHeight(Math.round(BBarHeight/10f)+5);
        instance.setCenterBased(BBSetCentered);
        instance.refreshLayout();
    }
    
    private void placeBatteryBar() {
        try {
            fullStatusbar.addView(BatteryBarView.getInstance(mContext));
            refreshBatteryBar(BatteryBarView.getInstance());
        }catch(Throwable ignored){}
    }
    //endregion
    
    //region volte related
    private void initVolte() {
        
        try {
            if (!telephonyCallbackRegistered) {
                Icon volteIcon = Icon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.ic_volte);
                //noinspection JavaReflectionMemberAccess
                volteStatusbarIcon = StatusBarIcon.getDeclaredConstructor(UserHandle.class, String.class, Icon.class, int.class, int.class, CharSequence.class).newInstance(UserHandle.class.getDeclaredConstructor(int.class).newInstance(0), BuildConfig.APPLICATION_ID, volteIcon, 0, 0, "volte");
                SystemUtils.TelephonyManager().registerTelephonyCallback(volteExec, volteCallback);
                telephonyCallbackRegistered = true;
            }
        }catch(Exception ignored){}
    }
    
    private void removeVolte() {
        try
        {
            SystemUtils.TelephonyManager().unregisterTelephonyCallback(volteCallback);
            telephonyCallbackRegistered = false;
        }catch(Exception ignored){}
        removeVolteIcon();
    }
    
    private class serverStateCallback extends TelephonyCallback implements
            TelephonyCallback.ServiceStateListener{
        @Override
        public void onServiceStateChanged(@NonNull ServiceState serviceState) {
            updateVolte();
        }
    }
    
    private void updateVolte()
    {
        int newVolteState = (Boolean) XposedHelpers.callMethod(SystemUtils.TelephonyManager(), "isVolteAvailable") ? VOLTE_AVAILABLE : VOLTE_NOT_AVAILABLE;
        if(lastVolteState != newVolteState)
        {
            lastVolteState = newVolteState;
            switch(newVolteState)
            {
                case VOLTE_AVAILABLE:
                    mStatusBar.post(() -> {
                        try {
                            XposedHelpers.callMethod(mStatusBarIconController, "setIcon", "volte", volteStatusbarIcon);
                        } catch(Exception ignored){}
                    });
                    break;
                case VOLTE_NOT_AVAILABLE:
                    removeVolteIcon();
                    break;
            }
        }
    }
    
    private void removeVolteIcon() {
        if(mStatusBar == null) return; //probably it's too soon to have a statusbar
        mStatusBar.post(() -> {
            try {
                XposedHelpers.callMethod(mStatusBarIconController, "removeIcon", "volte");
            } catch(Exception ignored){}
        });
    }
    //endregion
    
    //region vibrationicon related
    private void setShowVibrationIcon()
    {
        try {
            @SuppressWarnings("unchecked") List<String> mBlockedIcons = (List<String>) XposedHelpers.getObjectField(mCollapsedStatusBarFragment, "mBlockedIcons");
            Object mStatusBarIconController = XposedHelpers.getObjectField(mCollapsedStatusBarFragment, "mStatusBarIconController");
            Object mDarkIconManager = XposedHelpers.getObjectField(mCollapsedStatusBarFragment, "mDarkIconManager");

            if (showVibrationIcon) {
                mBlockedIcons.remove("volume");
            } else {
                mBlockedIcons.add("volume");
            }
            XposedHelpers.callMethod(mDarkIconManager, "setBlockList", mBlockedIcons);
            XposedHelpers.callMethod(mStatusBarIconController, "refreshIconGroups");
        }catch(Throwable ignored){}
    }
    //endregion
    
    //region network traffic related
    private void placeNTSB() {
        if(networkTrafficSB == null)
        {
            return;
        }
        try
        {
            ((ViewGroup) networkTrafficSB.getParent()).removeView(networkTrafficSB);
        }
        catch(Exception ignored){}
        if(!networkOnSBEnabled) return;

        try {
            LinearLayout.LayoutParams ntsbLayoutP;
            switch (networkTrafficPosition) {
                case POSITION_RIGHT:
                    mSystemIconArea.addView(networkTrafficSB, 0);
                    break;
                case POSITION_LEFT:
                    mClockParent.addView(networkTrafficSB, 0);
                    break;
                case POSITION_CENTER:
                    mClockParent.addView(networkTrafficSB);
                    break;
            }
            ntsbLayoutP = (LinearLayout.LayoutParams) networkTrafficSB.getLayoutParams();
            ntsbLayoutP.gravity = Gravity.CENTER_VERTICAL;
            networkTrafficSB.setLayoutParams(ntsbLayoutP);
//            networkTrafficSB.setPadding(10, 0, 10, 0);
        } catch(Throwable ignored){}
    }
    //endregion

    //region icon tap related
    class ClickListener implements View.OnClickListener, View.OnLongClickListener
    {
        Object parent;
        public ClickListener(Object parent)
        {
            this.parent = parent;
        }
        @Override
        public void onClick(View v) {
            Object mBatteryRemainingIcon = XposedHelpers.getObjectField(parent, "mBatteryRemainingIcon");
            Object mDateView = XposedHelpers.getObjectField(parent, "mDateView");
            Object mClockView = XposedHelpers.getObjectField(parent, "mClockView");
            boolean mExpanded = (boolean) XposedHelpers.getObjectField(parent, "mExpanded");


            if(v.equals(mBatteryRemainingIcon))
            {
                XposedHelpers.callMethod(mActivityStarter, "postStartActivityDismissingKeyguard", new Intent(Intent.ACTION_POWER_USAGE_SUMMARY),0);
            }
            else if(mExpanded && v.equals(mClockView))
            {
                XposedHelpers.callMethod(mActivityStarter, "postStartActivityDismissingKeyguard", new Intent(AlarmClock.ACTION_SHOW_ALARMS),0);
            }
            else if (v == mDateView || (v == mClockView && !mExpanded))
            {
                Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
                builder.appendPath("time");
                builder.appendPath(Long.toString(java.lang.System.currentTimeMillis()));
                Intent todayIntent = new Intent(Intent.ACTION_VIEW, builder.build());
                XposedHelpers.callMethod(mActivityStarter, "postStartActivityDismissingKeyguard", todayIntent,0);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            Object mDateView = XposedHelpers.getObjectField(parent, "mDateView");
            Object mClockView = XposedHelpers.getObjectField(parent, "mClockView");

            if (v == mClockView || v == mDateView) {
                Intent mIntent = new Intent(Intent.ACTION_MAIN);
                mIntent.setClassName("com.android.settings",
                        "com.android.settings.Settings$DateTimeSettingsActivity");
                XposedHelpers.callMethod(mActivityStarter, "startActivity", mIntent, true /* dismissShade */);
//                mVibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
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

        switch (clockPosition)
        {
            case POSITION_LEFT:
                targetArea = mClockParent;
                index = 0;
                mClockView.setPadding(0,0,20,0);
                break;
            case POSITION_CENTER:
                targetArea = (ViewGroup) mCenteredIconArea.getParent();
                mClockView.setPadding(20,0,20,0);
                break;
            case POSITION_RIGHT:
                mClockView.setPadding(20,0,0,0);
                targetArea = ((ViewGroup) mSystemIconArea.getParent());
                break;
        }
        parent.removeView(mClockView);
        if(index != null)
        {
            targetArea.addView(mClockView, index);
        }
        else {
            targetArea.addView(mClockView);
        }
    }

    private final StringFormatter stringFormatter = new StringFormatter();
    private CharSequence getFormattedString(String dateFormat, Calendar calendar, boolean small, @Nullable @ColorInt Integer textColor)
    {
        if(dateFormat.length() == 0) return "";

        String result = stringFormatter.formatString(dateFormat).toString();
        //There's some format to work on
        SpannableStringBuilder formatted = new SpannableStringBuilder(result);

        if (small) {
            //small size requested
            CharacterStyle style = new RelativeSizeSpan(0.7f);
            formatted.setSpan(style, 0, formatted.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if(textColor != null)
        {
            formatted.setSpan(new NetworkTraffic.trafficStyle(textColor), 0 , (formatted).length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return formatted;
    }
    //endregion
    //region callbacks
    public static void registerClockVisibilityCallback(ClockVisibilityCallback callback){
        clockVisibilityCallbacks.add(callback);
    }

    @SuppressWarnings("unused")
    public static void unRegisterClockVisibilityCallback(ClockVisibilityCallback callback){
        clockVisibilityCallbacks.remove(callback);
    }

    public interface ClockVisibilityCallback extends Callback {
        void OnVisibilityChanged(boolean isVisible);
    }
    //endregion
}
