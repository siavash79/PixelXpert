package sh.siava.AOSPMods.Utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.TypedArrayUtils;
import androidx.core.graphics.ColorUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import de.robv.android.xposed.XposedBridge;

public class CircleFilledBatteryDrawable extends Drawable {
	private Context mContext;
	private boolean isCharging = false;
	private boolean isFastCharging = false;
	private int batteryLevel = 0;
	private static float[] batteryLevels;
	private static int[] batteryColors;
	private static boolean showColors = false;
	private static boolean showCharging = false;
	private static boolean showFastCharing = false;
	private static int chargingColor = 0;
	private static int fastChargingColor = 0;
	private static boolean transitColors = false;
	private int intrinsicHeight;
	private int intrinsicWidth;
	private int size;
	private Rect padding = new Rect();
	private int fgColor = Color.WHITE;
	private int bgColor = Color.WHITE;
	private boolean isPowerSaving = false;
	private int alpha = 255;
	private int[] colors;
	private boolean isColorful = false;
	private static int[] shadeColors = null;
	private static float[] shadeLevels = null;
	
	public CircleFilledBatteryDrawable(Context context)
	{
		this.mContext = context;
//		XposedBridge.log("init");
		
		
		Resources res = mContext.getResources();
		intrinsicHeight = res.getDimensionPixelSize(res.getIdentifier("battery_height", "dimen", mContext.getPackageName()));
		intrinsicWidth = res.getDimensionPixelSize(res.getIdentifier("battery_height", "dimen", mContext.getPackageName()));
		size = Math.min(intrinsicHeight, intrinsicWidth);
	}
	
	public void setColorful(boolean colorful)
	{
		if(isColorful != colorful)
		{
			isColorful = colorful;
			if(isColorful) refresh();
		}
	}
	
	public CircleFilledBatteryDrawable(Context context, int frameColor) {
		this(context);
	}
	
	public static void setStaticColor(float[] batteryLevels, int[] batteryColors, boolean indicateCharging, int charingColor, boolean indicateFastCharging, int fastChargingColor, boolean transitColors) {
		CircleFilledBatteryDrawable.batteryColors = batteryColors;
		CircleFilledBatteryDrawable.batteryLevels = batteryLevels;
		CircleFilledBatteryDrawable.showCharging = indicateCharging;
		CircleFilledBatteryDrawable.showFastCharing = indicateFastCharging;
		CircleFilledBatteryDrawable.chargingColor = charingColor;
		CircleFilledBatteryDrawable.fastChargingColor = fastChargingColor;
		CircleFilledBatteryDrawable.transitColors = transitColors;
	}
	
	private static void refreshShadeColors() {
		if(batteryColors == null) return;
		
		shadeColors = new int[batteryLevels.length*2+2];
		XposedBridge.log("len" + batteryColors.length);

		XposedBridge.log("len2" + shadeColors.length);
		shadeLevels = new float[shadeColors.length];
		float prev = 0;
		for(int i = 0; i < batteryLevels.length; i++)
		{
			float rangeLength = batteryLevels[i] - prev;
			XposedBridge.log("range:" + rangeLength);
			shadeLevels[2*i]=(prev + rangeLength*.3f)/100;
			shadeColors[2*i]=batteryColors[i];
			XposedBridge.log("level " + shadeLevels[2*i] + " color " + shadeColors[2*i]);

			shadeLevels[2*i+1]=(batteryLevels[i] - rangeLength*.3f)/100;
			shadeColors[2*i+1]=batteryColors[i];
			XposedBridge.log("level " + shadeLevels[2*i+1] + " color " + shadeColors[2*i+1]);
			
			prev = batteryLevels[i];
		}
		
		shadeLevels[shadeLevels.length-2] = (batteryLevels[batteryLevels.length-1]+(100-batteryLevels[batteryLevels.length-1])*.3f)/100;
		shadeColors[shadeColors.length-2] = Color.GREEN;
		shadeLevels[shadeLevels.length-1] = 1f;
		shadeColors[shadeColors.length-1] = Color.GREEN;
	}
	
	public void refresh()
	{
		refreshShadeColors();
		this.invalidateSelf();
	}
	
	@Override
	public int getIntrinsicHeight()
	{
		return intrinsicHeight;
	}
	@Override
	public int getIntrinsicWidth()
	{
		return intrinsicWidth;
	}
	
