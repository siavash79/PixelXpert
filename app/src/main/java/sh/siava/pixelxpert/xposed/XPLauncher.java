package sh.siava.pixelxpert.xposed;

import static android.content.Context.CONTEXT_IGNORE_SECURITY;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.BuildConfig.APPLICATION_ID;
import static sh.siava.pixelxpert.xposed.XPrefs.Xprefs;
import static sh.siava.pixelxpert.xposed.utils.BootLoopProtector.isBootLooped;

import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
import io.github.libxposed.api.XposedInterface;
import sh.siava.pixelxpert.BuildConfig;
import sh.siava.pixelxpert.IPixelXpertProxy;
import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.service.PixelXpertProxy;
import sh.siava.pixelxpert.xposed.modpacks.android.StatusbarSize;
import sh.siava.pixelxpert.xposed.utils.ExtendedRemotePreferences;
import sh.siava.pixelxpert.xposed.utils.SystemUtils;
import sh.siava.pixelxpert.xposed.utils.reflection.ReflectedClass;
import sh.siava.pixelxpert.xposed.utils.toolkit.Logger;

public class XPLauncher extends XposedModule implements ServiceConnection {
	public static String processName = "";
	public static boolean isSystemServer = false;

	public static ArrayList<XposedModPack> runningMods = new ArrayList<>();
	public Context mContext = null;
	@SuppressLint("StaticFieldLeak")
	static XPLauncher instance;

	private CountDownLatch rootProxyCountdown = new CountDownLatch(1);
	private static IPixelXpertProxy rootProxyIPC;
	private static final Queue<ProxyRunnable> proxyQueue = new LinkedList<>();
	private static boolean EARLY_SYSTEM_CONTEXT_HOOK_INSTALLED = false;
	private static boolean EARLY_SYSTEM_SERVER_HOOKS_INSTALLED = false;
	private static boolean TELECOM_SERVER_LOADED = false;
	public static Resources moduleResources;

	public XPLauncher()
	{
		instance = this;
		Logger.setXposedInterface(this);
	}

	@Override
	public void onModuleLoaded(@NonNull ModuleLoadedParam param) {
		super.onModuleLoaded(param);

		processName = param.getProcessName();
		isSystemServer = param.isSystemServer();
	}

	@Override
	public void onSystemServerStarting(@NonNull XposedModuleInterface.SystemServerStartingParam SSSP)
	{
		ReflectedClass.setDefaultXposedInterface(this);
		ReflectedClass.setFrameworkClassloader(SSSP.getClassLoader());
		try {
			StatusbarSize.installEarlyNoCutoutHook(SSSP.getClassLoader(), false);
			hookEarlySystemContextStatusBarSize(SSSP.getClassLoader());
			hookEarlySystemServerStatusBarSize(SSSP.getClassLoader());
		} catch (Throwable t) {
			Logger.log(t);
		}
	}

	private static void hook17BetaAudioManagerSRWorkaround(PackageReadyParam PRParam) {
		ReflectedClass.of("android.media.AudioManager", PRParam.getClassLoader())
				.before("requestAudioFocus")
				.run(instance,param -> {
					if(getObjectField(param.thisObject, "mApplicationContext") == null) {
						setObjectField(param.thisObject, "mApplicationContext", getObjectField(param.thisObject, "mOriginalContext"));
					}
				});
	}

	@Override
	public void onPackageReady(@NonNull PackageReadyParam PRParam){
		ReflectedClass.setDefaultXposedInterface(this);

		hook17BetaAudioManagerSRWorkaround(PRParam);

		if (isSystemServer && !PRParam.getPackageName().equals(Constants.TELECOM_SERVER_PACKAGE)) {
			hookEarlySystemServerStatusBarSize(PRParam.getClassLoader());

			ReflectedClass PhoneWindowManagerClass = ReflectedClass.of("com.android.server.policy.PhoneWindowManager");

			PhoneWindowManagerClass
					.before("init")
					.run(instance,param -> {
						try {
							if (mContext == null) {
								mContext = (Context) param.args[0];

								moduleResources = mContext.createPackageContext(APPLICATION_ID, CONTEXT_IGNORE_SECURITY)
										.getResources();

								XPrefs.init(mContext);

								CompletableFuture.runAsync(() -> waitForXprefsLoad(PRParam));
							}
						} catch (Throwable t) {
							Logger.log(t);
						}
					});
		}

		if(!isSystemServer || PRParam.getPackageName().equals(Constants.TELECOM_SERVER_PACKAGE)) {
			ReflectedClass.of(Instrumentation.class)
					.after("newApplication")
					.run(this, param -> {
				try {
					if (mContext == null || (PRParam.getPackageName().equals(Constants.TELECOM_SERVER_PACKAGE) && !TELECOM_SERVER_LOADED)) {
						if (PRParam.getPackageName().equals(Constants.TELECOM_SERVER_PACKAGE))
							TELECOM_SERVER_LOADED = true;

						mContext = (Context) param.args[param.args.length - 1];

						moduleResources = mContext.createPackageContext(APPLICATION_ID, CONTEXT_IGNORE_SECURITY)
								                  .getResources();

						XPrefs.init(mContext);

						waitForXprefsLoad(PRParam);
					}
				} catch (Throwable t) {
					Logger.log(t);
				}
			});
		}
	}

