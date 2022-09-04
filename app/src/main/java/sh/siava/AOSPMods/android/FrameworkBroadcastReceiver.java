package sh.siava.AOSPMods.android;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class FrameworkBroadcastReceiver extends XposedModPack {
    public static final String listenPackage = AOSPMods.SYSTEM_FRAMEWORK_PACKAGE;

    private Object windowMan = null;
    private static boolean broadcastRegistered = false;

    public FrameworkBroadcastReceiver(Context context) {
        super(context);
    }

    @Override
    public void updatePrefs(String... Key) {
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                if(action.equals(AOSPMods.ACTION_SCREENSHOT)) {
                    callMethod(windowMan, "handleScreenShot", 1, 1);
                }
                else if (action.equals(AOSPMods.ACTION_BACK))
                {
                    callMethod(windowMan, "backKeyPress");
                }
            }catch (Throwable ignored){}
        }
    };

    IntentFilter intentFilter = new IntentFilter();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(listenPackage)) return;

        if(!broadcastRegistered) {
            broadcastRegistered = true;
            intentFilter.addAction(AOSPMods.ACTION_SCREENSHOT);
            intentFilter.addAction(AOSPMods.ACTION_BACK);
            mContext.registerReceiver(broadcastReceiver, intentFilter);
        }

        try {
            Class<?> PhoneWindowManagerClass = findClass("com.android.server.policy.PhoneWindowManager", lpparam.classLoader);

            hookAllMethods(PhoneWindowManagerClass, "enableScreen", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    windowMan = param.thisObject;
                }
            });

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public boolean listensTo(String packageName) {
        return listenPackage.equals(packageName);
    }
}