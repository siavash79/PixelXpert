package sh.siava.AOSPMods.Utils;

import android.graphics.drawable.Drawable;

import java.util.Calendar;

public abstract class IBatteryDrawable extends Drawable {
	
	public abstract void setShowPercent(boolean showPercent);
	public abstract void setMeterStyle(int batteryStyle);
	public abstract void setFastCharging(boolean isFastCharging);
	public abstract void setCharging(boolean isCharging);
	public abstract void setBatteryLevel(int mLevel);
	public abstract void setColors(int fgColor, int bgColor, int singleToneColor);
	public abstract void setPowerSaveEnabled(boolean isPowerSaving);
	
	public static boolean colorful;
	public static float[] batteryLevels = new float[0];
	public static int[] batteryColors;
	public static boolean showCharging = false;
	public static boolean showFastCharing = false;
	public static int chargingColor = 0;
	public static int fastChargingColor = 0;
	public static boolean transitColors = false;
	
	public static long lastVarUpdate = -1;
	
	public static void setStaticColor(float[] batteryLevels, int[] batteryColors, boolean indicateCharging, int charingColor, boolean indicateFastCharging, int fastChargingColor, boolean transitColors, boolean colorful) {
		IBatteryDrawable.batteryColors = batteryColors;
		IBatteryDrawable.batteryLevels = batteryLevels;
		IBatteryDrawable.showCharging = indicateCharging;
		IBatteryDrawable.showFastCharing = indicateFastCharging;
		IBatteryDrawable.chargingColor = charingColor;
		IBatteryDrawable.fastChargingColor = fastChargingColor;
		IBatteryDrawable.transitColors = transitColors;
		IBatteryDrawable.colorful = colorful;
		
		lastVarUpdate = Calendar.getInstance().getTime().getTime();
	}
	
}
