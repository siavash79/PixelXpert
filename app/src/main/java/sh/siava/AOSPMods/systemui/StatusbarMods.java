package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nfx.android.rangebarpreference.RangeBarHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.IXposedModPack;
import sh.siava.AOSPMods.Utils.batteryStyles.BatteryBarView;
import sh.siava.AOSPMods.Utils.NetworkTrafficSB;
import sh.siava.AOSPMods.XPrefs;

public class StatusbarMods implements IXposedModPack {
    public static final String listenPackage = "com.android.systemui";
    
    public static final int CHARGING_FAST = 2;

    //Clock Settings
    private static final int POSITION_LEFT = 0;
    private static final int POSITION_CENTER = 1;
    private static final int POSITION_RIGHT = 2;

    private static final int AM_PM_STYLE_NORMAL  = 0;
    private static final int AM_PM_STYLE_SMALL   = 1;
    private static final int AM_PM_STYLE_GONE    = 2;

    public static int clockPosition = POSITION_LEFT;
    public static int mAmPmStyle = AM_PM_STYLE_GONE;
    public static boolean mShowSeconds = false;
    public static String mDateFormatBefore = "", mDateFormatAfter = "";
    public static boolean mBeforeSmall = true, mAfterSmall = true;

    //clickable settings
    Object mActivityStarter;

    //vibration icon
    public static boolean showVibrationIcon = false;

    //network traffic
    public static boolean networkOnSBEnabled = false;
    public static int networkTrafficPosition = POSITION_LEFT;
    public static int networkTrafficTreshold = 10;
    public NetworkTrafficSB networkTrafficSB = null;
    private static Context mContext;
    private static Object mCollapsedStatusBarFragment = null;
    private static View mClockView = null;
    private static ViewGroup mClockParent = null;
    private static View mCenteredIconArea = null;
    private static LinearLayout mSystemIconArea = null;
    public static int clockColor = 0;
    private FrameLayout fullStatusbar;
    
    private static boolean BBarEnabled;
    private static boolean BBarColorful;
    private static boolean BBOnlyWhileCharging;
    private static boolean BBOnBottom;
    private static boolean BBSetCentered;
    private static int BBOpacity = 100;
    private static int BBarHeight = 10;
    private static Object KIC = null;
    private static float[] batteryLevels = new float[]{20f, 40f};
    private static int[] batteryColors = new int[]{Color.RED, Color.YELLOW};
    private static int charingColor = Color.WHITE;
    private static int fastChargingColor = Color.WHITE;
    private static boolean indicateCharging = false;
    private static boolean indicateFastCharging = false;
    private static boolean BBarTransitColors = false;
    
    Object STB;

