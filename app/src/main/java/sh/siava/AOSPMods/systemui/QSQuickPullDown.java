package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findFieldIfExists;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.content.Context;
import android.view.MotionEvent;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class QSQuickPullDown extends XposedModPack {
	private static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;

	private static final int PULLDOWN_SIDE_RIGHT = 1;
	@SuppressWarnings("unused")
	private static final int PULLDOWN_SIDE_LEFT = 2;
	private static final int STATUSBAR_MODE_SHADE = 0;

	private static int pullDownSide = PULLDOWN_SIDE_RIGHT;
	private static boolean oneFingerPulldownEnabled = false;
	private static float statusbarPortion = 0.25f; // now set to 25% of the screen. it can be anything between 0 to 100%

	public QSQuickPullDown(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		if (Xprefs == null) return;
		oneFingerPulldownEnabled = Xprefs.getBoolean("QSPullodwnEnabled", false);
		statusbarPortion = Xprefs.getInt("QSPulldownPercent", 25) / 100f;
		pullDownSide = Integer.parseInt(Xprefs.getString("QSPulldownSide", "1"));
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(listenPackage)) return;

		Class<?> NotificationPanelViewControllerClass = findClass("com.android.systemui.shade.NotificationPanelViewController", lpparam.classLoader);

		if(findFieldIfExists(NotificationPanelViewControllerClass, "mStatusBarViewTouchEventHandler") != null) {
			hookAllConstructors(NotificationPanelViewControllerClass, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					Object mStatusBarViewTouchEventHandler = getObjectField(param.thisObject, "mStatusBarViewTouchEventHandler");

					hookAllMethods(mStatusBarViewTouchEventHandler.getClass(), "handleTouchEvent", new XC_MethodHook() {
						@Override
						protected void beforeHookedMethod(MethodHookParam param1) throws Throwable {
							MotionEvent event = (MotionEvent) param1.args[0];
							if (!oneFingerPulldownEnabled) return;

							int mBarState = (int) getObjectField(param.thisObject, "mBarState");
							if (mBarState != STATUSBAR_MODE_SHADE) return;

							int w = (int) callMethod(
									getObjectField(param.thisObject, "mView"),
									"getMeasuredWidth");

							float x = event.getX();
							float region = w * statusbarPortion;

							boolean pullDownApproved = (pullDownSide == PULLDOWN_SIDE_RIGHT)
									? w - region < x
									: x < region;

							if (pullDownApproved) {
								callMethod(param.thisObject, "expandWithQs");
							}
						}
					});
				}
			});
		}
		else
		{
			hookAllMethods(NotificationPanelViewControllerClass, "createTouchHandler", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					hookAllMethods(param.getResult().getClass(), "onTouch", new XC_MethodHook() {
						@Override
						protected void beforeHookedMethod(MethodHookParam param1) throws Throwable {
							MotionEvent event = (MotionEvent) param1.args[1];
							if (!oneFingerPulldownEnabled) return;

							if (!(boolean) getObjectField(param.thisObject, "mPulsing")
									&& !(boolean) getObjectField(param.thisObject, "mDozing")
									&& (int) getObjectField(param.thisObject, "mBarState") == STATUSBAR_MODE_SHADE
									&& (boolean) callMethod(param.thisObject, "isFullyCollapsed")) {
								int w = (int) callMethod(
										getObjectField(param.thisObject, "mView"),
										"getMeasuredWidth");

								float x = event.getX();
								float region = w * statusbarPortion;

								boolean pullDownApproved = (pullDownSide == PULLDOWN_SIDE_RIGHT)
										? w - region < x
										: x < region;

								if (pullDownApproved) {
									callMethod(param.thisObject, "expandWithQs");
								}
							}
						}
					});
				}
			});
		}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}
}