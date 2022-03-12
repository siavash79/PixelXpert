package sh.siava.AOSPMods;

import android.app.Application;
import android.content.SharedPreferences;
import android.view.inputmethod.InputMethodSession;

import androidx.preference.Preference;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.util.concurrent.ThreadPoolExecutor;

import javax.security.auth.callback.Callback;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import sh.siava.AOSPMods.systemui.BackGestureManager;
import sh.siava.AOSPMods.systemui.BackToKill;
import sh.siava.AOSPMods.systemui.BatteryStyleManager;
import sh.siava.AOSPMods.systemui.CarrierTextManager;
import sh.siava.AOSPMods.systemui.DoubleTapSleepLS;
import sh.siava.AOSPMods.systemui.LTEiconChange;
import sh.siava.AOSPMods.systemui.NavBarResizer;
import sh.siava.AOSPMods.systemui.QSFooterTextManager;
import sh.siava.AOSPMods.systemui.QSHeaderManager;
import sh.siava.AOSPMods.systemui.QSQuickPullDown;
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

        configLoader.start(); //while the data partition isn't decrypted, we can't access config
        //first we assume that module isn't enabled for systemui, until it's proved otherwise


    }

    private static void loadEverything()
    {
        if(Xprefs == null) return;

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
    }
}
