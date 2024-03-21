package sh.siava.pixelxpert.modpacks.utils.toolkit;

import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import sh.siava.pixelxpert.modpacks.utils.NetworkTraffic;

/** @noinspection unused, RedundantThrows */
@SuppressWarnings("CommentedOutCode")
public class ObjectTools {
	private static final int KILO = 1024;
	private static final int MEGA = 1024 * KILO;
	private static final int GIGA = 1024 * MEGA;


	public static int tryParseInt(String string, int fallbackResult) {
		try {
			return Integer.parseInt(string);
		} catch (Exception ignored) {
			return fallbackResult;
		}
	}


	static <T> Stream<T> concatArrays(T[] array1, T[] array2) {
		return Stream.concat(Arrays.stream(array1), Arrays.stream(array2)).distinct();
	}


	public static SpannableStringBuilder getHumanizedBytes(long bytes, boolean showInBits, float unitSizeFactor, String unitSeparator, String indicatorSymbol, @Nullable @ColorInt Integer textColor) {
		if(showInBits)
		{
			bytes *= 8;
		}

		DecimalFormat decimalFormat;
		CharSequence formattedData;
		SpannableString spanSizeString;
		SpannableString spanUnitString;
		String unit;
		if (bytes >= GIGA) {
			unit = showInBits ? "Gb" : "GB";
			decimalFormat = new DecimalFormat("0.00");
			formattedData = decimalFormat.format(bytes / (float) GIGA);
		} else if (bytes >= 100 * MEGA) {
			decimalFormat = new DecimalFormat("000");
			unit = showInBits ? "Mb" : "MB";
			formattedData = decimalFormat.format(bytes / (float) MEGA);
		} else if (bytes >= 10 * MEGA) {
			decimalFormat = new DecimalFormat("00.0");
			unit = showInBits ? "Mb" : "MB";
			formattedData = decimalFormat.format(bytes / (float) MEGA);
		} else if (bytes >= MEGA) {
			decimalFormat = new DecimalFormat("0.00");
			unit = showInBits ? "Mb" : "MB";
			formattedData = decimalFormat.format(bytes / (float) MEGA);
		} else if (bytes >= 100 * KILO) {
			decimalFormat = new DecimalFormat("000");
			unit = showInBits ? "Kb" : "KB";
			formattedData = decimalFormat.format(bytes / (float) KILO);
		} else if (bytes >= 10 * KILO) {
			decimalFormat = new DecimalFormat("00.0");
			unit = showInBits ? "Kb" : "KB";
			formattedData = decimalFormat.format(bytes / (float) KILO);
		} else {
			decimalFormat = new DecimalFormat("0.00");
			unit = showInBits ? "Kb" : "KB";
			formattedData = decimalFormat.format(bytes / (float) KILO);
		}
		spanSizeString = new SpannableString(formattedData);

		if (textColor != null) {
			spanSizeString.setSpan(new NetworkTraffic.TrafficStyle(textColor), 0, (formattedData).length(),
					Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		spanUnitString = new SpannableString(unit + indicatorSymbol);
		spanUnitString.setSpan(new RelativeSizeSpan(unitSizeFactor), 0, (unit + indicatorSymbol).length(),
				Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		return new SpannableStringBuilder().append(spanSizeString).append(unitSeparator).append(spanUnitString);
	}


	public static boolean isColorDark(@ColorInt int color) {
		return ColorUtils.calculateLuminance(color) < 0.5;
	}


	public static String removeItemFromCommaString(String string, String key)
	{
		return string.replaceAll(getCommaSearchPattern(key), "$2$3$5");
	}

	public static String addItemToCommaStringIfNotPresent(String string, String key)
	{
		if(Pattern.matches(getCommaSearchPattern(key), string)) return string;

		return String.format("%s%s%s", key, !string.isEmpty() ? "," : "", string);
	}
	private static String getCommaSearchPattern(String tile) {
		return String.format("^(%s,)(.+)|(.+)(,%s)(,.+|$)", tile, tile);
	}


}
