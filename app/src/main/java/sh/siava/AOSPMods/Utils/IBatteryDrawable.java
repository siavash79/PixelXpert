package sh.siava.AOSPMods.Utils;

import android.graphics.drawable.Drawable;

public abstract class IBatteryDrawable extends Drawable {
	public abstract void setColorful(boolean colorful);
	public abstract void setShowPercent(boolean showPercent);
	public abstract void setMeterStyle(int batteryStyle);
	public abstract void setFastCharging(boolean isFastCharging);
	public abstract void setCharging(boolean isCharging);
	public abstract void setBatteryLevel(int mLevel);
	public abstract void setColors(int fgColor, int bgColor, int singleToneColor);
	public abstract void setPowerSaveEnabled(boolean isPowerSaving);
	
	public static void setStaticColor(float[] batteryLevels, int[] batteryColors, boolean indicateCharging, int charingColor, boolean indicateFastCharging, int fastChargingColor, boolean transitColors) {
		return;
	}
	
}
