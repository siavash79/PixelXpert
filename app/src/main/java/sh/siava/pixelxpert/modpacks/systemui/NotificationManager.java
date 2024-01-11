package sh.siava.pixelxpert.modpacks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;
import android.service.notification.StatusBarNotification;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.rangesliderpreference.RangeSliderPreference;

@SuppressWarnings("RedundantThrows")
public class NotificationManager extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	private Object HeadsUpManager = null;

	private static int HeadupAutoDismissNotificationDecay = -1;
	private boolean DisableOngoingNotifDismiss = false;

	public NotificationManager(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		try {
			HeadupAutoDismissNotificationDecay = RangeSliderPreference.getValues(Xprefs, "HeadupAutoDismissNotificationDecay", -1).get(0).intValue();
			DisableOngoingNotifDismiss = Xprefs.getBoolean("DisableOngoingNotifDismiss", false);
			applyDurations();
		} catch (Throwable ignored) {}
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		XC_MethodHook headsupFinder = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				HeadsUpManager = param.thisObject;

				applyDurations();
			}
		};

		Class<?> HeadsUpManagerClass = findClass("com.android.systemui.statusbar.policy.HeadsUpManager", lpparam.classLoader);
		hookAllConstructors(HeadsUpManagerClass, headsupFinder); //interface in 14QPR2, class in older

		try //A14 QPR2
		{
			Class<?> BaseHeadsUpManagerClass = findClass("com.android.systemui.statusbar.policy.BaseHeadsUpManager", lpparam.classLoader);
			hookAllConstructors(BaseHeadsUpManagerClass, headsupFinder);
		}
		catch (Throwable ignored){}


		hookAllMethods(StatusBarNotification.class, "isNonDismissable", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(DisableOngoingNotifDismiss) {
					param.setResult((boolean) param.getResult() || ((StatusBarNotification) param.thisObject).isOngoing());
				}
			}
		});
	}

	private void applyDurations() {
		if(HeadsUpManager != null && HeadupAutoDismissNotificationDecay > 0)
		{
			setObjectField(HeadsUpManager, "mMinimumDisplayTime", Math.round(HeadupAutoDismissNotificationDecay/2.5f));

			try //A14 QPR2B3
			{
				setObjectField(HeadsUpManager, "mAutoDismissTime", HeadupAutoDismissNotificationDecay);
			}
			catch (Throwable ignored) //Older
			{
				setObjectField(HeadsUpManager, "mAutoDismissNotificationDecay", HeadupAutoDismissNotificationDecay);
			}
		}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}
}