	@Override
	public void draw(@NonNull Canvas canvas) {
		Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		basePaint.setColor(bgColor);
		basePaint.setAlpha(80*(alpha/255));
		
		Paint levelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		levelPaint.setAlpha(alpha);
		
		float cx = size/2f + padding.left;
		float cy = size/2f + padding.top;
		
		float baseRadius = size/2f;
		
		float levelRedius = baseRadius*batteryLevel/100f;
		
		try {
			setLevelPaint(levelPaint, cx, cy, baseRadius);
		}
		catch (Throwable t)
		{
			levelPaint.setColor(Color.BLACK);
		}
		
		canvas.drawCircle(cx, cy, baseRadius, basePaint);
		canvas.drawCircle(cy, cy, levelRedius, levelPaint);
	}
	
	private void setLevelPaint(Paint paint, float cx, float cy, float baseRadius) {
		int singleColor = fgColor;
		
		if(isFastCharging && showFastCharing && batteryLevel < 100)
		{
			paint.setColor(fastChargingColor);
			return;
		}
		else if (isCharging && showFastCharing && batteryLevel < 100)
		{
			paint.setColor(chargingColor);
			return;
		}
		else if (isPowerSaving)
		{
			paint.setColor(Color.RED);
			return;
		}
		
		if(!isColorful || shadeColors == null) {
			for (int i = 0; i < batteryLevels.length; i++) {
				if (batteryLevel <= batteryLevels[i]) {
					if (transitColors && i > 0) {
						float range = batteryLevels[i] - batteryLevels[i - 1];
						float currentPos = batteryLevel - batteryLevels[i - 1];
						float ratio = currentPos / range;
						singleColor = ColorUtils.blendARGB(batteryColors[i - 1], batteryColors[i], ratio);
					} else {
						singleColor = batteryColors[i];
					}
					XposedBridge.log("level" + batteryLevel);
					XposedBridge.log("i:" + i);
					XposedBridge.log("level I"+ batteryLevels[i]);
					XposedBridge.log("color I" + batteryColors[i]);
					break;
				}
			}
			paint.setColor(singleColor);
		}
		else
		{
			for(int i = 0; i< shadeColors.length; i++)
			{
				XposedBridge.log(String.format("i = %s level = %s color = %s", i, shadeLevels[i], shadeColors[i]));
			}
			RadialGradient shader = new RadialGradient(cx,cy,baseRadius, shadeColors, shadeLevels, Shader.TileMode.CLAMP);
			paint.setShader(shader);
		}
	}
	
	@Override
	public void setAlpha(int alpha) {
		if(this.alpha != alpha) {
			this.alpha = alpha;
			invalidateSelf();
		}
	}
	
	@Override
	public void setColorFilter(@Nullable ColorFilter colorFilter) {
		return;
	}
	
	@Override
	public void setBounds(Rect bounds)
	{
		super.setBounds(bounds);
		this.size = Math.max((bounds.height() - padding.height()), (bounds.width() - padding.width()));
		invalidateSelf();
	}
	
	
	@Override
	public int getOpacity() {
		return PixelFormat.UNKNOWN;
	}
	
	public void setShowPercent(boolean showPercent) { //not applicable
		return;
	}
	
	public void setMeterStyle(int batteryStyle) { //not applicable
		return;
	}
	
	public void setFastCharing(boolean isFastCharging) {
		if(this.isFastCharging != isFastCharging) {
			this.isFastCharging = isFastCharging;
			if (isFastCharging) isCharging = true;
			invalidateSelf();
		}
	}
	
	public void setCharging(boolean mCharging) {
		if(mCharging != isCharging) {
			isCharging = mCharging;
			if (!isCharging) isFastCharging = false;
			invalidateSelf();
		}
	}
	
	public void setBatteryLevel(int mLevel) {
		if(mLevel != batteryLevel) {
			batteryLevel = mLevel;
			invalidateSelf();
		}
	}
	
	public void setColors(int fgColor, int bgColor, int singleToneColor) {
		this.fgColor = fgColor;
		this.bgColor = bgColor;
		invalidateSelf();
	}
	
	public void setPowerSaveEnabled(boolean isPowerSaving) {
		if(isPowerSaving != this.isPowerSaving) {
			this.isPowerSaving = isPowerSaving;
			invalidateSelf();
		}
	}
	
}
