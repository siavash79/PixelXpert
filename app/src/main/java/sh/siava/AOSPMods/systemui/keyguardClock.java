package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.Utils.StringFormatter;
import sh.siava.AOSPMods.XposedModPack;

public class keyguardClock extends XposedModPack {
    private static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;

    public keyguardClock(Context context) {
        super(context);
    }

    @Override
    public void updatePrefs(String... Key) {

    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        Class<?> AnimatableClockViewClass = XposedHelpers.findClass("com.android.keyguard.AnimatableClockView", lpparam.classLoader);
        Class<?> DefaultClockControllerClass = XposedHelpers.findClass("com.android.keyguard.clock.DefaultClockController", lpparam.classLoader);

        XposedBridge.hookAllConstructors(DefaultClockControllerClass, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("init");
            }
        });

        XposedBridge.hookAllMethods(DefaultClockControllerClass, "createViews", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("create asked");
                StringFormatter s = new StringFormatter();

                RelativeLayout mView = (RelativeLayout) XposedHelpers.getObjectField(param.thisObject, "mView");
                View mdat = (View) XposedHelpers.getObjectField(param.thisObject, "mDate");

                TextView t = new TextView(mContext);
                t.setGravity(Gravity.CENTER_HORIZONTAL);
                t.setLetterSpacing(.03f);
                t.setText("ممد");

                mView.removeView(mView.getChildAt(0));
                mView.addView(t);

                XposedHelpers.setObjectField(param.thisObject, "mDate", t);
            }
        });

        XposedBridge.hookAllMethods(AnimatableClockViewClass, "refreshTime", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if(true) return;
                TextView t = (TextView) param.thisObject;

                StringFormatter s = new StringFormatter();
                SimpleDateFormat f = new SimpleDateFormat("h:m");

                t.setText(f.format(System.currentTimeMillis()) + s.formatString(" $PM/$Pd"));
                param.setResult(null);
                TextClock d = new TextClock(mContext);
                d.refreshTime();
            }
        });
    }
}
