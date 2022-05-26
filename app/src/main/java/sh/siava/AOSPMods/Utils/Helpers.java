package sh.siava.AOSPMods.Utils;

import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;

@SuppressWarnings("CommentedOutCode")
public class Helpers {
    private static final int KB = 1024;
    private static final int MB = 1024 * KB;
    private static final int GB = 1024 * MB;

    public static List<String> activeOverlays = null;

    @SuppressWarnings("unused")
    public static void dumpClass(String className, XC_LoadPackage.LoadPackageParam lpparam){
        Class<?> ourClass = XposedHelpers.findClassIfExists(className, lpparam.classLoader);
        if(ourClass == null)
        {
            XposedBridge.log("Class: " + className + " not found");
            return;
        }
        Method[] ms = ourClass.getDeclaredMethods();
        XposedBridge.log("Class: " + className);
        XposedBridge.log("Methods:");

        for(Method m : ms)
        {
            XposedBridge.log(m.getName() + " - " + m.getReturnType() + " - " + m.getParameterCount());
            Class<?>[] cs = m.getParameterTypes();
            for(Class<?> c: cs)
            {
                XposedBridge.log("\t\t" + c.getTypeName());
            }
        }
        XposedBridge.log("Fields:");

        Field[] fs = ourClass.getDeclaredFields();
        for(Field f: fs)
        {
            XposedBridge.log("\t\t" + f.getName() + "-" + f.getType().getName());
        }
        XposedBridge.log("End dump");
    }

    public static void getActiveOverlays(){
        List<String> result = new ArrayList<>();
        List<String> lines = com.topjohnwu.superuser.Shell.cmd("cmd overlay list --user 0").exec().getOut();
        //List<String> lines = Shell.sh("cmd overlay list --user 0").exec().getOut();
        for(String thisLine : lines)
        {
            if(thisLine.startsWith("[x]"))
            {
                result.add(thisLine.replace("[x] ", ""));
            }
        }
        activeOverlays = result;
    }

    public static void setOverlay(String Key, boolean enabled, boolean refresh) {
        if(refresh) getActiveOverlays();
        setOverlay(Key, enabled);
    }

    public static void setOverlay(String Key, boolean enabled) {
        if(AOSPMods.isSecondProcess) return;
    
        if(activeOverlays == null) getActiveOverlays(); //make sure we have a list in hand

        String mode = (enabled) ? "enable" : "disable";
        String packname;
//        boolean exclusive = false;

        if(Key.endsWith("Overlay")) {
            Overlays.overlayProp op = (Overlays.overlayProp) Overlays.Overlays.get(Key);
            //noinspection ConstantConditions
            packname = op.name;
//            exclusive = op.exclusive;
        }
        else if(Key.endsWith("OverlayG")) //It's a group of overlays to work together as a team
        {
            try {
                setOverlayGroup(Key, enabled);
            }catch (Exception ignored){}
            return;
        }
        else
        {
            packname = Key;
//            exclusive = true;
        }

/*        if (enabled && exclusive) {
            mode += "-exclusive"; //since we are checking all overlays, we don't need exclusive anymore.
        }*/

        boolean wasEnabled = (activeOverlays.contains(packname));

        if(enabled == wasEnabled)
        {
            return; //nothing to do. We're already set
        }

        try {
            com.topjohnwu.superuser.Shell.cmd("cmd overlay " + mode + " --user 0 " + packname).exec();
        }
        catch(Throwable t)
        {
            t.printStackTrace();
        }
    }

    private static void setOverlayGroup(String key, boolean enabled) {
        Overlays.overlayGroup thisGroup = (Overlays.overlayGroup) Overlays.Overlays.get(key);

        //noinspection ConstantConditions
        for(Overlays.overlayProp thisProp : thisGroup.members)
        {
            Helpers.setOverlay(thisProp.name, enabled);
        }
    }

    public static SpannableStringBuilder getHumanizedBytes(long bytes, float unitSizeFactor, String unitSpearator, String indicatorSymbol, @Nullable @ColorInt Integer textColor)
    {
        DecimalFormat decimalFormat;
        CharSequence formattedData;
        SpannableString spanSizeString;
        SpannableString spanUnitString;
        String unit;
        if (bytes >= GB) {
            unit = "GB";
            decimalFormat = new DecimalFormat("0.00");
            formattedData =  decimalFormat.format(bytes / (float)GB);
        } else if (bytes >= 100 * MB) {
            decimalFormat = new DecimalFormat("000");
            unit = "MB";
            formattedData =  decimalFormat.format(bytes / (float)MB);
        } else if (bytes >= 10 * MB) {
            decimalFormat = new DecimalFormat("00.0");
            unit = "MB";
            formattedData =  decimalFormat.format(bytes / (float)MB);
        } else if (bytes >= MB) {
            decimalFormat = new DecimalFormat("0.00");
            unit = "MB";
            formattedData =  decimalFormat.format(bytes / (float)MB);
        } else if (bytes >= 100 * KB) {
            decimalFormat = new DecimalFormat("000");
            unit = "KB";
            formattedData =  decimalFormat.format(bytes / (float)KB);
        } else if (bytes >= 10 * KB) {
            decimalFormat = new DecimalFormat("00.0");
            unit = "KB";
            formattedData =  decimalFormat.format(bytes / (float)KB);
        } else {
            decimalFormat = new DecimalFormat("0.00");
            unit = "KB";
            formattedData = decimalFormat.format(bytes / (float)KB);
        }
        spanSizeString = new SpannableString(formattedData);

        if(textColor != null)
        {
            spanSizeString.setSpan(new NetworkTraffic.trafficStyle(textColor), 0 , (formattedData).length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        spanUnitString = new SpannableString(unit + indicatorSymbol);
        spanUnitString.setSpan(new RelativeSizeSpan(unitSizeFactor), 0, (unit + indicatorSymbol).length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return new SpannableStringBuilder().append(spanSizeString).append(unitSpearator).append(spanUnitString);

    }
}
