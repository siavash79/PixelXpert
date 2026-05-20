package sh.siava.pixelxpert.xposed.modpacks.systemui;

import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.xposed.XPrefs.Xprefs;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.UserManager;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import io.github.libxposed.api.XposedModuleInterface;
import sh.siava.pixelxpert.annotations.ChildProcessModPack;
import sh.siava.pixelxpert.xposed.XposedModPack;
import sh.siava.pixelxpert.xposed.annotations.SystemUIModPack;
import sh.siava.pixelxpert.xposed.utils.reflection.ReflectedClass;

@SystemUIModPack
@ChildProcessModPack(processNameContains = "screenshot")
@SuppressWarnings("RedundantThrows")
public class ScreenshotManager extends XposedModPack {
	private static boolean disableScreenshotSound = false;
	private boolean ScreenshotChordInsecure = false;

	public ScreenshotManager(Context context) {
		super(context);
	}

	@Override
	public void onPreferenceUpdated(String... Key) {
		if (Xprefs == null) return;
		disableScreenshotSound = Xprefs.getBoolean("disableScreenshotSound", false);
		ScreenshotChordInsecure = Xprefs.getBoolean("ScreenshotChordInsecure", false);
	}

	@Override
	public void onPackageLoaded(XposedModuleInterface.PackageReadyParam PRParam) throws Throwable {
		if (android.os.Build.VERSION.SDK_INT >= 37) return;
		ReflectedClass NewCaptureArgsClass = ReflectedClass.ofIfPossible("android.window.ScreenCaptureInternal.CaptureArgs"); //A16QPR2
		ReflectedClass CaptureArgsClass = ReflectedClass.ofIfPossible("android.window.ScreenCapture.CaptureArgs"); //A16QPR1
		ReflectedClass TakeScreenshotExecutorImplClass = ReflectedClass.of("com.android.systemui.screenshot.TakeScreenshotExecutorImpl");
		ReflectedClass ScreenshotSoundControllerImplClass = ReflectedClass.ofIfPossible("com.android.systemui.screenshot.ScreenshotSoundControllerImpl");

		if (android.os.Build.VERSION.SDK_INT < 37) {
			ReflectedClass.of(UserManager.class)
					.before("getUserInfo")
					.run(param -> param.args[0] = 0);
		}

		ReflectedClass ScreenshotPolicyImplClass = ReflectedClass.ofIfPossible("com.android.systemui.screenshot.ScreenshotPolicyImpl");

		if(ScreenshotPolicyImplClass.getClazz() != null) {
			ScreenshotPolicyImplClass
					.before(Pattern.compile(".*isManagedProfile.*"))
					.run(param -> {
						if (ScreenshotChordInsecure)
							param.setResult(false);
					});
		}

		NewCaptureArgsClass
				.afterConstruction()
				.run(param -> {
					if(ScreenshotChordInsecure) {
						setObjectField(param.thisObject, "mSecureContentPolicy", 1); //No source available. but apparently 1 works.
					}
				});

		CaptureArgsClass
				.afterConstruction()
				.run(param -> {
					if(ScreenshotChordInsecure) {
						setObjectField(param.thisObject, "mCaptureSecureLayers", true);
					}
				});


		if (android.os.Build.VERSION.SDK_INT < 37) {
			//17 - much easier approach: killing mediaplayer totally
			ReflectedClass.of(MediaPlayer.class)
					.before("start")
					.run(param -> {
						if(disableScreenshotSound)
							param.setResult(null);
					});

			//16 qpr2
			TakeScreenshotExecutorImplClass
					.after("getScreenshotController")
					.run(param -> {
						if(disableScreenshotSound) {
							setObjectField(
									getObjectField(param.getResult(), "screenshotSoundController"),
									"bgDispatcher",
									ReflectedClass.of("kotlinx.coroutines.ExecutorCoroutineDispatcherImpl").getClazz().getConstructors()[0].newInstance(new NoExecutor()));
						}
					});

			//16 qpr1
			ScreenshotSoundControllerImplClass
					.beforeConstruction()
					.run(param -> {
						if(disableScreenshotSound) {
							for (int i = 0; i < param.args.length; i++) {
								if (param.args[i].getClass().getName().toLowerCase().contains("dispatcher")) {
									param.args[i] = ReflectedClass.of("kotlinx.coroutines.ExecutorCoroutineDispatcherImpl").getClazz().getConstructors()[0].newInstance(new NoExecutor());
								}
							}
						}
					});
		}
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
