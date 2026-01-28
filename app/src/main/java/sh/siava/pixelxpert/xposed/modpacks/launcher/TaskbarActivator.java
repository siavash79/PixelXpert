package sh.siava.pixelxpert.xposed.modpacks.launcher;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.getStaticObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.xposed.XPrefs.Xprefs;
import static sh.siava.pixelxpert.xposed.utils.SystemUtils.idOf;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.xposed.XposedModPack;
import sh.siava.pixelxpert.xposed.annotations.LauncherModPack;
import sh.siava.pixelxpert.xposed.utils.SystemUtils;
import sh.siava.pixelxpert.xposed.utils.toolkit.ReflectedClass;
import sh.siava.pixelxpert.xposed.utils.toolkit.ReflectedClass.ReflectionConsumer;

@LauncherModPack
public class TaskbarActivator extends XposedModPack {
	public static final int TASKBAR_DEFAULT = 0;
	public static final int TASKBAR_ON = 1;
	/**
	 * @noinspection unused
	 */
	public static final int TASKBAR_OFF = 2;

	private static int taskbarMode = 0;
	private static int numShownHotseatIcons = 0;
	private static boolean TaskbarAsRecents = false;
	private static boolean TaskbarOnLauncher = false;
	private static boolean TaskbarOnIme = false;
	private static boolean GoogleRecents = false;
	private static boolean TaskbarShowAllRecents = false;
	private static float taskbarHeightOverride = 1f;
	private static float TaskbarRadiusOverride = 1f;

	private boolean ThreeButtonLayoutMod;
	private String ThreeButtonLeft, ThreeButtonCenter, ThreeButtonRight;
	private ReflectedClass TopTaskTrackerClass;
	private Object mCurrentTopTask;

	public TaskbarActivator(Context context) {
		super(context);
	}

	@Override
	public void onPreferenceUpdated(String... Key) {

		//3button nav has moved to taskbar since 15QPR2
		ThreeButtonLayoutMod = Xprefs.getBoolean("ThreeButtonLayoutMod", false);
		ThreeButtonLeft = Xprefs.getString("ThreeButtonLeft", "back").replace("recent", "recent_apps");
		ThreeButtonCenter = Xprefs.getString("ThreeButtonCenter", "home").replace("recent", "recent_apps");
		ThreeButtonRight = Xprefs.getString("ThreeButtonRight", "recent").replace("recent", "recent_apps");

		boolean noToggle = false;
		try
		{
			//noinspection ResultOfMethodCallIgnored
			Set.of(ThreeButtonLeft, ThreeButtonCenter, ThreeButtonRight);
		}
		catch (Throwable ignored){
			noToggle = true;
			ThreeButtonLayoutMod = false;
		}

		List<String> darkToggleKeys = Arrays.asList(
				"ThreeButtonLayoutMod",
				"ThreeButtonLeft",
				"ThreeButtonCenter",
				"ThreeButtonRight");

		if (Key.length > 0 && !noToggle && darkToggleKeys.contains(Key[0])) {
			SystemUtils.doubleToggleDarkMode();
		}


		List<String> restartKeys = Arrays.asList(
				"taskBarMode",
				"TaskbarAsRecents",
				"taskbarHeightOverride",
				"TaskbarRadiusOverride",
				"TaskbarHideAllAppsIcon",
				"EnableGoogleRecents");

		if (Key.length > 0 && restartKeys.contains(Key[0])) {
			SystemUtils.killSelf();
		}

		taskbarMode = Integer.parseInt(Xprefs.getString("taskBarMode", String.valueOf(TASKBAR_DEFAULT)));

		TaskbarAsRecents = Xprefs.getBoolean("TaskbarAsRecents", false);

		TaskbarRadiusOverride = Xprefs.getSliderFloat("TaskbarRadiusOverride", 1f);

		taskbarHeightOverride = Xprefs.getSliderFloat("taskbarHeightOverride", 100f) / 100f;

		taskbarMode = Integer.parseInt(Xprefs.getString("taskBarMode", "0"));

		TaskbarOnLauncher = Xprefs.getBoolean("TaskbarOnLauncher", false);

		TaskbarOnIme = Xprefs.getBoolean("TaskbarOnIme", false);

		GoogleRecents = Xprefs.getBoolean("EnableGoogleRecents", false);

		TaskbarShowAllRecents = Xprefs.getBoolean("ShowAllRecentIcons", false);
	}