	private void hookEarlySystemServerStatusBarSize(ClassLoader classLoader) {
		if (EARLY_SYSTEM_SERVER_HOOKS_INSTALLED) return;

		try {
			Set<XposedInterface.HookHandle> hooks = ReflectedClass.of("com.android.server.wm.WindowManagerService", classLoader)
					.before("main")
					.run(instance, param -> {
						try {
							updateEarlyStatusBarSizePrefs((Context) param.args[0], classLoader);
						} catch (Throwable t) {
							Logger.log(t);
						}
					});
			EARLY_SYSTEM_SERVER_HOOKS_INSTALLED = !hooks.isEmpty();
			if (!EARLY_SYSTEM_SERVER_HOOKS_INSTALLED) {
				Logger.log("PixelXpert: failed to find WindowManagerService.main for early status bar hooks");
			}
		} catch (Throwable t) {
			Logger.log(t);
		}
	}

	private void hookEarlySystemContextStatusBarSize(ClassLoader classLoader) {
		if (EARLY_SYSTEM_CONTEXT_HOOK_INSTALLED) return;

		try {
			Set<XposedInterface.HookHandle> hooks = ReflectedClass.of("com.android.server.SystemServer", classLoader)
					.after("createSystemContext")
					.run(this, param -> {
						try {
							updateEarlyStatusBarSizePrefs((Context) getObjectField(param.thisObject, "mSystemContext"), classLoader);
						} catch (Throwable t) {
							Logger.log(t);
						}
					});
			EARLY_SYSTEM_CONTEXT_HOOK_INSTALLED = !hooks.isEmpty();
			if (!EARLY_SYSTEM_CONTEXT_HOOK_INSTALLED) {
				Logger.log("PixelXpert: failed to find SystemServer.createSystemContext for early status bar hooks");
			}
		} catch (Throwable t) {
			Logger.log(t);
		}
	}

	private static void updateEarlyStatusBarSizePrefs(Context context, ClassLoader classLoader) {
		boolean noCutoutEnabled = getEarlyBooleanPref(context, "noCutoutEnabled", false);
		StatusbarSize.installEarlyNoCutoutHook(classLoader, noCutoutEnabled);
	}

	private static boolean getEarlyBooleanPref(Context context, String key, boolean defaultValue) {
		try {
			ExtendedRemotePreferences prefs = new ExtendedRemotePreferences(context, APPLICATION_ID, APPLICATION_ID + "_preferences", true);
			return prefs.getBoolean(key, defaultValue);
		} catch (Throwable ignored) {
		}

		try {
			Context moduleContext = context.createPackageContext(APPLICATION_ID, CONTEXT_IGNORE_SECURITY)
					.createDeviceProtectedStorageContext();
			return moduleContext
					.getSharedPreferences(APPLICATION_ID + "_preferences", Context.MODE_PRIVATE)
					.getBoolean(key, defaultValue);
		} catch (Throwable ignored) {
		}

		return defaultValue;
	}

	private void waitForXprefsLoad(PackageReadyParam PRParam) {
		while (true) {
			try {
				Xprefs.getBoolean("LoadTestBooleanValue", false);
				break;
			} catch (Throwable ignored) {
				SystemUtils.threadSleep(1000);
			}
		}

		Logger.log(String.format("Loading PixelXpert version: %s on %s", BuildConfig.VERSION_NAME, PRParam.getPackageName()));
		try {
			Logger.log("PixelXpert Records: " + Xprefs.getAll().size());
		} catch (Throwable ignored) {
		}

		onXPrefsReady(PRParam);
	}

