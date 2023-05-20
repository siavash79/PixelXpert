package sh.siava.AOSPMods.modpacks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import android.content.Context;

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
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.XPLauncher;
import sh.siava.AOSPMods.modpacks.XPrefs;
import sh.siava.AOSPMods.modpacks.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class ScreenshotMuter extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	private static boolean disableScreenshotSound = false;

	public ScreenshotMuter(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		if (XPrefs.Xprefs == null) return;
		disableScreenshotSound = XPrefs.Xprefs.getBoolean("disableScreenshotSound", false);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(listenPackage)) return;

		Class<?> ScreenshotControllerClass = findClass("com.android.systemui.screenshot.ScreenshotController", lpparam.classLoader);

		hookAllConstructors(ScreenshotControllerClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (!disableScreenshotSound) return;

				setObjectField(param.thisObject, "mBgExecutor", notExecutor);
			}
		});
	}

	//Seems like an executor, but doesn't act! perfect thing
	ExecutorService notExecutor = new ExecutorService() {
		@Override
		public void shutdown() {}

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
		public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
			return false;
		}

		@Override
		public <T> Future<T> submit(Callable<T> task) {
			return null;
		}

		@Override
		public <T> Future<T> submit(Runnable task, T result) {
			return null;
		}

		@Override
		public Future<?> submit(Runnable task) {
			return null;
		}

		@Override
		public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
			return null;
		}

		@Override
		public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
			return null;
		}

		@Override
		public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws ExecutionException, InterruptedException {
			return null;
		}

		@Override
		public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
			return null;
		}

		@Override
		public void execute(Runnable command) {
		}
	};


	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && XPLauncher.isChildProcess;
	}
}
