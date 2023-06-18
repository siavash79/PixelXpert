package sh.siava.AOSPMods.modpacks.dialer;

import static sh.siava.AOSPMods.modpacks.XPrefs.Xprefs;

import android.content.Context;

import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.ResourceManager;
import sh.siava.AOSPMods.modpacks.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class RecordingMessage extends XposedModPack {
	public static final String listenPackage = Constants.DIALER_PACKAGE;

	private static boolean removeRecodingMessage = false;
	public RecordingMessage(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		if (Xprefs == null) return;

		if(Key.length > 0 && Key[0].equals("DialerRemoveRecordMessage"))
		{
			android.os.Process.killProcess(android.os.Process.myPid());
		}
		removeRecodingMessage = Xprefs.getBoolean("DialerRemoveRecordMessage", false);
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if(removeRecodingMessage)
		{
			XC_InitPackageResources.InitPackageResourcesParam resparam = ResourceManager.resparams.get(listenPackage);

			if(resparam != null) {
				resparam.res.setReplacement(listenPackage, "string", "call_recording_starting_voice", "");
				resparam.res.setReplacement(listenPackage, "string", "call_recording_ending_voice", "");
			}
		}
	}
}