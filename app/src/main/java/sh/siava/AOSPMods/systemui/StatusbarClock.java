package sh.siava.AOSPMods.systemui;

import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.security.auth.callback.Callback;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.XPrefs;

public class StatusbarClock implements IXposedHookLoadPackage {
    public static final String listenPackage = "com.android.systemui";

    private static final int POSITION_LEFT = 0;
    private static final int POSITION_CENTER = 1;
    private static final int POSITION_RIGHT = 2;

    private static final int AM_PM_STYLE_NORMAL  = 0;
    private static final int AM_PM_STYLE_SMALL   = 1;
    private static final int AM_PM_STYLE_GONE    = 2;

    public static int position = POSITION_LEFT;
    public static int mAmPmStyle = AM_PM_STYLE_GONE;
    public static boolean mShowSeconds = false;
    public static String mDateFormatBefore = "", mDateFormatAfter = "";
    public static boolean mBeforeSmall = true, mAfterSmall = true;

    public static void updatePrefs()
    {
        position = Integer.parseInt(XPrefs.Xprefs.getString("SBClockLoc", String.valueOf(POSITION_LEFT)));
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
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;


        Class CollapsedStatusBarFragmentClass;
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
        XposedHelpers.findAndHookMethod(CollapsedStatusBarFragmentClass,
                "onViewCreated", View.class, Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(position == POSITION_LEFT) return;

                        View mClockView = (View) XposedHelpers.getObjectField(param.thisObject, "mClockView");
                        ViewGroup mClockParent = (ViewGroup) mClockView.getParent();

                        ViewGroup targetArea = null;

                        switch (position)
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
                        Calendar mCalendar = (Calendar) XposedHelpers.getObjectField(param.thisObject, "mCalendar");

                        SpannableStringBuilder result = new SpannableStringBuilder();
                        result.append(getFormattedDate(mDateFormatBefore, mCalendar, mBeforeSmall));
                        result.append((CharSequence) param.getResult());
                        result.append(getFormattedDate(mDateFormatAfter, mCalendar, mAfterSmall));
                        param.setResult(result);
                    }
                });
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
