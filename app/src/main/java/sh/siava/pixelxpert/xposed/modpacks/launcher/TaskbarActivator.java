package sh.siava.pixelxpert.xposed.modpacks.launcher;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.xposed.XPrefs.Xprefs;
import static sh.siava.pixelxpert.xposed.utils.SystemUtils.idOf;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.UserHandle;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.BuildConfig;
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

	private Object model;

	private boolean ThreeButtonLayoutMod;
	private String ThreeButtonLeft, ThreeButtonCenter, ThreeButtonRight;

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
		ReflectedClass LauncherModelClass = ReflectedClass.of("com.android.launcher3.LauncherModel");
		ReflectedClass BaseActivityClass = ReflectedClass.of("com.android.launcher3.BaseActivity");
		ReflectedClass DisplayControllerClass = ReflectedClass.of("com.android.launcher3.util.DisplayController");
		ReflectedClass DisplayControllerInfoClass = ReflectedClass.of("com.android.launcher3.util.DisplayController$Info");
		ReflectedClass StateControllerClass = ReflectedClass.of("com.android.launcher3.taskbar.TaskbarLauncherStateController");
		ReflectedClass AbstractNavButtonLayoutterClass = ReflectedClass.of("com.android.launcher3.taskbar.navbutton.AbstractNavButtonLayoutter");
		ReflectedClass RecentAppsControllerClass = ReflectedClass.of("com.android.launcher3.taskbar.TaskbarRecentAppsController");
		ReflectedClass QuickSwitchStateClass = ReflectedClass.of("com.android.launcher3.uioverrides.states.QuickSwitchState");
		ReflectedClass TaskbarUiControllerClass = ReflectedClass.of("com.android.launcher3.taskbar.FallbackTaskbarUIController");
		ReflectedClass TaskbarProfileClass = ReflectedClass.of("com.android.launcher3.deviceprofile.TaskbarProfile");
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

		LauncherModelClass
				.afterConstruction()
				.run(param -> model = param.thisObject);

		BaseActivityClass
				.after("onResume")
				.run(param -> {
					if (taskbarMode == TASKBAR_ON && model != null) {
						XposedHelpers.callMethod(model, "onAppIconChanged", BuildConfig.APPLICATION_ID, UserHandle.getUserHandleForUid(0));
					}
				});

		//hide on home screen
		StateControllerClass
				.before("isInLauncher")
				.run(param -> {
					if (TaskbarOnLauncher) {
						param.setResult(false);
					}
				});


		QuickSwitchStateClass
				.before("isTaskbarStashed")
				.run(param -> {
					if (TaskbarOnLauncher) {
						param.setResult(false);
					}
				});
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

		// Show taskbar even with keyboard displayed
		TaskbarStashControllerClass.after("shouldStashForIme").run (param -> {
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

						//we control the list ourselves to remove all suggestions
						List<?> newShownTasks = allRecentTasks.subList(Math.max(0, allRecentTasks.size() - numShownHotseatIcons - 1), Math.max(allRecentTasks.size(), 0));

						List<?> oldShownTasks = (List<?>) getObjectField(param.thisObject, "shownTasks");

						if(newShownTasks.equals(oldShownTasks))
						{
							param.setResult(false);
							return;
						}

						setObjectField(param.thisObject, "shownTasks", newShownTasks);
						callMethod(param.thisObject, "fetchIcons");
						param.setResult(true);
					}
				});
	}
}
