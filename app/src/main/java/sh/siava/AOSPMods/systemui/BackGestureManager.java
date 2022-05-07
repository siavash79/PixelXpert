package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.graphics.Point;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.XPrefs;

public class BackGestureManager extends XposedModPack {
    private static final String listenPackage = "com.android.systemui";
    public static float backGestureHeightFractionLeft = 1f; // % of screen height. can be anything between 0 to 1
    public static float backGestureHeightFractionRight = 1f; // % of screen height. can be anything between 0 to 1
    public static boolean leftEnabled = true;
    public static boolean rightEnabled = true;
    
    public BackGestureManager(Context context) { super(context); }
    
    @Override
    public void updatePrefs(String...Key)
    {
        if(XPrefs.Xprefs == null) return;
        leftEnabled = XPrefs.Xprefs.getBoolean("BackFromLeft", true);
        rightEnabled = XPrefs.Xprefs.getBoolean("BackFromRight", true);
        backGestureHeightFractionLeft = XPrefs.Xprefs.getInt("BackLeftHeight", 100) / 100f;

        backGestureHeightFractionRight = XPrefs.Xprefs.getInt("BackRightHeight", 100) / 100f;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if(!lpparam.packageName.equals(listenPackage)) return;

        XposedHelpers.findAndHookMethod("com.android.systemui.navigationbar.gestural.EdgeBackGestureHandler", lpparam.classLoader,
                "isWithinInsets", int.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                        int x = (int) param.args[0];
                        int y = (int) param.args[1];
                        Point mDisplaySize = (Point) XposedHelpers.getObjectField(param.thisObject, "mDisplaySize");
                        boolean isLeftSide = x < (mDisplaySize.x/3);

                        float mBottomGestureHeight = (float) XposedHelpers.getObjectField(param.thisObject, "mBottomGestureHeight");

                        int mEdgeHeight;
                        if(isLeftSide)
                        {
                            if(!leftEnabled)
                            {
                                param.setResult(false);
                                return;
                            }
                            mEdgeHeight = Math.round(mDisplaySize.y * backGestureHeightFractionLeft);
                        }
                        else
                        {
                            if(!rightEnabled)
                            {
                                param.setResult(false);
                                return;
                            }
                            mEdgeHeight = Math.round(mDisplaySize.y * backGestureHeightFractionRight);
                        }
                        
                        if (mEdgeHeight != 0) {
                            if (y < (mDisplaySize.y - mBottomGestureHeight - mEdgeHeight)) {
                                param.setResult(false);
                            }
                        }
                    }
                });
    }
    
    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
    
}
