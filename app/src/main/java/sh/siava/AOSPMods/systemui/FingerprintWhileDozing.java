package sh.siava.AOSPMods.systemui;

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
    private static boolean fingerprintWhileDozing = false;

    public FingerprintWhileDozing(Context context) {
        super(context);
    }

    @Override
    public void updatePrefs(String... Key) {
        fingerprintWhileDozing = XPrefs.Xprefs.getBoolean("fingerprintWhileDozing", false);
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;

        Class<?> KeyguardUpdateMonitorClass = XposedHelpers.findClass("com.android.keyguard.KeyguardUpdateMonitor", lpparam.classLoader);

        XposedBridge.hookAllMethods(KeyguardUpdateMonitorClass,
                "shouldListenForFingerprint", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if(!fingerprintWhileDozing) return;
                        boolean currentResult = (boolean) param.getResult();
                        XposedBridge.log("current: " + currentResult);
                        if(currentResult)
                        {
                            boolean userDoesNotHaveTrust = !(boolean) XposedHelpers.callMethod(param.thisObject,
                                    "getUserHasTrust",
                                    XposedHelpers.callMethod(param.thisObject, "getCurrentUser"));

                            boolean shouldlisten2 =
                                    (XposedHelpers.getBooleanField(param.thisObject,"mKeyguardIsVisible")
                                        || XposedHelpers.getBooleanField(param.thisObject,"mBouncer")
                                        || (boolean) XposedHelpers.callMethod(param.thisObject, "shouldListenForFingerprintAssistant")
                                        || (XposedHelpers.getBooleanField(param.thisObject,"mKeyguardOccluded") && XposedHelpers.getBooleanField(param.thisObject,"mIsDreaming")))
                                        && XposedHelpers.getBooleanField(param.thisObject,"mDeviceInteractive") && !XposedHelpers.getBooleanField(param.thisObject,"mGoingToSleep") && !XposedHelpers.getBooleanField(param.thisObject,"mKeyguardGoingAway")
                                        || (XposedHelpers.getBooleanField(param.thisObject,"mKeyguardOccluded") && userDoesNotHaveTrust
                                            && (XposedHelpers.getBooleanField(param.thisObject,"mOccludingAppRequestingFp") || (boolean)param.args[0]));

                            XposedBridge.log("should2: " + shouldlisten2);

                            param.setResult(shouldlisten2);
                        }
                    }
                });
    }
}
