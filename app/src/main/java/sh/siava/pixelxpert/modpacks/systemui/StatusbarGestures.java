package sh.siava.pixelxpert.modpacks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findFieldIfExists;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.pixelxpert.modpacks.utils.SystemUtils;
import sh.siava.rangesliderpreference.RangeSliderPreference;

@SuppressWarnings("RedundantThrows")
public class StatusbarGestures extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	private static final int PULLDOWN_SIDE_RIGHT = 1;
	@SuppressWarnings("unused")
	private static final int PULLDOWN_SIDE_LEFT = 2;
	private static final int STATUSBAR_MODE_SHADE = 0;
	private static final int STATUSBAR_MODE_KEYGUARD = 1;
	/** @noinspection unused*/
	private static final int STATUSBAR_MODE_SHADE_LOCKED = 2;

	private static int pullDownSide = PULLDOWN_SIDE_RIGHT;
	private static boolean oneFingerPulldownEnabled = false;
	private boolean oneFingerPullupEnabled = false;
	private static float statusbarPortion = 0.25f; // now set to 25% of the screen. it can be anything between 0 to 100%
	private Object NotificationPanelViewController;
	GestureDetector mGestureDetector;
	private boolean StatusbarLongpressAppSwitch = false;

	public StatusbarGestures(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		if (Xprefs == null) return;
		oneFingerPulldownEnabled = Xprefs.getBoolean("QSPullodwnEnabled", false);
		oneFingerPullupEnabled = oneFingerPulldownEnabled && Xprefs.getBoolean("oneFingerPullupEnabled", false);
		statusbarPortion = RangeSliderPreference.getSingleIntValue(Xprefs, "QSPulldownPercent", 25) / 100f;
		pullDownSide = Integer.parseInt(Xprefs.getString("QSPulldownSide", "1"));

		StatusbarLongpressAppSwitch = Xprefs.getBoolean("StatusbarLongpressAppSwitch", false);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(listenPackage)) return;

		Class<?> NotificationPanelViewControllerClass = findClass("com.android.systemui.shade.NotificationPanelViewController", lpparam.classLoader);

		if(findFieldIfExists(NotificationPanelViewControllerClass, "mStatusBarViewTouchEventHandler") != null) { //13 QPR1
			hookAllConstructors(NotificationPanelViewControllerClass, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					Object mStatusBarViewTouchEventHandler = getObjectField(param.thisObject, "mStatusBarViewTouchEventHandler");

					mGestureDetector = new GestureDetector(mContext, new LongpressListener(true));

					hookAllMethods(mStatusBarViewTouchEventHandler.getClass(), "handleTouchEvent", new XC_MethodHook() {
						@Override
						protected void beforeHookedMethod(MethodHookParam param1) throws Throwable {
							MotionEvent event = (MotionEvent) param1.args[0];

							mGestureDetector.onTouchEvent(event);

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
			mGestureDetector = new GestureDetector(mContext, new LongpressListener(true));

			if(hookAllMethods(NotificationPanelViewControllerClass, "createTouchHandler", new XC_MethodHook() { //13 QPR2
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					hookAllMethods(param.getResult().getClass(), "onTouch", new XC_MethodHook() {
						@Override
						protected void beforeHookedMethod(MethodHookParam param1) throws Throwable {
							MotionEvent event = (MotionEvent) param1.args[1];

							mGestureDetector.onTouchEvent(event);

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
			}).size() == 0)
			{ //13 QPR3 - 14
				Class<?> PhoneStatusBarViewControllerClass = findClass("com.android.systemui.statusbar.phone.PhoneStatusBarViewController", lpparam.classLoader);
				hookAllConstructors(NotificationPanelViewControllerClass, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						NotificationPanelViewController = param.thisObject;
						Object mTouchHandler = getObjectField(param.thisObject, "mTouchHandler");
						GestureDetector pullUpDetector = new GestureDetector(mContext, getPullUpListener());
						hookAllMethods(mTouchHandler.getClass(), "onTouchEvent", new XC_MethodHook() {
							@Override
							protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
								if(oneFingerPullupEnabled
										&& STATUSBAR_MODE_KEYGUARD != (int) getObjectField(NotificationPanelViewController, "mBarState")) {
									pullUpDetector.onTouchEvent((MotionEvent) param.args[0]);
								}
							}
						});
					}
				});

				String QSExpandMethodName = Arrays.stream(NotificationPanelViewControllerClass.getMethods())
						.anyMatch(m -> m.getName().equals("expandToQs"))
						? "expandToQs" //A14
						: "expandWithQs"; //A13

				mGestureDetector = new GestureDetector(mContext, getPullDownLPListener(QSExpandMethodName));

				hookAllMethods(PhoneStatusBarViewControllerClass, "onTouch", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						if (!oneFingerPulldownEnabled) return;

						MotionEvent event =
								param.args[0] instanceof MotionEvent
										? (MotionEvent) param.args[0]
										: (MotionEvent) param.args[1];

						mGestureDetector.onTouchEvent(event);

					}
				});
			}
		}
	}

	//speedfactor & heightfactor are based on display height
	private boolean isValidFling(MotionEvent e1, MotionEvent e2, float velocityY, float speedFactor, float heightFactor) {
		//noinspection DataFlowIssue
		Rect displayBounds = SystemUtils.WindowManager().getCurrentWindowMetrics().getBounds();

		return (e2.getY() - e1.getY()) / heightFactor > displayBounds.height() //enough travel in right direction
				&& isTouchInRegion(e1, displayBounds.width()) //start point in hot zone
				&& (velocityY / speedFactor > displayBounds.height()); //enough speed in right direction
	}

	private boolean isTouchInRegion(MotionEvent motionEvent, float width) {
		float x = motionEvent.getX();
		float region = width * statusbarPortion;

		return (pullDownSide == PULLDOWN_SIDE_RIGHT)
				? width - region < x
				: x < region;
	}

	private void onStatusbarLongpress() {
		if(StatusbarLongpressAppSwitch)
		{
			sendAppSwitchBroadcast();
		}
	}

	private void sendAppSwitchBroadcast() {
		new Thread(() -> mContext.sendBroadcast(new Intent()
				.setAction(Constants.ACTION_SWITCH_APP_PROFILE)
				.addFlags(Intent.FLAG_RECEIVER_FOREGROUND))).start();
	}

	private GestureDetector.OnGestureListener getPullDownLPListener(String QSExpandMethodName) {
		return new LongpressListener(true)
		{
			@Override
			public boolean onFling(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
				if (STATUSBAR_MODE_SHADE == (int) getObjectField(NotificationPanelViewController, "mBarState")
						&& isValidFling(e1, e2, velocityY, .15f, 0.01f))
				{
					callMethod(NotificationPanelViewController, QSExpandMethodName);
					return true;
				}
				return false;
			}
		};
	}

	private GestureDetector.OnGestureListener getPullUpListener() {
		return new LongpressListener(false)
		{
			@Override
			public boolean onFling(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
				//noinspection DataFlowIssue
				if(isValidFling(e1, e2, velocityY, -.15f, -.06f))
				{
					callMethod(NotificationPanelViewController, "collapse", 1f, true);
					return true;
				}
				return false;
			}
		};
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}

	private class LongpressListener implements GestureDetector.OnGestureListener {
		boolean mDetectLongpress;
		public LongpressListener(boolean detectLongpress)
		{
			mDetectLongpress = detectLongpress;
		}
		@Override
		public boolean onDown(@NonNull MotionEvent e) {
			return false;
		}

		@Override
		public void onShowPress(@NonNull MotionEvent e) {}

		@Override
		public boolean onSingleTapUp(@NonNull MotionEvent e) {
			return false;
		}

		@Override
		public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
			return false;
		}

		@Override
		public void onLongPress(@NonNull MotionEvent e) {
			if(mDetectLongpress)
				onStatusbarLongpress();
		}

		@Override
		public boolean onFling(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
			return false;
		}
	}
}