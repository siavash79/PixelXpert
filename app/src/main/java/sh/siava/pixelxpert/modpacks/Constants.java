package sh.siava.pixelxpert.modpacks;

import java.util.Arrays;
import java.util.List;

import sh.siava.pixelxpert.BuildConfig;
import sh.siava.pixelxpert.modpacks.utils.BootLoopProtector;

public final class Constants {
	public static final String ACTION_SCREENSHOT = BuildConfig.APPLICATION_ID + ".ACTION_SCREENSHOT";
	@SuppressWarnings("DeprecatedIsStillUsed")
	@Deprecated
	public static final String ACTION_INSECURE_SCREENSHOT = BuildConfig.APPLICATION_ID + ".ACTION_INSECURE_SCREENSHOT";
	public static final String ACTION_BACK = BuildConfig.APPLICATION_ID + ".ACTION_BACK";
	public static final String ACTION_SLEEP = BuildConfig.APPLICATION_ID + ".ACTION_SLEEP";
	public static final String ACTION_SWITCH_APP_PROFILE = BuildConfig.APPLICATION_ID + ".ACTION_SWITCH_APP_PROFILE";
	public static final String ACTION_PROFILE_SWITCH_AVAILABLE = BuildConfig.APPLICATION_ID + ".ACTION_PROFILE_SWITCH_AVAILABLE";
	public static final String ACTION_CHECK_XPOSED_ENABLED = BuildConfig.APPLICATION_ID + ".ACTION_CHECK_XPOSED_ENABLED";
	public static final String ACTION_XPOSED_CONFIRMED = BuildConfig.APPLICATION_ID + ".ACTION_XPOSED_CONFIRMED";

	public static final String SYSTEM_UI_PACKAGE = "com.android.systemui";
	public static final String SYSTEM_FRAMEWORK_PACKAGE = "android";
	public static final String TELECOM_SERVER_PACKAGE = "com.android.server.telecom";
	public static final String LAUNCHER_PACKAGE = "com.google.android.apps.nexuslauncher";
	public static final String SETTINGS_PACKAGE = "com.android.settings";
	public static final String DIALER_PACKAGE = "com.google.android.dialer";

	public static final List<String> PREF_UPDATE_EXCLUSIONS = Arrays.asList(BootLoopProtector.LOAD_TIME_KEY_KEY, BootLoopProtector.PACKAGE_STRIKE_KEY_KEY);
}
