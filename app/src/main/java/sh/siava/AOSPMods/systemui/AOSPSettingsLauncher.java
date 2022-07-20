package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class AOSPSettingsLauncher extends XposedModPack {
    private static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;


    private static Object activityStarter = null;
    
    public AOSPSettingsLauncher(Context context) { super(context); }
    
    
    @Override
    public void updatePrefs(String...Key) { }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;
        
        Class<?> FooterActionsControllerClass = findClass("com.android.systemui.qs.FooterActionsController", lpparam.classLoader);

        View.OnLongClickListener listener = v -> {
            try {
                Intent launchInent = mContext.getPackageManager().getLaunchIntentForPackage("sh.siava.AOSPMods");
                callMethod(activityStarter, "startActivity", launchInent, true, null);
            }catch(Exception ignored){}
            return true;
        };

        hookAllMethods(FooterActionsControllerClass,
                "onViewAttached", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Object settingsButton = getObjectField(param.thisObject, "settingsButton");
                        activityStarter = getObjectField(param.thisObject, "activityStarter");
                        callMethod(settingsButton, "setOnLongClickListener", listener);
                    }
                });
    }
}
