package sh.siava.AOSPMods.android;

import static de.robv.android.xposed.XposedHelpers.*;
import static de.robv.android.xposed.XposedBridge.*;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.view.DisplayCutout;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XPrefs;
import sh.siava.AOSPMods.XposedModPack;


//We are playing in system framework. should be extra cautious..... many try-catchs, still not enough!
public class StatusbarSize extends XposedModPack {
    private final List<String> listenPacks = new ArrayList<>();

    private static final int BOUNDS_POSITION_TOP = 1;

    static int sizeFactor = 100; // % of normal
    static int currentHeight = 0;

    public StatusbarSize(Context context) {
        super(context);
        listenPacks.add(AOSPMods.SYSTEM_FRAMEWORK_PACKAGE);
        listenPacks.add(AOSPMods.SYSTEM_UI_PACKAGE);
    }

    @Override
    public void updatePrefs(String... Key) {
        if(XPrefs.Xprefs == null) return;

        sizeFactor = XPrefs.Xprefs.getInt("statusbarHeightFactor", 100);
        if(sizeFactor != 100)
            currentHeight = Math.round(
                    mContext.getResources().getDimensionPixelSize(
                            mContext.getResources().getIdentifier(
                                    "status_bar_height",
                                    "dimen",
                                    "android")
                    )
                            * sizeFactor
                            / 100f);
    }

    @Override
    public boolean listensTo(String packageName) { return listenPacks.contains(packageName);}

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        try
        {
            try {
                Class<?> WmDisplayCutoutClass = findClass("com.android.server.wm.utils.WmDisplayCutout", lpparam.classLoader);

                hookAllMethods(WmDisplayCutoutClass, "getDisplayCutout", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (sizeFactor >= 100) return;

                        DisplayCutout displayCutout = (DisplayCutout)param.getResult();

                        ((Rect[])getObjectField(
                                getObjectField(
                                        displayCutout,
                                        "mBounds"),
                                "mRects")
                        )[BOUNDS_POSITION_TOP].bottom = currentHeight;

                        ((Rect) getObjectField(
                                displayCutout,
                                "mSafeInsets")
                        ).top = currentHeight;
                    }
                });
            }catch (Throwable ignored){}

            XC_MethodHook resizedResultHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        if(sizeFactor == 100) return;
                        param.setResult(currentHeight);
                    }catch (Throwable ignored){}
                }
            };

            try {
                Class<?> SystemBarUtilsClass = findClass("com.android.internal.policy.SystemBarUtils", lpparam.classLoader);

                findAndHookMethod(SystemBarUtilsClass, "getStatusBarHeight", Resources.class, DisplayCutout.class, resizedResultHook);
                hookAllMethods(SystemBarUtilsClass, "getStatusBarHeightForRotation", resizedResultHook);
            }catch (Throwable ignored){}
        } catch (Throwable ignored){}
    }
}
