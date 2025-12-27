package sh.siava.pixelxpert.xposed.modpacks.systemui;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static sh.siava.pixelxpert.xposed.XPrefs.Xprefs;

import android.content.Context;
import android.view.View;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.xposed.XposedModPack;
import sh.siava.pixelxpert.xposed.annotations.SystemUIModPack;
import sh.siava.pixelxpert.xposed.utils.toolkit.ReflectedClass;

@SuppressWarnings("RedundantThrows")
@SystemUIModPack
public class EasyUnlock extends XposedModPack {
	private int expectedPassLen = -1;
	private boolean easyUnlockEnabled = false;

	private int lastPassLen = 0;
	private static boolean WakeUpToSecurityInput = false;

	public EasyUnlock(Context context) {
		super(context);
	}

	@Override
	public void onPreferenceUpdated(String... Key) {
		easyUnlockEnabled = Xprefs.getBoolean("easyUnlockEnabled", false);
		expectedPassLen = Xprefs.getInt("expectedPassLen", -1);
		WakeUpToSecurityInput = Xprefs.getBoolean("WakeUpToSecurityInput", false);
	}

	@Override
	public void onPackageLoaded(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		ReflectedClass KeyguardAbsKeyInputViewControllerClass = ReflectedClass.of("com.android.keyguard.KeyguardAbsKeyInputViewController");
		ReflectedClass LockscreenCredentialClass = ReflectedClass.of("com.android.internal.widget.LockscreenCredential");
		ReflectedClass StatusBarKeyguardViewManagerClass = ReflectedClass.of("com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager");

		StatusBarKeyguardViewManagerClass
				.before("onDozingChanged")
				.run(param -> {
					//noinspection ConstantValue
					if(WakeUpToSecurityInput && param.args[0].equals(false) && (!getBooleanField(getObjectField(param.thisObject, "mKeyguardStateController"), "mCanDismissLockScreen")))//waking up
					{
						callMethod(param.thisObject, "showPrimaryBouncer", /*reason*/"PXAsked", true);
					}
				});

		KeyguardAbsKeyInputViewControllerClass
				.after("onUserInput")
				.run(param -> {
					if (!easyUnlockEnabled) return;

					int passwordLen = (int) callMethod(getObjectField(getObjectField(param.thisObject, "mPasswordEntry"), "mText"), "length");

					if (passwordLen == expectedPassLen && passwordLen > lastPassLen) {
						new Thread(() -> {
							try { //don't crash systemUI if failed
								int userId;
								try { //14 QPR3 beta 2.1
									userId = (int) callMethod(
											getObjectField(
													getObjectField(param.thisObject, "mKeyguardUpdateMonitor"),
													"mSelectedUserInteractor")
											, "getSelectedUserId");
								}
								catch (Throwable ignored)
								{ //14 QPR3 beta 2 and older
									userId = (int) getObjectField(getObjectField(param.thisObject, "mKeyguardUpdateMonitor"), "sCurrentUser");
								}

								String methodName = param.thisObject.getClass().getName().contains("Password") ? "createPassword" : "createPin";

								Object password = LockscreenCredentialClass.callStaticMethod(methodName, getObjectField(getObjectField(param.thisObject, "mPasswordEntry"), "mText").toString());

								Object verificationResult = callMethod(
										getObjectField(param.thisObject, "mLockPatternUtils"),
										"checkCredential",
										password,
										userId,
										null /* callback */);

								boolean accepted;

								try{ //16qpr3
									accepted = (boolean) callMethod(verificationResult, "isMatched");
								}
								catch (Throwable ignored) //older
								{
									accepted = (boolean) verificationResult;
								}

								if (accepted) {
									View mView = (View) getObjectField(param.thisObject, "mView");
									int finalUserId = userId;
									mView.post(() -> {
										try { //13 QPR3
											callMethod(callMethod(param.thisObject, "getKeyguardSecurityCallback"), "dismiss", finalUserId, getObjectField(param.thisObject, "mSecurityMode"));
										} catch (Throwable ignored) {}
									});
								}
							} catch (Throwable ignored){}
						}).start();
					}
					lastPassLen = passwordLen;
				});

		KeyguardAbsKeyInputViewControllerClass
				.after("onPasswordChecked")
				.run(param -> {
					if (!easyUnlockEnabled) return;

					boolean successful = (boolean) param.args[1];

					if (successful) {
						expectedPassLen = lastPassLen;
						Xprefs.edit().putInt("expectedPassLen", expectedPassLen).apply();
					}
				});
	}
}