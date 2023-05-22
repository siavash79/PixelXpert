package sh.siava.AOSPMods.modpacks.systemui;

import static android.view.MotionEvent.ACTION_DOWN;
import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getFloatField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.XPLauncher;
import sh.siava.AOSPMods.modpacks.XPrefs;
import sh.siava.AOSPMods.modpacks.XposedModPack;
import sh.siava.AOSPMods.modpacks.launcher.TaskbarActivator;
import sh.siava.AOSPMods.modpacks.utils.Helpers;

@SuppressWarnings("RedundantThrows")
public class GestureNavbarManager extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	//region Back gesture
	private static float backGestureHeightFractionLeft = 1f; // % of screen height. can be anything between 0 to 1
	private static float backGestureHeightFractionRight = 1f; // % of screen height. can be anything between 0 to 1
	private static boolean leftEnabled = true;
	private static boolean rightEnabled = true;
	float initialBackX = 0;

	Object EdgeBackGestureHandler;
	//endregion

	//region pill size
	private static int GesPillHeightFactor = 100;
	private static float widthFactor = 1f;

	private Object mNavigationBarInflaterView = null;
	//endregion

	//region pill color
	private boolean colorReplaced = false;
	private static boolean navPillColorAccent = false;
	private static final int mLightColor = Color.parseColor("#EBffffff"), mDarkColor = Color.parseColor("#99000000"); //original navbar colors
	//endregion

	public GestureNavbarManager(Context context) {
		super(context);
	}

	public void updatePrefs(String... Key) {
		if (XPrefs.Xprefs == null) return;

		//region Back gesture
		leftEnabled = XPrefs.Xprefs.getBoolean("BackFromLeft", true);
		rightEnabled = XPrefs.Xprefs.getBoolean("BackFromRight", true);
		backGestureHeightFractionLeft = XPrefs.Xprefs.getInt("BackLeftHeight", 100) / 100f;
		backGestureHeightFractionRight = XPrefs.Xprefs.getInt("BackRightHeight", 100) / 100f;
		//endregion

		//region pill size
		widthFactor = XPrefs.Xprefs.getInt("GesPillWidthModPos", 50) * .02f;
		GesPillHeightFactor = XPrefs.Xprefs.getInt("GesPillHeightFactor", 100);

		int taskbarMode = TaskbarActivator.TASKBAR_DEFAULT;
		String taskbarModeStr = XPrefs.Xprefs.getString("taskBarMode", "0");
		try {
			taskbarMode = Integer.parseInt(taskbarModeStr);
		} catch (Exception ignored) {
		}

		if (taskbarMode == TaskbarActivator.TASKBAR_ON) {
			widthFactor = 0f;
		}

		if (Key.length > 0) {
			switch (Key[0]) {
				case "GesPillWidthModPos":
				case "GesPillHeightFactor":
					refreshNavbar();
					break;
			}
		}
		//endregion

		//region pill color
		navPillColorAccent = XPrefs.Xprefs.getBoolean("navPillColorAccent", false);
		//endregion
	}

	//region pill size
	private void refreshNavbar() {
		try {
			callMethod(mNavigationBarInflaterView, "clearViews");
			Object defaultLayout = callMethod(mNavigationBarInflaterView, "getDefaultLayout");
			callMethod(mNavigationBarInflaterView, "inflateLayout", defaultLayout);
		} catch (Throwable ignored) {
		}
	}
	//endregion

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(listenPackage)) return;

		Class<?> NavigationBarInflaterViewClass = findClass("com.android.systemui.navigationbar.NavigationBarInflaterView", lpparam.classLoader);
		Class<?> NavigationHandleClass = findClass("com.android.systemui.navigationbar.gestural.NavigationHandle", lpparam.classLoader);
		Class<?> EdgeBackGestureHandlerClass = findClassIfExists("com.android.systemui.navigationbar.gestural.EdgeBackGestureHandler", lpparam.classLoader);
		Class<?> NavigationBarEdgePanelClass = findClassIfExists("com.android.systemui.navigationbar.gestural.NavigationBarEdgePanel", lpparam.classLoader);
		Class<?> BackPanelControllerClass = findClass("com.android.systemui.navigationbar.gestural.BackPanelController", lpparam.classLoader);

		//region back gesture
		//A14
		hookAllConstructors(EdgeBackGestureHandlerClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				EdgeBackGestureHandler = param.thisObject;
			}
		});

		hookAllMethods(BackPanelControllerClass, "onMotionEvent", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				MotionEvent ev = (MotionEvent) param.args[0];

				if(ev.getActionMasked() == ACTION_DOWN) //down action is enough. once gesture is refused it won't accept further actions
				{
					if(notWithinInsets(ev.getX(),
							ev.getY(),
							(Point) getObjectField(EdgeBackGestureHandler, "mDisplaySize"),
							getFloatField(EdgeBackGestureHandler, "mBottomGestureHeight")))
					{
						setObjectField(EdgeBackGestureHandler, "mAllowGesture", false); //act like the gesture was not good enough
						param.setResult(null); //and stop the current method too
					}
				}
			}
		});

		//Android 13
		Helpers.tryHookAllMethods(NavigationBarEdgePanelClass, "onMotionEvent", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				MotionEvent event = (MotionEvent) param.args[0];
				if(event.getAction() == ACTION_DOWN)
				{
					initialBackX = event.getX();
				}
				if (notWithinInsets(initialBackX, event.getY(), (Point) getObjectField(param.thisObject, "mDisplaySize"), 0)) {
					//event.setAction(MotionEvent.ACTION_CANCEL);
					param.setResult(null);
				}
			}
		});
		//endregion

		//region pill color
		hookAllMethods(NavigationHandleClass, "setDarkIntensity", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (navPillColorAccent || colorReplaced) {
					setObjectField(param.thisObject, "mLightColor", (navPillColorAccent) ? mContext.getResources().getColor(android.R.color.system_accent1_200, mContext.getTheme()) : mLightColor);
					setObjectField(param.thisObject, "mDarkColor", (navPillColorAccent) ? mContext.getResources().getColor(android.R.color.system_accent1_600, mContext.getTheme()) : mDarkColor);
					colorReplaced = true;
				}
			}
		});
		//endregion

		//region pill size
		hookAllMethods(NavigationHandleClass,
				"setVertical", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						if (widthFactor != 1f) {
							View result = (View) param.thisObject;
							ViewGroup.LayoutParams resultLayoutParams = result.getLayoutParams();
							int originalWidth;
							try {
								originalWidth = (int) getAdditionalInstanceField(param.thisObject, "originalWidth");
							} catch (Throwable ignored) {
								originalWidth = resultLayoutParams.width;
								setAdditionalInstanceField(param.thisObject, "originalWidth", originalWidth);
							}

							resultLayoutParams.width = Math.round(originalWidth * widthFactor);
						}
					}
				});

		hookAllMethods(NavigationHandleClass,
				"onDraw", new XC_MethodHook() {
					int mRadius = 0;

					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						if (GesPillHeightFactor != 100) {
							mRadius = Math.round((float) getObjectField(param.thisObject, "mRadius"));

							setObjectField(param.thisObject, "mRadius", Math.round(mRadius * GesPillHeightFactor / 100f));
						}
					}

					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						if (mRadius > 0) {
							setObjectField(param.thisObject, "mRadius", Math.round(mRadius));
						}
					}
				});


		hookAllConstructors(NavigationBarInflaterViewClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				mNavigationBarInflaterView = param.thisObject;
				refreshNavbar();
			}
		});

		hookAllMethods(NavigationBarInflaterViewClass,
				"createView", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						if (widthFactor != 1f) {
							String button = (String) callMethod(param.thisObject, "extractButton", param.args[0]);
							if (!button.equals("home_handle")) return;

							View result = (View) param.getResult();
							ViewGroup.LayoutParams resultLayoutParams = result.getLayoutParams();
							resultLayoutParams.width = Math.round(resultLayoutParams.width * widthFactor);
							result.setLayoutParams(resultLayoutParams);
						}
					}
				});
		//endregion

	}

	//region Back gesture
	private boolean notWithinInsets(float x, float y, Point mDisplaySize, float mBottomGestureHeight) {
		boolean isLeftSide = x < (mDisplaySize.x / 3f);
		if ((isLeftSide && !leftEnabled)
				|| (!isLeftSide && !rightEnabled)) {
			return true;
		}

		int mEdgeHeight = isLeftSide ?
				Math.round(mDisplaySize.y * backGestureHeightFractionLeft) :
				Math.round(mDisplaySize.y * backGestureHeightFractionRight);

		return mEdgeHeight != 0
				&& y < (mDisplaySize.y
				- mBottomGestureHeight
				- mEdgeHeight);
	}
	//endregion

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}
}