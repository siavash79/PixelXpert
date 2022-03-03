package sh.siava.AOSPMods.android;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.*;

public class HideGoogle2021  {

    private static final String[] pixel6Codenames = {
            "oriole",
            "raven",
    };

    private static final String[] featuresPixel = {
            "com.google.android.apps.photos.PIXEL_2019_PRELOAD",
            "com.google.android.apps.photos.PIXEL_2019_MIDYEAR_PRELOAD",
            "com.google.android.apps.photos.PIXEL_2018_PRELOAD",
            "com.google.android.apps.photos.PIXEL_2017_PRELOAD",
            "com.google.android.feature.PIXEL_2021_MIDYEAR_EXPERIENCE",
            "com.google.android.feature.PIXEL_2020_EXPERIENCE",
            "com.google.android.feature.PIXEL_2020_MIDYEAR_EXPERIENCE",
            "com.google.android.feature.PIXEL_2019_EXPERIENCE",
            "com.google.android.feature.PIXEL_2019_MIDYEAR_EXPERIENCE",
            "com.google.android.feature.PIXEL_2018_EXPERIENCE",
            "com.google.android.feature.PIXEL_2017_EXPERIENCE",
            "com.google.android.feature.PIXEL_EXPERIENCE",
            "com.google.android.feature.GOOGLE_BUILD",
            "com.google.android.feature.GOOGLE_EXPERIENCE"
    };

    private static final String[] featuresPixel6 = {
            "com.google.android.feature.PIXEL_2021_EXPERIENCE"
    };

    private static final String[] featuresNexus = {
            "com.google.android.apps.photos.NEXUS_PRELOAD",
            "com.google.android.apps.photos.nexus_preload",
            "com.google.android.feature.PIXEL_EXPERIENCE",
            "com.google.android.feature.GOOGLE_BUILD",
            "com.google.android.feature.GOOGLE_EXPERIENCE"
    };
    XC_LoadPackage.LoadPackageParam lpparam;
    public HideGoogle2021(XC_LoadPackage.LoadPackageParam lpparam) {
        this.lpparam = lpparam;
    }


    protected void hookMethods() {
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader,
                "hasSystemFeature", String.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        //XposedBridge.log("SIAPOSED: found feature req");

                        String name = (String) param.args[0];
                        XposedBridge.log("SIAPOSED: " + lpparam.packageName + " asking for: " + name);
/*
                        Class SysPropClass = XposedHelpers.findClass("android.os.SystemProperties", lpparam.classLoader);
                        boolean isPixel6Device = false;//Arrays.asList(pixel6Codenames).contains(SystemProperties.get(DEVICE));
                        String packageName = lpparam.packageName;

                        if (packageName != null &&
                                packageName.equals("com.google.android.apps.photos") &&
                                (boolean) XposedHelpers.callStaticMethod(SysPropClass, "getBoolean", "persist.sys.pixelprops.gphotos", true)) {
                            if (Arrays.asList(featuresPixel).contains(name)) { param.setResult(false); log(name,false); return;}
                            if (Arrays.asList(featuresNexus).contains(name)) { param.setResult(true); log(name,true); return;}

                        }
                        if (isPixel6Device) {
                            if (Arrays.asList(featuresPixel6).contains(name)) { param.setResult(true); log(name,true); return;}
                        } else {
                            if (Arrays.asList(featuresPixel6).contains(name)) { param.setResult(false); log(name,false); return;}
                        }
                        if (Arrays.asList(featuresPixel).contains(name)) { param.setResult(true); log(name,true); return;}*/
                    }
                });

    }

    private static void log(String name, boolean res)
    {
        XposedBridge.log("SIAPOSED: asked for " + name + " I said " + res);
    }
}
