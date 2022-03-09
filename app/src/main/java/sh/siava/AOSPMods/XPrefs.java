package sh.siava.AOSPMods;

import android.app.Application;
import android.content.SharedPreferences;
import android.view.inputmethod.InputMethodSession;

import androidx.preference.Preference;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceManager;

import java.io.File;

import javax.security.auth.callback.Callback;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import sh.siava.AOSPMods.systemui.CarrierTextManager;
import sh.siava.AOSPMods.systemui.DoubleTapSleepLS;
import sh.siava.AOSPMods.systemui.QSFooterTextManager;
import sh.siava.AOSPMods.systemui.QSHeaderManager;
import sh.siava.AOSPMods.systemui.UDFPSManager;

public class XPrefs implements IXposedHookZygoteInit {

    public static XSharedPreferences Xprefs;
    private static XSharedPreferences getPref(String path) {
        XSharedPreferences pref = new XSharedPreferences(BuildConfig.APPLICATION_ID, path);
        if(pref == null)
        {
            return null;
        }

        return pref.getFile().canRead() ? pref : null;
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {

        Xprefs = getPref("sh.siava.AOSPMods_preferences");
        loadEverything();

        Xprefs.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                loadEverything();
            }
        });
    }

    private static void loadEverything()
    {
        Xprefs.reload();

        UDFPSManager.updatePrefs();
        CarrierTextManager.updatePrefs();
        DoubleTapSleepLS.updatePrefs();
        QSFooterTextManager.updatePrefs();
        QSHeaderManager.updatePrefs();

    }
}