    public void updatePrefs(String...Key)
    {
        if(XPrefs.Xprefs == null) return;
        
        //BatteryBar Settings
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
    
        charingColor = XPrefs.Xprefs.getInt("batteryChargingColor", Color.GREEN);
        fastChargingColor = XPrefs.Xprefs.getInt("batteryFastChargingColor", Color.BLUE);
        
        if(BatteryBarView.hasInstance())
        {
            refreshBatteryBar(BatteryBarView.getInstance());
        }
        //end BatteryBar Settings
        
        //network Traffic settings
        boolean newnetworkOnSBEnabled = XPrefs.Xprefs.getBoolean("networkOnSBEnabled", false);
        if(newnetworkOnSBEnabled != networkOnSBEnabled)
        {
            networkOnSBEnabled = newnetworkOnSBEnabled;
            if(networkTrafficSB == null && mContext != null)
            {
                networkTrafficSB = new NetworkTrafficSB(mContext);
            }
            networkTrafficPosition = -1; //anyway we have to call placer method
        }
        int newnetworkTrafficPosition = Integer.parseInt(XPrefs.Xprefs.getString("networkTrafficPosition", "2"));
        if(newnetworkTrafficPosition != networkTrafficPosition)
        {
            networkTrafficPosition = newnetworkTrafficPosition;
            placeNTSB();
        }
        String tresholdText = XPrefs.Xprefs.getString("networkTrafficTreshold", "10");

        try {
            networkTrafficTreshold = Math.round(Float.parseFloat(tresholdText));
        }
        catch (Exception e) {
            networkTrafficTreshold = 10;
        }
        finally {
            NetworkTrafficSB.setHideTreshold(networkTrafficTreshold);
        }
        //end network settings

        //vibration settings

        boolean newshowVibrationIcon = XPrefs.Xprefs.getBoolean("SBshowVibrationIcon", false);
        if(newshowVibrationIcon != showVibrationIcon)
        {
            showVibrationIcon = newshowVibrationIcon;
            setShowVibrationIcon();
        }

        //clock settings
        clockPosition = Integer.parseInt(XPrefs.Xprefs.getString("SBClockLoc", String.valueOf(POSITION_LEFT)));
        mShowSeconds = XPrefs.Xprefs.getBoolean("SBCShowSeconds", false);
        mAmPmStyle = Integer.parseInt(XPrefs.Xprefs.getString("SBCAmPmStyle", String.valueOf(AM_PM_STYLE_GONE)));

        mDateFormatBefore = XPrefs.Xprefs.getString("DateFormatBeforeSBC", "");
        mDateFormatAfter = XPrefs.Xprefs.getString("DateFormatAfterSBC", "");
        mBeforeSmall = XPrefs.Xprefs.getBoolean("BeforeSBCSmall", true);
        mAfterSmall = XPrefs.Xprefs.getBoolean("AfterSBCSmall", true);

        if((mDateFormatBefore+mDateFormatAfter).trim().length() == 0) {
            int SBCDayOfWeekMode = Integer.parseInt(XPrefs.Xprefs.getString("SBCDayOfWeekMode", "0"));

            switch (SBCDayOfWeekMode)
            {
                case 0:
                    mDateFormatAfter = mDateFormatBefore = "";
                    break;
                case 1:
                    mDateFormatBefore = "EEE ";
                    mDateFormatAfter = "";
                    mBeforeSmall = false;
                    break;
                case 2:
                    mDateFormatBefore = "EEE ";
                    mDateFormatAfter = "";
                    mBeforeSmall = true;
                    break;
                case 3:
                    mDateFormatBefore = "";
                    mDateFormatAfter = " EEE";
                    mAfterSmall = false;
                    break;
                case 4:
                    mDateFormatBefore = "";
                    mDateFormatAfter = " EEE";
                    mAfterSmall = true;
                    break;
            }
        }
        //end clock settings

        //clickable battery settings

    }
    
    private void refreshBatteryBar(BatteryBarView instance) {
        BatteryBarView.setStaticColor(batteryLevels, batteryColors, indicateCharging, charingColor, indicateFastCharging, fastChargingColor, BBarTransitColors);
        instance.setVisibility((BBarEnabled) ? View.VISIBLE : View.GONE);
        instance.setColorful(BBarColorful);
        instance.setOnlyWhileCharging(BBOnlyWhileCharging);
        instance.setOnTop(!BBOnBottom);
        instance.setSingleColorTone(clockColor);
        instance.setAlphaPct(BBOpacity);
        instance.setBarHeight(Math.round(BBarHeight/10)+5);
        instance.setCenterBased(BBSetCentered);
        instance.refreshLayout();
    }
    
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;
        
        Class ActivityStarterClass = XposedHelpers.findClass("com.android.systemui.plugins.ActivityStarter", lpparam.classLoader);
        Class DependencyClass = XposedHelpers.findClass("com.android.systemui.Dependency", lpparam.classLoader);
        Class CollapsedStatusBarFragmentClass;
        Class KeyguardStatusBarViewControllerClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.KeyguardStatusBarViewController", lpparam.classLoader);
        Class QuickStatusBarHeaderControllerClass = XposedHelpers.findClass("com.android.systemui.qs.QuickStatusBarHeaderController", lpparam.classLoader);
        Class QuickStatusBarHeaderClass = XposedHelpers.findClass("com.android.systemui.qs.QuickStatusBarHeader", lpparam.classLoader);
        Class ClockClass = XposedHelpers.findClass("com.android.systemui.statusbar.policy.Clock", lpparam.classLoader);
        Class PhoneStatusBarViewClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.PhoneStatusBarView", lpparam.classLoader);
        Class KeyGuardIndicationClass = XposedHelpers.findClass("com.android.systemui.statusbar.KeyguardIndicationController", lpparam.classLoader);
        Class BatteryTrackerClass = XposedHelpers.findClass("com.android.systemui.statusbar.KeyguardIndicationController$BaseKeyguardCallback", lpparam.classLoader);
        
        CollapsedStatusBarFragmentClass = XposedHelpers.findClassIfExists("com.android.systemui.statusbar.phone.fragment.CollapsedStatusBarFragment", lpparam.classLoader);

        if(CollapsedStatusBarFragmentClass == null)
        {
            CollapsedStatusBarFragmentClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.CollapsedStatusBarFragment", lpparam.classLoader);
        }
        
