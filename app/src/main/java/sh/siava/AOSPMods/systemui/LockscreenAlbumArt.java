package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.os.AsyncTask;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class LockscreenAlbumArt extends XposedModPack {
	public static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;
	
	private static boolean albumArtLockScreenEnabled = false;
	private static boolean albumArtLockScreenHookEnabled = true;
	private static float albumArtLockScreenBlurLevel = 0f; //50%
	private Object NMM;

	public LockscreenAlbumArt(Context context) { super(context); }
	
	@Override
	public void updatePrefs(String... Key) {
		albumArtLockScreenEnabled = XPrefs.Xprefs.getBoolean("albumArtLockScreenEnabled", true);
		albumArtLockScreenHookEnabled = XPrefs.Xprefs.getBoolean("albumArtLockScreenHookEnabled", true);
		albumArtLockScreenBlurLevel = XPrefs.Xprefs.getInt("albumArtLockScreenBlurLevel", 0)/100f;

		if(Key.length > 0)
		{
			switch (Key[0])
			{
				case "albumArtLockScreenBlurLevel":
					XposedHelpers.callMethod(NMM, "updateMediaMetaData", true, false);
					break;
			}
		}
	}
	
	@Override
	public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
	
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if(!lpparam.packageName.equals(listenPackage) || !albumArtLockScreenHookEnabled) return;
		
		Class<?> NotificationMediaManagerClass = XposedHelpers.findClass("com.android.systemui.statusbar.NotificationMediaManager", lpparam.classLoader);

		XposedBridge.hookAllConstructors(NotificationMediaManagerClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				NMM = param.thisObject;
			}
		});

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
				if(albumArtLockScreenBlurLevel > 0) {
					artworkBitmap = new BlurBuilder().blur(mContext, artworkBitmap, albumArtLockScreenBlurLevel);
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
	public class BlurBuilder {
		public Bitmap blur(Context context, Bitmap image, float level) {
			try {
				Bitmap inputBitmap = Bitmap.createBitmap(image);
				Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);

				level *= 24.999f; // % of value: 25 is Max

				RenderScript rs = RenderScript.create(context);
				ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
				Allocation tmpIn = Allocation.createFromBitmap(rs, inputBitmap);
				Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);
				theIntrinsic.setRadius(level);
				theIntrinsic.setInput(tmpIn);
				theIntrinsic.forEach(tmpOut);
				tmpOut.copyTo(outputBitmap);

				return outputBitmap;
			}
			catch (Exception ignored)
			{
				return image;
			}
		}
	}
}
