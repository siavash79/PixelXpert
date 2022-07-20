package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.os.AsyncTask;
import android.util.ArraySet;

import com.google.android.renderscript.Toolkit;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.BuildConfig;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class LockscreenAlbumArt extends XposedModPack {
	public static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;
	private static final int LEVEL_BLUR_DISABLED = 0;

	private static boolean albumArtLockScreenEnabled = false;
	private static boolean albumArtLockScreenHookEnabled = true;
	private static int albumArtLockScreenBlurLevel = 0; // min=1 max=25 0=disabled
	private Object NMM;
	private static Toolkit renderToolkit;
	private static boolean albumArtLockScreenGrayscale = false;
	private Bitmap artworkBitmap = null;

	public LockscreenAlbumArt(Context context) {
		super(context);
		if(renderToolkit == null) renderToolkit = Toolkit.INSTANCE;
	}
	
	@Override
	public void updatePrefs(String... Key) {
		albumArtLockScreenEnabled = Xprefs.getBoolean("albumArtLockScreenEnabled", true);
		albumArtLockScreenHookEnabled = Xprefs.getBoolean("albumArtLockScreenHookEnabled", true);
		albumArtLockScreenBlurLevel = Math.round(Xprefs.getInt("albumArtLockScreenBlurLevel", 0)/4f);
		albumArtLockScreenGrayscale = Xprefs.getBoolean("albumArtLockScreenGrayscale", false);

		if(Key.length > 0)
		{
			switch (Key[0]) {
				case "albumArtLockScreenBlurLevel":
				case "albumArtLockScreenGrayscale":
					try {
						callMethod(NMM, "updateMediaMetaData", true, false);
					}catch (Exception ignored){}
					break;
			}
		}
	}
	
	@Override
	public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
	
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if(!lpparam.packageName.equals(listenPackage) || !albumArtLockScreenHookEnabled) return;
		
		Class<?> NotificationMediaManagerClass = findClass("com.android.systemui.statusbar.NotificationMediaManager", lpparam.classLoader);

		hookAllConstructors(NotificationMediaManagerClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				NMM = param.thisObject;
			}
		});

		hookAllMethods(NotificationMediaManagerClass, "updateMediaMetaData", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if(!albumArtLockScreenEnabled) return;
				try {
					MediaMetadata mediaMetadata = (MediaMetadata) callMethod(param.thisObject, "getMediaMetadata");
					Object mKeyguardBypassController = getObjectField(param.thisObject, "mKeyguardBypassController");
					boolean byPassEnabld = (boolean) callMethod(mKeyguardBypassController, "getBypassEnabled");

					boolean metaDataChanged = (boolean) param.args[0];
					boolean allowEnterAnimation = (boolean) param.args[1];
					ArraySet<AsyncTask> mProcessArtworkTasks = (ArraySet) getObjectField(param.thisObject, "mProcessArtworkTasks");
					if (metaDataChanged) {
						if (mediaMetadata != null && !byPassEnabld) {
							artworkBitmap = mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
							if (artworkBitmap == null) {
								artworkBitmap = mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
							}
						}

						if(artworkBitmap != null)
						{
							if(albumArtLockScreenBlurLevel != LEVEL_BLUR_DISABLED)  // we shall never provide 0 to the blue method
							{
								artworkBitmap = renderToolkit.blur(artworkBitmap, Math.round(albumArtLockScreenBlurLevel));
							}
							if(albumArtLockScreenGrayscale)
							{
								artworkBitmap = renderToolkit.colorMatrix(artworkBitmap, renderToolkit.getGreyScaleColorMatrix());
							}
						}

						for (AsyncTask<?, ?, ?> task : mProcessArtworkTasks) {
							task.cancel(true);
						}
						mProcessArtworkTasks.clear();
					}
					if(mediaMetadata == null)
					{
						artworkBitmap = null;
					}

					callMethod(param.thisObject, "finishUpdateMediaMetaData", metaDataChanged, allowEnterAnimation, artworkBitmap);
					param.setResult(null);
				}
				catch (Throwable t){
					if(BuildConfig.DEBUG)
					{
						log("Start error dump");
						t.printStackTrace();
						log("Start end error dump");
					}
				}
			}
		});
	}
}