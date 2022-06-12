package sh.siava.AOSPMods;

import static android.content.Intent.FLAG_RECEIVER_FOREGROUND;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import android.app.AndroidAppHelper;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

import java.io.File;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class QuickUnlock implements IXposedHookZygoteInit, IXposedHookLoadPackage {

    private static final File prefFile = new File("/data/user_de/0/sh.siava.AOSPMods/shared_prefs/sh.siava.AOSPMods_preferences.xml");
    private static XSharedPreferences pref;
    private Object mLockPatterUtils;
    private Object mLockCallback;
    private Object mKeyguardMonitor;
    private ClassLoader classLoader;

    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {

        if (XposedBridge.getXposedVersion() < 93)
            pref = new XSharedPreferences(prefFile);
        else
            pref = new XSharedPreferences("sh.siava.AOSPMods");

        if (pref.getBoolean("trick_quickUnlock", false)) {
            findAndHookMethod("com.android.keyguard.KeyguardPasswordViewController", param.classLoader, "onViewAttached", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    pref.reload();
                    mLockPatterUtils = getObjectField(param.thisObject, "mLockPatternUtils");
                    mLockCallback = getObjectField(param.thisObject, "mKeyguardSecurityCallback");
                    mKeyguardMonitor = getObjectField(param.thisObject, "mKeyguardUpdateMonitor");
                }
            });

            findAndHookMethod("com.android.keyguard.KeyguardPinViewController", param.classLoader, "onViewAttached", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    pref.reload();
                    mLockPatterUtils = getObjectField(param.thisObject, "mLockPatternUtils");
                    mLockCallback = getObjectField(param.thisObject, "mKeyguardSecurityCallback");
                    mKeyguardMonitor = getObjectField(param.thisObject, "mKeyguardUpdateMonitor");
                }
            });

            findAndHookMethod("com.android.keyguard.KeyguardPasswordView", param.classLoader, "onFinishInflate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    TextView passwordEntry = (TextView) getObjectField(param.thisObject, "mPasswordEntry");
                    passwordEntry.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                            String entry = passwordEntry.getText().toString();
                            int passwordLength = pref.getInt("passwordLength", -1);
                            if (entry.length() == passwordLength) {
                                int userId = (int) callMethod(mKeyguardMonitor, "getCurrentUser");
                                Class<?> credential = findClass("com.android.internal.widget.LockscreenCredential", classLoader);
                                Object password = callStaticMethod(credential, "createPassword", entry);
                                boolean valid = (boolean) callMethod(mLockPatterUtils,
                                        "checkCredential", password, userId, (Object) null);
                                if (valid) {
                                    callMethod(mLockCallback, "reportUnlockAttempt", userId, true, 0);
                                    callMethod(mLockCallback, "dismiss", true, userId);
                                }
                            }
                        }
                    });
                }
            });

            findAndHookMethod("com.android.keyguard.PasswordTextView", param.classLoader, "append", char.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    String entry = (String) getObjectField(param.thisObject, "mText");
                    int passwordLength = pref.getInt("passwordLength", -1);
                    if (entry.length() == passwordLength) {
                        int userId = (int) callMethod(mKeyguardMonitor, "getCurrentUser");
                        Class<?> credential = findClass("com.android.internal.widget.LockscreenCredential", classLoader);
                        Object pin = callStaticMethod(credential, "createPin", entry);
                        boolean valid = (boolean) callMethod(mLockPatterUtils,
                                "checkCredential", pin, userId, (Object) null);
                        if (valid) {
                            callMethod(mLockCallback, "reportUnlockAttempt", userId, true, 0);
                            callMethod(mLockCallback, "dismiss", true, userId);
                        }
                    }
                }
            });

        } else if (param.packageName.equals("com.android.settings")) {
            findAndHookMethod("com.android.settings.password.ChooseLockGeneric$ChooseLockGenericFragment", param.classLoader, "setUnlockMethod", String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    String lock = (String) param.args[0];
                    Context context = getContext();
                    if (lock.equals("unlock_set_off") || lock.equals("unlock_set_none")) {
                        if (context != null) {
                            Intent password = new Intent("sh.siava.AOSPMods.SET_INTEGER");
                            password.setFlags(FLAG_RECEIVER_FOREGROUND);
                            password.setPackage("sh.siava.AOSPMods");
                            password.putExtra("preference", "passwordLength");
                            password.putExtra("value", -1);
                            context.sendBroadcast(password);

                            Intent unlock = new Intent("sh.siava.AOSPMods.SET_BOOLEAN");
                            unlock.setFlags(FLAG_RECEIVER_FOREGROUND);
                            unlock.setPackage("sh.siava.AOSPMods");
                            unlock.putExtra("preference", "trick_quickUnlock");
                            unlock.putExtra("value", false);
                            context.sendBroadcast(unlock);
                        }
                    }
                }
            });

            findAndHookMethod("com.android.settings.password.ChooseLockPattern$ChooseLockPatternFragment", param.classLoader, "startSaveAndFinish", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Context context = getContext();
                    if (context != null) {
                        Intent password = new Intent("sh.siava.AOSPMods.SET_INTEGER");
                        password.setFlags(FLAG_RECEIVER_FOREGROUND);
                        password.setPackage("sh.siava.AOSPMods");
                        password.putExtra("preference", "passwordLength");
                        password.putExtra("value", -1);
                        context.sendBroadcast(password);

                        Intent unlock = new Intent("sh.siava.AOSPMods.SET_BOOLEAN");
                        unlock.setFlags(FLAG_RECEIVER_FOREGROUND);
                        unlock.setPackage("sh.siava.AOSPMods");
                        unlock.putExtra("preference", "trick_quickUnlock");
                        unlock.putExtra("value", false);
                        context.sendBroadcast(unlock);
                    }
                }
            });

            findAndHookMethod("com.android.settings.password.ChooseLockPassword$ChooseLockPasswordFragment", param.classLoader, "startSaveAndFinish", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Object mChosenPassword = getObjectField(param.thisObject, "mChosenPassword");
                    int length = (int) callMethod(mChosenPassword, "size");

                    Context context = getContext();
                    if (context != null) {
                        Intent password = new Intent("sh.siava.AOSPMods.SET_INTEGER");
                        password.setFlags(FLAG_RECEIVER_FOREGROUND);
                        password.setPackage("sh.siava.AOSPMods");
                        password.putExtra("preference", "passwordLength");
                        password.putExtra("value", length);
                        context.sendBroadcast(password);
                    }
                }
            });
        }
    }

    private Context getContext() {
        Context context = null;
        Application app = AndroidAppHelper.currentApplication();
        try {
            context = app.createPackageContext("sh.siava.AOSPMods", 0);
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return context;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

    }
}