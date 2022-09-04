package sh.siava.AOSPMods.launcher;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.Toast;

import com.topjohnwu.superuser.Shell;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.rangesliderpreference.RangeSliderPreference;

@SuppressWarnings("RedundantThrows")
public class CustomNavGestures extends XposedModPack {
	private static final String listenPackage = AOSPMods.LAUNCHER_PACKAGE;

	private static final int NO_ACTION = -1;
	private static final int ACTION_SCREENSHOT = 1;
	private static final int ACTION_BACK = 2;
	private static final int ACTION_KILL_APP = 3;
	private static final int ACTION_NOTIFICATION = 4;
	private static final int ACTION_ONE_HANDED = 5;

	private boolean twoFingerActive = false;
	private boolean FCHandled = false;
	private float leftSwipeUpPercentage = 0.25f, rightSwipeUpPercentage = 0.25f;
	private int displayW = -1 , displayH = -1;
	private static float swipeUpPercentage = 0.2f;

	private boolean isLandscape = false;
	private float mSwipeUpThreshold = 0;
	private float mLongThreshold = 0;
	private static boolean FCLongSwipeEnabled = false;
	private boolean isLeftSwipe = false, isRightSwipe = false;
	private Object mSystemUIProxy = null;
	private static int leftSwipeUpAction = NO_ACTION, rightSwipeUpAction = NO_ACTION, twoFingerSwipeUpAction = NO_ACTION;
	private boolean isOverViewListener = false;

	public CustomNavGestures(Context context) { super(context); }
	
	@Override
	public void updatePrefs(String... Key) {
		FCLongSwipeEnabled = Xprefs.getBoolean("FCLongSwipeEnabled", false);
		leftSwipeUpAction = readAction(Xprefs, "leftSwipeUpAction");
		rightSwipeUpAction = readAction(Xprefs, "rightSwipeUpAction");
		twoFingerSwipeUpAction = readAction(Xprefs, "twoFingerSwipeUpAction");
		leftSwipeUpPercentage = readRangeSlider(Xprefs, "leftSwipeUpPercentage", 25f)/100f;
		rightSwipeUpPercentage = readRangeSlider(Xprefs, "rightSwipeUpPercentage", 25f)/100f;
		swipeUpPercentage = readRangeSlider(Xprefs, "swipeUpPercentage", 20f)/100f;
	}

	private float readRangeSlider(SharedPreferences xprefs, String prefName, @SuppressWarnings("SameParameterValue") float defaultVal) {
		try
		{
			return RangeSliderPreference.getValues(xprefs, prefName, 25f).get(0);
		}
		catch (Exception ignored)
		{
			return defaultVal;
		}
	}

	private static int readAction(SharedPreferences xprefs, String prefName) {
		try
		{
			return Integer.parseInt(xprefs.getString(prefName, "").trim());
		}
		catch (Exception ignored)
		{
			return NO_ACTION;
		}
	}

