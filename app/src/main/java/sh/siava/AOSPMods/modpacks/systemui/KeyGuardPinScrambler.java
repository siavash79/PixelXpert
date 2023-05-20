package sh.siava.AOSPMods.modpacks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import android.content.Context;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.XPLauncher;
import sh.siava.AOSPMods.modpacks.XPrefs;
import sh.siava.AOSPMods.modpacks.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class KeyGuardPinScrambler extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	private static boolean shufflePinEnabled = false;

	public KeyGuardPinScrambler(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		shufflePinEnabled = XPrefs.Xprefs.getBoolean("shufflePinEnabled", false);
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}

	List<Integer> digits = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 0);

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(listenPackage)) return;

		Class<?> KeyguardPinBasedInputViewClass = findClass("com.android.keyguard.KeyguardPinBasedInputView", lpparam.classLoader);

		XC_MethodHook pinShuffleHook = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (!shufflePinEnabled) return;

				Collections.shuffle(digits);

				Object[] mButtons = (Object[]) getObjectField(param.thisObject, "mButtons");

				for(Object button : mButtons)
				{
					int mDigit = getIntField(button, "mDigit");
					setObjectField(button, "mDigit", digits.get(mDigit));

					callMethod(
							getObjectField(button, "mDigitText"),
							"setText",
							Integer.toString(digits.get(mDigit)));
				}
			}
		};

		hookAllMethods(KeyguardPinBasedInputViewClass, "onFinishInflate", pinShuffleHook);
		hookAllMethods(KeyguardPinBasedInputViewClass, "resetPasswordText",  pinShuffleHook);
	}
}