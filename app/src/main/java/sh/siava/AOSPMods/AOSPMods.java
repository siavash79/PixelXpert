package sh.siava.AOSPMods;

import android.app.Instrumentation;
import android.content.Context;

import java.util.ArrayList;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.Utils.SystemUtils;
import sh.siava.AOSPMods.allApps.overScrollDisabler;
import sh.siava.AOSPMods.android.screenOffKeys;
import sh.siava.AOSPMods.launcher.TaskbarActivator;
import sh.siava.AOSPMods.systemui.AOSPSettingsLauncher;
import sh.siava.AOSPMods.systemui.BackGestureManager;
import sh.siava.AOSPMods.systemui.BackToKill;
import sh.siava.AOSPMods.systemui.BatteryStyleManager;
import sh.siava.AOSPMods.systemui.CarrierTextManager;
import sh.siava.AOSPMods.systemui.ScreenGestures;
import sh.siava.AOSPMods.systemui.FeatureFlagsMods;
import sh.siava.AOSPMods.systemui.KeyGuardPinScrambler;
import sh.siava.AOSPMods.systemui.KeyguardBottomArea;
import sh.siava.AOSPMods.systemui.LTEiconChange;
import sh.siava.AOSPMods.systemui.LockscreenAlbumArt;
import sh.siava.AOSPMods.systemui.NavBarResizer;
import sh.siava.AOSPMods.systemui.NotificationExpander;
import sh.siava.AOSPMods.systemui.BrightnessSlider;
import sh.siava.AOSPMods.systemui.QSFooterTextManager;
import sh.siava.AOSPMods.systemui.QSHaptic;
import sh.siava.AOSPMods.systemui.QSHeaderManager;
import sh.siava.AOSPMods.systemui.QSQuickPullDown;
import sh.siava.AOSPMods.systemui.ScreenshotController;
import sh.siava.AOSPMods.systemui.StatusbarMods;
import sh.siava.AOSPMods.systemui.UDFPSManager;

public class AOSPMods implements IXposedHookLoadPackage{

    public static ArrayList<Class<?>> modPacks = new ArrayList<>();
    public static ArrayList<XposedModPack> runningMods = new ArrayList<>();
    public Context mContext = null;
    public static boolean isSecondProcess = false;
    
    public AOSPMods()
    {
        //region Mod list definition
        modPacks.add(StatusbarMods.class);
        modPacks.add(BackGestureManager.class);
        modPacks.add(BackToKill.class);
        modPacks.add(BatteryStyleManager.class);
        modPacks.add(CarrierTextManager.class);
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
        modPacks.add(screenOffKeys.class);
        modPacks.add(ScreenGestures.class);
        modPacks.add(AOSPSettingsLauncher.class);
        modPacks.add(miscSettings.class);
        modPacks.add(BrightnessSlider.class);
        modPacks.add(NotificationExpander.class);
        modPacks.add(TaskbarActivator.class);
        modPacks.add(LockscreenAlbumArt.class);
        modPacks.add(KeyGuardPinScrambler.class);
        modPacks.add(overScrollDisabler.class);
        //endregion
    }
    
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        isSecondProcess =  lpparam.processName.contains(":");

        //        Helpers.dumpClass("android.app.Instrumentation", lpparam);
    
        XposedHelpers.findAndHookMethod(Instrumentation.class, "newApplication", ClassLoader.class, String.class, Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (mContext == null) setContext((Context) param.args[2]);
    
                for (Class<?> mod : modPacks)
                {
                    try {
                        XposedModPack instance = ((XposedModPack) mod.getConstructor(Context.class).newInstance(mContext));
                        if(!instance.listensTo(lpparam.packageName)) continue;
                        try {
                            instance.updatePrefs();
                        } catch(Throwable ignored){ }
                        instance.handleLoadPackage(lpparam);
                        runningMods.add(instance);
                    }
                    catch (Throwable T)
                    {
                        T.printStackTrace();
                    }
                }
            }
        });
 
    }

    private void setContext(Context context) {
        mContext = context;
        XposedBridge.log(mContext.getPackageName());
        new SystemUtils(context);
        XPrefs.loadPrefs(mContext);
    }
    
}