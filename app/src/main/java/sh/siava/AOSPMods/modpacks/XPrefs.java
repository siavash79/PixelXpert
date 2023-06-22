package sh.siava.AOSPMods.modpacks;

import static de.robv.android.xposed.XposedBridge.log;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import com.crossbowffs.remotepreferences.RemotePreferences;

import sh.siava.AOSPMods.BuildConfig;
import sh.siava.AOSPMods.modpacks.utils.Overlays;


public class XPrefs {

	public static SharedPreferences Xprefs;
	public static String MagiskRoot = "/data/adb/modules/AOSPMods";
	private static String packageName;

	private static final OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> loadEverything(packageName, key);

	public static void init(Context context)
	{
		packageName = context.getPackageName();
		Xprefs = new RemotePreferences(context, BuildConfig.APPLICATION_ID, BuildConfig.APPLICATION_ID + "_preferences", true);
		log("AOSPMods Version: " + BuildConfig.VERSION_NAME);
		log("AOSPMods Records: " + Xprefs.getAll().keySet().size());
		Xprefs.registerOnSharedPreferenceChangeListener(listener);
	}

	public static void loadEverything(String packageName, String... key) {
		if (packageName.equals(Constants.SYSTEM_UI_PACKAGE)) {
			Overlays.setAll(false);
		}
		for (XposedModPack thisMod : XPLauncher.runningMods) {
			thisMod.updatePrefs(key);
		}
	}
}
