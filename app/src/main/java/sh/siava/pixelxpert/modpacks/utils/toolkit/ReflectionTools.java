package sh.siava.pixelxpert.modpacks.utils.toolkit;

import static de.robv.android.xposed.XposedBridge.hookMethod;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static sh.siava.pixelxpert.modpacks.utils.toolkit.ObjectTools.concatArrays;

import android.util.ArraySet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** @noinspection unused, RedundantThrows */
public class ReflectionTools {
	@NonNull
	public static Class<?> findAndDumpClass(String className, ClassLoader classLoader)
	{
		dumpClass(className, classLoader);
		return findClass(className, classLoader);
	}

	public static Class<?> findAndDumpClassIfExists(String className, ClassLoader classLoader)
	{
		dumpClass(className, classLoader);
		return findClassIfExists(className, classLoader);
	}

	public static Set<XC_MethodHook.Unhook> hookAllMethods(Class<?> clazz, String method, XC_MethodHook callback)
	{
		XC_MethodHook hook = new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if(false) {
					log(param.method.getName() + " called");
				}
				callMethod(callback, "beforeHookedMethod", param);
			}

			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				callMethod(callback, "afterHookedMethod", param);
			}
		};

		Set<XC_MethodHook.Unhook> result = XposedBridge.hookAllMethods(clazz, method, hook);
		if(false) {
			Throwable t = new Throwable();
			log(t.getStackTrace()[1].getClassName() + " " + t.getStackTrace()[1].getLineNumber() + " hook size " + result.size());
		}
		return result;
	}

	public static Set<XC_MethodHook.Unhook> hookAllConstructors(Class<?> clazz, XC_MethodHook callback)
	{
		Set<XC_MethodHook.Unhook> result = XposedBridge.hookAllConstructors(clazz, callback);
		if(false) {
			Throwable t = new Throwable();
			log(t.getStackTrace()[1].getClassName() + " " + t.getStackTrace()[1].getLineNumber() + " hook size " + result.size());
		}
		return result;
	}

	public static Set<XC_MethodHook.Unhook> hookAllMethodsMatchPattern(Class<?> clazz, String namePatter, XC_MethodHook callback)
	{
		Set<XC_MethodHook.Unhook> result = new ArraySet<>();

		for(Method method : findMethods(clazz, namePatter))
		{
			result.add(hookMethod(method, callback));
		}
		return result;
	}

	public static Set<Method> findMethods(Class<?> clazz, String namePattern)
	{
		Set<Method> result = new ArraySet<>();

		Method[] methods = clazz.getMethods();

		for(Method method : methods)
		{
			if(Pattern.matches(namePattern, method.getName()))
			{
				result.add(method);
			}
		}
		return result;
	}

	public static Method findMethod(Class<?> clazz, String namePattern)
	{
		Method[] methods = clazz.getMethods();

		for(Method method : methods)
		{
			if(Pattern.matches(namePattern, method.getName()))
			{
				return method;
			}
		}
		return null;
	}

	@SuppressWarnings("unused")
	public static void dumpClass(String className, ClassLoader classLoader)
	{
		Class<?> ourClass = findClassIfExists(className, classLoader);
		if (ourClass == null) {
			log("Class: " + className + " not found");
			return;
		}
		dumpClass(ourClass);
	}

	public static void dumpClass(Class<?> ourClass) {
		Method[] ms = ourClass.getDeclaredMethods();
		log("Class: " + ourClass.getName());
		log("extends: " + ourClass.getSuperclass().getName());
		log("Subclasses:");
		Class<?>[] scs = ourClass.getClasses();
		for(Class <?> c : scs)
		{
			log(c.getName());
		}
		log("Methods:");

		Constructor<?>[] cons = ourClass.getDeclaredConstructors();
		for (Constructor<?> m : cons) {
			log(m.getName() + " - " + " - " + m.getParameterCount());
			Class<?>[] cs = m.getParameterTypes();
			for (Class<?> c : cs) {
				log("\t\t" + c.getTypeName());
			}
		}


		for (Method m : ms) {
			log(m.getName() + " - " + m.getReturnType() + " - " + m.getParameterCount());
			Class<?>[] cs = m.getParameterTypes();
			for (Class<?> c : cs) {
				log("\t\t" + c.getTypeName());
			}
		}
		log("Fields:");

		Field[] fs = ourClass.getDeclaredFields();
		for (Field f : fs) {
			log("\t\t" + f.getName() + "-" + f.getType().getName());
		}
		log("End dump");
	}

	public static Method findFirstMethodByName(Class<?> targetClass, String methodName)
	{
		return concatArrays(
				targetClass.getMethods(),
				targetClass.getDeclaredMethods())
				.filter(m -> m.getName().contains(methodName))
				.findFirst()
				.orElse(null);
	}

	public static void tryHookAllMethods(Class<?> clazz, String method, XC_MethodHook hook) {
		try {
			XposedBridge.hookAllMethods(clazz, method, hook);
		} catch (Throwable ignored) {
		}
	}

	public static void tryHookAllConstructors(Class<?> clazz, XC_MethodHook hook) {
		try {
			XposedBridge.hookAllConstructors(clazz, hook);
		} catch (Throwable ignored) {
		}
	}

	public static void dumpIDs(View v)
	{
		dumpIDs(v, 0);
	}

	private static void dumpIDs(View v, int level)
	{
		dumpID(v, level);
		if(v instanceof ViewGroup)
		{
			for(int i = 0; i < ((ViewGroup) v).getChildCount(); i++)
			{
				dumpIDs(((ViewGroup) v).getChildAt(i), level+1);
			}
		}
	}
	private static void dumpID(View v, int level)
	{
		try {
			StringBuilder str = new StringBuilder();
			for(int i = 0; i < level; i++)
			{
				str.append("\t");
			}
			String name = v.getContext().getResources().getResourceName(v.getId());
			log(str+ "id " + name + " type " + v.getClass().getName());
		}
		catch (Throwable ignored){}
	}

}
