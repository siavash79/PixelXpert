package sh.siava.pixelxpert.modpacks.utils;

import static sh.siava.pixelxpert.modpacks.utils.toolkit.OverlayTools.getActiveOverlays;
import static sh.siava.pixelxpert.modpacks.utils.toolkit.OverlayTools.setOverlay;

import android.content.SharedPreferences;
import android.content.res.Resources;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.modpacks.ResourceManager;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.XPrefs;

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
		Overlays.put("HideNavbarOverlay", new OverlayProp("com.android.overlay.removenavbar", false));
		Overlays.put("QSLightThemeOverlay", new OverlayProp("sh.siava.pixelxpert_QSTheme", false));
		Overlays.put("QSLightThemeBSTOverlay", new OverlayProp("sh.siava.pixelxpert_QSTheme_BST", false));
		Overlays.put("QSDualToneOverlay", new OverlayProp("sh.siava.pixelxpert_QSDualTone", false));
		Overlays.put("CustomThemedIconsOverlay", new OverlayProp("com.romcontrolicons.nexuslauncher", false));
		Overlays.put("DualToneBatteryOverlay", new OverlayProp("com.android.dualtonebattery", false));
		Overlays.put("UnreadMessagesNumberOverlay", new OverlayProp("com.android.systemui.shownumber", false));
        /*Deprecated shortcut...no longer makes sense
        Overlays.put("KeyguardLeftShortcutOverlay", new overlayProp("com.android.systemui.showleftshortcut", false));
         */
		Overlays.put("ReduceKeyboardSpaceOverlay", new OverlayProp("com.android.overlay.reducekeyboard", false));
		Overlays.put("BSThickTrackOverlay", new OverlayProp("com.android.systemui.bstrack.overlay", false));

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
					ArrayList<OverlayProp> members = new ArrayList<>();
					for (String overlayName : overlayNames) {
						members.add(new OverlayProp(overlayName, true));
					}
					Overlays.put(resources.getResourceName(resid).replace("sh.siava.pixelxpert:array/", ""), new OverlayGroup(resources.getResourceName(resid), members));
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
		XPLauncher.enqueueProxyCommand(proxy -> new Thread(() -> {
			if (prefs == null) {
				prefs = XPrefs.Xprefs;
			}

			if (prefs == null) return; // something not ready

			try {
				getActiveOverlays(proxy); //update the real active overlay list
			} catch (Throwable ignored) {}

			Map<String, ?> allPrefs = prefs.getAll();

			for (String pref : allPrefs.keySet()) {
				if (pref.endsWith("Overlay") && Overlays.containsKey(pref)) {
					try {
						setOverlay(pref, prefs.getBoolean(pref, false), force);
					} catch (Throwable ignored) {}
				}
				//overlay groups, like themes of select one
				else if (pref.endsWith("OverlayEx") && Overlays.containsKey(pref)) {
					String activeOverlay = prefs.getString(pref, "None");

					OverlayGroup thisGroup = (OverlayGroup) Overlays.get(pref);
					//noinspection ConstantConditions
					for (OverlayProp thisProp : thisGroup.members) {
						if (!thisProp.name.equals("None")) {
							try {
								setOverlay(thisProp.name, activeOverlay.equals(thisProp.name), force);
							} catch (Throwable ignored) {}
						}
					}
				}
			}
		}).start());
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


	public static class OverlayProp {
		public String name;
		public boolean exclusive;

		public OverlayProp(String name, boolean exclusive) {
			this.name = name;
			this.exclusive = exclusive;
		}
	}

	public static class OverlayGroup {
		public OverlayGroup(String name, ArrayList<OverlayProp> members) {
			this.name = name;
			this.members = members;
		}

		public String name;
		public ArrayList<OverlayProp> members;
	}
}