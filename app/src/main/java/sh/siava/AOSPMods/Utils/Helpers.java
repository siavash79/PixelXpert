package sh.siava.AOSPMods.Utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;

public class Helpers {

    public static List<String> activeOverlays = null;

    public static void dumpClass(String className, XC_LoadPackage.LoadPackageParam lpparam){
        Class ourClass = XposedHelpers.findClassIfExists(className, lpparam.classLoader);
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
            Class[] cs = m.getParameterTypes();
            for(Class c: cs)
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
        boolean exclusive = false;

        if(Key.endsWith("Overlay")) {
            Overlays.overlayProp op = (Overlays.overlayProp) Overlays.Overlays.get(Key);
            packname = op.name;
            exclusive = op.exclusive;
        }
        else if(Key.endsWith("OverlayG")) //It's a group of overlays to work together as a team
        {
            try {
                setOverlayGroup(Key, enabled);
            }catch (Exception ignored){}
            finally {
                return;
            }
        }
        else
        {
            packname = Key;
            exclusive = true;
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

        for(Overlays.overlayProp thisProp : thisGroup.members)
        {
            Helpers.setOverlay(thisProp.name, enabled);
        }
    }
}
