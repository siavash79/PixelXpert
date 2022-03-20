package sh.siava.AOSPMods;

import androidx.annotation.Nullable;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class Helpers {
    @Nullable
    public static XC_MethodHook.Unhook findAndHookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback)
    {
        Class clazz = XposedHelpers.findClassIfExists(className, classLoader);
        if(clazz == null) return null;
        return findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
    }

    @Nullable
    public static XC_MethodHook.Unhook findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        if (parameterTypesAndCallback.length == 0 || !(parameterTypesAndCallback[parameterTypesAndCallback.length-1] instanceof XC_MethodHook))
            throw new IllegalArgumentException("no callback defined");

        XC_MethodHook callback = (XC_MethodHook) parameterTypesAndCallback[parameterTypesAndCallback.length-1];
        Method m = XposedHelpers.findMethodExactIfExists(clazz, methodName, getParameterClasses(clazz.getClassLoader(), parameterTypesAndCallback));

        if(m == null) return null;

        return XposedBridge.hookMethod(m, callback);
    }

    public static Class<?> findClass(String className, ClassLoader classLoader)
    {
        return XposedHelpers.findClassIfExists(className, classLoader);
    }

    public static Method findMethod(Class<?> clazz, String methodName, Object... parameterTypes)
    {
        return XposedHelpers.findMethodExactIfExists(clazz, methodName, parameterTypes);
    }

    @Nullable
    public static Method findMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypes)
    {
        Class clazz = findClass(className, classLoader);
        if(clazz == null) return null;
        return findMethod(clazz, methodName, parameterTypes);
    }

    private static Class<?>[] getParameterClasses(ClassLoader classLoader, Object[] parameterTypesAndCallback) {
        Class<?>[] parameterClasses = null;
        for (int i = parameterTypesAndCallback.length - 1; i >= 0; i--) {
            Object type = parameterTypesAndCallback[i];
            if (type == null)
                throw new NullPointerException();

            // ignore trailing callback
            if (type instanceof XC_MethodHook)
                continue;

            if (parameterClasses == null)
                parameterClasses = new Class<?>[i+1];

            if (type instanceof Class)
                parameterClasses[i] = (Class<?>) type;
            else if (type instanceof String)
                parameterClasses[i] = XposedHelpers.findClass((String) type, classLoader);
            else
                throw new NullPointerException();
        }

        // if there are no arguments for the method
        if (parameterClasses == null)
            parameterClasses = new Class<?>[0];

        return parameterClasses;
    }

}
