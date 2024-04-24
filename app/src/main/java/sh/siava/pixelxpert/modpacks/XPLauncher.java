package sh.siava.pixelxpert.modpacks;

import static android.content.Context.CONTEXT_IGNORE_SECURITY;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static sh.siava.pixelxpert.BuildConfig.APPLICATION_ID;
import static sh.siava.pixelxpert.modpacks.Constants.SYSTEM_UI_PACKAGE;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;
import static sh.siava.pixelxpert.modpacks.utils.BootLoopProtector.isBootLooped;
import static sh.siava.pixelxpert.modpacks.utils.SystemUtils.sleep;

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
import sh.siava.pixelxpert.BuildConfig;
import sh.siava.pixelxpert.IRootProviderProxy;
import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.modpacks.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class XPLauncher implements ServiceConnection {

	public static boolean isChildProcess = false;
	public static String processName = "";

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

	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		try
		{
			isChildProcess = lpParam.processName.contains(":");
			processName = lpParam.processName;
		} catch (Throwable ignored)
		{
			isChildProcess = false;
		}

		//If example class isn't found, user is using an older version. Don't load the module at all
		if (Build.VERSION.SDK_INT ==  Build.VERSION_CODES.TIRAMISU && lpParam.packageName.equals(SYSTEM_UI_PACKAGE)) {
			Class<?> A33R18Example = findClassIfExists("com.android.systemui.shade.NotificationPanelViewController", lpParam.classLoader);
			if (A33R18Example == null)
			{
				log("This version isn't compatible with your ROM. Exiting...");
				return;
			}
		}

		if(lpParam.packageName.equals(Constants.SYSTEM_FRAMEWORK_PACKAGE))
		{
			Class<?> PhoneWindowManagerClass = findClass("com.android.server.policy.PhoneWindowManager", lpParam.classLoader);
			hookAllMethods(PhoneWindowManagerClass, "init", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					try {
						if (mContext == null) {
							mContext = (Context) param.args[0];

							ResourceManager.modRes = mContext.createPackageContext(APPLICATION_ID, CONTEXT_IGNORE_SECURITY)
									.getResources();

							XPrefs.init(mContext);

							CompletableFuture.runAsync(() -> waitForXprefsLoad(lpParam));
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
						if (mContext == null || lpParam.packageName.equals(Constants.TELECOM_SERVER_PACKAGE)) { //telecom service launches as a secondary process in framework, but has its own package name. context is not null when it loads
							mContext = (Context) param.args[2];

							ResourceManager.modRes = mContext.createPackageContext(APPLICATION_ID, CONTEXT_IGNORE_SECURITY)
									.getResources();

							XPrefs.init(mContext);

							waitForXprefsLoad(lpParam);
						}
					}
					catch (Throwable t){
						log(t);
					}
				}
			});
		}
	}

	private void onXPrefsReady(XC_LoadPackage.LoadPackageParam lpParam) {
		if (isBootLooped(lpParam.packageName)) {
			log(String.format("PixelXpert: Possible bootloop in %s. Will not load for now", lpParam.packageName));
			return;
		}

		new SystemUtils(mContext);
		XPrefs.setPackagePrefs(lpParam.packageName);

		loadModpacks(lpParam);
	}

	private void loadModpacks(XC_LoadPackage.LoadPackageParam lpParam) {
		if (Arrays.asList(ResourceManager.modRes.getStringArray(R.array.root_requirement)).contains(lpParam.packageName)) {
			forceConnectRootService();
		}

		for (Class<? extends XposedModPack> mod : ModPacks.getMods(lpParam.packageName)) {
			try {
				XposedModPack instance = mod.getConstructor(Context.class).newInstance(mContext);
				if (!instance.listensTo(lpParam.packageName)) continue;
				try {
					instance.updatePrefs();
				} catch (Throwable ignored) {
				}
				instance.handleLoadPackage(lpParam);
				runningMods.add(instance);
			} catch (Throwable T) {
				log("Start Error Dump - Occurred in " + mod.getName());
				log(T);
			}
		}
	}

	private void forceConnectRootService()
	{
		new Thread(() -> {
			while(SystemUtils.UserManager() == null
					|| !SystemUtils.UserManager().isUserUnlocked()) //device is still CE encrypted
			{
				sleep(2000);
			}
			sleep(5000); //wait for the unlocked account to settle down a bit

			while(rootProxyIPC == null)
			{
				connectRootService();
				sleep(5000);
			}
		}).start();
	}

	private void connectRootService()
	{
		try {
			Intent intent = new Intent();
			intent.setComponent(new ComponentName(APPLICATION_ID, APPLICATION_ID + ".service.RootProviderProxy"));
			mContext.bindService(intent, instance, Context.BIND_AUTO_CREATE | Context.BIND_ADJUST_WITH_ACTIVITY);
		}
		catch (Throwable t)
		{
			log(t);
		}
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
					Objects.requireNonNull(proxyQueue.poll()).run(rootProxyIPC);
				}
				catch (Throwable ignored){}
			}
		}
	}

	private void waitForXprefsLoad(XC_LoadPackage.LoadPackageParam lpParam) {
		while(true)
		{
			try
			{
				Xprefs.getBoolean("LoadTestBooleanValue", false);
				break;
			}
			catch (Throwable ignored)
			{
				sleep(1000);
			}
		}

		log("PixelXpert Version: " + BuildConfig.VERSION_NAME);
		try {
			log("PixelXpert Records: " + Xprefs.getAll().keySet().size());
		} catch (Throwable ignored) {}

		onXPrefsReady(lpParam);
	}


	@Override
	public void onServiceDisconnected(ComponentName name) {
		rootProxyIPC = null;

		forceConnectRootService();
	}

	public static void enqueueProxyCommand(ProxyRunnable runnable)
	{
		if(rootProxyIPC != null)
		{
			try {
				runnable.run(rootProxyIPC);
			} catch (RemoteException ignored) {}
		}
		else
		{
			synchronized (proxyQueue) {
				proxyQueue.add(runnable);
			}
			instance.forceConnectRootService();
		}
	}

	public interface ProxyRunnable
	{
		void run(IRootProviderProxy proxy) throws RemoteException;
	}
}