package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.IXposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class DoubleTapSleepLS implements IXposedModPack {
	public static final String listenPackage = "com.android.systemui";
	public static boolean doubleTapToSleepEnabled = false;
	
	private Context mContext = null;
	
	public void updatePrefs(String...Key)
	{
		if(XPrefs.Xprefs == null) return;
		doubleTapToSleepEnabled = XPrefs.Xprefs.getBoolean("DoubleTapSleep", false);
	}
	
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
		if(!lpparam.packageName.equals(listenPackage)) return;
		
		Class NotificationPanelViewControllerClass = XposedHelpers.findClass("com.android.systemui.statusbar.phone.NotificationPanelViewController", lpparam.classLoader);
		
		GestureDetector mLockscreenDoubleTapToSleep = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onDoubleTap(MotionEvent e) {
				
				Object pm = mContext.getSystemService(Context.POWER_SERVICE);
				if (pm != null) {
					XposedHelpers.callMethod(pm, "goToSleep", SystemClock.uptimeMillis());
				}
				return true;
			}
		});
		
		XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.NotificationPanelViewController", lpparam.classLoader,
				"createTouchHandler", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						Object touchHandler = param.getResult();
						Object ThisNotificationPanel = param.thisObject;
						
						XposedHelpers.findAndHookMethod(touchHandler.getClass(),
								"onTouch", View.class, MotionEvent.class, new XC_MethodHook() {
									@Override
									protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
										if(!doubleTapToSleepEnabled) return;
										
										boolean mPulsing = (boolean) XposedHelpers.getObjectField(ThisNotificationPanel, "mPulsing");
										boolean mDozing = (boolean) XposedHelpers.getObjectField(ThisNotificationPanel, "mDozing");
										int mBarState = (int) XposedHelpers.getObjectField(ThisNotificationPanel, "mBarState");
										
										if (!mPulsing && !mDozing
												&& mBarState < 2) {
											mLockscreenDoubleTapToSleep.onTouchEvent((MotionEvent) param.args[1]);
										}
									}
								});
					}
				});
		
		
		XposedBridge.hookAllConstructors(NotificationPanelViewControllerClass,
				new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						Object mView = XposedHelpers.getObjectField(param.thisObject, "mView");
						mContext = (Context) XposedHelpers.callMethod(mView, "getContext");
					}
				});
		
	}
	
	@Override
	public String getListenPack() {
		return listenPackage;
	}
	
}
