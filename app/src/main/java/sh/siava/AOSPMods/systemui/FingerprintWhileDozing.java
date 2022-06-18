package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedHelpers.*;
import static de.robv.android.xposed.XposedBridge.*;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XPrefs;
import sh.siava.AOSPMods.XposedModPack;

public class FingerprintWhileDozing extends XposedModPack {
    private static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;
    private static boolean fingerprintWhileDozing = true;

    public FingerprintWhileDozing(Context context) {
        super(context);
    }

    @Override
    public void updatePrefs(String... Key) {
        fingerprintWhileDozing = XPrefs.Xprefs.getBoolean("fingerprintWhileDozing", true);
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;

        Class<?> KeyguardUpdateMonitorClass = findClass("com.android.keyguard.KeyguardUpdateMonitor", lpparam.classLoader);

        hookAllMethods(KeyguardUpdateMonitorClass,
                "shouldListenForFingerprint", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(fingerprintWhileDozing) return;
                        boolean currentResult = (boolean) param.getResult();
                        log("current: " + currentResult);
                        if(currentResult)
                        {
                            boolean userDoesNotHaveTrust = !(boolean) callMethod(param.thisObject,
                                    "getUserHasTrust",
                                    callMethod(param.thisObject, "getCurrentUser"));

                            boolean shouldlisten2 =
                                    (getBooleanField(param.thisObject,"mKeyguardIsVisible")
                                        || getBooleanField(param.thisObject,"mBouncer")
                                        || (boolean) callMethod(param.thisObject, "shouldListenForFingerprintAssistant")
                                        || (getBooleanField(param.thisObject,"mKeyguardOccluded") && getBooleanField(param.thisObject,"mIsDreaming")))
                                        && getBooleanField(param.thisObject,"mDeviceInteractive") && !getBooleanField(param.thisObject,"mGoingToSleep") && !getBooleanField(param.thisObject,"mKeyguardGoingAway")
                                        || (getBooleanField(param.thisObject,"mKeyguardOccluded") && userDoesNotHaveTrust
                                            && (getBooleanField(param.thisObject,"mOccludingAppRequestingFp") || (boolean)param.args[0]));

                            log("should2: " + shouldlisten2);

                            param.setResult(shouldlisten2);
                        }
                    }
                });
    }
}
