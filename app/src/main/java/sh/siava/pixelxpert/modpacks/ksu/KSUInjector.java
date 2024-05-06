package sh.siava.pixelxpert.modpacks.ksu;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.topjohnwu.superuser.Shell;

import org.objenesis.ObjenesisHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.BuildConfig;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.pixelxpert.modpacks.utils.SystemUtils;

/** @noinspection RedundantThrows*/
public class KSUInjector extends XposedModPack {
	private static final String listenPackage = Constants.KSU_PACKAGE;

	public KSUInjector(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {

	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		Class<?> MainActivityClass = findClass("me.weishu.kernelsu.ui.MainActivity", lpParam.classLoader);
		Class<?> NativesClass = findClass("me.weishu.kernelsu.Natives", lpParam.classLoader);
		Class<?> ProfileClass = findClass("me.weishu.kernelsu.Natives$Profile", lpParam.classLoader);
		hookAllMethods(MainActivityClass, "onCreate", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				Intent launchIntent = ((Activity) param.thisObject).getIntent();
				if(launchIntent.hasExtra(Constants.PX_ROOT_EXTRA))
				{
					new Thread(() -> {
						try
						{
							Object nativeObject = ObjenesisHelper.newInstance(NativesClass);
							int[] rootUIDs = (int[]) callMethod(nativeObject, "getAllowList");

							PackageManager packageManager = mContext.getPackageManager();
							int ownUID = packageManager.getPackageUid(BuildConfig.APPLICATION_ID, PackageManager.GET_ACTIVITIES);

							boolean haveRoot = Arrays.stream(rootUIDs).anyMatch(uid -> uid == ownUID);

							if(!haveRoot) {
								Object ownRootProfile = ProfileClass.getConstructor(String.class, int.class, boolean.class, boolean.class, String.class, int.class, int.class, List.class, List.class, String.class, int.class, boolean.class, boolean.class, String.class)
										.newInstance(BuildConfig.APPLICATION_ID, ownUID, true, true, null, 0, 0, new ArrayList<>(), new ArrayList<>(), "u:r:su:s0", 0, true, true, "");

								callMethod(nativeObject, "setAppProfile", ownRootProfile);

								restartPX(launchIntent.hasExtra("launchApp"));
							}
							Thread.sleep(2000);
							SystemUtils.killSelf();
						}
						catch (Throwable ignored) {}
					}).start();
				}
			}
		});
	}

	private void restartPX(boolean launch) throws InterruptedException {
		Shell.cmd("killall " + BuildConfig.APPLICATION_ID).exec();

		if(launch) {
			Thread.sleep(1000);
			mContext.startActivity(
					mContext
							.getPackageManager()
							.getLaunchIntentForPackage(BuildConfig.APPLICATION_ID));
		}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}
}