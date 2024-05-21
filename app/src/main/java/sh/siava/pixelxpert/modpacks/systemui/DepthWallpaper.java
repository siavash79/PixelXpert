package sh.siava.pixelxpert.modpacks.systemui;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getFloatField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.XposedModPack;

/** @noinspection RedundantThrows*/
public class DepthWallpaper extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;
	protected static final float KEYGUARD_SCRIM_ALPHA = 0.2f; //from ScrimController

	private static boolean lockScreenSubjectCacheValid = false;
	private Object mScrimController;
	private static boolean DWallpaperEnabled = false;
	private static int DWOpacity = 192;

	private static boolean DWonAOD = false;
	private FrameLayout mLockScreenSubject;
	private Drawable mSubjectDimmingOverlay;
	private FrameLayout mWallpaperBackground;
	private FrameLayout mWallpaperBitmapContainer;
	private FrameLayout mWallpaperDimmingOverlay;
	private boolean mLayersCreated = false;
	private static float mDefaultDimAmount = KEYGUARD_SCRIM_ALPHA;

	public DepthWallpaper(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		DWallpaperEnabled = Xprefs.getBoolean("DWallpaperEnabled", false);
		DWOpacity = Xprefs.getSliderInt("DWOpacity", 192);
		DWonAOD = Xprefs.getBoolean("DWonAOD", false);

		float KeyGuardDimAmount = Xprefs.getSliderFloat( "KeyGuardDimAmount", -1f) / 100f;

		mDefaultDimAmount = KeyGuardDimAmount == -1
				? KEYGUARD_SCRIM_ALPHA
				: KeyGuardDimAmount;
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		Class<?> QSImplClass = findClass("com.android.systemui.qs.QSImpl", lpParam.classLoader);
		Class<?> CanvasEngineClass = findClass("com.android.systemui.wallpapers.ImageWallpaper$CanvasEngine", lpParam.classLoader);
		Class<?> CentralSurfacesImplClass = findClass("com.android.systemui.statusbar.phone.CentralSurfacesImpl", lpParam.classLoader);
		Class<?> ScrimControllerClass = findClass("com.android.systemui.statusbar.phone.ScrimController", lpParam.classLoader);
		Class<?> ScrimViewClass = findClass("com.android.systemui.scrim.ScrimView", lpParam.classLoader);

		hookAllMethods(ScrimViewClass, "setViewAlpha", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if(!mLayersCreated) return;

				if(DWonAOD
						&& !getObjectField(mScrimController, "mState").toString().equals("KEYGUARD")) {
					mLockScreenSubject.post(() -> mLockScreenSubject.setAlpha(DWOpacity));
				}
				else if(getObjectField(param.thisObject, "mScrimName").equals("notifications_scrim"))
				{
					float notificationAlpha = (float)param.args[0];

					if(notificationAlpha < mDefaultDimAmount)
						notificationAlpha = 0;

					float subjectAlpha = (notificationAlpha > mDefaultDimAmount)
							? (1f - notificationAlpha) / (1f - mDefaultDimAmount)
							: 1f;

					mLockScreenSubject.post(() -> mLockScreenSubject.setAlpha(subjectAlpha));
				}
			}
		});

		hookAllMethods(CentralSurfacesImplClass, "start", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(!DWallpaperEnabled) return;

				View scrimBehind = (View) getObjectField(mScrimController, "mScrimBehind");
				ViewGroup rootView = (ViewGroup) scrimBehind.getParent();

				@SuppressLint("DiscouragedApi")
				ViewGroup targetView = rootView.findViewById(mContext.getResources().getIdentifier("notification_container_parent", "id", mContext.getPackageName()));

				if(!mLayersCreated) {
					createLayers();
				}

				rootView.addView(mWallpaperBackground, 0);

				targetView.addView(mLockScreenSubject,1);
			}
		});

		hookAllMethods(CanvasEngineClass, "onSurfaceDestroyed", new XC_MethodHook() { //lockscreen wallpaper changed
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(DWallpaperEnabled && isLockScreenWallpaper(param.thisObject))
				{
					invalidateLSWSC();
				}
			}
		});

		hookAllMethods(CanvasEngineClass, "onCreate", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(
						callMethod(
								getObjectField(param.thisObject, "mWallpaperManager"),
								"getWallpaperInfo", WallpaperManager.FLAG_LOCK)
								!= null) //it's live wallpaper. we can't use that
				{
					invalidateLSWSC();
				}
			}
		});

		hookAllMethods(CanvasEngineClass, "drawFrameOnCanvas", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(DWallpaperEnabled && isLockScreenWallpaper(param.thisObject))
				{
					Bitmap wallpaperBitmap = Bitmap.createBitmap((Bitmap) param.args[0]);

					boolean cacheIsValid = assertCache(wallpaperBitmap);

					Rect displayBounds =  ((Context) callMethod(param.thisObject, "getDisplayContext")).getSystemService(WindowManager.class)
							.getCurrentWindowMetrics()
							.getBounds();

					float ratioW = 1f * displayBounds.width() / wallpaperBitmap.getWidth();
					float ratioH = 1f * displayBounds.height() / wallpaperBitmap.getHeight();

					int desiredHeight = Math.round(Math.max(ratioH, ratioW) * wallpaperBitmap.getHeight());
					int desiredWidth = Math.round(Math.max(ratioH, ratioW) * wallpaperBitmap.getWidth());

					int xPixelShift = (desiredWidth - displayBounds.width()) / 2;
					int yPixelShift = (desiredHeight - displayBounds.height()) / 2;

					Bitmap scaledWallpaperBitmap = Bitmap.createScaledBitmap(wallpaperBitmap, desiredWidth, desiredHeight, true);

					//crop to display bounds
					scaledWallpaperBitmap = Bitmap.createBitmap(scaledWallpaperBitmap, xPixelShift, yPixelShift, displayBounds.width(), displayBounds.height());
					Bitmap finalScaledWallpaperBitmap = scaledWallpaperBitmap;

					if(!mLayersCreated) {
						createLayers();
					}

					mWallpaperBackground.post(() -> mWallpaperBitmapContainer.setBackground(new BitmapDrawable(mContext.getResources(), finalScaledWallpaperBitmap)));

					if(!cacheIsValid)
					{
						XPLauncher.enqueueProxyCommand(proxy -> proxy.extractSubject(finalScaledWallpaperBitmap, Constants.getLockScreenSubjectCachePath(mContext)));
					}
				}
			}
		});

		hookAllConstructors(ScrimControllerClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				mScrimController = param.thisObject;
			}
		});

		hookAllMethods(ScrimControllerClass, "applyAndDispatchState", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				setDepthWallpaper();

			}
		});

		hookAllMethods(QSImplClass, "setQsExpansion", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if((boolean) callMethod(param.thisObject, "isKeyguardState"))
				{
					setDepthWallpaper();
				}
			}
		});
	}

	private boolean assertCache(Bitmap wallpaperBitmap) {

		boolean cacheIsValid = false;
		try
		{
			File wallpaperCacheFile = new File(Constants.getLockScreenBitmapCachePath(mContext));

			ByteArrayOutputStream compressedBitmap = new ByteArrayOutputStream();
			wallpaperBitmap.compress(Bitmap.CompressFormat.JPEG, 100, compressedBitmap);
			if(wallpaperCacheFile.exists())
			{
				FileInputStream cacheStream = new FileInputStream(wallpaperCacheFile);

				if(Arrays.equals(cacheStream.readAllBytes(), compressedBitmap.toByteArray()))
				{
					cacheIsValid = true;
				}
				else
				{
					FileOutputStream newCacheStream = new FileOutputStream(wallpaperCacheFile);
					compressedBitmap.writeTo(newCacheStream);
					newCacheStream.close();
				}
				cacheStream.close();
			}
			compressedBitmap.close();
		}
		catch (Throwable ignored)
		{}

		if(!cacheIsValid)
		{
			invalidateLSWSC();
		}

		return cacheIsValid;
	}

	private void createLayers() {
		mWallpaperBackground = new FrameLayout(mContext);
		mWallpaperDimmingOverlay = new FrameLayout(mContext);
		mWallpaperBitmapContainer = new FrameLayout(mContext);
		FrameLayout.LayoutParams lpw = new FrameLayout.LayoutParams(-1, -1);

		mWallpaperDimmingOverlay.setBackgroundColor(Color.BLACK);
		mWallpaperDimmingOverlay.setLayoutParams(lpw);
		mWallpaperBitmapContainer.setLayoutParams(lpw);

		mWallpaperBackground.addView(mWallpaperBitmapContainer);
		mWallpaperBackground.addView(mWallpaperDimmingOverlay);
		mWallpaperBackground.setLayoutParams(lpw);

		mLockScreenSubject = new FrameLayout(mContext);
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -1);
		mLockScreenSubject.setLayoutParams(lp);

		mLayersCreated = true;
	}

	private boolean isLockScreenWallpaper(Object canvasEngine)
	{
		return (getWallpaperFlag(canvasEngine)
				& WallpaperManager.FLAG_LOCK)
				== WallpaperManager.FLAG_LOCK;
	}
	private void setDepthWallpaper()
	{
		String state = getObjectField(mScrimController, "mState").toString();
		boolean showSubject = DWallpaperEnabled
				&&
				(
						state.equals("KEYGUARD")
						||
								(DWonAOD
										&&
										(state.equals("AOD") || state.equals("PULSING"))
								)
				);

		if(showSubject) {
			if(!lockScreenSubjectCacheValid && new File(Constants.getLockScreenSubjectCachePath(mContext)).exists())
			{
				try (FileInputStream inputStream = new FileInputStream(Constants.getLockScreenSubjectCachePath(mContext)))
				{
					Drawable bitmapDrawable = BitmapDrawable.createFromStream(inputStream, "");
					bitmapDrawable.setAlpha(255);

					mSubjectDimmingOverlay = bitmapDrawable.getConstantState().newDrawable().mutate();
					mSubjectDimmingOverlay.setTint(Color.BLACK);

					mLockScreenSubject.setBackground(new LayerDrawable(new Drawable[]{bitmapDrawable, mSubjectDimmingOverlay}));
					lockScreenSubjectCacheValid = true;
				}
				catch (Throwable ignored) {}
			}

			if(lockScreenSubjectCacheValid) {
				mLockScreenSubject.getBackground().setAlpha(DWOpacity);

				if(!state.equals("KEYGUARD")) {
					mSubjectDimmingOverlay.setAlpha(192 /*Math.round(192 * (DWOpacity / 255f))*/);
				}
				else {
					//this is the dimmed wallpaper coverage
					mSubjectDimmingOverlay.setAlpha(Math.round(getFloatField(mScrimController, "mScrimBehindAlphaKeyguard") * 240)); //A tad bit lower than max. show it a bit lighter than other stuff
					mWallpaperDimmingOverlay.setAlpha(getFloatField(mScrimController, "mScrimBehindAlphaKeyguard"));
				}

				mWallpaperBackground.setVisibility(VISIBLE);
				mLockScreenSubject.setVisibility(VISIBLE);
			}
		}
		else if(mLayersCreated)
		{
			mLockScreenSubject.setVisibility(GONE);

			if (state.equals("UNLOCKED")) {
				mWallpaperBackground.setVisibility(GONE);
			}
		}
	}

	private int getWallpaperFlag(Object canvasEngine) {
		return (int) callMethod(canvasEngine, "getWallpaperFlags");
	}

	private void invalidateLSWSC() //invalidate lock screen wallpaper subject cache
	{
		lockScreenSubjectCacheValid = false;
		if(mLayersCreated) {
			mLockScreenSubject.post(() -> {
				mLockScreenSubject.setVisibility(GONE);
				mLockScreenSubject.setBackground(null);
				mWallpaperBackground.setVisibility(GONE);
				mWallpaperBitmapContainer.setBackground(null);
			});
		}
		try {
			//noinspection ResultOfMethodCallIgnored
			new File(Constants.getLockScreenSubjectCachePath(mContext)).delete();
		}
		catch (Throwable ignored){}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}
}
