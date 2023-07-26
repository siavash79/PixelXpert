package sh.siava.AOSPMods.modpacks;

import java.util.Arrays;
import java.util.List;

import sh.siava.AOSPMods.modpacks.utils.BootLoopProtector;

public final class Constants {
	public static final String ACTION_SCREENSHOT = "sh.siava.AOSPMods.ACTION_SCREENSHOT";
	@SuppressWarnings("DeprecatedIsStillUsed")
	@Deprecated
	public static final String ACTION_INSECURE_SCREENSHOT = "sh.siava.AOSPMods.ACTION_INSECURE_SCREENSHOT";
	public static final String ACTION_BACK = "sh.siava.AOSPMods.ACTION_BACK";
	public static final String ACTION_SLEEP = "sh.siava.AOSPMods.ACTION_SLEEP";

	public static final String SYSTEM_UI_PACKAGE = "com.android.systemui";
	public static final String SYSTEM_FRAMEWORK_PACKAGE = "android";
	public static final String TELECOM_SERVER_PACKAGE = "com.android.server.telecom";
	public static final String LAUNCHER_PACKAGE = "com.google.android.apps.nexuslauncher";
	public static final String SETTINGS_PACKAGE = "com.android.settings";
	public static final String DIALER_PACKAGE = "com.google.android.dialer";

	public static final List<String> PREF_UPDATE_EXCLUSIONS = Arrays.asList(BootLoopProtector.LOAD_TIME_KEY_KEY, BootLoopProtector.PACKAGE_STRIKE_KEY_KEY);
}
