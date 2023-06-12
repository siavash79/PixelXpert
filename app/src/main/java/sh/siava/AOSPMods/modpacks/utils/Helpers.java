package sh.siava.AOSPMods.modpacks.utils;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;

import android.os.FileUtils;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.topjohnwu.superuser.Shell;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import de.robv.android.xposed.XC_MethodHook;
import sh.siava.AOSPMods.modpacks.XPLauncher;

@SuppressWarnings("CommentedOutCode")
public class Helpers {
	private static final int KB = 1024;
	private static final int MB = 1024 * KB;
	private static final int GB = 1024 * MB;

	public static List<String> activeOverlays = null;

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

	public static void getActiveOverlays() {
		List<String> result = new ArrayList<>();
		List<String> lines = com.topjohnwu.superuser.Shell.cmd("cmd overlay list --user 0").exec().getOut();
		//List<String> lines = Shell.sh("cmd overlay list --user 0").exec().getOut();
		for (String thisLine : lines) {
			if (thisLine.startsWith("[x]")) {
				result.add(thisLine.replace("[x] ", ""));
			}
		}
		activeOverlays = result;
	}

	public static void setOverlay(String Key, boolean enabled, boolean refresh, boolean force) {
		if (refresh) getActiveOverlays();
		setOverlay(Key, enabled, force);
	}

	public static void setOverlay(String Key, boolean enabled, boolean force) {
		if (XPLauncher.isChildProcess) return;

		if (activeOverlays == null) getActiveOverlays(); //make sure we have a list in hand

		String mode = (enabled) ? "enable" : "disable";
		String packname;
//        boolean exclusive = false;

		if (Key.endsWith("Overlay")) {
			Overlays.overlayProp op = (Overlays.overlayProp) Overlays.Overlays.get(Key);
			//noinspection ConstantConditions
			packname = op.name;
//            exclusive = op.exclusive;
		} else if (Key.endsWith("OverlayG")) //It's a group of overlays to work together as a team
		{
			try {
				setOverlayGroup(Key, enabled, force);
			} catch (Exception ignored) {
			}
			return;
		} else {
			packname = Key;
//            exclusive = true;
		}

/*        if (enabled && exclusive) {
            mode += "-exclusive"; //since we are checking all overlays, we don't need exclusive anymore.
        }*/

		boolean wasEnabled = (activeOverlays.contains(packname));

		if (enabled == wasEnabled && !force) {
			return; //nothing to do. We're already set
		}

		try {
			com.topjohnwu.superuser.Shell.cmd("cmd overlay " + mode + " --user 0 " + packname).exec();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private static void setOverlayGroup(String key, boolean enabled, boolean force) {
		Overlays.overlayGroup thisGroup = (Overlays.overlayGroup) Overlays.Overlays.get(key);

		//noinspection ConstantConditions
		for (Overlays.overlayProp thisProp : thisGroup.members) {
			Helpers.setOverlay(thisProp.name, enabled, force);
		}
	}

	public static SpannableStringBuilder getHumanizedBytes(long bytes, float unitSizeFactor, String unitSeparator, String indicatorSymbol, @Nullable @ColorInt Integer textColor) {
		DecimalFormat decimalFormat;
		CharSequence formattedData;
		SpannableString spanSizeString;
		SpannableString spanUnitString;
		String unit;
		if (bytes >= GB) {
			unit = "GB";
			decimalFormat = new DecimalFormat("0.00");
			formattedData = decimalFormat.format(bytes / (float) GB);
		} else if (bytes >= 100 * MB) {
			decimalFormat = new DecimalFormat("000");
			unit = "MB";
			formattedData = decimalFormat.format(bytes / (float) MB);
		} else if (bytes >= 10 * MB) {
			decimalFormat = new DecimalFormat("00.0");
			unit = "MB";
			formattedData = decimalFormat.format(bytes / (float) MB);
		} else if (bytes >= MB) {
			decimalFormat = new DecimalFormat("0.00");
			unit = "MB";
			formattedData = decimalFormat.format(bytes / (float) MB);
		} else if (bytes >= 100 * KB) {
			decimalFormat = new DecimalFormat("000");
			unit = "KB";
			formattedData = decimalFormat.format(bytes / (float) KB);
		} else if (bytes >= 10 * KB) {
			decimalFormat = new DecimalFormat("00.0");
			unit = "KB";
			formattedData = decimalFormat.format(bytes / (float) KB);
		} else {
			decimalFormat = new DecimalFormat("0.00");
			unit = "KB";
			formattedData = decimalFormat.format(bytes / (float) KB);
		}
		spanSizeString = new SpannableString(formattedData);

		if (textColor != null) {
			spanSizeString.setSpan(new NetworkTraffic.trafficStyle(textColor), 0, (formattedData).length(),
					Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		spanUnitString = new SpannableString(unit + indicatorSymbol);
		spanUnitString.setSpan(new RelativeSizeSpan(unitSizeFactor), 0, (unit + indicatorSymbol).length(),
				Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		return new SpannableStringBuilder().append(spanSizeString).append(unitSeparator).append(spanUnitString);
	}



	public static void tryHookAllMethods(Class<?> clazz, String method, XC_MethodHook hook) {
		try {
			hookAllMethods(clazz, method, hook);
		} catch (Throwable ignored) {
		}
	}

	public static void tryHookAllConstructors(Class<?> clazz, XC_MethodHook hook) {
		try {
			hookAllConstructors(clazz, hook);
		} catch (Throwable ignored) {
		}
	}

	public static String removeItemFromCommaString(String string, String key)
	{
		return string.replaceAll(getCommaSearchPattern(key), "$2$3$5");
	}

	public static String addItemToCommaStringIfNotPresent(String string, String key)
	{
		if(Pattern.matches(getCommaSearchPattern(key), string)) return string;

		return String.format("%s%s%s", key, (string.length() > 0) ? "," : "", string);
	}
	private static String getCommaSearchPattern(String tile) {
		return String.format("^(%s,)(.+)|(.+)(,%s)(,.+|$)", tile, tile);
	}


}
