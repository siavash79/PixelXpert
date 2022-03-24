package sh.siava.AOSPMods.Utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Helpers {

    public static void dumpClass(String className, XC_LoadPackage.LoadPackageParam lpparam)
    {
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
}
