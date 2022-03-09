package sh.siava.AOSPMods.systemui;

import android.widget.TextView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.XPrefs;

public class QSFooterTextManager implements IXposedHookLoadPackage {
    public static final String listenPackage = "com.android.systemui";
    public static boolean customQSFooterTextEnabled = false;
    public static String customText = "";

    public static void updatePrefs()
    {
        customQSFooterTextEnabled = XPrefs.Xprefs.getBoolean("QSFooterMod", false);
        customText = XPrefs.Xprefs.getString("QSFooterText", "");
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;

        XposedHelpers.findAndHookMethod("com.android.systemui.qs.QSFooterView", lpparam.classLoader,
                "setBuildText", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(!customQSFooterTextEnabled) return;

                        TextView mBuildText = (TextView) XposedHelpers.getObjectField(param.thisObject, "mBuildText");
                        boolean mShouldShowBuildText = (boolean) XposedHelpers.getObjectField(param.thisObject, "mShouldShowBuildText");

                        mBuildText.setText(customText);
                        mShouldShowBuildText = true;
                        mBuildText.setSelected(true);
                    }
                });
    }
}
