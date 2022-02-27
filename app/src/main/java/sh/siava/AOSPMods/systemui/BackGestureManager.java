package sh.siava.AOSPMods.systemui;

import android.graphics.Point;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.aModManager;

public class BackGestureManager extends aModManager {

    public BackGestureManager(XC_LoadPackage.LoadPackageParam lpparam) {
        super(lpparam);
    }

    @Override
    protected void hookMethods() {
        XposedHelpers.findAndHookMethod("com.android.systemui.navigationbar.gestural.EdgeBackGestureHandler", lpparam.classLoader,
                "isWithinInsets", int.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        int y = (int) param.args[1];
                        Point mDisplaySize = (Point) XposedHelpers.getObjectField(param.thisObject, "mDisplaySize");
                        float mBottomGestureHeight = (float) XposedHelpers.getObjectField(param.thisObject, "mBottomGestureHeight");
                        int mEdgeHeight = (int) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mEdgeHeight");

                        if (mEdgeHeight != 0) {
                            if (y < (mDisplaySize.y - mBottomGestureHeight - mEdgeHeight)) {
                                //Xposedbridge.log("SIAPOSED back i didn't approve" + mEdgeHeight);
                                param.setResult(false);
                                return;
                            }
                        }
                    }
                });

        XposedHelpers.findAndHookMethod("com.android.systemui.navigationbar.gestural.EdgeBackGestureHandler", lpparam.classLoader,
                "updateDisplaySize", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Point mDisplaySize = (Point) XposedHelpers.getObjectField(param.thisObject, "mDisplaySize");
                        //Xposedbridge.log("SIAPOSED back i set edge height");

                        XposedHelpers.setAdditionalInstanceField(param.thisObject, "mEdgeHeight", mDisplaySize.y / 2);
                    }
                });
    }


}
