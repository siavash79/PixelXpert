package sh.siava.AOSPMods.modpacks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClass;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.XPLauncher;
import sh.siava.AOSPMods.modpacks.XPrefs;
import sh.siava.AOSPMods.modpacks.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class ThreeButtonNavMods extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;
	private boolean ThreeButtonLayoutMod = false;
	private static String ThreeButtonCenter, ThreeButtonRight, ThreeButtonLeft;

	public ThreeButtonNavMods(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		if (XPrefs.Xprefs == null) return;
		ThreeButtonLayoutMod = XPrefs.Xprefs.getBoolean("ThreeButtonLayoutMod", false);

		ThreeButtonLeft = XPrefs.Xprefs.getString("ThreeButtonLeft", "back");
		ThreeButtonCenter = XPrefs.Xprefs.getString("ThreeButtonCenter", "home");
		ThreeButtonRight = XPrefs.Xprefs.getString("ThreeButtonRight", "recent");
	}


	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
		if (!lpparam.packageName.equals(listenPackage)) return;

		Class<?> NavigationBarInflaterViewClass = findClass("com.android.systemui.navigationbar.NavigationBarInflaterView", lpparam.classLoader);

		hookAllMethods(NavigationBarInflaterViewClass, "inflateLayout", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (!ThreeButtonLayoutMod || !((String) param.args[0]).contains("recent")) return;

				String layout = ((String) param.args[0])
						.replace("home", "XCenterX")
						.replace("back", "XLeftX")
						.replace("recent", "XRightX");

				param.args[0] = layout
						.replace("XCenterX", ThreeButtonCenter)
						.replace("XLeftX", ThreeButtonLeft)
						.replace("XRightX", ThreeButtonRight);
			}
		});
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}

}
