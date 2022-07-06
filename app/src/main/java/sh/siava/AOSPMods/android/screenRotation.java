package sh.siava.AOSPMods.android;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;

public class screenRotation extends XposedModPack {
    public static final String listenPackage = AOSPMods.SYSTEM_FRAMEWORK_PACKAGE;

    private static final int USER_ROTATION_LOCKED = 1;

    private static boolean allScreenRotations = false;

    public screenRotation(Context context) {
        super(context);
    }

    @Override
    public void updatePrefs(String... Key) {
        if(Xprefs == null) return;

        allScreenRotations = Xprefs.getBoolean("allScreenRotations", false);
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(listenPackage)) return;

        try
        {
            Class<?> DisplayRotationClass = findClass("com.android.server.wm.DisplayRotation", lpparam.classLoader);

            hookAllMethods(DisplayRotationClass, "rotationForOrientation", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        if(!allScreenRotations) return;

                        final int lastRotation = (int) param.args[1];

                        if (getIntField(param.thisObject, "mUserRotationMode") == USER_ROTATION_LOCKED) {
                            param.setResult(lastRotation);
                            return;
                        }

                        Object mOrientationListener = getObjectField(param.thisObject, "mOrientationListener");
                        int sensorRotation = mOrientationListener != null
                                ? (int) callMethod(mOrientationListener, "getProposedRotation") // may be -1
                                : -1;
                        if (sensorRotation < 0) {
                            sensorRotation = lastRotation;
                        }
                        param.setResult(sensorRotation);
                    }catch (Throwable ignored){}
                }
            });
        }catch (Exception ignored){}
    }
}
