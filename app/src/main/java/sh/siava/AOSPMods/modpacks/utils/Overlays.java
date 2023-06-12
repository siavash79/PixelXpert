package sh.siava.AOSPMods.modpacks.utils;

import android.content.SharedPreferences;
import android.content.res.Resources;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import sh.siava.AOSPMods.R;
import sh.siava.AOSPMods.modpacks.ResourceManager;
import sh.siava.AOSPMods.modpacks.XPLauncher;
import sh.siava.AOSPMods.modpacks.XPrefs;

public class Overlays {
	public static Map<String, Object> Overlays = null;

	private static Overlays instance = null;
	static SharedPreferences prefs = null; //we load prefs from different sources depending on if it's from Xposed or App
	Resources resources;

	public void initOverlays() //If called from UI OR Xposed
	{
		if (resources == null) { //so we're running from Xposed
			resources = ResourceManager.modRes;
		}

		Overlays = new HashMap<>();

		fillOverlays(); //these are groups - to be loaded from resource arrays that end with "OverlayEx" or "OverlayG"

		//independent overlays
		Overlays.put("HideNavbarOverlay", new overlayProp("com.android.overlay.removenavbar", false));
		Overlays.put("QSLightThemeOverlay", new overlayProp("sh.siava.AOSPMods_QSTheme", false));
		Overlays.put("QSLightThemeBSTOverlay", new overlayProp("sh.siava.AOSPMods_QSTheme_BST", false));
		Overlays.put("QSDualToneOverlay", new overlayProp("sh.siava.AOSPMods_QSDualTone", false));
		Overlays.put("CustomThemedIconsOverlay", new overlayProp("com.romcontrolicons.nexuslauncher", false));
		Overlays.put("DualToneBatteryOverlay", new overlayProp("com.android.dualtonebattery", false));
		Overlays.put("UnreadMessagesNumberOverlay", new overlayProp("com.android.systemui.shownumber", false));
        /*Deprecated shortcut...no longer makes sense
        Overlays.put("KeyguardLeftShortcutOverlay", new overlayProp("com.android.systemui.showleftshortcut", false));
         */
		Overlays.put("ReduceKeyboardSpaceOverlay", new overlayProp("com.android.overlay.reducekeyboard", false));
		Overlays.put("BSThickTrackOverlay", new overlayProp("com.android.systemui.bstrack.overlay", false));

		instance = this;

		new overlayStartupThread().start();
	}

	private void fillOverlays() { //filling overlay list from resources, using a bit of reflection :D

		Class<R.array> c = R.array.class;
		Field[] fileds = c.getDeclaredFields();

		for (Field field : fileds) {
			try {
				int resid = field.getInt(R.array.AcherusOverlayG);
				if (resources.getResourceName(resid).endsWith("OverlayEx") || resources.getResourceName(resid).endsWith("OverlayG")) {
					String[] overlayNames = resources.getStringArray(resid);
					ArrayList<overlayProp> members = new ArrayList<>();
					for (String overlayName : overlayNames) {
						members.add(new overlayProp(overlayName, true));
					}
					Overlays.put(resources.getResourceName(resid).replace("sh.siava.AOSPMods:array/", ""), new overlayGroup(resources.getResourceName(resid), members));
				}
			} catch (Exception ignored) {
			}
		}
	}

	public static void setAll(boolean force) //make sure settings are applied to device
	{
		if (XPLauncher.isChildProcess) return;

		if (instance == null) {
			new Overlays().initOverlays();
			return;
		}
		instance.setAllInternal(force);
	}

	private void setAllInternal(boolean force) {
		new Thread(() -> {
			if (prefs == null) {
				prefs = XPrefs.Xprefs;
			}

			if (prefs == null) return; // something not ready

			Helpers.getActiveOverlays(); //update the real active overlay list

			Map<String, ?> allPrefs = prefs.getAll();

			for (String pref : allPrefs.keySet()) {
				if (pref.endsWith("Overlay") && Overlays.containsKey(pref)) {
					Helpers.setOverlay(pref, prefs.getBoolean(pref, false), force);
				}
				//overlay groups, like themes of select one
				else if (pref.endsWith("OverlayEx") && Overlays.containsKey(pref)) {
					String activeOverlay = prefs.getString(pref, "None");

					overlayGroup thisGroup = (overlayGroup) Overlays.get(pref);
					//noinspection ConstantConditions
					for (overlayProp thisProp : thisGroup.members) {
						if (!thisProp.name.equals("None")) {
							Helpers.setOverlay(thisProp.name, activeOverlay.equals(thisProp.name), force);
						}
					}
				}
			}
		}).start();
	}

	class overlayStartupThread extends Thread {
		@Override
		public void run() {
			for (int i = 0; i < 2; i++) {
				setAllInternal(false);
				try {
					Thread.sleep(20000);//wait some seconds in case any other mod plays with us at system startup, and apply again in background
				} catch (Exception ignored) {
				}
			}
		}
	}


	static class overlayProp {
		public String name;
		public boolean exclusive;

		public overlayProp(String name, boolean exclusive) {
			this.name = name;
			this.exclusive = exclusive;
		}
	}

	static class overlayGroup {
		public overlayGroup(String name, ArrayList<overlayProp> members) {
			this.name = name;
			this.members = members;
		}

		public String name;
		public ArrayList<overlayProp> members;
	}
}