        XposedBridge.hookAllConstructors(KeyGuardIndicationClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                KIC = param.thisObject;
            }
        });

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
        
        XposedBridge.hookAllConstructors(CollapsedStatusBarFragmentClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mCollapsedStatusBarFragment = param.thisObject;
            }
        });

        XposedBridge.hookAllConstructors(PhoneStatusBarViewClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                STB = param.thisObject;
            }
        });
        
        XposedBridge.hookAllConstructors(QuickStatusBarHeaderClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mActivityStarter = XposedHelpers.callStaticMethod(DependencyClass, "get", ActivityStarterClass);
            }
        });

        XposedBridge.hookAllConstructors(QuickStatusBarHeaderControllerClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                XposedHelpers.setAdditionalInstanceField(
                        XposedHelpers.getObjectField(param.thisObject, "mClockView"),
                        "mClockParent",
                        2);
            }
        });

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
                        }catch(Exception e) { return;}
                        //to recognize clock's parent
                        XposedHelpers.setAdditionalInstanceField(
                                XposedHelpers.getObjectField(param.thisObject, "mClockView"),
                                "mClockParent",
                                3);
                    }
                });

        XposedBridge.hookAllConstructors(KeyguardStatusBarViewControllerClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                //Removing vibration icon from blocked icons in lockscreen
                if(showVibrationIcon && (XposedHelpers.findFieldIfExists(KeyguardStatusBarViewControllerClass, "mBlockedIcons") != null)) { //Android 12 doesn't have such thing at all
                    List<String> OldmBlockedIcons = (List<String>) XposedHelpers.getObjectField(param.thisObject, "mBlockedIcons");

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

        XposedHelpers.findAndHookMethod(CollapsedStatusBarFragmentClass,
                "hideClock", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if(BatteryBarView.hasInstance())
                        {
                            BatteryBarView.getInstance().setVisible(false);
                        }
                        if(networkTrafficSB != null) {
                            networkTrafficSB.setVisibility(View.INVISIBLE);
                        }
                    }
                });

        XposedHelpers.findAndHookMethod(CollapsedStatusBarFragmentClass,
                "showClock", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if(BatteryBarView.hasInstance())
                        {
                            BatteryBarView.getInstance().setVisible(true);
                        }
                        if(networkTrafficSB != null) {
                            networkTrafficSB.setVisibility(View.VISIBLE);
                        }
                    }
                });


        XposedHelpers.findAndHookMethod(CollapsedStatusBarFragmentClass,
                "onViewCreated", View.class, Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mContext = (Context) XposedHelpers.callMethod(param.thisObject, "getContext");

                        //View mNotificationIconAreaInner = (View) XposedHelpers.getObjectField(param.thisObject, "mNotificationIconAreaInner");
                        View mClockView = (View) XposedHelpers.getObjectField(param.thisObject, "mClockView");
                        mClockParent = (ViewGroup) mClockView.getParent();
                        mCenteredIconArea = (View) XposedHelpers.getObjectField(param.thisObject, "mCenteredIconArea");
                        mSystemIconArea = (LinearLayout) XposedHelpers.getObjectField(param.thisObject, "mSystemIconArea");
    
                        View mStatusbar = (View) XposedHelpers.getObjectField(mCollapsedStatusBarFragment, "mStatusBar");
                        fullStatusbar = (FrameLayout) mStatusbar.getParent();
                        
                        if(BBarEnabled)
                        {
                            try {
                                fullStatusbar.addView(BatteryBarView.getInstance(fullStatusbar.getContext()));
                                refreshBatteryBar(BatteryBarView.getInstance());
                            }catch(Throwable ignored){}
                        }
                        
                        if(networkOnSBEnabled)
                        {
                            networkTrafficSB = (networkTrafficSB == null) ? new NetworkTrafficSB(mContext) : networkTrafficSB;
                            networkTrafficSB.setHideTreshold(networkTrafficTreshold);
                            placeNTSB();
                        }

                        //<Showing vibration icon in collapsed statusbar>
                        if(showVibrationIcon) {
                            setShowVibrationIcon();
                        }
                        //</Showing vibration icon in collapsed statusbar>

                        //<modding clock>
                        XposedHelpers.setAdditionalInstanceField(mClockView, "mClockParent", 1);

                        if(clockPosition == POSITION_LEFT) return;

                        ViewGroup targetArea = null;

                        switch (clockPosition)
                        {
                            case POSITION_CENTER:
                                targetArea = (ViewGroup) mCenteredIconArea.getParent();
                                break;
                            case POSITION_RIGHT:
                                mClockView.setPadding(20,0,0,0);
                                targetArea = ((ViewGroup) mSystemIconArea.getParent());
                                break;
                        }
                        mClockParent.removeView(mClockView);
                        targetArea.addView(mClockView);
                        
                    }
                });

        XposedHelpers.findAndHookMethod(ClockClass,
                "getSmallTime", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedHelpers.setObjectField(param.thisObject, "mAmPmStyle", mAmPmStyle);
                        XposedHelpers.setObjectField(param.thisObject, "mShowSeconds", mShowSeconds);
                    }
                });

        XposedBridge.hookAllMethods(ClockClass,
                "onDarkChanged", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        int mClockParent = 1;
                        try {
                            mClockParent = (int) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mClockParent");
                        }
                        catch(Exception e){}

                        if(mClockParent > 1) return; //We don't want colors of QS header. only statusbar

                        clockColor = ((TextView) param.thisObject).getTextColors().getDefaultColor();
                        NetworkTrafficSB.setTint(clockColor);
                        if(BatteryBarView.hasInstance())
                        {
                            refreshBatteryBar(BatteryBarView.getInstance());
                        }
                    }
                });

        XposedHelpers.findAndHookMethod(ClockClass,
                "getSmallTime", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        int mClockParent = 1;
                        try {
                            mClockParent = (int) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mClockParent");
                        }
                        catch(Exception e){}

                        if(mClockParent > 1) return; //We don't want custom format in QS header. do we?

                        Calendar mCalendar = (Calendar) XposedHelpers.getObjectField(param.thisObject, "mCalendar");

                        SpannableStringBuilder result = new SpannableStringBuilder();
                        result.append(getFormattedDate(mDateFormatBefore, mCalendar, mBeforeSmall));
                        result.append((CharSequence) param.getResult());
                        result.append(getFormattedDate(mDateFormatAfter, mCalendar, mAfterSmall));
                        param.setResult(result);
                    }
                });
    }
    private void setShowVibrationIcon()
    {
        try {
            List<String> mBlockedIcons = (List<String>) XposedHelpers.getObjectField(mCollapsedStatusBarFragment, "mBlockedIcons");
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
    
    class lb extends LinearLayout
    {
        public lb(Context context) {
            super(context);
            setLayoutParams(new LayoutParams(200,50));
        }
    }
    private void placeNTSB() {
        if(networkTrafficSB == null)
        {
            return;
        }
        try
        {
            ((ViewGroup) networkTrafficSB.getParent()).removeView(networkTrafficSB);
        }
        catch(Exception e){}
        if(!networkOnSBEnabled) return;

        try {
            LinearLayout.LayoutParams ntsbLayoutP = null;

            switch (networkTrafficPosition) {
                case POSITION_RIGHT:
                    mSystemIconArea.addView(networkTrafficSB, 0);
                    networkTrafficSB.setPadding(40, 0, 5, 0);
                    break;
                case POSITION_LEFT:
                    mClockParent.addView(networkTrafficSB, 0);
                    networkTrafficSB.setPadding(0, 0, 10, 0);
                    break;
                case POSITION_CENTER:
                    mClockParent.addView(networkTrafficSB);
                    networkTrafficSB.setPadding(0, 0, 0, 0);
                    break;
            }
            ntsbLayoutP = (LinearLayout.LayoutParams) networkTrafficSB.getLayoutParams();
            ntsbLayoutP.gravity = Gravity.CENTER_VERTICAL;
            networkTrafficSB.setLayoutParams(ntsbLayoutP);
        }catch(Throwable ignored){}
    }

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
                builder.appendPath(Long.toString(System.currentTimeMillis()));
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

    private static CharSequence getFormattedDate(String dateFormat, Calendar calendar, boolean small)
    {
        if(dateFormat.length() == 0) return "";
        //If dateformat is illegal, at least don't break anything
        try {

            //There's some format to work on
            SimpleDateFormat df = new SimpleDateFormat(dateFormat);
            String result = df.format(calendar.getTime());
            if (!small) return result;

            //small size requested
            SpannableStringBuilder formatted = new SpannableStringBuilder(result);
            CharacterStyle style = new RelativeSizeSpan(0.7f);
            formatted.setSpan(style, 0, result.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            return formatted;
        }
        catch(Exception e)
        {
            return "";
        }

    }
    
    @Override
    public String getListenPack() {
        return listenPackage;
    }
}
