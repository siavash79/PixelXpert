package sh.siava.AOSPMods.utils.batteryStyles;

import static android.graphics.Color.WHITE;
import static android.graphics.Paint.ANTI_ALIAS_FLAG;
import static android.graphics.Paint.Align.CENTER;
import static android.graphics.Paint.Style.STROKE;
import static android.graphics.Typeface.BOLD;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.Typeface;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.PathParser;

public class CircleBatteryDrawable extends BatteryDrawable
{
	private static final int BATTERY_STYLE_CIRCLE = 1;
	private static final int BATTERY_STYLE_DOTTED_CIRCLE = 2;
	private static final String WARNING_STRING = "!";
	private static final int CRITICAL_LEVEL = 5;
	private static final int CIRCLE_DIAMETER = 45; //relative to dash effect size. Size doesn't matter as finally it gets scaled by parent
	private static final PathEffect DASH_PATH_EFFECT = new DashPathEffect(new float[]{3f, 2f}, 0f);
	private final Context mContext;
	private boolean mIsFastCharging = false;
	private boolean mIsCharging = false;
	private boolean mIsPowerSaving = false;
	private boolean mShowPercentage = false;
	private int mBatteryLevel;
	private int mDiameter;
	private final RectF mFrame = new RectF();
	private int mFGColor = WHITE;
	private final Paint mTextPaint = new Paint(ANTI_ALIAS_FLAG);
	private final Paint mFramePaint = new Paint(ANTI_ALIAS_FLAG);
	private final Paint mBatteryPaint = new Paint(ANTI_ALIAS_FLAG);
	private final Paint mWarningTextPaint = new Paint(ANTI_ALIAS_FLAG);
	private final Paint mBoltPaint = new Paint(ANTI_ALIAS_FLAG);
	private final Paint mPowerSavePaint = new Paint(ANTI_ALIAS_FLAG);
	private int[] mShadeColors;
	private float[] mShadeLevels;
	private long mLastUpdate;
	private int mAlpha = 255;
	private Path mBoltPath;

	@SuppressLint("DiscouragedApi")
	public CircleBatteryDrawable(Context context, int frameColor)
	{
		mContext = context;
		Resources res = mContext.getResources();

		mFramePaint.setDither(true);
		mFramePaint.setStyle(STROKE);

		mTextPaint.setTypeface(Typeface.create("sans-serif-condensed", BOLD));
		mTextPaint.setTextAlign(CENTER);

		mWarningTextPaint.setTypeface(Typeface.create("sans-serif", BOLD));
		mWarningTextPaint.setTextAlign(CENTER);

		mBatteryPaint.setDither(true);
		mBatteryPaint.setStyle(STROKE);

		int mPowerSaveColor = getColorStateListDefaultColor(mContext, res.getIdentifier(
				"batterymeter_plus_color",
				"color",
				mContext.getPackageName()));

		mPowerSavePaint.setColor(mPowerSaveColor);
		mPowerSavePaint.setStyle(STROKE);

		setColors(frameColor, frameColor, frameColor);

		setMeterStyle(BATTERY_STYLE_CIRCLE);
	}

	@Override
	public void setShowPercent(boolean showPercent) {
		mShowPercentage = showPercent;
		postInvalidate();
	}

	@Override
	public void setMeterStyle(int batteryStyle) {
		mFramePaint.setPathEffect(batteryStyle == BATTERY_STYLE_DOTTED_CIRCLE ? DASH_PATH_EFFECT : null);
		mBatteryPaint.setPathEffect(batteryStyle == BATTERY_STYLE_DOTTED_CIRCLE ? DASH_PATH_EFFECT : null);
		mPowerSavePaint.setPathEffect(batteryStyle == BATTERY_STYLE_DOTTED_CIRCLE ? DASH_PATH_EFFECT : null);

		postInvalidate();
	}

	@Override
	public void setFastCharging(boolean isFastCharging) {
		mIsFastCharging = isFastCharging;
		if(isFastCharging) mIsCharging = true;

		postInvalidate();
	}

