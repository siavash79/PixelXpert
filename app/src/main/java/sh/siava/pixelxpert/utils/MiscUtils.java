package sh.siava.pixelxpert.utils;

import android.content.Context;
import android.util.TypedValue;

import androidx.annotation.ColorInt;

public class MiscUtils {

	public static @ColorInt int getColorFromAttribute(Context context, int attr) {
		TypedValue typedValue = new TypedValue();
		context.getTheme().resolveAttribute(attr, typedValue, true);
		return typedValue.data;
	}

	public static String intToHex(int colorValue) {
		return String.format("#%06X", (0xFFFFFF & colorValue));
	}
}
