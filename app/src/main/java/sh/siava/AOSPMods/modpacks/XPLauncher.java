package sh.siava.AOSPMods.modpacks;

import static android.content.Context.CONTEXT_IGNORE_SECURITY;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static sh.siava.AOSPMods.BuildConfig.APPLICATION_ID;
import static sh.siava.AOSPMods.modpacks.Constants.SYSTEM_UI_PACKAGE;
import static sh.siava.AOSPMods.modpacks.XPrefs.Xprefs;
import static sh.siava.AOSPMods.modpacks.utils.BootLoopProtector.isBootLooped;

import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.BuildConfig;
import sh.siava.AOSPMods.IRootProviderProxy;
import sh.siava.AOSPMods.R;
import sh.siava.AOSPMods.modpacks.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class XPLauncher implements ServiceConnection {

	public static boolean isChildProcess = false;
	public static String processName = "";
	private String packageName;

	public static ArrayList<XposedModPack> runningMods = new ArrayList<>();
	public Context mContext = null;
	@SuppressLint("StaticFieldLeak")
	static XPLauncher instance;

	private static IRootProviderProxy rootProxyIPC;
	private static final Queue<ProxyRunnable> proxyQueue = new LinkedList<>();

	/** @noinspection FieldCanBeLocal*/
	public XPLauncher() {
		instance = this;
	}

	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		packageName = lpparam.packageName;
		try
		{
			isChildProcess = lpparam.processName.contains(":");
			processName = lpparam.processName;
		} catch (Throwable ignored)
		{
			isChildProcess = false;
		}

		//If example class isn't found, user is using an older version. Don't load the module at all
		if (Build.VERSION.SDK_INT ==  Build.VERSION_CODES.TIRAMISU && packageName.equals(SYSTEM_UI_PACKAGE)) {
			Class<?> A33R18Example = findClassIfExists("com.android.systemui.shade.NotificationPanelViewController", lpparam.classLoader);
			if (A33R18Example == null)
			{
				log("This version isn't compatible with your ROM. Exiting...");
				return;
			}
		}

		/*if (lpparam.packageName.equals(SYSTEM_UI_PACKAGE) && DEBUG && false) {
			log("------------");
			Helpers.dumpClass("com.android.systemui.statusbar.notification.collection.NotifCollection", lpparam.classLoader);
			log("------------");
		}*/

		Class<?> PhoneWindowManagerClass = findClassIfExists("com.android.server.policy.PhoneWindowManager", lpparam.classLoader);
		if(PhoneWindowManagerClass != null)
		{
			hookAllMethods(PhoneWindowManagerClass, "init", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					try {
						if (mContext == null) {
							mContext = (Context) param.args[0];

							ResourceManager.modRes = mContext.createPackageContext(APPLICATION_ID, CONTEXT_IGNORE_SECURITY)
									.getResources();

							XPrefs.init(mContext);

							CompletableFuture.runAsync(() -> waitForXprefsLoad(lpparam));
						}
					}
					catch (Throwable t){
						log(t);
					}
				}
			});
		}
		else {
			findAndHookMethod(Instrumentation.class, "newApplication", ClassLoader.class, String.class, Context.class, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					try {
						if (mContext == null) {
							mContext = (Context) param.args[2];

							ResourceManager.modRes = mContext.createPackageContext(APPLICATION_ID, CONTEXT_IGNORE_SECURITY)
									.getResources();

							XPrefs.init(mContext);

							waitForXprefsLoad(lpparam);
						}
					}
					catch (Throwable t){
						log(t);
					}
				}
			});
		}
	}

	private void onXPrefsReady(XC_LoadPackage.LoadPackageParam lpparam) {
		if (isBootLooped(packageName)) {
			log(String.format("AOSPMods: Possible bootloop in %s. Will not load for now", packageName));
			return;
		}

		new SystemUtils(mContext);
		XPrefs.setPackagePrefs(packageName);

		loadModpacks(lpparam);
	}

	private void loadModpacks(XC_LoadPackage.LoadPackageParam lpparam) {
		if (Arrays.asList(ResourceManager.modRes.getStringArray(R.array.root_requirement)).contains(packageName)) {
			connectRootService();
		}

		for (Class<? extends XposedModPack> mod : ModPacks.getMods(packageName)) {
			try {
				XposedModPack instance = mod.getConstructor(Context.class).newInstance(mContext);
				if (!instance.listensTo(packageName)) continue;
				try {
					instance.updatePrefs();
				} catch (Throwable ignored) {
				}
				instance.handleLoadPackage(lpparam);
				runningMods.add(instance);
			} catch (Throwable T) {
				log("Start Error Dump - Occurred in " + mod.getName());
				log(T);
			}
		}
	}

	private void connectRootService()
	{
		new Thread(() -> {
			// Start RootService connection
			Intent intent = new Intent();
			intent.setComponent(new ComponentName(APPLICATION_ID, APPLICATION_ID + ".service.RootProviderProxy"));

			if(!mContext.bindService(intent, instance, Context.BIND_AUTO_CREATE))
			{
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				connectRootService();
			}
		}).start();
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		rootProxyIPC = IRootProviderProxy.Stub.asInterface(service);
		synchronized (proxyQueue)
		{
			while(!proxyQueue.isEmpty())
			{
				try
				{
					Objects.requireNonNull(proxyQueue.poll()).run();
				}
				catch (Throwable ignored){}
			}
		}
	}

	private void waitForXprefsLoad(XC_LoadPackage.LoadPackageParam lpparam) {
		while(true)
		{
			try
			{
				Xprefs.getBoolean("LoadTestBooleanValue", false);
				break;
			}
			catch (Throwable ignored)
			{
				try {
					//noinspection BusyWait
					Thread.sleep(1000);
				} catch (Throwable ignored1) {}
			}
		}

		log("AOSPMods Version: " + BuildConfig.VERSION_NAME);
		try {
			log("AOSPMods Records: " + Xprefs.getAll().keySet().size());
		} catch (Throwable ignored) {}

		onXPrefsReady(lpparam);
	}


	@Override
	public void onServiceDisconnected(ComponentName name) {
		rootProxyIPC = null;

		connectRootService();
	}

	public static void enqueueProxyCommand(ProxyRunnable runnable)
	{
		if(rootProxyIPC != null)
		{
			runnable.run();
		}
		else
		{
			proxyQueue.add(runnable);
			instance.connectRootService();
		}
	}

	public abstract static class ProxyRunnable implements Runnable
	{
		public abstract void run(IRootProviderProxy proxy) throws RemoteException;

		@Override
		public void run()
		{
			try {
				run(rootProxyIPC);
			} catch (Throwable ignored) {}
		}
	}
}