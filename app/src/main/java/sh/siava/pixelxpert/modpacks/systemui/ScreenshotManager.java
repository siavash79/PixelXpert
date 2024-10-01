package sh.siava.pixelxpert.modpacks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.findFieldIfExists;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;
import static sh.siava.pixelxpert.modpacks.utils.toolkit.ReflectionTools.hookAllMethodsMatchPattern;
import static sh.siava.pixelxpert.modpacks.utils.toolkit.ReflectionTools.tryHookAllMethods;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaPlayer;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class ScreenshotManager extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	private static boolean disableScreenshotSound = false;
	private boolean ScreenshotChordInsecure = false;

	public ScreenshotManager(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		if (Xprefs == null) return;
		disableScreenshotSound = Xprefs.getBoolean("disableScreenshotSound", false);
		ScreenshotChordInsecure = Xprefs.getBoolean("ScreenshotChordInsecure", false);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		if (!listensTo(lpParam.packageName)) return;

		Class<?> ScreenshotControllerClass = findClassIfExists("com.android.systemui.screenshot.ScreenshotController", lpParam.classLoader);

		Class<?> CaptureArgsClass = findClassIfExists("android.window.ScreenCapture.CaptureArgs", lpParam.classLoader); //A14
		if(CaptureArgsClass == null)
		{
			CaptureArgsClass = findClass("android.view.SurfaceControl$DisplayCaptureArgs", lpParam.classLoader); //A13
		}

		Class<?> ScreenshotPolicyImplClass = findClass("com.android.systemui.screenshot.ScreenshotPolicyImpl", lpParam.classLoader);

		hookAllMethodsMatchPattern(ScreenshotPolicyImplClass, ".*isManagedProfile.*", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if(ScreenshotChordInsecure)
					param.setResult(false);
			}
		});

		hookAllConstructors(CaptureArgsClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(ScreenshotChordInsecure) {
					setObjectField(param.thisObject, "mCaptureSecureLayers", true);
				}
			}
		});

		if(ScreenshotControllerClass != null) {
			//A14 QPR1 and older
			hookAllConstructors(ScreenshotControllerClass, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					if (!disableScreenshotSound) return;

					if (findFieldIfExists(ScreenshotControllerClass, "mScreenshotSoundController") == null) { //Since 15B3 bg executor has other usages than sound. Don't kill it if that's the case
						((ExecutorService) getObjectField(param.thisObject, "mBgExecutor")).shutdownNow();

						setObjectField(param.thisObject, "mBgExecutor", new NoExecutor());
					}
				}
			});

			//A14 QPR2
			hookAllMethods(ScreenshotControllerClass, "playCameraSoundIfNeeded", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if (disableScreenshotSound)
						param.setResult(null);
				}
			});
		}

		//A14 QPR3
		Class<?> ScreenshotSoundProviderImplClass = findClassIfExists("com.android.systemui.screenshot.ScreenshotSoundProviderImpl", lpParam.classLoader);
		tryHookAllMethods(ScreenshotSoundProviderImplClass, "getScreenshotSound", new XC_MethodHook() {
			@SuppressLint("NewApi")
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if(disableScreenshotSound)
					param.setResult(new MediaPlayer(mContext));
			}
		});
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && XPLauncher.isChildProcess && XPLauncher.processName.contains("screenshot");
	}

	//Seems like an executor, but doesn't act! perfect thing
	private static class NoExecutor implements ExecutorService
	{
		@Override
		public void shutdown() {

		}

		@Override
		public List<Runnable> shutdownNow() {
			return null;
		}

		@Override
		public boolean isShutdown() {
			return false;
		}

		@Override
		public boolean isTerminated() {
			return false;
		}

		@Override
		public boolean awaitTermination(long l, TimeUnit timeUnit) throws InterruptedException {
			return false;
		}

		@Override
		public <T> Future<T> submit(Callable<T> callable) {
			return null;
		}

		@Override
		public <T> Future<T> submit(Runnable runnable, T t) {
			return null;
		}

		@Override
		public Future<?> submit(Runnable runnable) {
			return null;
		}

		@Override
		public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> collection) throws InterruptedException {
			return null;
		}

		@Override
		public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> collection, long l, TimeUnit timeUnit) throws InterruptedException {
			return null;
		}

		@Override
		public <T> T invokeAny(Collection<? extends Callable<T>> collection) throws ExecutionException, InterruptedException {
			return null;
		}

		@Override
		public <T> T invokeAny(Collection<? extends Callable<T>> collection, long l, TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException {
			return null;
		}

		@Override
		public void execute(Runnable runnable) {

		}
	}
}
