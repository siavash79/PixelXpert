package sh.siava.AOSPMods.modpacks.utils.batteryStyles;

import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public abstract class BatteryDrawable extends Drawable {

	public abstract void setShowPercent(boolean showPercent);

	public abstract void setMeterStyle(int batteryStyle);

	public abstract void setFastCharging(boolean isFastCharging);

	public abstract void setCharging(boolean isCharging);

	public abstract void setBatteryLevel(int mLevel);

	public abstract void setColors(int fgColor, int bgColor, int singleToneColor);

	public abstract void setPowerSaving(boolean isPowerSaving);

	public abstract void refresh();


	public static boolean colorful;
	public static List<Float> batteryLevels = new ArrayList<>();
	public static int[] batteryColors;
	public static boolean showCharging = false;
	public static boolean showFastCharging = false;
	public static int chargingColor = 0;
	public static int fastChargingColor = 0;
	public static boolean transitColors = false;

	public static long lastVarUpdate = -1;

	public static void setStaticColor(List<Float> batteryLevels, int[] batteryColors, boolean indicateCharging, int chargingColor, boolean indicateFastCharging, int fastChargingColor, boolean transitColors, boolean colorful) {
		BatteryDrawable.batteryColors = batteryColors;
		BatteryDrawable.batteryLevels = batteryLevels;
		BatteryDrawable.showCharging = indicateCharging;
		BatteryDrawable.showFastCharging = indicateFastCharging;
		BatteryDrawable.chargingColor = chargingColor;
		BatteryDrawable.fastChargingColor = fastChargingColor;
		BatteryDrawable.transitColors = transitColors;
		BatteryDrawable.colorful = colorful;

		lastVarUpdate = Calendar.getInstance().getTime().getTime();
	}
}
