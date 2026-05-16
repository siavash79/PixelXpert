package sh.siava.pixelxpert.xposed.modpacks.android;



import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.getStaticObjectField;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.setIntField;
import static sh.siava.pixelxpert.xposed.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Insets;
import android.graphics.Rect;
import android.view.DisplayCutout;
import android.view.View;
import android.view.WindowManager;

import io.github.libxposed.api.XposedModuleInterface;
import sh.siava.pixelxpert.xposed.XposedModPack;
import sh.siava.pixelxpert.xposed.annotations.FrameworkModPack;
import sh.siava.pixelxpert.xposed.annotations.SystemUIModPack;
import sh.siava.pixelxpert.xposed.utils.reflection.ReflectedClass;
import sh.siava.pixelxpert.xposed.utils.toolkit.Logger;


//We are playing in system framework. should be extra cautious..... many try-catchs, still not enough!
@SuppressWarnings("RedundantThrows")
@FrameworkModPack
@SystemUIModPack
public class StatusbarSize extends XposedModPack {
	private static final int BOUNDS_POSITION_TOP = 1;

	static int sizeFactor = 100; // % of normal
	static boolean noCutoutEnabled = true;
	private static boolean sNoCutoutHookInstalled = false;
	int currentHeight = 0;
	boolean edited = false; //if we touched it once during this instance, we'll have to continue setting it even if it's the original value
	private boolean mForceApplyHeight = false;

	public StatusbarSize(Context context) {
		super(context);
	}

	@SuppressLint({"DiscouragedApi", "InternalInsetResource"})
	@Override
	public void onPreferenceUpdated(String... Key) {
		if (Xprefs == null) return;

		noCutoutEnabled = Xprefs.getBoolean("noCutoutEnabled", false);

		mForceApplyHeight = Xprefs.getBoolean("allScreenRotations", false) //Particularly used for rotation Status bar
				|| noCutoutEnabled
				|| Xprefs.getBoolean("systemIconsMultiRow", false)
				|| Xprefs.getBoolean("notificationAreaMultiRow", false);

		sizeFactor = Xprefs.getSliderInt("statusbarHeightFactor", 100);
		if (sizeFactor != 100 || edited || mForceApplyHeight) {
			Configuration conf = new Configuration();
			conf.updateFrom(mContext.getResources().getConfiguration());

			conf.orientation = Configuration.ORIENTATION_PORTRAIT;
			Context portraitContext = mContext.createConfigurationContext(conf);

			currentHeight = Math.round(getPortraitStatusBarHeight(portraitContext) * sizeFactor / 100f);
		}
	}

	private static int getPortraitStatusBarHeight(Context context) {
		Resources resources = context.getResources();
		int id = resources.getIdentifier("status_bar_height_portrait", "dimen", "android");
		if (id == 0) {
			id = resources.getIdentifier("status_bar_height", "dimen", "android");
		}
		return resources.getDimensionPixelSize(id);
	}

	private boolean shouldApplyHeight() {
		return sizeFactor != 100 || edited || mForceApplyHeight;
	}

	private void applyCachedStatusBarHeight(Object statusBarWindowController) {
		if (!shouldApplyHeight()) return;

		setIntField(statusBarWindowController, "mBarHeight", currentHeight);
	}

	private void applyStatusBarLayoutParams(Object layoutParams) {
		if (!shouldApplyHeight() || !(layoutParams instanceof WindowManager.LayoutParams lp)) return;

		if (lp.type == WindowManager.LayoutParams.TYPE_STATUS_BAR) {
			lp.height = currentHeight;
			applyStatusBarLayoutParams(getLayoutParamsArray(lp, "paramsForRotation"));
			applyInsetsFrameProviders(getObjectArray(lp, "providedInsets"));
		}
	}

	private void applyStatusBarLayoutParams(WindowManager.LayoutParams[] paramsForRotation) {
		if (paramsForRotation == null) return;

		for (WindowManager.LayoutParams rotationParams : paramsForRotation) {
			if (rotationParams != null) {
				rotationParams.height = currentHeight;
				applyInsetsFrameProviders(getObjectArray(rotationParams, "providedInsets"));
			}
		}
	}

	private WindowManager.LayoutParams[] getLayoutParamsArray(Object target, String fieldName) {
		try {
			Object value = getObjectField(target, fieldName);
			return value instanceof WindowManager.LayoutParams[] ? (WindowManager.LayoutParams[]) value : null;
		} catch (Throwable ignored) {
			return null;
		}
	}

	private Object[] getObjectArray(Object target, String fieldName) {
		try {
			Object value = getObjectField(target, fieldName);
			return value instanceof Object[] ? (Object[]) value : null;
		} catch (Throwable ignored) {
			return null;
		}
	}

	private void applyInsetsFrameProviders(Object[] providers) {
		if (providers == null) return;

		Insets insets = Insets.of(0, currentHeight, 0, 0);
		for (Object provider : providers) {
			if (provider == null) continue;

			try {
				callMethod(provider, "setInsetsSize", insets);
			} catch (Throwable ignored) {
			}
		}
	}

