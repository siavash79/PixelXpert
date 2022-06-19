package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedHelpers.*;
import static de.robv.android.xposed.XposedBridge.*;

import android.content.Context;
import android.view.MotionEvent;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class QSQuickPullDown extends XposedModPack {
    private static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;

    private static final int PULLDOWN_SIDE_RIGHT = 1;
    private static final int PULLDOWN_SIDE_LEFT = 2;
    private static final int STATUSBAR_MODE_SHADE = 0;

    private static int pullDownSide = PULLDOWN_SIDE_RIGHT;
    private static boolean oneFingerPulldownEnabled = false;
    private static float statusbarPortion = 0.25f; // now set to 25% of the screen. it can be anything between 0 to 100%
    
    public QSQuickPullDown(Context context) { super(context); }
    
    @Override
    public void updatePrefs(String...Key)
    {
        if(XPrefs.Xprefs == null) return;
        oneFingerPulldownEnabled = XPrefs.Xprefs.getBoolean("QSPullodwnEnabled", false);
        statusbarPortion =  XPrefs.Xprefs.getInt("QSPulldownPercent", 25) / 100f;
        pullDownSide = Integer.parseInt(XPrefs.Xprefs.getString("QSPulldownSide", "1"));
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;

        Class<?> NotificationPanelViewControllerClass = findClass("com.android.systemui.statusbar.phone.NotificationPanelViewController", lpparam.classLoader);

        findAndHookMethod(NotificationPanelViewControllerClass,
                "isOpenQsEvent", MotionEvent.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(!oneFingerPulldownEnabled) return;

                        int mBarState = (int) getObjectField(param.thisObject, "mBarState");
                        if(mBarState != STATUSBAR_MODE_SHADE) return;

                        int w = (int) callMethod(
                                getObjectField(param.thisObject, "mView"),
                                "getMeasuredWidth");

                        float x = ((MotionEvent) param.args[0]).getX();
                        float region = w * statusbarPortion;

                        boolean showQsOverride = (pullDownSide == PULLDOWN_SIDE_RIGHT) ?
                                w - region < x :
                                x < region;

                        param.setResult((boolean) param.getResult() || showQsOverride);
                      }
                });
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
}
