package sh.siava.AOSPMods.systemui;

import android.graphics.Point;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.aModManager;

public class BackGestureManager implements IXposedHookLoadPackage {
    public static final String listenPackage = "com.android.systemui";
    public static int backGestureHeightFraction = 1;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.log(lpparam.packageName);
        XposedBridge.log(listenPackage);
        if(!lpparam.packageName.equals(listenPackage)) return;

        XposedHelpers.findAndHookMethod("com.android.systemui.navigationbar.gestural.EdgeBackGestureHandler", lpparam.classLoader,
                "isWithinInsets", int.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                        int y = (int) param.args[1];
                        Point mDisplaySize = (Point) XposedHelpers.getObjectField(param.thisObject, "mDisplaySize");
                        float mBottomGestureHeight = (float) XposedHelpers.getObjectField(param.thisObject, "mBottomGestureHeight");
                        int mEdgeHeight = mDisplaySize.y / backGestureHeightFraction;
                        XposedBridge.log("SIAPOSED: height:" + mEdgeHeight);

                        if (mEdgeHeight != 0) {
                            if (y < (mDisplaySize.y - mBottomGestureHeight - mEdgeHeight)) {
                                XposedBridge.log("SIAPOSED back i didn't approve" + mEdgeHeight);
                                param.setResult(false);
                                return;
                            }
                        }
                    }
                });
    }


}