	private void syncStatusBarLayoutParams(Object statusBarWindowController, boolean updateWindow) {
		if (!shouldApplyHeight()) return;

		try {
			applyCachedStatusBarHeight(statusBarWindowController);
			applyStatusBarLayoutParams(getObjectField(statusBarWindowController, "mLpChanged"));

			Object layoutParams = getObjectField(statusBarWindowController, "mLp");
			applyStatusBarLayoutParams(layoutParams);

			if (updateWindow && layoutParams instanceof WindowManager.LayoutParams) {
				Object windowManager = getObjectField(statusBarWindowController, "mWindowManager");
				Object statusBarWindowView = getObjectField(statusBarWindowController, "mStatusBarWindowView");
				if (windowManager != null && statusBarWindowView instanceof View) {
					callMethod(windowManager, "updateViewLayout", statusBarWindowView, layoutParams);
				}
			}
		} catch (Throwable ignored) {
		}
	}

	public static void installEarlyNoCutoutHook(ClassLoader classLoader, boolean enabled) {
		noCutoutEnabled = enabled;
		if (sNoCutoutHookInstalled) return;

		try {
			ReflectedClass WmDisplayCutoutClass = ReflectedClass.of("com.android.server.wm.utils.WmDisplayCutout", classLoader);
			ReflectedClass DisplayCutoutClass = ReflectedClass.of("android.view.DisplayCutout", classLoader);

			Object NO_CUTOUT = getStaticObjectField(DisplayCutoutClass.getClazz(), "NO_CUTOUT");

			WmDisplayCutoutClass
					.before("getDisplayCutout")
					.run(param -> {
						if (noCutoutEnabled) {
							param.setResult(NO_CUTOUT);
						}
			});

			sNoCutoutHookInstalled = true;
		} catch (Throwable t) {
			Logger.log("StatusbarSize: failed to install early display cutout hook", t);
		}
	}

	@Override
	public void onPackageLoaded(XposedModuleInterface.PackageReadyParam PRParam) throws Throwable {
		try {
			try {
				installEarlyNoCutoutHook(PRParam.getClassLoader(), noCutoutEnabled);

				ReflectedClass WmDisplayCutoutClass = ReflectedClass.of("com.android.server.wm.utils.WmDisplayCutout");
				WmDisplayCutoutClass
						.after("getDisplayCutout")
						.run(param -> {
							if (noCutoutEnabled) return;
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
						});
			} catch (Throwable ignored) {
			}

			ReflectedClass.ReflectionConsumer resizedResultConsumer = param -> {
				try {
					if (!shouldApplyHeight()) return;
					edited = true;
					param.setResult(currentHeight);
				} catch (Throwable ignored) {
				}
			};

			try {
				ReflectedClass SystemBarUtilsClass = ReflectedClass.of("com.android.internal.policy.SystemBarUtils");

				SystemBarUtilsClass.before("getStatusBarHeight").run(resizedResultConsumer);
				SystemBarUtilsClass.before("getStatusBarHeightForRotation").run(resizedResultConsumer);
			} catch (Throwable ignored) {
			}

			try {
				ReflectedClass StatusBarWindowControllerImplClass =
						ReflectedClass.ofIfPossible("com.android.systemui.statusbar.window.StatusBarWindowControllerImpl");
				ReflectedClass.ReflectionConsumer cachedStatusBarHeightConsumer = param -> {
					try {
						applyCachedStatusBarHeight(param.thisObject);
					} catch (Throwable ignored) {
					}
				};

				StatusBarWindowControllerImplClass.afterConstruction().run(cachedStatusBarHeightConsumer);
				StatusBarWindowControllerImplClass.before("attach").run(cachedStatusBarHeightConsumer);
				StatusBarWindowControllerImplClass.before("applyHeight").run(cachedStatusBarHeightConsumer);
				StatusBarWindowControllerImplClass.before("refreshStatusBarHeight").run(cachedStatusBarHeightConsumer);
				StatusBarWindowControllerImplClass.before("getStatusBarHeight").run(resizedResultConsumer);
				StatusBarWindowControllerImplClass.after("attach").run(param -> syncStatusBarLayoutParams(param.thisObject, true));
				StatusBarWindowControllerImplClass.after("apply").run(param -> syncStatusBarLayoutParams(param.thisObject, true));
				StatusBarWindowControllerImplClass.after("applyHeight").run(param -> syncStatusBarLayoutParams(param.thisObject, false));
				StatusBarWindowControllerImplClass.after("refreshStatusBarHeight").run(param -> syncStatusBarLayoutParams(param.thisObject, true));
				StatusBarWindowControllerImplClass.after("getBarLayoutParams").run(param -> applyStatusBarLayoutParams(param.getResult()));
				StatusBarWindowControllerImplClass.after("getBarLayoutParamsForRotation").run(param -> applyStatusBarLayoutParams(param.getResult()));
			} catch (Throwable ignored) {
			}
		} catch (Throwable ignored) {
		}
	}
}
