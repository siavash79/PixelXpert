package sh.siava.AOSPMods.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import androidx.preference.PreferenceManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XposedBridge;
import sh.siava.AOSPMods.R;
import sh.siava.AOSPMods.XPrefs;

public class Overlays {
    public static Map<String, Object> Overlays = null;

    static SharedPreferences prefs = null; //we load prefs from different sources depending on if it's from Xposed or App
    Resources resources;
    boolean fromApp = false;

    public void initOverlays(Context context)
    {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        resources = context.getResources();
        fromApp = true;

        initOverlays();
    }

    public void initOverlays()
    {
        Overlays = new HashMap<>();

        if(resources == null)
        { //so we're running from Xposed
            resources = XPrefs.modRes;
        }

        fillOverlays(); //these are groups - to be loaded from resource arrays that end with "OverlayEx"

        //independent overlays
        Overlays.put("HideNavbarOverlay", new overlayProp("com.android.overlay.removenavbar", false));
        Overlays.put("QSLightThemeOverlay", new overlayProp("sh.siava.AOSPMods_QSTheme", false));
        Overlays.put("CustomThemedIconsOverlay", new overlayProp("com.romcontrolicons.nexuslauncher", false));

        try {
            setAll();
        }catch(Throwable ignored){}
    }

    private void fillOverlays() { //filling overlay list from resources, using a bit of reflection :D

        Class<R.array> c = R.array.class;
        Field[] fileds = c.getDeclaredFields();

        for(int i = 0; i< fileds.length; i++)
        {
            try {
                int resid = fileds[i].getInt(R.array.module_scope);
                if(resources.getResourceName(resid).endsWith("OverlayEx"))
                {
                    String[] overlayNames = resources.getStringArray(resid);
                    ArrayList<overlayProp> members = new ArrayList<>();
                    for(int j = 0; j< overlayNames.length; j++)
                    {
                        members.add(new overlayProp(overlayNames[j], true));
                    }
                    Overlays.put(resources.getResourceName(resid).replace("sh.siava.AOSPMods:array/", ""), new overlayGroup(resources.getResourceName(resid), members));
                }
            }catch(Exception e){}
        }
    }

    public static void setAll() //make sure settings are applied to device
    {
        try
        {
        XposedBridge.log("inside set all");
        }catch(Throwable t){}

        if(prefs == null)
        {
            prefs = XPrefs.Xprefs;
        }

        if(prefs == null || Overlays == null) return; // something not ready

        Helpers.getActiveOverlays(); //update the real active overlay list

        Map<String, ?> allPrefs = prefs.getAll();

        for(String pref : allPrefs.keySet())
        {
            if(pref.endsWith("Overlay") && Overlays.containsKey(pref))
            {
                Helpers.setOverlay(pref, prefs.getBoolean(pref, false));
            }
            //overlay groups, like themes of select one
            else if(pref.endsWith("OverlayEx") && Overlays.containsKey(pref))
            {
                String activeOverlay = prefs.getString(pref, "None");

                overlayGroup thisGroup = (overlayGroup) Overlays.get(pref);
                for (overlayProp thisProp : thisGroup.members) {
                    boolean isEnabledNow = Helpers.activeOverlays.contains(thisProp.name);
                    boolean shouldBeActive = activeOverlay.equals(thisProp.name);

                    if(isEnabledNow != shouldBeActive)
                    {
                        if(!thisProp.name.equals("None")) {
                            Helpers.setOverlay(thisProp.name, shouldBeActive);
                        }
                    }
                }
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

    class overlayGroup
    {
        public overlayGroup(String name, ArrayList members)
        {
            this.name = name;
            this.members = members;
        }

        public String name;
        public ArrayList<overlayProp> members;
    }
}
