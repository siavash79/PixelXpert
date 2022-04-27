package sh.siava.AOSPMods.launcher;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.IXposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class TaskbarActivator implements IXposedModPack {
	private static final String listenPackage = "com.google.android.apps.nexuslauncher";
	
	private static final int TASKBAR_DEFAULT = 0;
	private static final int TASKBAR_ON = 1;
	private static final int TASKBAR_OFF = 2;
	
	private static int taskbarMode = 0;
	@Override
	public void updatePrefs(String... Key) {
		String taskbarModeStr = XPrefs.Xprefs.getString("taskBarMode", "0");
		
		if(Key.length > 0) {
			try {
				int newtaskbarMode = Integer.parseInt(taskbarModeStr);
				if (newtaskbarMode != taskbarMode) {
					taskbarMode = newtaskbarMode;
					android.os.Process.killProcess(android.os.Process.myPid());
				}
			} catch (Exception ignored) {
			}
		}
		else
		{
			taskbarMode = Integer.parseInt(taskbarModeStr);
		}
	}
	
	@Override
	public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
	
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		
		Class<?> info = XposedHelpers.findClass("com.android.launcher3.util.DisplayController$Info", lpparam.classLoader);
		
		
		XposedBridge.hookAllMethods(info, "isTablet", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				switch (taskbarMode)
				{
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
	}
}
