package sh.siava.AOSPMods.systemui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.IXposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class KeyGuardPinScrambler implements IXposedModPack {
	public static final String listenPackage = "com.android.systemui";
	
	private static boolean shufflePinEnabled = false;
	
	@Override
	public void updatePrefs(String... Key) {
		shufflePinEnabled = XPrefs.Xprefs.getBoolean("shufflePinEnabled", false);
	}
	
	@Override
	public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
	
	List digits = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 0);
	
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if(!lpparam.packageName.equals(listenPackage)) return;
		
		Class<?> KeyguardPinBasedInputViewControllerClass = XposedHelpers.findClass("com.android.keyguard.NumPadKey", lpparam.classLoader);
		Class<?> KeyguardAbsKeyInputViewControllerClass = XposedHelpers.findClass("com.android.keyguard.KeyguardAbsKeyInputViewController", lpparam.classLoader);
		
		Collections.shuffle(digits);
		
		XposedBridge.hookAllMethods(KeyguardAbsKeyInputViewControllerClass, "verifyPasswordAndUnlock", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				Collections.shuffle(digits);
			}
		});
		
		XposedBridge.hookAllConstructors(KeyguardPinBasedInputViewControllerClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(!shufflePinEnabled) return;
				
				int mDigit = XposedHelpers.getIntField(param.thisObject, "mDigit");
				Object mDigitText = XposedHelpers.getObjectField(param.thisObject, "mDigitText");
				XposedHelpers.setObjectField(param.thisObject, "mDigit", digits.get(mDigit));
				XposedHelpers.callMethod(mDigitText, "setText", Integer.toString((int) digits.get(mDigit)));
			}
		});
	
	}
}
