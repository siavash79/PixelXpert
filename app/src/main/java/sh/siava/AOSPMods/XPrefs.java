package sh.siava.AOSPMods;

import static de.robv.android.xposed.XposedBridge.log;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.XModuleResources;

import com.crossbowffs.remotepreferences.RemotePreferences;

import de.robv.android.xposed.IXposedHookZygoteInit;
import sh.siava.AOSPMods.utils.Overlays;


public class XPrefs implements IXposedHookZygoteInit {

	public static String MOD_PATH = "";
	public static XModuleResources modRes;
	public static SharedPreferences Xprefs;
	public static String MagiskRoot = "/data/adb/modules/AOSPMods";
	private static String packageName;

	static SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> loadEverything(packageName, key);

	public static void loadPrefs(Context context) {
		packageName = context.getPackageName();
		Xprefs = new RemotePreferences(context, BuildConfig.APPLICATION_ID, BuildConfig.APPLICATION_ID + "_preferences", true);
		log("AOSPMods Version: " + BuildConfig.VERSION_NAME);
		log("AOSPMods Records: " + Xprefs.getAll().keySet().size());
		Xprefs.registerOnSharedPreferenceChangeListener(listener);
		loadEverything(context.getPackageName());
	}

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		MOD_PATH = startupParam.modulePath;
		modRes = XModuleResources.createInstance(XPrefs.MOD_PATH, null);
	}

	public static void loadEverything(String packageName, String... key) {
		if (packageName.equals(AOSPMods.SYSTEM_UI_PACKAGE)) {
			Overlays.setAll(false);
		}
		for (XposedModPack thisMod : AOSPMods.runningMods) {
			thisMod.updatePrefs(key);
		}
	}
}
