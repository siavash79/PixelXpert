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
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import java.util.ArrayList;
import java.util.Arrays;

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
	private int size = 1;
	private Rect padding = new Rect();
	private int fgColor = Color.WHITE;
	private int bgColor = Color.WHITE;
	private boolean isPowerSaving = false;
	private int alpha = 255;
	private int[] colors;
	private boolean isColorful = false;
	
	public CircleFilledBatteryDrawable(Context context)
	{
		this.mContext = context;
		XposedBridge.log("init");
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
			invalidateSelf();
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
		
		setLevelPaint(levelPaint, cx, cy, baseRadius);
		
		XposedBridge.log("cx "+ cx);
		XposedBridge.log("cy "+ cy);
		XposedBridge.log("br "+ baseRadius);
		XposedBridge.log("lr "+ levelRedius);
		
		
		canvas.drawCircle(cx, cy, baseRadius, basePaint);
		canvas.drawCircle(cy, cy, levelRedius, levelPaint);
	}
	
	private void setLevelPaint(Paint paint, float cx, float cy, float baseRadius) {
		paint.setColor(Color.WHITE);
		if(true) return;
		int singleColor = fgColor;
		
		if(!isColorful) {
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
			ArrayList<Integer> colors = new ArrayList(Arrays.asList(batteryColors));
			colors.add(Color.GREEN);
			
			float[] levels = new float[colors.size()];
			
			levels[0] = 0;
			for(int i = 0; i < batteryLevels.length-1; i++)
			{
				levels[i+1] = (batteryLevels[i] + batteryLevels[i+1]) /2;
			}
			levels[levels.length-1] = 100;
			
			int[] colorsA = colors.stream().mapToInt(i -> i).toArray();
			RadialGradient g = new RadialGradient(cx,cy,baseRadius, colorsA, levels, Shader.TileMode.CLAMP);
			paint.setShader(g);
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
			batteryLevel = 50;
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
