package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class ThreeButtonNavMods extends XposedModPack {
	private static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;
	private boolean ThreeButtonLayoutMod = false;
	private static String ThreeButtonCenter, ThreeButtonRight, ThreeButtonLeft;

	public ThreeButtonNavMods(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		if (Xprefs == null) return;
		ThreeButtonLayoutMod = Xprefs.getBoolean("ThreeButtonLayoutMod", false);

		ThreeButtonLeft = Xprefs.getString("ThreeButtonLeft", "back");
		ThreeButtonCenter = Xprefs.getString("ThreeButtonCenter", "home");
		ThreeButtonRight = Xprefs.getString("ThreeButtonRight", "recent");
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
		return listenPackage.equals(packageName);
	}

}
