package sh.siava.AOSPMods.Utils.batteryStyles;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

public class CircleFilledBatteryDrawable extends BatteryDrawable {
	private boolean isCharging = false;
	private boolean isFastCharging = false;
	private int batteryLevel = 0;
	private final int intrinsicHeight;
	private final int intrinsicWidth;
	private int size;
	private final Rect padding = new Rect();
	private int fgColor = Color.WHITE;
	private int bgColor = Color.WHITE;
	private boolean isPowerSaving = false;
	private int alpha = 255;
	private static int[] shadeColors = null;
	private static float[] shadeLevels = null;
	private final int powerSaveColor;
	private long lastUpdate = -1;
	
	public CircleFilledBatteryDrawable(Context context)
	{
		
		Resources res = context.getResources();
		//noinspection SpellCheckingInspection
		powerSaveColor = getColorStateListDefaultColor(
				context,
				context.getResources().getIdentifier("batterymeter_plus_color", "color", context.getPackageName()));
		
		intrinsicHeight = res.getDimensionPixelSize(res.getIdentifier("battery_height", "dimen", context.getPackageName()));
		intrinsicWidth = res.getDimensionPixelSize(res.getIdentifier("battery_height", "dimen", context.getPackageName()));
		size = Math.min(intrinsicHeight, intrinsicWidth);
	}
	
	
	public CircleFilledBatteryDrawable(Context context, int frameColor) {
		this(context);
		bgColor = frameColor;
	}
	
	private static void refreshShadeColors() {
		if(batteryColors == null) return;
		
		shadeColors = new int[batteryLevels.length*2+2];

		shadeLevels = new float[shadeColors.length];
		float prev = 0;
		for(int i = 0; i < batteryLevels.length; i++)
		{
			float rangeLength = batteryLevels[i] - prev;
			shadeLevels[2*i]=(prev + rangeLength*.3f)/100;
			shadeColors[2*i]=batteryColors[i];
			
			shadeLevels[2*i+1]=(batteryLevels[i] - rangeLength*.3f)/100;
			shadeColors[2*i+1]=batteryColors[i];
			
			prev = batteryLevels[i];
		}
		
		shadeLevels[shadeLevels.length-2] = (batteryLevels[batteryLevels.length-1]+(100-batteryLevels[batteryLevels.length-1])*.3f)/100;
		shadeColors[shadeColors.length-2] = Color.GREEN;
		shadeLevels[shadeLevels.length-1] = 1f;
		shadeColors[shadeColors.length-1] = Color.GREEN;
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
		if(lastUpdate != lastVarUpdate)
		{
			lastUpdate = lastVarUpdate;
			refreshShadeColors();
		}
		Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		basePaint.setColor(bgColor);
		basePaint.setAlpha(Math.round(80f*(alpha/255f)));
		
		Paint levelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		
		float cx = size/2f + padding.left;
		float cy = size/2f + padding.top;
		
		float baseRadius = size/2f;
		
		float levelRadius = baseRadius*batteryLevel/100f;
		
		try {
			setLevelPaint(levelPaint, cx, cy, baseRadius);
			levelPaint.setAlpha(alpha);
		}
		catch (Throwable t)
		{
			levelPaint.setColor(Color.BLACK);
		}
		
		canvas.drawCircle(cx, cy, baseRadius, basePaint);
		canvas.drawCircle(cy, cy, levelRadius, levelPaint);
	}
	
	private void setLevelPaint(Paint paint, float cx, float cy, float baseRadius) {
		int singleColor = fgColor;
		
		paint.setShader(null);
		if(isFastCharging && showFastCharging && batteryLevel < 100)
		{
			paint.setColor(fastChargingColor);
			return;
		}
		else if (isCharging && showCharging && batteryLevel < 100)
		{
			paint.setColor(chargingColor);
			return;
		}
		else if (isPowerSaving)
		{
			paint.setColor(powerSaveColor);
			return;
		}
		
		if(!colorful || shadeColors == null) {
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
					break;
				}
			}
			paint.setColor(singleColor);
		}
		else
		{
			RadialGradient shader = new RadialGradient(cx,cy,baseRadius, shadeColors, shadeLevels, Shader.TileMode.CLAMP);
			paint.setShader(shader);
//			paint.setAlpha(128);
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
	
	@Override
	public void setShowPercent(boolean showPercent) { //not applicable
	}
	
	@Override
	public void setMeterStyle(int batteryStyle) { //not applicable
	}
	
	@Override
	public void setFastCharging(boolean isFastCharging) {
		if(this.isFastCharging != isFastCharging) {
			this.isFastCharging = isFastCharging;
			if (isFastCharging) isCharging = true;
			invalidateSelf();
		}
	}
	
	@Override
	public void setCharging(boolean mCharging) {
		if(mCharging != isCharging) {
			isCharging = mCharging;
			if (!isCharging) isFastCharging = false;
			invalidateSelf();
		}
	}
	
	@Override
	public void setBatteryLevel(int mLevel) {
		if(mLevel != batteryLevel) {
			batteryLevel = mLevel;
			invalidateSelf();
		}
	}
	
	@Override
	public void setColors(int fgColor, int bgColor, int singleToneColor) {
		this.fgColor = fgColor;
		this.bgColor = bgColor;
		invalidateSelf();
	}
	
	@Override
	public void setPowerSaveEnabled(boolean isPowerSaving) {
		if(isPowerSaving != this.isPowerSaving) {
			this.isPowerSaving = isPowerSaving;
			invalidateSelf();
		}
	}
	
	@Override
	public void refresh() {
		invalidateSelf();
	}
	
	
	@ColorInt
	private int getColorStateListDefaultColor(Context context, int resId){
		ColorStateList list = context.getResources().getColorStateList(resId, context.getTheme());
		return list.getDefaultColor();
	}
}