	private void postInvalidate()
	{
		unscheduleSelf(this::invalidateSelf);
		scheduleSelf(this::invalidateSelf, 0);
	}

	@Override
	public void setCharging(boolean isCharging) {
		mIsCharging = isCharging;
		if(!isCharging) mIsFastCharging = false;

		postInvalidate();
	}

	@Override
	public void setBatteryLevel(int level) {
		mBatteryLevel = level;
		postInvalidate();
	}

	@Override
	public void setBounds(int left, int top, int right, int bottom)
	{
		super.setBounds(left, top, right, bottom);
		updateSize();
	}

	@Override
	public void setColors(int fgColor, int bgColor, int singleToneColor) {
		mFGColor = fgColor;

		mBoltPaint.setColor(mFGColor);
		mFramePaint.setColor(bgColor);
		mTextPaint.setColor(mFGColor);

		setAlpha(mAlpha); //invalidate included here - alpha shall be re-applied after color change
	}

	@Override
	public void setPowerSaving(boolean isPowerSaving) {
		mIsPowerSaving = isPowerSaving;

		postInvalidate();
	}

	@Override
	public void refresh() {
		postInvalidate();
	}

	private void refreshShadeColors()
	{
		if(batteryColors == null) return;

		mShadeColors = new int[batteryLevels.size() * 2 + 2];
		mShadeLevels = new float[mShadeColors.length];

		float lastPCT = 0f;

		for(int i = 0; i < batteryLevels.size(); i++)
		{
			float rangeLength = batteryLevels.get(i) - lastPCT;

			int pointer = 2 * i;
			mShadeLevels[pointer] = (lastPCT + rangeLength * 0.3f) / 100;
			mShadeColors[pointer] = batteryColors[i];

			mShadeLevels[pointer + 1] = (batteryLevels.get(i) - rangeLength * 0.3f) / 100;
			mShadeColors[pointer + 1] = batteryColors[i];
			lastPCT = batteryLevels.get(i);
		}

		mShadeLevels[mShadeLevels.length - 2] = (batteryLevels.get(batteryLevels.size() - 1) + (100 - batteryLevels.get(batteryLevels.size() - 1) * 0.3f)) / 100;
		mShadeColors[mShadeColors.length -2] = Color.GREEN;

		mShadeLevels[mShadeLevels.length - 1] = 1f;
		mShadeColors[mShadeColors.length- 1] = Color.GREEN;
	}

	@Override
	public void draw(@NonNull Canvas canvas) {
		if(mBatteryLevel == -1 || mDiameter == 0) return;

		if (mLastUpdate != lastVarUpdate)
		{
			mLastUpdate = lastVarUpdate;
			refreshShadeColors();
		}

		if(!mIsPowerSaving)
			setLevelBasedColors(mBatteryPaint, mFrame.centerX(), mFrame.centerY());

		if(mIsCharging)
		{
			canvas.drawPath(mBoltPath, mBoltPaint);
		}

		canvas.drawArc(mFrame, 270f, 360f, false, mFramePaint);

		if(mBatteryLevel > 0)
		{
			canvas.drawArc(mFrame, 270f, 3.6f * mBatteryLevel, false,
					(!mIsCharging && mIsPowerSaving)
						? mPowerSavePaint
						: mBatteryPaint);
		}

		if(!mIsCharging && mBatteryLevel < 100 && mShowPercentage)
		{
			String pctText = mBatteryLevel > CRITICAL_LEVEL ? String.valueOf(mBatteryLevel) : WARNING_STRING;

			float textHeight = -mTextPaint.getFontMetrics().ascent;
			float pctX = mDiameter * .5f;
			float pctY = (mDiameter + textHeight) * 0.47f;
			canvas.drawText(pctText, pctX, pctY, mTextPaint);
		}
	}

