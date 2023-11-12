package sh.siava.pixelxpert.modpacks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;
import android.os.Handler;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.XposedModPack;

/** @noinspection RedundantThrows*/
public class VolumeDialog extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	private static final int DISMISS_REASON_TIMEOUT = 3;
	private static final int DISMISS = 2;

	private static int VolumeDialogTimeout = 3000;

	public VolumeDialog(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		VolumeDialogTimeout = Xprefs.getInt("VolumeDialogTimeout", 3000);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		Class<?> VolumeDialogImplClass = findClass("com.android.systemui.volume.VolumeDialogImpl", lpparam.classLoader);
		hookAllMethods(VolumeDialogImplClass, "rescheduleTimeoutH", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(VolumeDialogTimeout != 3000
						&& !getBooleanField(param.thisObject, "mHovering")
						&& getObjectField(param.thisObject, "mSafetyWarning") == null
						&& getObjectField(param.thisObject, "mODICaptionsTooltipView") == null)
				{
					Handler mHandler = (Handler) getObjectField(param.thisObject, "mHandler");
					mHandler.removeMessages(DISMISS);
					mHandler.sendMessageDelayed(mHandler.obtainMessage(DISMISS,DISMISS_REASON_TIMEOUT,0),500);
				}
			}
		});
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}
}
