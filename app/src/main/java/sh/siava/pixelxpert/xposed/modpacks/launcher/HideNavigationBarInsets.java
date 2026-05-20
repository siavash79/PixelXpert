package sh.siava.pixelxpert.xposed.modpacks.launcher;

import static sh.siava.pixelxpert.xposed.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.WindowManager;

import java.lang.reflect.Array;

import de.robv.android.xposed.XposedHelpers;

import sh.siava.pixelxpert.xposed.XposedModPack;
import sh.siava.pixelxpert.xposed.annotations.LauncherModPack;
import sh.siava.pixelxpert.xposed.utils.SystemUtils;
import sh.siava.pixelxpert.xposed.utils.reflection.ReflectedClass;

@LauncherModPack
public class HideNavigationBarInsets extends XposedModPack {
    private boolean HideNavbarInsets;

    public HideNavigationBarInsets(Context context) {
        super(context);
    }

    @Override
    public void onPreferenceUpdated(String... Key) {
        HideNavbarInsets = Xprefs.getBoolean("HideNavbarInsets", false);
        if (Key.length > 0 && Key[0].equals("HideNavbarInsets")) {
            SystemUtils.killSelf();
        }
    }

    @SuppressLint("DiscouragedApi")
    @Override
    public void onPackageLoaded(io.github.libxposed.api.XposedModuleInterface.PackageReadyParam PRParam) throws Throwable {
        ReflectedClass TaskbarActivityContextClass = ReflectedClass.of("com.android.launcher3.taskbar.TaskbarActivityContext");
        TaskbarActivityContextClass
                .before("notifyUpdateLayoutParams")
                .run(param -> {
                    if (!HideNavbarInsets)
                        return;

                    WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) XposedHelpers.getObjectField(param.thisObject, "mWindowLayoutParams");
                    transformLayoutParams(layoutParams);

                    WindowManager.LayoutParams[] rotationParams = (WindowManager.LayoutParams[]) XposedHelpers.getObjectField(layoutParams, "paramsForRotation");
                    if (rotationParams == null)
                        return;

                    for (WindowManager.LayoutParams rotationLayoutParams : rotationParams)
                        transformLayoutParams(rotationLayoutParams);
                });
    }

    private void transformLayoutParams(WindowManager.LayoutParams layoutParams) {
        if (layoutParams == null)
            return;

        Object providedInsets = XposedHelpers.getObjectField(layoutParams, "providedInsets");
        if (providedInsets == null)
            return;

        int providedInsetsLength = Array.getLength(providedInsets);
        for (int i = 0; i < providedInsetsLength; i++) {
            Object insetsFrame = Array.get(providedInsets, i);

            if (insetsFrame == null)
                continue;
            if (!insetsFrame.toString().contains("type=navigationBars")) // no constants, maximum compatibility with Android versions
                continue;

            XposedHelpers.callMethod(insetsFrame, "setInsetsSize", android.graphics.Insets.NONE);
        }
    }
}
