package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class EasyUnlock extends XposedModPack {
	private static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;

	private int expectedPassLen = -1;
	private boolean easyUnlockEnabled = false;

	private int lastPassLen = 0;
	private static boolean WakeUpToSecurityInput = false;

	public EasyUnlock(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		easyUnlockEnabled = Xprefs.getBoolean("easyUnlockEnabled", false);
		expectedPassLen = Xprefs.getInt("expectedPassLen", -1);
		WakeUpToSecurityInput = Xprefs.getBoolean("WakeUpToSecurityInput", false);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(listenPackage)) return;

		Class<?> KeyguardAbsKeyInputViewControllerClass = findClass("com.android.keyguard.KeyguardAbsKeyInputViewController", lpparam.classLoader);
		Class<?> LockscreenCredentialClass = findClass("com.android.internal.widget.LockscreenCredential", lpparam.classLoader);
		Class<?> StatusBarKeyguardViewManagerClass = findClass("com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager", lpparam.classLoader);

		hookAllMethods(StatusBarKeyguardViewManagerClass, "onDozingChanged", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

				if(WakeUpToSecurityInput && param.args[0].equals(false) && (!(boolean)callMethod(getObjectField(param.thisObject, "mKeyguardStateController"), "canDismissLockScreen"))) //waking up
				{
					callMethod(getObjectField(param.thisObject, "mBouncer"), "show", true, true);
				}
			}
		});

		hookAllMethods(KeyguardAbsKeyInputViewControllerClass, "onUserInput", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (!easyUnlockEnabled) return;

				int passwordLen = (Build.VERSION.SDK_INT == 33)
						? (int) callMethod(getObjectField(getObjectField(param.thisObject, "mPasswordEntry"), "mText"), "length") //A13
						: (int) callMethod(callMethod(getObjectField(param.thisObject, "mPasswordEntry"), "getText"), "length"); //A12

				if (passwordLen == expectedPassLen && passwordLen > lastPassLen) {
					new Thread(() -> {
						int userId = (int) callMethod(getObjectField(param.thisObject, "mKeyguardUpdateMonitor"), "getCurrentUser");

						String methodName = param.thisObject.getClass().getName().contains("Password") ? "createPassword" : "createPin";

						Object password = (Build.VERSION.SDK_INT == 33)
								? callStaticMethod(LockscreenCredentialClass, methodName, getObjectField(getObjectField(param.thisObject, "mPasswordEntry"), "mText").toString()) //A13
								: callStaticMethod(LockscreenCredentialClass, methodName, callMethod(getObjectField(param.thisObject, "mPasswordEntry"), "getText").toString()); //A12

						boolean accepted = (boolean) callMethod(
								getObjectField(param.thisObject, "mLockPatternUtils"),
								"checkCredential",
								password,
								userId,
								null /* callback */);

						if (accepted) {
							View mView = (View) getObjectField(param.thisObject, "mView");
							mView.post(() -> {
								if (Build.VERSION.SDK_INT == 33) { //A13
									try
									{ //New(er) signature
										callMethod(callMethod(param.thisObject, "getKeyguardSecurityCallback"), "dismiss", userId, true /* sucessful */, getObjectField(param.thisObject, "mSecurityMode"));
									}
									catch (Throwable ignored)
									{ //Previous signature
										callMethod(callMethod(param.thisObject, "getKeyguardSecurityCallback"), "dismiss", userId, true /* sucessful */);
									}
								} else { //A12
									callMethod(callMethod(param.thisObject, "getKeyguardSecurityCallback"), "reportUnlockAttempt", userId, true /* sucessful */, 0 /* timeout */);
									callMethod(callMethod(param.thisObject, "getKeyguardSecurityCallback"), "dismiss", true /* sucessful */, userId);
								}
							});
						}
					}).start();
				}
				lastPassLen = passwordLen;
			}
		});

		hookAllMethods(KeyguardAbsKeyInputViewControllerClass, "onPasswordChecked", new XC_MethodHook() {
			@SuppressLint("ApplySharedPref")
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (!easyUnlockEnabled) return;

				boolean succesful = (Build.VERSION.SDK_INT == 33)
						? (boolean) param.args[2] //A13
						: (boolean) param.args[1]; //A12

				if (succesful) {
					expectedPassLen = lastPassLen;
					Xprefs.edit().putInt("expectedPassLen", expectedPassLen).commit();
				}
			}
		});
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}
}
