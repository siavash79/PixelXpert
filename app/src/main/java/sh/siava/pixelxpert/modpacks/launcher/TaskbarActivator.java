package sh.siava.pixelxpert.modpacks.launcher;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedBridge.invokeOriginalMethod;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findFieldIfExists;
import static de.robv.android.xposed.XposedHelpers.findMethodExact;
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.BuildConfig;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XPrefs;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.pixelxpert.modpacks.utils.SystemUtils;
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
	private static boolean TaskbarAsRecents = false;
	private static boolean TaskbarTransient = false;
	private boolean refreshing = false;
	private static float taskbarHeightOverride = 1f;
	private static float TaskbarRadiusOverride = 1f;

	private static boolean TaskbarHideAllAppsIcon = false;
	private Object model;
	String mTasksFieldName = null; // in case the code was obfuscated
	private Object TaskbarModelCallbacks;
	private int mItemsLength = 0;

	public TaskbarActivator(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {

		List<String> restartKeys = Arrays.asList(
				"taskBarMode",
				"TaskbarAsRecents",
				"TaskbarTransient",
				"taskbarHeightOverride",
				"TaskbarRadiusOverride",
				"TaskbarHideAllAppsIcon");

		if (Key.length > 0 && restartKeys.contains(Key[0])) {
			SystemUtils.killSelf();
		}

		taskbarMode = Integer.parseInt(XPrefs.Xprefs.getString("taskBarMode", "0"));

		TaskbarAsRecents = XPrefs.Xprefs.getBoolean("TaskbarAsRecents", false);
		TaskbarHideAllAppsIcon = true;//Xprefs.getBoolean("TaskbarHideAllAppsIcon", false);

		TaskbarRadiusOverride = RangeSliderPreference.getSingleFloatValue(XPrefs.Xprefs, "TaskbarRadiusOverride", 1f);

		taskbarHeightOverride = RangeSliderPreference.getSingleFloatValue(XPrefs.Xprefs, "taskbarHeightOverride", 100f) / 100f;

		taskbarMode = Integer.parseInt(XPrefs.Xprefs.getString("taskBarMode", "0"));

		TaskbarTransient = XPrefs.Xprefs.getBoolean("TaskbarTransient", false);

	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

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
		Class<?> DisplayControllerClass = findClass("com.android.launcher3.util.DisplayController", lpparam.classLoader);
		Class<?> DisplayControllerInfoClass = findClass("com.android.launcher3.util.DisplayController$Info", lpparam.classLoader);
		Method commitItemsToUIMethod =  findMethodExact(TaskbarModelCallbacksClass, "commitItemsToUI");

		hookAllMethods(DisplayControllerInfoClass, "isTablet", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if(taskbarMode == TASKBAR_DEFAULT) return;

				boolean taskbarEnabled = taskbarMode == TASKBAR_ON;

				if(taskbarEnabled) param.setResult(true);
			}
		});

		hookAllMethods(DisplayControllerClass, "isTransientTaskbar", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if(taskbarMode == TASKBAR_ON)
					param.setResult(TaskbarTransient);
			}
		});

		hookAllMethods(BaseDraggingActivityClass, "onResume", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(taskbarMode == TASKBAR_ON && model != null) {
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
				if(taskbarMode == TASKBAR_ON && TaskbarRadiusOverride != 1f) {
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

//				setObjectField(param.thisObject, "isTaskbarPresent", taskbarEnabled);

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

		hookAllMethods(TaskbarViewClass, "setClickAndLongClickListenersForIcon", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				//Icon must be launched from recents
				if(taskbarMode == TASKBAR_ON
						&& TaskbarAsRecents
						&& mItemsLength > 0)
					((View) param.args[0]).setOnClickListener(listener);
			}
		});
		hookAllConstructors(TaskbarViewClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				TaskBarView = (ViewGroup) param.thisObject;

				if(taskbarMode == TASKBAR_ON && TaskbarHideAllAppsIcon)
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
				if (taskbarMode != TASKBAR_ON
						|| !TaskbarAsRecents
						|| refreshing
						|| TaskBarView == null)
					return;
				new Thread(() -> {
					refreshing = true;
					try {
						Thread.sleep(100);
					} catch (Throwable ignored) {}

					TaskBarView.post(() -> {
						try {
							Object mSysUiProxy = getObjectField(param.thisObject, "mSysUiProxy");

							ArrayList<?> recentTaskList = (ArrayList<?>) callMethod(
									mSysUiProxy,
									"getRecentTasks",
									numShownHotseatIcons + 1,
									UID);

							if(mTasksFieldName == null)
							{
								for(Field f : recentTaskList.get(0).getClass().getDeclaredFields())
								{
									if(f.getType().getName().contains("RecentTaskInfo"))
									{
										mTasksFieldName = f.getName();
									}
								}
							}

							recentTaskList.removeIf(r ->
									(boolean) getObjectField(
											((Object[]) getObjectField(r, mTasksFieldName))[0],
											"isFocused"
									)
							);

							if (recentTaskList.size() > numShownHotseatIcons)
								recentTaskList.remove(recentTaskList.size() - 1);

							Object[] itemInfos = (Object[]) Array.newInstance(
									ItemInfoClass,
									Math.min(numShownHotseatIcons, recentTaskList.size()));

							int prevItemsLength = mItemsLength;
							mItemsLength = itemInfos.length;
							if(mItemsLength == 0)
							{
								invokeOriginalMethod(commitItemsToUIMethod, TaskbarModelCallbacks,null);
								return;
							}
							else if(prevItemsLength == 0 && mItemsLength == 1)
							{
								TaskBarView.removeAllViews(); //moving from suggested apps to recent apps. old ones are not valid anymore
							}

							for (int i = 0; i < itemInfos.length; i++) {
								TaskInfo taskInfo = (TaskInfo) ((Object[]) getObjectField(recentTaskList.get(i), mTasksFieldName))[0];

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
							}
						} catch (Throwable ignored) {}
					});
					refreshing = false;
				}).start();
			}
		});
		hookAllConstructors(TaskbarModelCallbacksClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				TaskbarModelCallbacks = param.thisObject;
			}
		});

		hookAllMethods(TaskbarModelCallbacksClass, "commitItemsToUI", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (taskbarMode != TASKBAR_ON || !TaskbarAsRecents) return;

				if (TaskBarView.getChildCount() == 0 && recentTasksList != null) {
					callMethod(recentTasksList, "onRecentTasksChanged");
				}
				param.setResult(null);
			}
		});
		//endregion
	}

}