	@SuppressLint("DiscouragedApi")
	@Override
	public void onPackageLoaded(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		ReflectedClass DeviceProfileBuilderClass = ReflectedClass.of("com.android.launcher3.DeviceProfile$Builder");
		ReflectedClass TaskbarActivityContextClass = ReflectedClass.of("com.android.launcher3.taskbar.TaskbarActivityContext");
//		ReflectedClass LauncherModelClass = ReflectedClass.of("com.android.launcher3.LauncherModel");
//		ReflectedClass LauncherModelFactoryClass = ReflectedClass.of("com.android.launcher3.LauncherModel_Factory");
//		ReflectedClass BaseActivityClass = ReflectedClass.of("com.android.launcher3.BaseActivity");
		ReflectedClass DisplayControllerClass = ReflectedClass.of("com.android.launcher3.util.DisplayController");
		ReflectedClass DisplayControllerInfoClass = ReflectedClass.of("com.android.launcher3.util.DisplayController$Info");
		ReflectedClass StateControllerClass = ReflectedClass.of("com.android.launcher3.taskbar.TaskbarLauncherStateController");
		ReflectedClass AbstractNavButtonLayoutterClass = ReflectedClass.of("com.android.launcher3.taskbar.navbutton.AbstractNavButtonLayoutter");
		ReflectedClass RecentAppsControllerClass = ReflectedClass.of("com.android.launcher3.taskbar.TaskbarRecentAppsController");
		ReflectedClass QuickSwitchStateClass = ReflectedClass.of("com.android.launcher3.uioverrides.states.QuickSwitchState");
		ReflectedClass TaskbarUiControllerClass = ReflectedClass.of("com.android.launcher3.taskbar.FallbackTaskbarUIController");
		ReflectedClass TaskbarProfileClass = ReflectedClass.of("com.android.launcher3.deviceprofile.TaskbarProfile");
		ReflectedClass TaskbarOverlayDragLayerClass = ReflectedClass.of("com.android.launcher3.taskbar.overlay.TaskbarOverlayDragLayer");
		ReflectedClass KeyboardQuickSwitchControllerClass = ReflectedClass.of("com.android.launcher3.taskbar.KeyboardQuickSwitchController");
		ReflectedClass TaskbarViewClass = ReflectedClass.of("com.android.launcher3.taskbar.TaskbarView");
		TopTaskTrackerClass = ReflectedClass.of("com.android.quickstep.TopTaskTracker");

		ReflectedClass TaskbarStashControllerClass = ReflectedClass.of("com.android.launcher3.taskbar.TaskbarStashController");

		//3 button nav order on A15+
		AbstractNavButtonLayoutterClass
				.afterConstruction()
				.run(param -> {
					if(!ThreeButtonLayoutMod) return;

					ViewGroup navButtonContainer = (ViewGroup) getObjectField(param.thisObject, "navButtonContainer");

					setObjectField(param.thisObject, "backButton", navButtonContainer.findViewById(idOf(ThreeButtonLeft)));
					setObjectField(param.thisObject, "homeButton", navButtonContainer.findViewById(idOf( ThreeButtonCenter)));
					setObjectField(param.thisObject, "recentsButton", navButtonContainer.findViewById(idOf( ThreeButtonRight)));
				});

		//enable taskbar
		DisplayControllerInfoClass
				.before("isTablet")
				.run(param -> {
					if (taskbarMode == TASKBAR_DEFAULT) return;

					param.setResult(taskbarMode == TASKBAR_ON);
				});

		//auto hide
		DisplayControllerClass
				.before("isTransientTaskbar")
				.run(param -> {
					if (taskbarMode == TASKBAR_ON)
						param.setResult(true);
				});

		//workaround of taskbar recents overflow fails to capture touch events for no known reason
		TaskbarOverlayDragLayerClass
				.findFirstInstance(instance ->
				{
					if(!TaskbarAsRecents) return;

					ReflectedClass.of(
									getObjectField(instance, "mTaskbarInsetsComputer").getClass())
							.before("onComputeInternalInsets")
							.run(param -> {
								onComputeInternalInsets(instance, param.args[0]);
								param.setResult(null);
							});
				});

		//workaround of taskbar recents overflow falsely showing "no recent items"
		KeyboardQuickSwitchControllerClass
				.before("processLoadedTasks")
				.run(param -> {
							 if(TaskbarAsRecents)
						        param.args[0] = false;
				});

		//temp workaround of launcher crash bug on split tasks
		TaskbarViewClass
				.before("updateRecents")
				.run(param -> {
					@SuppressWarnings("unchecked")
					List<Object> recents = (List<Object>) param.args[1];
					param.args[1] = recents.stream().filter(t -> !t.getClass().getName().contains("Split")).toList();
				});

		KeyboardQuickSwitchControllerClass
				.before("openQuickSwitchView")
				.run(param -> {
					@SuppressWarnings("unchecked")
					HashSet<Object> exclusionList = (HashSet<Object>) param.args[1];
					if(notInHomeScreen())
					{
						exclusionList.add(callMethod(mCurrentTopTask, "getTaskId"));
					}
				});

		//show on home screen
		StateControllerClass
				.before("isInLauncher")
				.run(param -> {
					if (TaskbarOnLauncher) {
						param.setResult(false);
					}
				});

		//show on home screen
		QuickSwitchStateClass
				.before("isTaskbarStashed")
				.run(param -> {
					if (TaskbarOnLauncher) {
						param.setResult(false);
					}
				});

		//show on home screen
		TaskbarUiControllerClass
				.before("isIn3pHomeOrRecents")
				.run(param -> {
					if (TaskbarOnLauncher) {
						param.setResult(false);
					}
				});

		//region taskbar corner radius
		ReflectionConsumer cornerRadiusConsumer = param -> {
			if (taskbarMode == TASKBAR_ON && TaskbarRadiusOverride != 1f) {
				param.setResult(
						Math.round((int) param.getResult() * TaskbarRadiusOverride));
			}
		};

		TaskbarActivityContextClass.after("getLeftCornerRadius").run(cornerRadiusConsumer);
		TaskbarActivityContextClass.after("getRightCornerRadius").run(cornerRadiusConsumer);
		//endregion

		//region recentbar
		DeviceProfileBuilderClass
				.after("build")
				.run(param -> {
					if (taskbarMode == TASKBAR_DEFAULT) return;

					boolean taskbarEnabled = taskbarMode == TASKBAR_ON;

					if (taskbarEnabled) {
						numShownHotseatIcons = getIntField(param.getResult(), "numShownHotseatIcons");
					}
				});

		TaskbarProfileClass
				.after("getHeight")
				.run(param -> {
					if(taskbarMode == TASKBAR_ON && taskbarHeightOverride != 1f)
					{
						param.setResult(Math.round((int)param.getResult() * taskbarHeightOverride));
					}
				});

		RecentAppsControllerClass
				.afterConstruction()
				.run(param -> {
					if (GoogleRecents || (taskbarMode == TASKBAR_ON && TaskbarAsRecents)) { //on 16+ we use the builtin recent tasks
						//noinspection OptionalGetWithoutIsPresent
						RecentAppsControllerClass.findMethods(
								Pattern.compile("setCanShowRecentApps")).stream().findFirst().get()
								.invoke(param.thisObject, true);
					}
				});

		// TaskbarShowAllRecents Pre-16QPR2
		RecentAppsControllerClass
				.before("computeShownRecentTasks")
				.run(param -> {
					if (TaskbarShowAllRecents) {
						Object dedupedTasks = callMethod(param.thisObject,
								"dedupeHotseatTasks",
								getObjectField(param.thisObject, "allRecentTasks"),
								getObjectField(param.thisObject, "shownHotseatItems")
						);

						param.setResult(dedupedTasks);
					}
				});

		// TaskbarShowAllRecents 16QPR2
		RecentAppsControllerClass.after("onRecentsOrHotseatChanged").run(param -> {
			if (TaskbarShowAllRecents) {
				ArrayList allRecentTasks = (ArrayList) getObjectField(param.thisObject, "allRecentTasks");
				setObjectField(param.thisObject, "shownTasks", allRecentTasks.subList(0, allRecentTasks.size() - 1));
				callMethod(param.thisObject, "fetchIcons");
			}
		});


		// Show taskbar even with keyboard displayed
		// Pre 16 QPR2
		TaskbarStashControllerClass.after("shouldStashForIme").run (param -> {
			if (TaskbarOnIme) {
				param.setResult(false);
			}
		});

		// 16 QPR2
		TaskbarActivityContextClass.before("isImeDocked").run(param -> {
			if (TaskbarOnIme) {
				param.setResult(false);
			}
		});


		RecentAppsControllerClass
				.before("onRecentsOrHotseatChanged")
				.run(param -> {
					if(taskbarMode == TASKBAR_ON && TaskbarAsRecents) {
						List<?> allRecentTasks = (List<?>) getObjectField(param.thisObject, "allRecentTasks");

						if (allRecentTasks.size() < 2) //there's nothing to show as recent
						{
							return;
						}

						List<?> shownHotseatItems = (List<?>) getObjectField(param.thisObject, "shownHotseatItems");
						if (!shownHotseatItems.isEmpty()) {
							shownHotseatItems.clear();
						}

						mCurrentTopTask = getCurrentTopTask();

						List<?> newShownTasks = allRecentTasks.subList(Math.max(0, allRecentTasks.size() - numShownHotseatIcons - 1), Math.max(allRecentTasks.size(), 0));

						if(notInHomeScreen()) //hiding running task from taskbar
						{
							newShownTasks = newShownTasks.subList(0, Math.max(0, newShownTasks.size() - 1));
						}

						List<?> oldShownTasks = (List<?>) getObjectField(param.thisObject, "shownTasks");

						if(newShownTasks.equals(oldShownTasks))
						{
							param.setResult(false);
							return;
						}

						setObjectField(param.thisObject, "shownTasks", newShownTasks);

						try { //16qpr3
							callMethod(param.thisObject, "fetchIcons", false);
						}
						catch (Throwable ignored) { //16qpr2
							callMethod(param.thisObject, "fetchIcons");
						}

						setObjectField(param.thisObject, "needsRecentsTasksReload", true);
						callMethod(param.thisObject, "access$reloadRecentTasksIfNeeded", param.thisObject);

						param.setResult(true);
					}
				});
	}

