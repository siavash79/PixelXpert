package sh.siava.AOSPMods.systemui;

import android.widget.TextView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class QSFooterTextManager implements IXposedHookLoadPackage {
    public static final String listenPackage = "com.android.systemui";
    public static boolean customQSFooterTextEnabled = true;
    public static String customText = "SIAVASH-XPOSED :D";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;

        //TODO: Here is where we load the values from settings...
        //IF config has changed, we need to restart systemui, because the text doesn't refresh frequently

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
