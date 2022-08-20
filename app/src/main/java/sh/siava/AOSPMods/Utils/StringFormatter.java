package sh.siava.AOSPMods.Utils;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.callback.Callback;

import sh.siava.AOSPMods.BuildConfig;

public class StringFormatter {
    private static final ArrayList<StringFormatter> instances = new ArrayList<>();
    private final ArrayList<formattedStringCallback> callbacks = new ArrayList<>();
    private boolean hasDate = false;
    private final NetworkStats.networkStatCallback networkStatCallback = stats -> informCallbacks();
    public static Integer RXColor, TXColor;

    public StringFormatter()
    {
        instances.add(this);
        scheduleNextDateUpdate();
    }

    public static void refreshAll()
    {
        instances.forEach(StringFormatter::informCallbacks);
    }

    private void informCallbacks() {
        for(formattedStringCallback callback : callbacks)
        {
            callback.onRefreshNeeded();
        }
    }

    private void scheduleNextDateUpdate()
    {
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
                        if(hasDate)
                        {
                            informCallbacks();
                        }
                    },
                    null);

        }
        catch (Throwable t){
            if(BuildConfig.DEBUG)
            {
                log("Error setting formatted string update schedule");
                t.printStackTrace();
            }
        }
    }

    public CharSequence formatString(String input)
    {
        SpannableStringBuilder result = new SpannableStringBuilder(input);
        hasDate = false;
        Pattern pattern = Pattern.compile("\\$([a-zA-Z]+)"); //variables start with $ and continue with characters, until they don't!

        //We'll locate each variable and replace it with a value, if possible
        Matcher matcher = pattern.matcher(input);
        while (matcher.find())
        {
            String match = matcher.group(1);

            int start = result.toString().indexOf("$"+match);
            result.replace(start, start+match.length()+1, valueOf(match));
        }
        return  result;
    }

    private CharSequence valueOf(String match) {
        if(match.startsWith("P")) //P is reserved for "Persian Calendar". Then goes normal Java dateformat, like $Pdd or $Pyyyy
        {
            return persianDateOf(match.substring(1));
        }
        else if(match.startsWith("G")) //G is reserved for "Georgian Calendar". Then goes normal Java dateformat, like $Gyyyy or $GEEE
        {
            return georgianDateOf(match.substring(1));
        }
        if(match.startsWith("N"))
        {
            return networkStatOf(match.substring(1));
        }
        return match;
    }

    @SuppressWarnings("ConstantConditions")
    private CharSequence networkStatOf(String variable) {
        if(!SystemUtils.NetworkStats().isEnabled())
        {
            return "N"+variable;
        }
        long traffic = 0;
        Integer textColor = null;
        variable = variable.toLowerCase();
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
            }
            SystemUtils.NetworkStats().registerCallback(networkStatCallback);
            return Helpers.getHumanizedBytes(traffic, .6f, "","", textColor);
        }
        catch (Exception ignored)
        {
            return "N"+variable;
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
        }catch (Exception ignored)
        {
            return "G"+format;
        }
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
        }catch (Exception ignored)
        {
            return "P"+format;
        }
    }

    public void registerCallback(@NonNull formattedStringCallback callback)
    {
        callbacks.add(callback);
    }

    @SuppressWarnings("unused")
    public void unRegisterCallback(@NonNull formattedStringCallback callback)
    {
        callbacks.remove(callback);
    }

    @SuppressWarnings("unused")
    public void resetCallbacks()
    {
        callbacks.clear();
    }

    public interface formattedStringCallback extends Callback {
        void onRefreshNeeded();
    }
}