package sh.siava.AOSPMods;

import android.app.Instrumentation;
import android.content.Context;

import java.util.ArrayList;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.Utils.Overlays;
import sh.siava.AOSPMods.android.powerTorch;
import sh.siava.AOSPMods.systemui.BackGestureManager;
import sh.siava.AOSPMods.systemui.BackToKill;
import sh.siava.AOSPMods.systemui.BatteryStyleManager;
import sh.siava.AOSPMods.systemui.CarrierTextManager;
import sh.siava.AOSPMods.systemui.DoubleTapSleepLS;
import sh.siava.AOSPMods.systemui.FeatureFlagsMods;
import sh.siava.AOSPMods.systemui.KeyguardBottomArea;
import sh.siava.AOSPMods.systemui.LTEiconChange;
import sh.siava.AOSPMods.systemui.NavBarResizer;
import sh.siava.AOSPMods.systemui.QSFooterTextManager;
import sh.siava.AOSPMods.systemui.QSHaptic;
import sh.siava.AOSPMods.systemui.QSHeaderManager;
import sh.siava.AOSPMods.systemui.QSQuickPullDown;
import sh.siava.AOSPMods.systemui.ScreenshotController;
import sh.siava.AOSPMods.systemui.StatusbarMods;
import sh.siava.AOSPMods.systemui.UDFPSManager;

public class AOSPMods implements IXposedHookLoadPackage{

    public static ArrayList<Class> modPacks = new ArrayList<>();
    public static ArrayList<IXposedModPack> runningMods = new ArrayList<>();
    public Context mContext = null;

    public AOSPMods()
    {
        new Overlays().initOverlays();

        modPacks.add(StatusbarMods.class);
        modPacks.add(BackGestureManager.class);
        modPacks.add(BackToKill.class);
        modPacks.add(BatteryStyleManager.class);
        modPacks.add(CarrierTextManager.class);
        modPacks.add(DoubleTapSleepLS.class);
        modPacks.add(FeatureFlagsMods.class);
        modPacks.add(KeyguardBottomArea.class);
        modPacks.add(LTEiconChange.class);
        modPacks.add(NavBarResizer.class);
        modPacks.add(QSFooterTextManager.class);
        modPacks.add(QSHaptic.class);
        modPacks.add(QSHeaderManager.class);
        modPacks.add(QSQuickPullDown.class);
        modPacks.add(ScreenshotController.class);
        modPacks.add(UDFPSManager.class);
        modPacks.add(powerTorch.class);

    }
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

//        Helpers.dumpClass("android.app.Instrumentation", lpparam);

        if(mContext == null) {
            if (lpparam.packageName.equals("com.android.systemui") || lpparam.packageName.equals("android")) {
                XposedHelpers.findAndHookMethod(Instrumentation.class, "newApplication", ClassLoader.class, String.class, Context.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (mContext == null) setContext((Context) param.args[2]);
                    }
                });
            }
        }

        if (lpparam.packageName.equals("com.android.systemui")) {
            try {
                XPrefs.Xprefs.edit().putBoolean("SystemUIConncted", true).commit();
            }catch(Throwable t){}
        }

        for (Class mod : modPacks)
        {
            try {
                IXposedModPack instance = ((IXposedModPack) mod.newInstance());
                if(!instance.getListenPack().equals(lpparam.packageName)) continue;
                try {
                    instance.updatePrefs();
                } catch(Throwable ignored){}
                instance.handleLoadPackage(lpparam);
                runningMods.add(instance);
            }
            catch (Throwable T)
            {
                T.printStackTrace();
            }
        }
    }

    private void setContext(Context context) {
        mContext = context;
        XPrefs.loadPrefs(mContext);
    }
}




