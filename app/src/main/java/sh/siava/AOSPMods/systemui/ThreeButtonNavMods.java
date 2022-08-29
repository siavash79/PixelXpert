package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.content.Context;
import android.os.Build;
import android.view.View;

import com.topjohnwu.superuser.Shell;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class ThreeButtonNavMods extends XposedModPack {
    private static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;
    private static boolean BackLongPressKill = false;
    private boolean ThreeButtonLayoutMod = false;
    private static String ThreeButtonCenter, ThreeButtonRight, ThreeButtonLeft;

    public ThreeButtonNavMods(Context context) { super(context); }
    
    @Override
    public void updatePrefs(String...Key)
    {
        if(Xprefs == null) return;
        BackLongPressKill = Xprefs.getBoolean("BackLongPressKill", false);
        ThreeButtonLayoutMod = Xprefs.getBoolean("ThreeButtonLayoutMod", false);

        ThreeButtonLeft = Xprefs.getString("ThreeButtonLeft", "back");
        ThreeButtonCenter = Xprefs.getString("ThreeButtonCenter", "home");
        ThreeButtonRight = Xprefs.getString("ThreeButtonRight", "recent");
    }


    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if(!lpparam.packageName.equals(listenPackage)) return;

        Class<?> NavigationBarInflaterViewClass = findClass("com.android.systemui.navigationbar.NavigationBarInflaterView", lpparam.classLoader);

        hookAllMethods(NavigationBarInflaterViewClass, "inflateLayout", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if(!ThreeButtonLayoutMod || !((String)param.args[0]).contains("recent")) return;

                String layout = ((String) param.args[0])
                        .replace("home", "XCenterX")
                        .replace("back", "XLeftX")
                        .replace("recent", "XRightX");

                param.args[0] = layout
                        .replace("XCenterX", ThreeButtonCenter)
                        .replace("XLeftX", ThreeButtonLeft)
                        .replace("XRightX", ThreeButtonRight);
                log("layout 2 " + param.args[0]);

            }
        });

        if(Build.VERSION.SDK_INT >= 33) return;

        Class<?> NavBarClass = findClass("com.android.systemui.navigationbar.NavigationBar", lpparam.classLoader);

        View.OnLongClickListener listener = v -> {
            if(!BackLongPressKill) return true;

            Shell.cmd("am force-stop $(dumpsys window | grep mCurrentFocus | cut -d \"/\" -f1 | cut -d \" \" -f5)").submit();

            return true;
        };

        findAndHookMethod(NavBarClass,
                "getView", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object mNavigationBarView = getObjectField(param.thisObject, "mNavigationBarView");
                        Object backButton = callMethod(mNavigationBarView, "getBackButton");

                        callMethod(backButton, "setLongClickable", true);
                        callMethod(backButton, "setOnLongClickListener", listener);
                    }
                });
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName);}

}
