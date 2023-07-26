package sh.siava.AOSPMods.modpacks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static sh.siava.AOSPMods.modpacks.XPrefs.Xprefs;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.media.projection.MediaProjection;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.XPLauncher;
import sh.siava.AOSPMods.modpacks.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class ScreenRecord extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	private static boolean InsecureScreenRecord = false;

	public ScreenRecord(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		InsecureScreenRecord = Xprefs.getBoolean("InsecureScreenRecord", false);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(listenPackage)) return;

		hookAllMethods(MediaProjection.class, "createVirtualDisplay", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if(InsecureScreenRecord
				&& ((Method) param.method).getParameterCount() == 8)
				{
					int flags = (int) param.args[4];
					flags |= DisplayManager.VIRTUAL_DISPLAY_FLAG_SECURE;
					param.args[4] = flags;
				}
			}
		});
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}
}
