package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.os.AsyncTask;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.ArraySet;
import android.widget.ImageView;

import com.google.android.renderscript.Toolkit;
import com.google.android.renderscript.ToolkitKt;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.BuildConfig;
import sh.siava.AOSPMods.XPrefs;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class LockscreenAlbumArt extends XposedModPack {
	public static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;
	
	private static boolean albumArtLockScreenEnabled = false;
	private static boolean albumArtLockScreenHookEnabled = true;
	private static float albumArtLockScreenBlurLevel = 0f; //50%
	private Object NMM;
	private static Toolkit renderToolkit;

	public LockscreenAlbumArt(Context context) {
		super(context);
		if(renderToolkit == null) renderToolkit = Toolkit.INSTANCE;
	}
	
	@Override
	public void updatePrefs(String... Key) {
		albumArtLockScreenEnabled = XPrefs.Xprefs.getBoolean("albumArtLockScreenEnabled", true);
		albumArtLockScreenHookEnabled = XPrefs.Xprefs.getBoolean("albumArtLockScreenHookEnabled", true);
		albumArtLockScreenBlurLevel = XPrefs.Xprefs.getInt("albumArtLockScreenBlurLevel", 0)/100f;

		if(Key.length > 0)
		{
			if ("albumArtLockScreenBlurLevel".equals(Key[0])) {
				XposedHelpers.callMethod(NMM, "updateMediaMetaData", true, false);
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
				try {
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

					ArraySet<AsyncTask> mProcessArtworkTasks = (ArraySet) XposedHelpers.getObjectField(param.thisObject, "mProcessArtworkTasks");
					if (metaDataChanged) {
						for (AsyncTask<?, ?, ?> task : mProcessArtworkTasks) {
							task.cancel(true);
						}
						mProcessArtworkTasks.clear();
					}

					if(artworkBitmap == null) return; //we're not interested anymore!

					if(albumArtLockScreenBlurLevel > 0)
					{
						artworkBitmap = renderToolkit.blur(artworkBitmap, Math.round(albumArtLockScreenBlurLevel*25f));
					}

					XposedHelpers.callMethod(param.thisObject, "finishUpdateMediaMetaData", metaDataChanged, allowEnterAnimation, artworkBitmap);
					param.setResult(null);
				}
				catch (Throwable t){
					if(BuildConfig.DEBUG)
					{
						XposedBridge.log("Start error dump");
						t.printStackTrace();
						XposedBridge.log("Start end error dump");
					}
				}
			}
		});
	}
}
