package sh.siava.AOSPMods.systemui;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.XPrefs;

public class StatusbarMods implements IXposedHookLoadPackage {
    public static final String listenPackage = "com.android.systemui";

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
    public static boolean showVibrationIcon = true;

    public static void updatePrefs()
    {

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

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;

        Class ActivityStarterClass = XposedHelpers.findClass("com.android.systemui.plugins.ActivityStarter", lpparam.classLoader);
        Class DependencyClass = XposedHelpers.findClass("com.android.systemui.Dependency", lpparam.classLoader);
        Class CollapsedStatusBarFragmentClass;
        Class KeyguardStatusBarViewControllerClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.KeyguardStatusBarViewController", lpparam.classLoader);
        Class QuickStatusBarHeaderControllerClass = XposedHelpers.findClass("com.android.systemui.qs.QuickStatusBarHeaderController", lpparam.classLoader);
        Class QuickStatusBarHeaderClass = XposedHelpers.findClass("com.android.systemui.qs.QuickStatusBarHeader", lpparam.classLoader);

        if(Build.VERSION.SDK_INT == 31) { //Andriod 12
            CollapsedStatusBarFragmentClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.CollapsedStatusBarFragment", lpparam.classLoader);
        }
        else if (Build.VERSION.SDK_INT == 32) //Android 12L-13
        {
            CollapsedStatusBarFragmentClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.fragment.CollapsedStatusBarFragment", lpparam.classLoader);
        } else
        {
            return;
        }

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


                        XposedHelpers.callMethod(mBatteryRemainingIcon, "setOnClickListener", clickListener);
                        XposedHelpers.callMethod(mClockView, "setOnClickListener", clickListener);
                        XposedHelpers.callMethod(mClockView, "setOnLongClickListener", clickListener);
                        XposedHelpers.callMethod(mDateView, "setOnClickListener", clickListener);
                        XposedHelpers.callMethod(mDateView, "setOnLongClickListener", clickListener);

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
                if(showVibrationIcon) {
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
                "onViewCreated", View.class, Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                        //<Showing vibration icon in collapsed statusbar>
                        if(showVibrationIcon) {
                            List<String> mBlockedIcons = (List<String>) XposedHelpers.getObjectField(param.thisObject, "mBlockedIcons");
                            Object mStatusBarIconController = XposedHelpers.getObjectField(param.thisObject, "mStatusBarIconController");
                            Object mDarkIconManager = XposedHelpers.getObjectField(param.thisObject, "mDarkIconManager");

                            XposedHelpers.callMethod(mStatusBarIconController, "removeIconGroup", mDarkIconManager);

                            mBlockedIcons.remove("volume");
                            XposedHelpers.callMethod(mDarkIconManager, "setBlockList", mBlockedIcons);
                            XposedHelpers.callMethod(mStatusBarIconController, "addIconGroup", mDarkIconManager);
                        }
                        //</Showing vibration icon in collapsed statusbar>

                        //<modding clock>
                        View mClockView = (View) XposedHelpers.getObjectField(param.thisObject, "mClockView");
                        XposedHelpers.setAdditionalInstanceField(mClockView, "mClockParent", 1);

                        if(clockPosition == POSITION_LEFT) return;

                        ViewGroup mClockParent = (ViewGroup) mClockView.getParent();

                        ViewGroup targetArea = null;

                        switch (clockPosition)
                        {
                            case POSITION_CENTER:
                                View mCenteredIconArea = (View) XposedHelpers.getObjectField(param.thisObject, "mCenteredIconArea");
                                targetArea = (ViewGroup) mCenteredIconArea.getParent();
                                break;
                            case POSITION_RIGHT:
                                LinearLayout mSystemIconArea = (LinearLayout) XposedHelpers.getObjectField(param.thisObject, "mSystemIconArea");
                                mClockView.setPadding(20,0,0,0);
                                targetArea = ((ViewGroup) mSystemIconArea.getParent());
                                break;
                        }
                        mClockParent.removeView(mClockView);
                        targetArea.addView(mClockView);
                        //</modding clock>
                    }
                });

        XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.policy.Clock", lpparam.classLoader,
                "getSmallTime", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedHelpers.setObjectField(param.thisObject, "mAmPmStyle", mAmPmStyle);
                        XposedHelpers.setObjectField(param.thisObject, "mShowSeconds", mShowSeconds);
                    }
                });

        XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.policy.Clock", lpparam.classLoader,
                "getSmallTime", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        int mClockParent = 1;
                        try {
                            mClockParent = (int) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mClockParent");
//                            XposedBridge.log("SIAPOSED clock parent:" + mClockParent);
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
                Intent nIntent = new Intent(Intent.ACTION_MAIN);
                nIntent.setClassName("com.android.settings",
                        "com.android.settings.Settings$DateTimeSettingsActivity");
                XposedHelpers.callMethod(mActivityStarter, "startActivity", nIntent, true /* dismissShade */);
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

}
