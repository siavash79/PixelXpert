package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class FlashLightLevel extends XposedModPack {
    public static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;
    private static boolean leveledFlashTile = false;

    public FlashLightLevel(Context context) {
        super(context);
    }

    @Override
    public void updatePrefs(String... Key) {
        leveledFlashTile = Xprefs.getBoolean("leveledFlashTile", false);
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage) || Build.VERSION.SDK_INT < 33) return; //Only SDK 33 and above

        Class<?> QSTileViewImplClass = findClass("com.android.systemui.qs.tileimpl.QSTileViewImpl", lpparam.classLoader);

        hookAllMethods(QSTileViewImplClass, "handleStateChanged", new XC_MethodHook() {
            @SuppressLint("DiscouragedApi")
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if(!SystemUtils.supportsFlashLevels() || !leveledFlashTile)
                {
 //                 cleanUp(param.thisObject);
                    return;
                }

                //View levelLayout = getLevelLayout(param.thisObject);

                Object state = param.args[0];
                if(getObjectField(state, "spec").equals("flashlight"))
                {
                    Resources res = mContext.getResources();
                    final float[] currentPct = {Xprefs.getFloat("flashPCT", 0.5f)};

                    setObjectField(state, "label",
                            String.format("%s - %s%%",
                                    res.getText(
                                            res.getIdentifier(
                                                    "quick_settings_flashlight_label",
                                                    "string", mContext.getPackageName())),
                                    Math.round(currentPct[0] *100f)
                            )
                    );
                    View thisView = (View) param.thisObject;

                    thisView.setOnTouchListener(new View.OnTouchListener() {
                        float initX = 0;
                        float initPct = 0;
                        boolean moved = false;

                        @SuppressLint({"DiscouragedApi", "ClickableViewAccessibility"})
                        @Override
                        public boolean onTouch(View view, MotionEvent motionEvent) {
                            if(!SystemUtils.supportsFlashLevels() || !leveledFlashTile)
                                return false;

                            switch(motionEvent.getAction())
                            {
                                case MotionEvent.ACTION_DOWN:{
                                    initX = motionEvent.getX();
                                    initPct = initX / view.getWidth();
                                    return true;
                                }
                                case MotionEvent.ACTION_MOVE:{
                                    float newPct = motionEvent.getX() / view.getWidth();
                                    float deltaPct = Math.abs(newPct - initPct);
                                    if(deltaPct > .03f)
                                    {
                                        view.getParent().requestDisallowInterceptTouchEvent(true);
                                        moved = true;
                                        currentPct[0] = Math.max(0.01f, Math.min(newPct, 1));
                                        handleFlashLightClick(false, currentPct[0]);
                                        TextView label = (TextView) getObjectField(thisView, "label");
                                        label.setText(
                                                String.format("%s - %s%%",
                                                        res.getText(
                                                                res.getIdentifier(
                                                                        "quick_settings_flashlight_label",
                                                                        "string", mContext.getPackageName())),
                                                        Math.round(currentPct[0] *100f)
                                                )
                                        );
                                    }
                                    return true;
                                }
                                case MotionEvent.ACTION_UP:{
                                    if (moved) {
                                        moved = false;
                                        Xprefs.edit().putFloat("flashPCT", currentPct[0]).apply();
                                    }
                                    else
                                    {
                                        handleFlashLightClick(true, currentPct[0]);
                                    }
                                    return true;
                                }
                            }
                            return true;
                        }
                    });
                }
            }
        });

    }

    private View getLevelLayout(Object thisObject) {
        LinearLayout levelLayout = (LinearLayout) getAdditionalInstanceField(thisObject, "levelLayout");
        if(levelLayout == null)
        {
            levelLayout = new LinearLayout(mContext);
            levelLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            LinearLayout l1 = new LinearLayout(mContext);
            l1.setLayoutParams(new LinearLayout.LayoutParams(-1,-1,1));
            l1.setAlpha(.5f);
            l1.setBackgroundColor(Color.RED);
            levelLayout.addView(l1);

            LinearLayout l2 = new LinearLayout(mContext);
            l2.setLayoutParams(new LinearLayout.LayoutParams(-1,-1,1));
            l2.setAlpha(0);
            levelLayout.addView(l2);

            setAdditionalInstanceField(thisObject, "levelLayout", levelLayout);
            callMethod(thisObject, "addView", levelLayout);
        }
        return levelLayout;
    }

    private void cleanUp(Object thisObject) {
        View levelLayout = (View) getAdditionalInstanceField(thisObject, "levelLayout");
        if(levelLayout != null) {
            ((ViewGroup) levelLayout.getParent()).removeView(levelLayout);
            setAdditionalInstanceField(thisObject, "levelLayout", null);
        }
    }

    private void handleFlashLightClick(boolean toggle, float pct) {
        boolean currState = SystemUtils.isFlashOn();

        if(!toggle && !currState) return; //nothing to do

        if(toggle)
        {
            currState = !currState;
        }

        if(currState)
        {
            SystemUtils.setFlash(true, pct);
        }
        else
        {
            SystemUtils.setFlash(false);
        }
    }
}
