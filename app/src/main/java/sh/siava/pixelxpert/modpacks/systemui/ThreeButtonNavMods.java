package sh.siava.pixelxpert.modpacks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.XposedModPack;

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
		if (Xprefs == null) return;
		ThreeButtonLayoutMod = Xprefs.getBoolean("ThreeButtonLayoutMod", false);

		ThreeButtonLeft = Xprefs.getString("ThreeButtonLeft", "back");
		ThreeButtonCenter = Xprefs.getString("ThreeButtonCenter", "home");
		ThreeButtonRight = Xprefs.getString("ThreeButtonRight", "recent");
	}


	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) {
		if (!lpParam.packageName.equals(listenPackage)) return;

		Class<?> NavigationBarInflaterViewClass = findClassIfExists("com.android.systemui.navigationbar.views.NavigationBarInflaterView", lpParam.classLoader);
		if(NavigationBarInflaterViewClass == null)
		{
			NavigationBarInflaterViewClass = findClassIfExists("com.android.systemui.navigationbar.NavigationBarInflaterView", lpParam.classLoader);
		}

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
