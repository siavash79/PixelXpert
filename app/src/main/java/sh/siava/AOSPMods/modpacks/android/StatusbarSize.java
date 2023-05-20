package sh.siava.AOSPMods.modpacks.android;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.getStaticObjectField;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.view.DisplayCutout;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.XPLauncher;
import sh.siava.AOSPMods.modpacks.XPrefs;
import sh.siava.AOSPMods.modpacks.XposedModPack;


//We are playing in system framework. should be extra cautious..... many try-catchs, still not enough!
@SuppressWarnings("RedundantThrows")
public class StatusbarSize extends XposedModPack {
	private final List<String> listenPacks = new ArrayList<>();

	private static final int BOUNDS_POSITION_TOP = 1;

	static int sizeFactor = 100; // % of normal
	static boolean noCutoutEnabled = true;
	int currentHeight = 0;
	boolean edited = false; //if we touched it once during this instance, we'll have to continue setting it even if it's the original value
	private boolean mForceApplyHeight = false;

	public StatusbarSize(Context context) {
		super(context);
		listenPacks.add(Constants.SYSTEM_FRAMEWORK_PACKAGE);
		listenPacks.add(Constants.SYSTEM_UI_PACKAGE);
	}

	@SuppressLint({"DiscouragedApi", "InternalInsetResource"})
	@Override
	public void updatePrefs(String... Key) {
		if (XPrefs.Xprefs == null) return;

		noCutoutEnabled = XPrefs.Xprefs.getBoolean("noCutoutEnabled", false);

		mForceApplyHeight = XPrefs.Xprefs.getBoolean("allScreenRotations", false) //Particularly used for rotation Status bar
				|| noCutoutEnabled
				|| XPrefs.Xprefs.getBoolean("systemIconsMultiRow", false)
				|| XPrefs.Xprefs.getBoolean("notificationAreaMultiRow", false);

		sizeFactor = XPrefs.Xprefs.getInt("statusbarHeightFactor", 100);
		if (sizeFactor != 100 || edited || mForceApplyHeight) {
			Configuration conf = new Configuration();
			conf.updateFrom(mContext.getResources().getConfiguration());

			conf.orientation = Configuration.ORIENTATION_PORTRAIT;
			Context portraitContext = mContext.createConfigurationContext(conf);

			currentHeight = Math.round(
					portraitContext.getResources().getDimensionPixelSize(
							portraitContext.getResources().getIdentifier(
									"status_bar_height",
									"dimen",
									"android")
					)
							* sizeFactor
							/ 100f);
		}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPacks.contains(packageName) && !XPLauncher.isChildProcess;
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		try {
			try {
				Class<?> WmDisplayCutoutClass = findClass("com.android.server.wm.utils.WmDisplayCutout", lpparam.classLoader);
				Class<?> DisplayCutoutClass = findClass("android.view.DisplayCutout", lpparam.classLoader);

				Object NO_CUTOUT = getStaticObjectField(DisplayCutoutClass, "NO_CUTOUT");

				hookAllMethods(WmDisplayCutoutClass, "getDisplayCutout", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						if (noCutoutEnabled) {
							param.setResult(NO_CUTOUT);
						}
					}

					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						if (sizeFactor >= 100 && !edited) return;

						DisplayCutout displayCutout = (DisplayCutout) param.getResult();

						Rect boundTop = ((Rect[]) getObjectField(
								getObjectField(
										displayCutout,
										"mBounds"),
								"mRects")
						)[BOUNDS_POSITION_TOP];
						boundTop.bottom = Math.min(boundTop.bottom, currentHeight);

						Rect mSafeInsets = (Rect) getObjectField(
								displayCutout,
								"mSafeInsets");
						mSafeInsets.top = Math.min(mSafeInsets.top, currentHeight);
					}
				});
			} catch (Throwable ignored) {
			}

			XC_MethodHook resizedResultHook = new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) {
					try {
						if (sizeFactor == 100 && !edited && !mForceApplyHeight) return;
						edited = true;
						param.setResult(currentHeight);
					} catch (Throwable ignored) {
					}
				}
			};

			try {
				Class<?> SystemBarUtilsClass = findClass("com.android.internal.policy.SystemBarUtils", lpparam.classLoader);

				hookAllMethods(SystemBarUtilsClass, "getStatusBarHeight", resizedResultHook);
				hookAllMethods(SystemBarUtilsClass, "getStatusBarHeightForRotation", resizedResultHook);
			} catch (Throwable ignored) {
			}
		} catch (Throwable ignored) {
		}
	}
}
