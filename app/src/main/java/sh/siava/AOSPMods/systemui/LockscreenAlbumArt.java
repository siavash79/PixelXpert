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

import com.google.android.renderscript.BlendingMode;
import com.google.android.renderscript.Toolkit;
import com.google.android.renderscript.ToolkitKt;

import java.util.concurrent.ExecutionException;

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
	private static final int LEVEL_BLUR_DISABLED = 0;

	private static boolean albumArtLockScreenEnabled = false;
	private static boolean albumArtLockScreenHookEnabled = true;
	private static int albumArtLockScreenBlurLevel = 0; // min=1 max=25 0=disabled
	private Object NMM;
	private static Toolkit renderToolkit;
	private static boolean albumArtLockScreenGrayscale = false;

	public LockscreenAlbumArt(Context context) {
		super(context);
		if(renderToolkit == null) renderToolkit = Toolkit.INSTANCE;
	}
	
	@Override
	public void updatePrefs(String... Key) {
		albumArtLockScreenEnabled = XPrefs.Xprefs.getBoolean("albumArtLockScreenEnabled", true);
		albumArtLockScreenHookEnabled = XPrefs.Xprefs.getBoolean("albumArtLockScreenHookEnabled", true);
		albumArtLockScreenBlurLevel = Math.round(XPrefs.Xprefs.getInt("albumArtLockScreenBlurLevel", 0)/4f);
		albumArtLockScreenGrayscale = XPrefs.Xprefs.getBoolean("albumArtLockScreenGrayscale", false);

		if(Key.length > 0)
		{
			switch (Key[0]) {
				case "albumArtLockScreenBlurLevel":
				case "albumArtLockScreenGrayscale":
					try {
						XposedHelpers.callMethod(NMM, "updateMediaMetaData", true, false);
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

					if(albumArtLockScreenBlurLevel != LEVEL_BLUR_DISABLED)  // we shall never provide 0 to the blue method
					{
						artworkBitmap = renderToolkit.blur(artworkBitmap, Math.round(albumArtLockScreenBlurLevel));
					}
					if(albumArtLockScreenGrayscale)
					{
						artworkBitmap = renderToolkit.colorMatrix(artworkBitmap, renderToolkit.getGreyScaleColorMatrix());
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
