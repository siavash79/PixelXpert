package sh.siava.AOSPMods;

import android.widget.TextView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class AOSPMods implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals("com.android.systemui"))
            return;

        XposedBridge.log("SIAPOSED : " + lpparam.packageName);
        XposedHelpers.findAndHookMethod("com.android.systemui.qs.QSFooterView", lpparam.classLoader, "setBuildText", new removeBuildText());
    }
}

class removeBuildText extends XC_MethodHook {
    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        super.afterHookedMethod(param);

        XposedBridge.log("SIAPOSED : in hook");

        TextView mBuildText = (TextView) XposedHelpers.getObjectField(param.thisObject, "mBuildText");
        boolean mShouldShowBuildText = (boolean) XposedHelpers.getObjectField(param.thisObject, "mShouldShowBuildText");

        if (mBuildText == null) {
            XposedBridge.log("SIAPOSED : textview is null?");
        }
        XposedBridge.log("SIAPOSED : got objects test was:" + mBuildText.getText());

        mBuildText.setText("Siavash + XPOSED");
        mShouldShowBuildText = true;
        mBuildText.setSelected(true);
        XposedBridge.log("SIAPOSED : set objects");
    }
}