	private void setLevelBasedColors(Paint paint, float centerX, float centerY)
	{
		paint.setShader(null);

		if(mIsFastCharging && showFastCharging && mBatteryLevel < 100)
		{
			paint.setColor(fastChargingColor);
			return;
		} else if (mIsCharging && showCharging && mBatteryLevel < 100) {
			paint.setColor(chargingColor);
			return;
		}

		if(!colorful || mShadeColors == null)
		{
			for(int i = 0; i < batteryLevels.size(); i++)
			{
				if(mBatteryLevel <= batteryLevels.get(i))
				{
					if(transitColors && i > 0)
					{
						float range = batteryLevels.get(i) - batteryLevels.get(i - 1);
						float currentPos = mBatteryLevel - batteryLevels.get(i - 1);

						float ratio = currentPos / range;

						paint.setColor(ColorUtils.blendARGB(batteryColors[i - 1], batteryColors[i], ratio));
					}
					else
					{
						paint.setColor(batteryColors[i]);
					}
					return;
				}
			}
			paint.setColor(mFGColor);
		}
		else
		{
			SweepGradient shader = new SweepGradient(centerX, centerY, mShadeColors, mShadeLevels);
			Matrix shaderMatrix = new Matrix();
			shaderMatrix.preRotate(270f, centerX, centerY);
			shader.setLocalMatrix(shaderMatrix);
			paint.setShader(shader);
		}

		mBatteryPaint.setAlpha(mAlpha);
	}

	@Override
	public void setAlpha(int alpha) {
		mAlpha = alpha;

		mFramePaint.setAlpha(Math.round(70 * alpha / 255f));

		mBoltPaint.setAlpha(alpha);
		mPowerSavePaint.setAlpha(alpha);
		mTextPaint.setAlpha(alpha);

		postInvalidate();
	}

	@SuppressLint("DiscouragedApi")
	private void updateSize()
	{
		Resources res = mContext.getResources();

		mDiameter = getBounds().bottom - getBounds().top;

		mWarningTextPaint.setTextSize(mDiameter * 0.75f);

		float strokeWidth = mDiameter / 6.5f;
		mFramePaint.setStrokeWidth(strokeWidth);
		mBatteryPaint.setStrokeWidth(strokeWidth);
		mPowerSavePaint.setStrokeWidth(strokeWidth);

		mTextPaint.setTextSize(mDiameter * 0.52f);

		mFrame.set(strokeWidth / 2.0f,
				strokeWidth / 2.0f,
				mDiameter - strokeWidth / 2.0f,
				mDiameter - strokeWidth / 2.0f);

		@SuppressLint("DiscouragedApi") 
		Path unscaledBoltPath = new Path();
		unscaledBoltPath.set(
				PathParser.createPathFromPathData(
						res.getString(
								res.getIdentifier(
										"android:string/config_batterymeterBoltPath",
										"string",
										"android"))));

		//Bolt icon
		Matrix scaleMatrix = new Matrix();
		RectF pathBounds = new RectF();

		unscaledBoltPath.computeBounds(pathBounds, true);

		float scaleF = (getBounds().height() - strokeWidth * 2) * .8f / pathBounds.height(); //scale comparing to 80% of icon's inner space

		scaleMatrix.setScale(scaleF, scaleF);

		mBoltPath = new Path();

		unscaledBoltPath.transform(scaleMatrix, mBoltPath);

		mBoltPath.computeBounds(pathBounds, true);

		//moving it to center
		mBoltPath.offset(getBounds().centerX() - pathBounds.centerX(),
				getBounds().centerY() - pathBounds.centerY());
	}

	@Override
	public void setColorFilter(@Nullable ColorFilter colorFilter) {
		mFramePaint.setColorFilter(colorFilter);
		mBatteryPaint.setColorFilter(colorFilter);
		mWarningTextPaint.setColorFilter(colorFilter);
		mBoltPaint.setColorFilter(colorFilter);
	}

	@Override
	public int getOpacity() {
		return PixelFormat.UNKNOWN;
	}

	@Override
	public int getIntrinsicHeight()
	{
		return CIRCLE_DIAMETER;
	}

	@Override
	public int getIntrinsicWidth()
	{
		return CIRCLE_DIAMETER;
	}

	@ColorInt
	private static int getColorStateListDefaultColor(Context context, int resId){
		return context.getResources().getColorStateList(resId, context.getTheme()).getDefaultColor();
	}
}