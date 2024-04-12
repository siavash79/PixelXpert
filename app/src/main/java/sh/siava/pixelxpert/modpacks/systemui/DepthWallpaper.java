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

import java.io.File;
import java.io.FileInputStream;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.XposedModPack;

/** @noinspection RedundantThrows*/
public class DepthWallpaper extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	private static boolean lockScreenSubjectCacheValid = false;
	private FrameLayout mLockScreenSubject;
	private Object mQS;
	private Object mScrimController;

	private static boolean DWallpaperEnabled = false;
	private static int DWOpacity = 192;
	private Drawable mDimmingOverlay;
	private int mLastXShift = -1, mLastYShift = -1;
	private Bitmap mWallpaperBitmap;


	public DepthWallpaper(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		DWallpaperEnabled = Xprefs.getBoolean("DWallpaperEnabled", false);
		DWOpacity = Xprefs.getSliderInt("DWOpacity", 192);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		Class<?> QSImplClass = findClass("com.android.systemui.qs.QSImpl", lpparam.classLoader);
		Class<?> CanvasEngineClass = findClass("com.android.systemui.wallpapers.ImageWallpaper$CanvasEngine", lpparam.classLoader);
		Class<?> CentralSurfacesImplClass = findClass("com.android.systemui.statusbar.phone.CentralSurfacesImpl", lpparam.classLoader);
		Class<?> ScrimControllerClass = findClass("com.android.systemui.statusbar.phone.ScrimController", lpparam.classLoader);

		hookAllMethods(CentralSurfacesImplClass, "start", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(!DWallpaperEnabled) return;

				View scrimBehind = (View) getObjectField(mScrimController, "mScrimBehind");
				ViewGroup rootView = (ViewGroup) scrimBehind.getParent();

				@SuppressLint("DiscouragedApi")
				ViewGroup targetView = rootView.findViewById(mContext.getResources().getIdentifier("notification_container_parent", "id", mContext.getPackageName()));

				mLockScreenSubject = new FrameLayout(mContext);
				FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -1);
				mLockScreenSubject.setLayoutParams(lp);

				targetView.addView(mLockScreenSubject,1);
			}
		});
		hookAllConstructors(QSImplClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				mQS = param.thisObject;
			}
		});

		hookAllMethods(CanvasEngineClass, "onOffsetsChanged", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if(!isLockScreenWallpaper(param.thisObject)) return;

				processPossibleChange(param.thisObject, (float) param.args[0], (float) param.args[1]);
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
			@SuppressLint("NewApi")
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(DWallpaperEnabled && isLockScreenWallpaper(param.thisObject))
				{
					invalidateLSWSC();
					mWallpaperBitmap = Bitmap.createBitmap((Bitmap) param.args[0]);
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

	private boolean isLockScreenWallpaper(Object canvasEngine)
	{
		return ((int) callMethod(canvasEngine, "getWallpaperFlags")
				& WallpaperManager.FLAG_LOCK)
				== WallpaperManager.FLAG_LOCK;
	}
	private void setDepthWallpaper()
	{
		if(DWallpaperEnabled
				&& getObjectField(mScrimController, "mState").toString().equals("KEYGUARD")
				&& (boolean) callMethod(mQS, "isFullyCollapsed")) {

			if(!lockScreenSubjectCacheValid && new File(Constants.getLockScreenCachePath(mContext)).exists())
			{
				try (FileInputStream inputStream = new FileInputStream(Constants.getLockScreenCachePath(mContext)))
				{
					Drawable bitmapDrawable = BitmapDrawable.createFromStream(inputStream, "");
					bitmapDrawable.setAlpha(255);

					mDimmingOverlay = bitmapDrawable.getConstantState().newDrawable().mutate();
					mDimmingOverlay.setTint(Color.BLACK);

					mLockScreenSubject.setBackground(new LayerDrawable(new Drawable[]{bitmapDrawable, mDimmingOverlay}));
					lockScreenSubjectCacheValid = true;
				}
				catch (Throwable ignored) {}
			}

			if(lockScreenSubjectCacheValid) {
				mLockScreenSubject.getBackground().setAlpha(DWOpacity);

				//this is the dimmed wallpaper coverage
				mDimmingOverlay.setAlpha(Math.round(getFloatField(mScrimController, "mScrimBehindAlphaKeyguard")*240)); //A tad bit lower than max. show it a bit lighter than other stuff

				mLockScreenSubject.setVisibility(VISIBLE);
			}
		}
		else
		{
			mLockScreenSubject.setVisibility(GONE);
		}
	}

	private void processPossibleChange(Object canvasEngine, float xOff, float yOff) {
		if(mWallpaperBitmap == null || mWallpaperBitmap.isRecycled()) return;

		Rect displayBounds =  ((Context) callMethod(canvasEngine, "getDisplayContext")).getSystemService(WindowManager.class)
				.getCurrentWindowMetrics()
				.getBounds();

		float ratioW = 1f * displayBounds.width() / mWallpaperBitmap.getWidth();
		float ratioH = 1f * displayBounds.height() / mWallpaperBitmap.getHeight();

		int desiredHeight = Math.round(Math.max(ratioH, ratioW) * mWallpaperBitmap.getHeight());
		int desiredWidth = Math.round(Math.max(ratioH, ratioW) * mWallpaperBitmap.getWidth());

		int xPixelShift;
		int yPixelShift;

		if(getWallpaperFlag(canvasEngine) != WallpaperManager.FLAG_LOCK)
		{
			xPixelShift = (desiredWidth - displayBounds.width()) / 2;
			yPixelShift = (desiredHeight - displayBounds.height()) / 2;
		}
		else
		{
			xPixelShift = Math.round(xOff * desiredWidth);
			yPixelShift = Math.round(yOff * desiredHeight);

			if(xPixelShift + displayBounds.width() > desiredWidth)
			{
				xPixelShift = desiredWidth - displayBounds.width();
			}
			if (yPixelShift + displayBounds.height() > desiredHeight
			) {
				yPixelShift = desiredHeight - displayBounds.height();
			}
		}

		if(xPixelShift != mLastXShift || yPixelShift != mLastYShift) {
			invalidateLSWSC();

			mLastXShift = xPixelShift;
			mLastYShift = yPixelShift;

			Bitmap scaledWallpaperBitmap = Bitmap.createScaledBitmap(mWallpaperBitmap, desiredWidth, desiredHeight, true);

			//crop to display bounds
			scaledWallpaperBitmap = Bitmap.createBitmap(scaledWallpaperBitmap, xPixelShift, yPixelShift, displayBounds.width(), displayBounds.height());
			Bitmap finalScaledWallpaperBitmap = scaledWallpaperBitmap;
			XPLauncher.enqueueProxyCommand(proxy -> proxy.extractSubject(finalScaledWallpaperBitmap, Constants.getLockScreenCachePath(mContext)));
		}

	}

	private int getWallpaperFlag(Object canvasEngine) {
		return (int) callMethod(canvasEngine, "getWallpaperFlags");
	}

	private void invalidateLSWSC() //invalidate lock screen wallpaper subject cache
	{
		mLastXShift = mLastYShift = -1;

		lockScreenSubjectCacheValid = false;
		if(mLockScreenSubject != null) {
			mLockScreenSubject.post(() -> mLockScreenSubject.setVisibility(GONE));
		}
		try {
			//noinspection ResultOfMethodCallIgnored
			new File(Constants.getLockScreenCachePath(mContext)).delete();
		}
		catch (Throwable ignored){}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}
}