	@Override
	public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
	
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(listenPackage)) return;

		Class<?> OtherActivityInputConsumerClass = findClass("com.android.quickstep.inputconsumers.OtherActivityInputConsumer", lpparam.classLoader); //When apps are open
		Class<?> OverviewInputConsumerClass = findClass("com.android.quickstep.inputconsumers.OverviewInputConsumer", lpparam.classLoader); //When on Home screen and Recents
		Class<?> SystemUiProxyClass = findClass("com.android.quickstep.SystemUiProxy", lpparam.classLoader);

		WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		Rect displayBounds = wm.getMaximumWindowMetrics().getBounds();
		displayW = Math.min(displayBounds.width(), displayBounds.height());
		displayH = Math.max(displayBounds.width(), displayBounds.height());

		hookAllConstructors(SystemUiProxyClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				mSystemUIProxy = param.thisObject;
			}
		});

		XC_MethodHook navGestureHook = new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				MotionEvent e = (MotionEvent) param.args[0];

				isOverViewListener = param.thisObject.getClass().getName().endsWith("OverviewInputConsumer");

				boolean mPassedWindowMoveSlop = isOverViewListener //if it's overview page (read: home page) we don't need this. true is good
						|| getBooleanField(param.thisObject, "mPassedWindowMoveSlop"); //checking if they've swiped long enough to cancel touch for app

				int action = e.getActionMasked();
				int pointers = e.getPointerCount();

				if(action == MotionEvent.ACTION_DOWN ) //Let's get ready
				{
/*					isLeftSwipe = false;
					isRightSwipe = false;
					twoFingerActive = false;*/

					if(isOverViewListener && getBooleanField(param.thisObject, "mStartingInActivityBounds"))
					{
						return;
					}

					mSwipeUpThreshold = e.getY() * (1f - swipeUpPercentage);
					mLongThreshold = e.getY() / 10f;

					isLandscape = e.getY() < displayW; //launcher rotation can be always 0. So....
					int currentW = isLandscape ? displayH : displayW;

					if (pointers == 1) {
						if (leftSwipeUpAction != NO_ACTION && e.getX() < currentW * leftSwipeUpPercentage) {
							isLeftSwipe = true;
						} else if (rightSwipeUpAction != NO_ACTION && e.getX() > currentW * (1f - rightSwipeUpPercentage)) {
							isRightSwipe = true;
						}
					}
				}

				if (twoFingerSwipeUpAction != NO_ACTION && pointers == 2 && !twoFingerActive) { //must be outside down. usually down is one finger
					twoFingerActive = true;
					isLeftSwipe = isRightSwipe = false;
				}

				if (mPassedWindowMoveSlop && (isLeftSwipe || isRightSwipe || twoFingerActive)) { //shouldn't reach the main code anymore
					param.setResult(null);
				}

				if(pointers == 1)
				{
					boolean FCAllowed = !(isLeftSwipe || isRightSwipe);

					if(FCAllowed && FCLongSwipeEnabled && e.getY() < mLongThreshold) { //swiped up too much
						if(!FCHandled) {
							setObjectField(param.thisObject, "mActivePointerId", MotionEvent.INVALID_POINTER_ID);
							setObjectField(param.thisObject, "mPassedWindowMoveSlop", false);
							FCHandled = true;
							runAction(ACTION_KILL_APP);
						}
					}
				}

				if(action == MotionEvent.ACTION_UP && (isLeftSwipe || isRightSwipe || twoFingerActive))
				{
					if(twoFingerActive || isLeftSwipe || isRightSwipe)
					{
						if(!isOverViewListener) {
							callMethod(param.thisObject, "forceCancelGesture", e);
						}

						if(e.getY() < mSwipeUpThreshold)
						{
							if(isRightSwipe)
							{
								runAction(rightSwipeUpAction);
							}
							else if (isLeftSwipe)
							{
								runAction(leftSwipeUpAction);
							}
							else if (twoFingerActive)
							{
								runAction(twoFingerSwipeUpAction);
							}
						}
					}
					isLeftSwipe = false;
					isRightSwipe = false;
					twoFingerActive = false;
					FCHandled = false;
				}
			}
		};
		hookAllMethods(OtherActivityInputConsumerClass, "onMotionEvent", navGestureHook);
		hookAllMethods(OverviewInputConsumerClass, "onMotionEvent", navGestureHook);
	}

	private void runAction(int action) {
		switch (action)
		{
			case ACTION_BACK:
				goBack();
				break;
			case ACTION_SCREENSHOT:
				takeScreenshot();
				break;
			case ACTION_NOTIFICATION:
				toggleNotification();
				break;
			case ACTION_KILL_APP:
				killForeground();
				break;
			case ACTION_ONE_HANDED:
				startOneHandedMode();
				break;
		}
	}

	private void killForeground() {
		Toast.makeText(mContext, "App Killed", Toast.LENGTH_SHORT).show();
		Shell.cmd("am force-stop $(dumpsys window | grep mCurrentFocus | cut -d \"/\" -f1 | cut -d \" \" -f5)").submit();
	}

	private void goBack()
	{
		log("going back");
		callMethod(mSystemUIProxy, "onBackPressed");
		log("gone back");
	}

	private void startOneHandedMode()
	{
		callMethod(getObjectField(mSystemUIProxy, "mOneHanded"), "startOneHanded");
	}

	private void toggleNotification()
	{
		callMethod(mSystemUIProxy, "toggleNotificationPanel");
	}

	private void takeScreenshot()
	{
		Intent broadcast = new Intent();
		broadcast.setAction(AOSPMods.ACTION_SCREENSHOT);
		mContext.sendBroadcast(broadcast);
	}
}