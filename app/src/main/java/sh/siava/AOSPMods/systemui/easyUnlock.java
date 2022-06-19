package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedHelpers.*;
import static de.robv.android.xposed.XposedBridge.*;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;

public class easyUnlock extends XposedModPack {
    private static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;

    private int expectedPassLen = -1;
    private boolean easyUnlockEnabled = false;

    private int lastPassLen = 0;

    public easyUnlock(Context context) {super(context);}

    @Override
    public void updatePrefs(String... Key) {
        easyUnlockEnabled = Xprefs.getBoolean("easyUnlockEnabled", false);
        expectedPassLen = Xprefs.getInt("expectedPassLen", -1);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;

        Class<?> KeyguardAbsKeyInputViewControllerClass = findClass("com.android.keyguard.KeyguardAbsKeyInputViewController", lpparam.classLoader);
        Class<?> LockscreenCredentialClass = findClass("com.android.internal.widget.LockscreenCredential", lpparam.classLoader);

        hookAllMethods(KeyguardAbsKeyInputViewControllerClass, "onUserInput", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if(!easyUnlockEnabled) return;

                int passwordLen = (int) callMethod(callMethod(getObjectField(param.thisObject, "mPasswordEntry"), "getText"), "length");
                if(passwordLen == expectedPassLen && passwordLen > lastPassLen)
                {
                    new Thread(() -> {
                        int userId = (int) callMethod(getObjectField(param.thisObject, "mKeyguardUpdateMonitor"), "getCurrentUser");

                        String methodName = param.thisObject.getClass().getName().contains("Password") ? "createPassword" : "createPin";

                        Object password = callStaticMethod(LockscreenCredentialClass, methodName, (String) callMethod(getObjectField(param.thisObject, "mPasswordEntry"), "getText").toString());

                        boolean accepted = (boolean) callMethod(
                                getObjectField(param.thisObject, "mLockPatternUtils"),
                                "checkCredential",
                                password,
                                userId,
                                null /* callback */);

                        if(accepted)
                        {
                            View mView = (View) getObjectField(param.thisObject, "mView");
                            mView.post(() -> {
                                callMethod(callMethod(param.thisObject, "getKeyguardSecurityCallback"), "reportUnlockAttempt", userId, true /* sucessful */, 0 /* timeout */);
                                callMethod(callMethod(param.thisObject, "getKeyguardSecurityCallback"), "dismiss", true /* sucessful */, userId);
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
                if(!easyUnlockEnabled) return;

                if((boolean)param.args[1] /* successful? */)
                {
                    expectedPassLen = lastPassLen;
                    Xprefs.edit().putInt("expectedPassLen", expectedPassLen).commit();
                }
            }
        });
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
}
