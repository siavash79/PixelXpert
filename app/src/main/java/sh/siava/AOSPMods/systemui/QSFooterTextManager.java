package sh.siava.AOSPMods.systemui;

import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.IXposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class QSFooterTextManager implements IXposedModPack {
    public static final String listenPackage = "com.android.systemui";
    public static boolean customQSFooterTextEnabled = false;
    public static String customText = "";

    public void updatePrefs(String...Key)
    {
        if(XPrefs.Xprefs == null) return;
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

                        mBuildText.setText(customText);

                        boolean mShouldShowBuildText = customText.trim().length() > 0;

                        XposedHelpers.setObjectField(param.thisObject, "mShouldShowBuildText", mShouldShowBuildText);
                        mBuildText.setSelected(true);
                    }
                });
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }


}