	private void onXPrefsReady(PackageReadyParam PRParam) {
		if (isBootLooped(PRParam.getPackageName())) {
			Logger.log(String.format("PixelXpert: Possible bootloop in %s. Will not load for now", PRParam.getPackageName()));
			return;
		}

		new SystemUtils(mContext);
		XPrefs.setPackagePrefs(PRParam.getPackageName());

		loadModPacks(PRParam);

		XPrefs.onContentProviderLoaded();
	}

	private void loadModPacks(PackageReadyParam PRParam) {
		ReflectedClass.setDefaultClassloader(PRParam.getClassLoader());

		if (Arrays.asList(moduleResources.getStringArray(R.array.root_requirement)).contains(PRParam.getPackageName())) {
			forceConnectRootService();
		}

		ModPacks.getModPacks()
				.forEach(modPackData -> {
					String partOfProcessName = modPackData.targetsMainProcess ? "" : modPackData.childProcessName;

					if((modPackData.targetPackage.equals(PRParam.getPackageName()) || modPackData.targetPackage.isEmpty() /*common mod packs*/ || (modPackData.targetPackage.equals(Constants.SYSTEM_FRAMEWORK_PACKAGE) && isSystemServer))
							   && processName.contains(partOfProcessName))
					{
						//noinspection unchecked
						loadModPack((Class<? extends XposedModPack>) modPackData.clazz, PRParam);
					}
				});
	}

	private void loadModPack(Class<? extends XposedModPack> thisClass, PackageReadyParam PRParam) {
		try {
			XposedModPack instance = thisClass.getConstructor(Context.class).newInstance(mContext);
			try {
				instance.onPreferenceUpdated();
			} catch (Throwable ignored) {
			}

			instance.onPackageLoaded(PRParam);
			runningMods.add(instance);
		} catch (Throwable T) {
			Logger.log("Start Error Dump - Occurred in " + thisClass.getName());
			Logger.log(T);
		}
	}

	private void forceConnectRootService() {
		new Thread(() -> {
			while (SystemUtils.UserManager() == null
					       || !SystemUtils.UserManager().isUserUnlocked()) //device is still CE encrypted
			{
				SystemUtils.threadSleep(2000);
			}
			SystemUtils.threadSleep(5000); //wait for the unlocked account to settle down a bit

			while (rootProxyIPC == null) {
				connectRootService();
				SystemUtils.threadSleep(5000);
			}
		}).start();
	}

	private void connectRootService() {
		try {
			Intent intent = new Intent();
			intent.setComponent(new ComponentName(APPLICATION_ID, PixelXpertProxy.class.getName()));
			mContext.bindService(intent, instance, Context.BIND_AUTO_CREATE | Context.BIND_ADJUST_WITH_ACTIVITY);
		} catch (Throwable t) {
			Logger.log(t);
		}
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		rootProxyIPC = IPixelXpertProxy.Stub.asInterface(service);
		rootProxyCountdown.countDown();

		synchronized (proxyQueue) {
			while (!proxyQueue.isEmpty()) {
				try {
					Objects.requireNonNull(proxyQueue.poll()).run(rootProxyIPC);
				} catch (Throwable ignored) {
				}
			}
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		rootProxyIPC = null;

		forceConnectRootService();
	}

	public static IPixelXpertProxy getRootProviderProxy() {
		if (rootProxyIPC == null) {
			instance.rootProxyCountdown = new CountDownLatch(1);
			instance.forceConnectRootService();
			try {
				//noinspection ResultOfMethodCallIgnored
				instance.rootProxyCountdown.await(5, TimeUnit.SECONDS);
			} catch (Throwable ignored) {
			}
		}
		return rootProxyIPC;
	}

	public static void enqueueProxyCommand(ProxyRunnable runnable) {
		if (rootProxyIPC != null) {
			try {
				runnable.run(rootProxyIPC);
			} catch (RemoteException ignored) {
			}
		} else {
			synchronized (proxyQueue) {
				proxyQueue.add(runnable);
			}
			instance.forceConnectRootService();
		}
	}

	public interface ProxyRunnable {
		void run(IPixelXpertProxy proxy) throws RemoteException;
	}
}
