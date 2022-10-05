package sh.siava.AOSPMods.launcher;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.getStaticObjectField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.app.TaskInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.os.UserHandle;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Array;
import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.rangesliderpreference.RangeSliderPreference;

@SuppressWarnings("RedundantThrows")
public class TaskbarActivator extends XposedModPack {
	private static final String listenPackage = AOSPMods.LAUNCHER_PACKAGE;

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

	public TaskbarActivator(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		String taskbarModeStr = Xprefs.getString("taskBarMode", "0");

		TaskbarAsRecents = Xprefs.getBoolean("TaskbarAsRecents", false);

		if (Key.length > 0) {
			switch (Key[0]) {
				case "taskBarMode":
					try {
						int newtaskbarMode = Integer.parseInt(taskbarModeStr);
						if (newtaskbarMode != taskbarMode) {
							taskbarMode = newtaskbarMode;
							android.os.Process.killProcess(android.os.Process.myPid());
						}
					} catch (Exception ignored) {
					}
					break;
				case "TaskbarAsRecents":
				case "taskbarHeightOverride":
				case "TaskbarRadiusOverride":
					android.os.Process.killProcess(android.os.Process.myPid());
					break;
			}
		} else {
			try
			{
				TaskbarRadiusOverride = RangeSliderPreference.getValues(Xprefs, "TaskbarRadiusOverride", 1f).get(0);
			}catch (Throwable ignored){}

			try {
				taskbarHeightOverride = RangeSliderPreference.getValues(Xprefs, "taskbarHeightOverride", 100f).get(0) / 100f;
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

		Class<?> info = findClass("com.android.launcher3.util.DisplayController$Info", lpparam.classLoader);
		Class<?> RecentTasksListClass = findClass("com.android.quickstep.RecentTasksList", lpparam.classLoader);
		Class<?> AppInfoClass = findClass("com.android.launcher3.model.data.AppInfo", lpparam.classLoader);
		Class<?> TaskbarViewClass = findClass("com.android.launcher3.taskbar.TaskbarView", lpparam.classLoader);
		Class<?> ItemInfoClass = findClass("com.android.launcher3.model.data.ItemInfo", lpparam.classLoader);
		Class<?> TaskbarModelCallbacksClass = findClass("com.android.launcher3.taskbar.TaskbarModelCallbacks", lpparam.classLoader);
		Class<?> DeviceProfileClass = findClass("com.android.launcher3.DeviceProfile", lpparam.classLoader);
		Class<?> ActivityManagerWrapperClass = findClass("com.android.systemui.shared.system.ActivityManagerWrapper", lpparam.classLoader);
		Class<?> TaskbarActivityContextClass = findClass("com.android.launcher3.taskbar.TaskbarActivityContext", lpparam.classLoader);

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

		hookAllConstructors(DeviceProfileClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				numShownHotseatIcons = getIntField(param.thisObject, "numShownHotseatIcons");

				if (taskbarHeightOverride != 1f) {
					setObjectField(param.thisObject, "taskbarSize", Math.round(getIntField(param.thisObject, "taskbarSize") * taskbarHeightOverride));
				}
			}
		});

		hookAllConstructors(TaskbarViewClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				TaskBarView = (ViewGroup) param.thisObject;
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
						Thread.sleep(1000);
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
									getBooleanField(
											getObjectField(r, "mTaskInfo1"),
											"isFocused")
							);

							if (recentTaskList.size() > numShownHotseatIcons)
								recentTaskList.remove(recentTaskList.size() - 1);

							Object[] itemInfos = (Object[]) Array.newInstance(
									ItemInfoClass,
									Math.min(numShownHotseatIcons, recentTaskList.size()));

							for (int i = 0; i < itemInfos.length; i++) {
								TaskInfo taskInfo = (TaskInfo) getObjectField(recentTaskList.get(i), "mTaskInfo1");

								itemInfos[i] = AppInfoClass.getConstructor(ComponentName.class, CharSequence.class, UserHandle.class, Intent.class)
										.newInstance(
												(ComponentName) getObjectField(taskInfo, "realActivity"),
												"",
												UserHandle.getUserHandleForUid(getIntField(taskInfo, "userId")),
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

		//region taskbar activator
		hookAllMethods(info, "isTablet", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				switch (taskbarMode) {
					case TASKBAR_OFF:
						param.setResult(false);
						break;
					case TASKBAR_ON:
						param.setResult(true);
						break;
					case TASKBAR_DEFAULT:
						break;
				}
			}
		});
		//endregion
	}
}