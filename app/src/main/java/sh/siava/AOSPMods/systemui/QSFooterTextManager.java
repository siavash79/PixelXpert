package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.content.Context;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.utils.StringFormatter;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class QSFooterTextManager extends XposedModPack {
    private static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;
    private static boolean customQSFooterTextEnabled = false;
    private static String customText = "";
    private Object QSFV;
    private final StringFormatter stringFormatter = new StringFormatter();

    private final StringFormatter.formattedStringCallback refreshCallback = this::setQSFooterText;

    public void updatePrefs(String...Key)
    {
        if(Xprefs == null) return;
        customQSFooterTextEnabled = Xprefs.getBoolean("QSFooterMod", false);
        customText = Xprefs.getString("QSFooterText", "");

        setQSFooterText();
    }
    
    public QSFooterTextManager(Context context) { super(context); }
    
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;

        stringFormatter.registerCallback(refreshCallback);

        Class<?> QSFooterViewClass = findClass("com.android.systemui.qs.QSFooterView", lpparam.classLoader);

        hookAllConstructors(QSFooterViewClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                QSFV = param.thisObject;
            }
        });

        hookAllMethods(QSFooterViewClass,
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
                TextView mBuildText = (TextView) getObjectField(QSFV, "mBuildText");

                setObjectField(QSFV,
                        "mShouldShowBuildText",
                        customText.trim().length() > 0);

                mBuildText.setText(stringFormatter.formatString(customText));
                mBuildText.setSelected(true);
            }
            else
            {
                callMethod(QSFV,
                        "setBuildText");
            }
        } catch (Throwable ignored){} //probably not initiated yet
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
}
