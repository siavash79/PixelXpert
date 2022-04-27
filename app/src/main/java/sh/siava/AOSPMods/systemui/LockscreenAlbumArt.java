package sh.siava.AOSPMods.systemui;

import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.os.AsyncTask;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.IXposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class LockscreenAlbumArt implements IXposedModPack {
	public static final String listenPackage = "com.android.systemui";
	
	private static boolean albumArtLockScreenEnabled = false;
	private static boolean albumArtLockScreenHookEnabled = true;
	
	@Override
	public void updatePrefs(String... Key) {
		albumArtLockScreenEnabled = XPrefs.Xprefs.getBoolean("albumArtLockScreenEnabled", false);
		albumArtLockScreenHookEnabled = XPrefs.Xprefs.getBoolean("albumArtLockScreenHookEnabled", true);
	}
	
	@Override
	public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
	
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if(!lpparam.packageName.equals(listenPackage) || !albumArtLockScreenHookEnabled) return;
		
		Class<?> NotificationMediaManagerClass = XposedHelpers.findClass("com.android.systemui.statusbar.NotificationMediaManager", lpparam.classLoader);
		
		XposedBridge.hookAllMethods(NotificationMediaManagerClass, "updateMediaMetaData", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if(!albumArtLockScreenEnabled) return;
				
				MediaMetadata mediaMetadata = (MediaMetadata) XposedHelpers.callMethod(param.thisObject, "getMediaMetadata");
				Bitmap artworkBitmap = null;
				Object mKeyguardBypassController = XposedHelpers.getObjectField(param.thisObject, "mKeyguardBypassController");
				boolean byPassEnabld = (boolean) XposedHelpers.callMethod(mKeyguardBypassController, "getBypassEnabled");
				
				if (mediaMetadata != null && !byPassEnabld) {
					artworkBitmap = mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
					if (artworkBitmap == null) {
						artworkBitmap = mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
					}
				}
				boolean metaDataChanged = (boolean) param.args[0];
				boolean allowEnterAnimation = (boolean) param.args[1];
				android.util.ArraySet<AsyncTask> mProcessArtworkTasks = (android.util.ArraySet) XposedHelpers.getObjectField(param.thisObject, "mProcessArtworkTasks");
				if (metaDataChanged) {
					for (AsyncTask<?, ?, ?> task : mProcessArtworkTasks) {
						task.cancel(true);
					}
					mProcessArtworkTasks.clear();
				}
				
				XposedHelpers.callMethod(param.thisObject, "finishUpdateMediaMetaData", metaDataChanged, allowEnterAnimation, artworkBitmap);
				param.setResult(null);
			}
		});
	}
}
