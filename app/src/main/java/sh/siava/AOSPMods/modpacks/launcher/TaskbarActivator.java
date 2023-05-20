package sh.siava.AOSPMods.modpacks.launcher;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findFieldIfExists;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.getStaticObjectField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import android.annotation.SuppressLint;
import android.app.TaskInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Process;
import android.os.UserHandle;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Array;
import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.BuildConfig;
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.XPrefs;
import sh.siava.AOSPMods.modpacks.XposedModPack;
import sh.siava.AOSPMods.modpacks.utils.SystemUtils;
import sh.siava.rangesliderpreference.RangeSliderPreference;

@SuppressWarnings("RedundantThrows")
public class TaskbarActivator extends XposedModPack {
	private static final String listenPackage = Constants.LAUNCHER_PACKAGE;

	public static final int TASKBAR_DEFAULT = 0;
	public static final int TASKBAR_ON = 1;
	public static final int TASKBAR_OFF = 2;

	private static int taskbarMode = 0;
	private ViewGroup TaskBarView = null;
	private static int numShownHotseatIcons = 0;
	private int UID = 0;
	private Object recentTasksList;
	private boolean TaskbarAsRecents = false;
	private boolean refreshing = false;
	private static float taskbarHeightOverride = 1f;
	private float TaskbarRadiusOverride = 1f;

	private static boolean TaskbarHideAllAppsIcon = false;
	private Object model;

