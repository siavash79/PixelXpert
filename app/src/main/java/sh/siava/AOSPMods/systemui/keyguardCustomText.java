package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.Utils.Helpers;
import sh.siava.AOSPMods.Utils.StringFormatter;
import sh.siava.AOSPMods.XPrefs;
import sh.siava.AOSPMods.XposedModPack;

public class keyguardCustomText extends XposedModPack {
    private static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;

    StringFormatter stringFormatter = new StringFormatter();
    private TextView KGMiddleCustomTextView;
    private static String KGMiddleCustomText = "";
    LinearLayout mStatusArea = null;
    private Object KGCS;
    private boolean mDozing = false;

    public keyguardCustomText(Context context) {
        super(context);
    }

    @Override
    public void updatePrefs(String... Key) {
        KGMiddleCustomText = XPrefs.Xprefs.getString("KGMiddleCustomText", "");

        if(Key.length > 0 && Key[0].equals("KGMiddleCustomText"))
            setTheText();
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {


        Class<?> KeyguardSliceViewClass = XposedHelpers.findClass("com.android.keyguard.KeyguardSliceView$Row", lpparam.classLoader);
        Class<?> KeyguardClockSwitchClass = XposedHelpers.findClass("com.android.keyguard.KeyguardClockSwitch", lpparam.classLoader);
/*
        Class<?> AnimatableClockControllerClass = XposedHelpers.findClass("com.android.keyguard.AnimatableClockController", lpparam.classLoader);
        Class<?> KeyguardClockSwitchControllerClass = XposedHelpers.findClass("com.android.keyguard.KeyguardClockSwitchController", lpparam.classLoader);
        Class<?> DefaultClockControllerClass = XposedHelpers.findClass("com.android.keyguard.clock.DefaultClockController", lpparam.classLoader);
        Class<?> AvailableClocksClass = XposedHelpers.findClass("com.android.keyguard.clock.ClockManager$AvailableClocks", lpparam.classLoader);*/

        Resources res = mContext.getResources();

        XposedBridge.hookAllMethods(KeyguardSliceViewClass, "setDarkAmount", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                boolean isDozing = ((float) param.args[0]) > .5f;
                if(mDozing != isDozing)
                {
                    mDozing = isDozing;
                    setColor();
                }
            }
        });

        XposedBridge.hookAllMethods(KeyguardClockSwitchClass, "onFinishInflate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    KGCS = param.thisObject;
                    KGMiddleCustomTextView = new TextView(mContext);
                    KGMiddleCustomTextView.setMaxLines(2);
                    KGMiddleCustomTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                    KGMiddleCustomTextView.setLetterSpacing(.03f);

                    int padding = res.getDimensionPixelSize(
                            res.getIdentifier(
                                    "clock_padding_start",
                                    "dimen",
                                    mContext.getPackageName()));

                    KGMiddleCustomTextView.setPadding(padding, 0, padding, 0);

                    mStatusArea = ((LinearLayout)XposedHelpers.getObjectField(param.thisObject,"mStatusArea"));

                    stringFormatter.registerCallback(() -> setTheText());

                    setTheText();
                    setColor();
                }
                catch (Exception ignored){}
            }
        });
    }

    private void setColor() {
        int color = (mDozing) ? Color.WHITE : (int)XposedHelpers.callMethod(KGCS,"getCurrentTextColor");

        KGMiddleCustomTextView.setTextColor(color);
    }

    private void setTheText() {
        if(KGCS == null) return;

        if(KGMiddleCustomText.length() == 0)
        {
            mStatusArea.removeView(KGMiddleCustomTextView);
        }
        else
        {
            try {
                ViewGroup parent = (ViewGroup) KGMiddleCustomTextView.getParent();
                if(parent != null) {
                    ((ViewGroup) KGMiddleCustomTextView.getParent()).removeView(KGMiddleCustomTextView);
                }
                mStatusArea.addView(KGMiddleCustomTextView, 0);
                KGMiddleCustomTextView.setText(stringFormatter.formatString(KGMiddleCustomText));

            }catch (Exception ignored){}
        }
    }
}