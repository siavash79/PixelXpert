package sh.siava.AOSPMods.Utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import java.util.HashMap;
import java.util.Map;

import sh.siava.AOSPMods.XPrefs;

public class Overlays {
    public static Map<String, overlayProp> Overlays = null;

    static SharedPreferences prefs = null; //we load prefs from different sources depending on if it's from Xposed or App

    public void initOverlays(Context context)
    {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);

        initOverlays();
    }

    public void initOverlays()
    {
        Overlays = new HashMap<>();

        Overlays.put("HideNavbarOverlay", new overlayProp("com.android.overlay.removenavbar", false));
        Overlays.put("QSLightTheme", new overlayProp("sh.siava.AOSPMods_QSTheme", false));

        try {
            setAll();
        }catch(Throwable ignored){}
    }

    public static void setAll()
    {
        if(prefs == null)
        {
            prefs = XPrefs.Xprefs;
        }

        if(prefs == null || Overlays == null) return; // something not ready

        Map<String, ?> allPrefs = prefs.getAll();

        for(String pref : allPrefs.keySet())
        {
            if(pref.endsWith("Overlay") && Overlays.containsKey(pref))
            {
                Helpers.setOverlay(pref, (boolean) allPrefs.get(pref));
            }
        }
    }

    class overlayProp
    {
        public String name;
        public boolean exclusive;

        public overlayProp(String name, boolean exclusive)
        {
            this.name = name;
            this.exclusive = exclusive;
        }
    }

}
