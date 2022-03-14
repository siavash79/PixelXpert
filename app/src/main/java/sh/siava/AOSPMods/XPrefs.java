package sh.siava.AOSPMods;

import android.content.SharedPreferences;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import sh.siava.AOSPMods.systemui.BackGestureManager;
import sh.siava.AOSPMods.systemui.BackToKill;
import sh.siava.AOSPMods.systemui.BatteryStyleManager;
import sh.siava.AOSPMods.systemui.CarrierTextManager;
import sh.siava.AOSPMods.systemui.DoubleTapSleepLS;
import sh.siava.AOSPMods.systemui.FeatureFlagsMods;
import sh.siava.AOSPMods.systemui.LTEiconChange;
import sh.siava.AOSPMods.systemui.NavBarResizer;
import sh.siava.AOSPMods.systemui.QSFooterTextManager;
import sh.siava.AOSPMods.systemui.QSHaptic;
import sh.siava.AOSPMods.systemui.QSHeaderManager;
import sh.siava.AOSPMods.systemui.QSQuickPullDown;
import sh.siava.AOSPMods.systemui.StatusbarMods;
import sh.siava.AOSPMods.systemui.UDFPSManager;

public class XPrefs implements IXposedHookZygoteInit {

    public static XSharedPreferences Xprefs;
    private static XSharedPreferences getPref(String path) {
        XSharedPreferences pref = new XSharedPreferences(BuildConfig.APPLICATION_ID, path);
        if(pref == null)
        {
            XposedBridge.log("SIAPOS null pref");
            return null;
        }

        XposedBridge.log("SIAPOS pref not null");
        XposedBridge.log("SIAPOS pref readable: " + pref.getFile().canRead());

        return pref.getFile().canRead() ? pref : null;
    }

    Thread configLoader = new Thread()
    {
        @Override
        public void run()
        {
            while (true) {
                Xprefs = getPref("sh.siava.AOSPMods_preferences");
                if (Xprefs != null) break;
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {}
            }

            loadEverything();
            Xprefs.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    loadEverything();
                }
            });
        }
    };

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {

        loadEverything();

    }

    private void loadEverything()
    {
        if(Xprefs == null || !Xprefs.getFile().canRead())
        {
            configLoader.start();
            return;
        }

        Xprefs.reload();

        UDFPSManager.updatePrefs();
        CarrierTextManager.updatePrefs();
        DoubleTapSleepLS.updatePrefs();
        QSFooterTextManager.updatePrefs();
        QSHeaderManager.updatePrefs();
        QSQuickPullDown.updatePrefs();
        BatteryStyleManager.updatePrefs();
        BackGestureManager.updatePrefs();
        NavBarResizer.updatePrefs();
        LTEiconChange.updatePrefs();
        BackToKill.updatePrefs();
        StatusbarMods.updatePrefs();
        QSHaptic.updatePrefs();
        FeatureFlagsMods.updatePrefs();


    }
}
