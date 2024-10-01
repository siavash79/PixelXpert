package sh.siava.pixelxpert.modpacks.utils.toolkit;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;

import androidx.annotation.ColorInt;

public class ColorUtils {
	@ColorInt
	public static int getColorAttrDefaultColor(Context context, int attr) {
		return getColorAttrDefaultColor(context, attr, 0);
	}

	/**
	 * Get color styled attribute {@code attr}, default to {@code defValue} if not found.
	 */
	@ColorInt
	public static int getColorAttrDefaultColor(Context context, int attr, @ColorInt int defValue) {
		TypedArray ta = context.obtainStyledAttributes(new int[]{attr});
		@ColorInt int colorAccent = ta.getColor(0, defValue);
		ta.recycle();
		return colorAccent;
	}

	public static ColorStateList getColorAttr(Context context, int attr) {
		TypedArray ta = context.obtainStyledAttributes(new int[]{attr});
		//noinspection ReassignedVariable,UnusedAssignment
		ColorStateList stateList = null;
		try {
			stateList = ta.getColorStateList(0);
		} finally {
			ta.recycle();
		}
		return stateList;
	}

	public static int compositeAlpha(int foregroundAlpha, int backgroundAlpha) {
		return 0xFF - (((0xFF - backgroundAlpha) * (0xFF - foregroundAlpha)) / 0xFF);
	}
}
