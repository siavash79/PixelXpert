package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.rangesliderpreference.RangeSliderPreference;

@SuppressWarnings("RedundantThrows")
public class KeyguardDimmer extends XposedModPack {
    public static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;
    public static float KeyGuardDimAmount = -1f;

    public KeyguardDimmer(Context context) { super(context); }
    
    @Override
    public void updatePrefs(String...Key)
    {
        if(Xprefs == null) return;

        KeyGuardDimAmount = RangeSliderPreference.getValues(Xprefs, "KeyGuardDimAmount", -1f).get(0)/100f;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;

        Class<?> ScrimControllerClass = findClass("com.android.systemui.statusbar.phone.ScrimController", lpparam.classLoader);
        Class<?> ScrimStateEnum = findClass("com.android.systemui.statusbar.phone.ScrimState", lpparam.classLoader);

        hookAllMethods(ScrimControllerClass, "scheduleUpdate", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if(KeyGuardDimAmount < 0 || KeyGuardDimAmount > 1) return;

                setObjectField(param.thisObject, "mScrimBehindAlphaKeyguard", KeyGuardDimAmount);
                Object[] constants =  ScrimStateEnum.getEnumConstants();
                for (Object constant : constants) {
                    setObjectField(constant, "mScrimBehindAlphaKeyguard", KeyGuardDimAmount);
                }
            }
        });
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
}