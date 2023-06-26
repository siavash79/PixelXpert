package sh.siava.AOSPMods.modpacks.utils;

import static de.robv.android.xposed.XposedBridge.log;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.text.SpannableStringBuilder;

import androidx.annotation.NonNull;

import com.github.mfathi91.time.PersianDate;

import java.awt.font.NumericShaper;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.callback.Callback;

import sh.siava.AOSPMods.BuildConfig;
import sh.siava.AOSPMods.modpacks.systemui.ThermalProvider;

public class StringFormatter {
	private static final ArrayList<StringFormatter> instances = new ArrayList<>();
	private final ArrayList<FormattedStringCallback> callbacks = new ArrayList<>();
	private boolean hasDate = false;
	private final NetworkStats.networkStatCallback networkStatCallback = stats -> informCallbacks();
	public static Integer RXColor, TXColor;

	private Timer scheduler = new Timer();

	public StringFormatter() {
		instances.add(this);
		scheduleNextDateUpdate();
	}

	public static void refreshAll() {
		instances.forEach(StringFormatter::informCallbacks);
	}

	private void informCallbacks() {
		for (FormattedStringCallback callback : callbacks) {
			callback.onRefreshNeeded();
		}
	}

	private void scheduleNextDateUpdate() {
		try {
			AlarmManager alarmManager = SystemUtils.AlarmManager();

			Calendar alarmTime = Calendar.getInstance();
			alarmTime.set(Calendar.HOUR_OF_DAY, 0);
			alarmTime.set(Calendar.MINUTE, 0);
			alarmTime.add(Calendar.DATE, 1);

			//noinspection ConstantConditions
			alarmManager.set(AlarmManager.RTC,
					alarmTime.getTimeInMillis(),
					"",
					() -> {
						scheduleNextDateUpdate();
						if (hasDate) {
							informCallbacks();
						}
					},
					null);

		} catch (Throwable t) {
			if (BuildConfig.DEBUG) {
				log("Error setting formatted string update schedule");
				t.printStackTrace();
			}
		}
	}

	public CharSequence formatString(String input) {
		SpannableStringBuilder result = new SpannableStringBuilder(input);
		hasDate = false;
		Pattern pattern = Pattern.compile("\\$((T[a-zA-Z][0-9]*)|([A-Z][A-Za-z]+))"); //variables start with $ and continue with characters, until they don't!

		//We'll locate each variable and replace it with a value, if possible
		Matcher matcher = pattern.matcher(input);
		while (matcher.find()) {
			String match = matcher.group(1);

			int start = result.toString().indexOf("$" + match);
			//noinspection ConstantConditions
			result.replace(start, start + match.length() + 1, valueOf(match));
		}
		return result;
	}

	private CharSequence valueOf(String match) {
		if (match.startsWith("P")) //P is reserved for "Persian Calendar". Then goes normal Java dateformat, like $Pdd or $Pyyyy
		{
			return persianDateOf(match.substring(1));
		}
		else if (match.startsWith("G")) //G is reserved for "Georgian Calendar". Then goes normal Java dateformat, like $Gyyyy or $GEEE
		{
			return georgianDateOf(match.substring(1));
		}
		else if (match.startsWith("N")) {
			return networkStatOf(match.substring(1));
		}
		else if(match.startsWith("T"))
		{
			return temperatureOf(match.substring(1));
		}
		return "$" + match;
	}

	@SuppressWarnings("ConstantConditions")
	private CharSequence networkStatOf(String variable) {
		if (!SystemUtils.NetworkStats().isEnabled()) {
			return "$N" + variable;
		}
		long traffic = 0;
		Integer textColor = null;
		variable = variable.toLowerCase();
		CharSequence transformed = null;
		try {
			switch (variable) {
				case "crx":
				case "rx":
					textColor = RXColor;
					traffic = SystemUtils.NetworkStats().getTodayDownloadBytes(variable.startsWith("c"));
					break;
				case "ctx":
				case "tx":
					textColor = TXColor;
					traffic = SystemUtils.NetworkStats().getTodayUploadBytes(variable.startsWith("c"));
					break;
				case "call":
				case "all":
					traffic = SystemUtils.NetworkStats().getTodayDownloadBytes(variable.startsWith("c"))
							+ SystemUtils.NetworkStats().getTodayUploadBytes(variable.startsWith("c"));
					break;
				case "ssid":
					transformed = SystemUtils.NetworkStats().getSSIDName();
					break;
			}
			SystemUtils.NetworkStats().registerCallback(networkStatCallback);
			if (transformed == null) {
				transformed = Helpers.getHumanizedBytes(traffic, .6f, "", "", textColor);
			}
			return transformed;
		} catch (Exception ignored) {
			return "$N" + variable;
		}
	}

	private CharSequence georgianDateOf(String format) {
		try {
			@SuppressLint("SimpleDateFormat")
			String result = new SimpleDateFormat(format).
					format(
							Calendar.getInstance().getTime()
					);
			hasDate = true;
			return result;
		} catch (Exception ignored) {
			return "$G" + format;
		}
	}

	private CharSequence temperatureOf(String format)
	{
		try
		{
			Matcher match = Pattern.compile("^([A-Za-z])([0-9]*)$").matcher(format);

			if(!match.find())
			{
				throw new Exception();
			}
			String typeStr = match.group(1);

			long nextUpdate;

			try
			{
				//noinspection ConstantConditions
				nextUpdate = Integer.parseInt(match.group(2));
			}
			catch (Throwable ignored)
			{
				nextUpdate = 60;
			}

			nextUpdate *= 1000L;

			int type;

			//noinspection ConstantConditions
			switch (typeStr.toLowerCase())
			{
				case "b":
					type = ThermalProvider.BATTERY;
					break;
				case "c":
					type = ThermalProvider.CPU;
					break;
				case "g":
					type = ThermalProvider.GPU;
					break;
				case "s":
					type = ThermalProvider.SKIN;
					break;
				default:
					throw new Exception();
			}

			int temperature = ThermalProvider.getTemperatureMaxInt(type);

			if(temperature < -990)
			{
				scheduleUpdate(1000L);
				return "Err";
			}

			scheduleUpdate(nextUpdate);

			return String.valueOf(temperature);

		} catch (Exception ignored)
		{
			return "$T" + format;
		}
	}

	private void scheduleUpdate(long nextUpdate) {
		scheduler.cancel();

		scheduler = new Timer();
		scheduler.schedule(new TimerTask() {
			@Override
			public void run() {
				informCallbacks();
			}
		}, nextUpdate);
	}

	private CharSequence persianDateOf(String format) {
		try {
			String result = PersianDate.now().format(
					DateTimeFormatter.ofPattern(
							format,
							Locale.forLanguageTag("fa")
					)
			);
			hasDate = true;
			char[] bytes = result.toCharArray();
			NumericShaper.getShaper(NumericShaper.EASTERN_ARABIC).shape(bytes, 0, bytes.length); //Numbers to be shown in correct font
			return String.copyValueOf(bytes);
		} catch (Exception ignored) {
			return "$P" + format;
		}
	}

	public void registerCallback(@NonNull FormattedStringCallback callback) {
		callbacks.add(callback);
	}

	@SuppressWarnings("unused")
	public void unRegisterCallback(@NonNull FormattedStringCallback callback) {
		callbacks.remove(callback);
	}

	@SuppressWarnings("unused")
	public void resetCallbacks() {
		callbacks.clear();
	}

	public interface FormattedStringCallback extends Callback {
		void onRefreshNeeded();
	}
}