	public Object getCurrentTopTask()
	{
		Object INSTANCE = getStaticObjectField(TopTaskTrackerClass.getClazz(), "INSTANCE");
		Object topTaskTracker = callMethod(INSTANCE, "get", mContext);
		return callMethod(topTaskTracker, "getCachedTopTask", true, mContext.getDisplay().getDisplayId());
	}

	public boolean notInHomeScreen()
	{
		return !((boolean) callMethod(mCurrentTopTask, "isHomeTask"));
	}
	public void onComputeInternalInsets(Object thisObject, Object internalInsetsInfo) {
		Object mOverlayController = getObjectField(getObjectField(thisObject, "mContainer"), "mOverlayController");
		Object mOverlayContext = getObjectField(mOverlayController, "mOverlayContext");
		Object mDragController = getObjectField(mOverlayContext, "mDragController");
		Object mTaskbarContext = getObjectField(mOverlayController, "mTaskbarContext");
		Object mControllers = getObjectField(mTaskbarContext, "mControllers");
		Object taskbarDragController = getObjectField(mControllers, "taskbarDragController");
		if ((mOverlayContext == null || !getBooleanField(mDragController, "mIsSystemDragInProgress")) && !getBooleanField(taskbarDragController, "mIsSystemDragInProgress")) {
			return;
		}
		callMethod(getIntField(internalInsetsInfo,"touchableRegion"), "setEmpty");
		callMethod(internalInsetsInfo, "setTouchableInsets", 3); //from android.view.ViewTreeObserver
	}
}