	public TaskbarActivator(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		String taskbarModeStr = XPrefs.Xprefs.getString("taskBarMode", "0");

		if (Key.length > 0) {
			switch (Key[0]) {
				case "taskBarMode":
					try {
						int newtaskbarMode = Integer.parseInt(taskbarModeStr);
						if (newtaskbarMode != taskbarMode) {
							taskbarMode = newtaskbarMode;
							SystemUtils.killSelf();
						}
					} catch (Exception ignored) {
					}
					break;
				case "TaskbarAsRecents":
				case "taskbarHeightOverride":
				case "TaskbarRadiusOverride":
				case "TaskbarHideAllAppsIcon":
					SystemUtils.killSelf();
					break;
			}
		} else {
			TaskbarAsRecents = XPrefs.Xprefs.getBoolean("TaskbarAsRecents", false);
			TaskbarHideAllAppsIcon = true;//Xprefs.getBoolean("TaskbarHideAllAppsIcon", false);

			try
			{
				TaskbarRadiusOverride = RangeSliderPreference.getValues(XPrefs.Xprefs, "TaskbarRadiusOverride", 1f).get(0);
			}catch (Throwable ignored){}

			try {
				taskbarHeightOverride = RangeSliderPreference.getValues(XPrefs.Xprefs, "taskbarHeightOverride", 100f).get(0) / 100f;
			} catch (Throwable ignored) {
			}

			taskbarMode = Integer.parseInt(taskbarModeStr);
		}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

//		Class<?> info = findClass("com.android.launcher3.util.DisplayController$Info", lpparam.classLoader);
		Class<?> RecentTasksListClass = findClass("com.android.quickstep.RecentTasksList", lpparam.classLoader);
		Class<?> AppInfoClass = findClass("com.android.launcher3.model.data.AppInfo", lpparam.classLoader);
		Class<?> TaskbarViewClass = findClass("com.android.launcher3.taskbar.TaskbarView", lpparam.classLoader);
		Class<?> ItemInfoClass = findClass("com.android.launcher3.model.data.ItemInfo", lpparam.classLoader);
		Class<?> TaskbarModelCallbacksClass = findClass("com.android.launcher3.taskbar.TaskbarModelCallbacks", lpparam.classLoader);
		Class<?> DeviceProfileClass = findClass("com.android.launcher3.DeviceProfile", lpparam.classLoader);
		Class<?> ActivityManagerWrapperClass = findClass("com.android.systemui.shared.system.ActivityManagerWrapper", lpparam.classLoader);
		Class<?> TaskbarActivityContextClass = findClass("com.android.launcher3.taskbar.TaskbarActivityContext", lpparam.classLoader);
		Class<?> LauncherModelClass = findClass("com.android.launcher3.LauncherModel", lpparam.classLoader);
		Class<?> BaseDraggingActivityClass = findClass("com.android.launcher3.BaseDraggingActivity", lpparam.classLoader);
		//Transient taskbar. kept disabled until further notice
		Class<?> DisplayControllerClass = findClass("com.android.launcher3.util.DisplayController", lpparam.classLoader);

		hookAllMethods(DisplayControllerClass, "isTransientTaskbar", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				param.setResult(false);
			}
		});

		hookAllMethods(BaseDraggingActivityClass, "onResume", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(model != null) {
					XposedHelpers.callMethod(model, "onAppIconChanged", BuildConfig.APPLICATION_ID, UserHandle.getUserHandleForUid(0));
				}
			}
		});
		hookAllConstructors(LauncherModelClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				model = param.thisObject;
			}
		});

		//region taskbar corner radius
		XC_MethodHook cornerRadiusHook = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(TaskbarRadiusOverride != 1f) {
					param.setResult(
							Math.round(
									(int) param.getResult() * TaskbarRadiusOverride
							));
				}
			}
		};

		hookAllMethods(TaskbarActivityContextClass, "getLeftCornerRadius", cornerRadiusHook);
		hookAllMethods(TaskbarActivityContextClass, "getRightCornerRadius", cornerRadiusHook);
		//endregion

		//region recentbar
		UID = (int) callMethod(Process.myUserHandle(), "getIdentifier");

		View.OnClickListener listener = view -> {
			try {
				int id = (int) getAdditionalInstanceField(view.getTag(), "taskId");
				callMethod(
						getStaticObjectField(ActivityManagerWrapperClass, "sInstance"),
						"startActivityFromRecents",
						id,
						null);
			} catch (Throwable ignored) {
			}
		};

		String taskbarHeightField = findFieldIfExists(DeviceProfileClass, "taskbarSize") != null
				? "taskbarSize" //pre 13 QPR3
				: "taskbarHeight"; //13 QPR3

		String stashedTaskbarHeightField = findFieldIfExists(DeviceProfileClass, "stashedTaskbarSize") != null
				? "stashedTaskbarSize" //pre 13 QPR3
				: "stashedTaskbarHeight"; //13 QPR3

		hookAllConstructors(DeviceProfileClass, new XC_MethodHook() {
			@SuppressLint("DiscouragedApi")
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(taskbarMode == TASKBAR_DEFAULT) return;

				boolean taskbarEnabled = taskbarMode == TASKBAR_ON;

				setObjectField(param.thisObject, "isTaskbarPresent", taskbarEnabled);

				if(taskbarEnabled)
				{
					numShownHotseatIcons = getIntField(param.thisObject, "numShownHotseatIcons") +
							(TaskbarHideAllAppsIcon
									? 1
									: 0);

					Resources res = mContext.getResources();

					setObjectField(param.thisObject, taskbarHeightField, res.getDimensionPixelSize(res.getIdentifier("taskbar_size", "dimen", mContext.getPackageName())));
					setObjectField(param.thisObject, stashedTaskbarHeightField, res.getDimensionPixelSize(res.getIdentifier("taskbar_stashed_size", "dimen", mContext.getPackageName())));

					if (taskbarHeightOverride != 1f) {
						setObjectField(param.thisObject, taskbarHeightField, Math.round(getIntField(param.thisObject, taskbarHeightField) * taskbarHeightOverride));
					}
				}
			}
		});

		hookAllConstructors(TaskbarViewClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				TaskBarView = (ViewGroup) param.thisObject;

				if(TaskbarHideAllAppsIcon)
					setObjectField(TaskBarView, "mAllAppsButton", null);
			}
		});

		hookAllConstructors(RecentTasksListClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				recentTasksList = param.thisObject;
			}
		});

		hookAllMethods(RecentTasksListClass, "onRecentTasksChanged", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (!TaskbarAsRecents
						|| refreshing
						|| TaskBarView == null)
					return;
				new Thread(() -> {
					refreshing = true;
					try {
						Thread.sleep(100);
					} catch (Throwable ignored) {
					}

					TaskBarView.post(() -> {
						try {
							Object mSysUiProxy = getObjectField(param.thisObject, "mSysUiProxy");

							ArrayList<?> recentTaskList = (ArrayList<?>) callMethod(
									mSysUiProxy,
									"getRecentTasks",
									numShownHotseatIcons + 1,
									UID);

							recentTaskList.removeIf(r ->
									(boolean) getObjectField(
											((Object[]) getObjectField(r, "mTasks"))[0],
											"isFocused"
									)
							);

							if (recentTaskList.size() > numShownHotseatIcons)
								recentTaskList.remove(recentTaskList.size() - 1);

							Object[] itemInfos = (Object[]) Array.newInstance(
									ItemInfoClass,
									Math.min(numShownHotseatIcons, recentTaskList.size()));

							for (int i = 0; i < itemInfos.length; i++) {
								TaskInfo taskInfo = (TaskInfo) ((Object[]) getObjectField(recentTaskList.get(i), "mTasks"))[0];

								// noinspection JavaReflectionMemberAccess
								itemInfos[i] = AppInfoClass.getConstructor(ComponentName.class, CharSequence.class, UserHandle.class, Intent.class)
										.newInstance(
												(ComponentName) getObjectField(taskInfo, "realActivity"),
												"",
												UserHandle.class.getConstructor(int.class).newInstance(getIntField(taskInfo, "userId")),
												(Intent) getObjectField(taskInfo, "baseIntent"));

								setAdditionalInstanceField(itemInfos[i], "taskId", taskInfo.taskId);
							}

							callMethod(TaskBarView, "updateHotseatItems", new Object[]{itemInfos});

							for (int i = 0; i < itemInfos.length; i++) {
								View iconView = TaskBarView.getChildAt(i);

								try {
									if (getAdditionalInstanceField(iconView, "taskId")
											.equals(getAdditionalInstanceField(itemInfos[itemInfos.length - i - 1], "taskId")))
										continue;
								} catch (Throwable ignored) {
								}

								setAdditionalInstanceField(iconView, "taskId", getAdditionalInstanceField(itemInfos[itemInfos.length - i - 1], "taskId"));
								callMethod(iconView, "applyFromApplicationInfo", itemInfos[itemInfos.length - i - 1]);
								iconView.setOnClickListener(listener);
							}
						} catch (Throwable ignored) {
						}
					});
					refreshing = false;
				}).start();
			}
		});

		hookAllMethods(TaskbarModelCallbacksClass, "commitItemsToUI", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (!TaskbarAsRecents) return;

				if (TaskBarView.getChildCount() == 0 && recentTasksList != null) {
					callMethod(recentTasksList, "onRecentTasksChanged");
				}
				param.setResult(null);
			}
		});
		//endregion
	}

}