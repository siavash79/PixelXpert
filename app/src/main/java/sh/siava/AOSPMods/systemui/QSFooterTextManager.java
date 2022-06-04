package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.Utils.StringFormatter;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class QSFooterTextManager extends XposedModPack {
    private static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;
    private static boolean customQSFooterTextEnabled = false;
    private static String customText = "";
    private Object QSFV;
    private StringFormatter stringFormatter = new StringFormatter();

    private final StringFormatter.formattedStringCallback refreshCallback = this::setQSFooterText;

    public void updatePrefs(String...Key)
    {
        if(XPrefs.Xprefs == null) return;
        customQSFooterTextEnabled = XPrefs.Xprefs.getBoolean("QSFooterMod", false);
        customText = XPrefs.Xprefs.getString("QSFooterText", "");

        setQSFooterText();
    }
    
    public QSFooterTextManager(Context context) { super(context); }
    
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;

        stringFormatter.registerCallback(refreshCallback);

        Class<?> QSFooterViewClass = XposedHelpers.findClass("com.android.systemui.qs.QSFooterView", lpparam.classLoader);

        XposedBridge.hookAllConstructors(QSFooterViewClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                QSFV = param.thisObject;
            }
        });

        XposedBridge.hookAllMethods(QSFooterViewClass,
                "setBuildText", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(!customQSFooterTextEnabled) return;
                        setQSFooterText();
                    }
                });
    }

    private void setQSFooterText() {
        try {
            if(customQSFooterTextEnabled) {
                TextView mBuildText = (TextView) XposedHelpers.getObjectField(QSFV, "mBuildText");

                XposedHelpers.setObjectField(QSFV,
                        "mShouldShowBuildText",
                        customText.trim().length() > 0);

                mBuildText.setText(stringFormatter.formatString(customText));
                mBuildText.setSelected(true);
            }
            else
            {
                XposedHelpers.callMethod(QSFV,
                        "setBuildText");
            }
        } catch (Throwable ignored){} //probably not initiated yet
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
}
