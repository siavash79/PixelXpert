package sh.siava.AOSPMods.modpacks.utils.batteryStyles;

import static android.graphics.Color.WHITE;

import static java.lang.Math.round;

import static de.robv.android.xposed.XposedBridge.log;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import sh.siava.AOSPMods.modpacks.systemui.SettingsLibUtilsProvider;

public class CircleFilledBatteryDrawable extends BatteryDrawable {
	private static final int INTRINSIC_DIMENSION = 45;
	private final ValueAnimator mLevelAlphaAnimator;
	private boolean mIsCharging = false;
	private boolean mIsFastCharging = false;
	private boolean mChargingAnimationEnabled = true;
	private int mBatteryLevel = 0;
	private int mDimension = INTRINSIC_DIMENSION;
	private final Rect mPadding = new Rect();
	private int mFGColor = WHITE;
	private int mBGColor = WHITE;
	private boolean mIsPowerSaving = false;
	private int mAlpha = 255;
	private static int[] mShadeColors = null;
	private static float[] mShadeLevels = null;
	private final int mPowerSaveColor;
	private long lastUpdate = -1;

	@SuppressLint("DiscouragedApi")
	public CircleFilledBatteryDrawable(Context context) {

		mPowerSaveColor = SettingsLibUtilsProvider.getColorAttrDefaultColor(android.R.attr.colorError, context);

		mLevelAlphaAnimator = ValueAnimator.ofInt(255, 255, 255, 45);

		mLevelAlphaAnimator.setDuration(2000);
		mLevelAlphaAnimator.setInterpolator(new FastOutSlowInInterpolator());
		mLevelAlphaAnimator.setRepeatMode(ValueAnimator.REVERSE);
		mLevelAlphaAnimator.setRepeatCount(ValueAnimator.INFINITE);

		mLevelAlphaAnimator.addUpdateListener(valueAnimator -> invalidateSelf());
	}

	public CircleFilledBatteryDrawable(Context context, int frameColor) {
		this(context);
		mBGColor = frameColor;
	}

	private static void refreshShadeColors() {
		if (batteryColors == null) return;

		mShadeColors = new int[batteryLevels.size() * 2 + 2];

		mShadeLevels = new float[mShadeColors.length];
		float prev = 0;
		for (int i = 0; i < batteryLevels.size(); i++) {
			float rangeLength = batteryLevels.get(i) - prev;
			mShadeLevels[2 * i] = (prev + rangeLength * .3f) / 100;
			mShadeColors[2 * i] = batteryColors[i];

			mShadeLevels[2 * i + 1] = (batteryLevels.get(i) - rangeLength * .3f) / 100;
			mShadeColors[2 * i + 1] = batteryColors[i];

			prev = batteryLevels.get(i);
		}

		mShadeLevels[mShadeLevels.length - 2] = (batteryLevels.get(batteryLevels.size() - 1) + (100 - batteryLevels.get(batteryLevels.size() - 1)) * .3f) / 100;
		mShadeColors[mShadeColors.length - 2] = Color.GREEN;
		mShadeLevels[mShadeLevels.length - 1] = 1f;
		mShadeColors[mShadeColors.length - 1] = Color.GREEN;
	}

	@Override
	public int getIntrinsicHeight() {
		return INTRINSIC_DIMENSION;
	}

	@Override
	public int getIntrinsicWidth() {
		return INTRINSIC_DIMENSION;
	}

	@Override
	public void draw(@NonNull Canvas canvas) {
		if (lastUpdate != lastVarUpdate) {
			lastUpdate = lastVarUpdate;
			refreshShadeColors();
		}
		Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		basePaint.setColor(mBGColor);
		basePaint.setAlpha(round(80f * (mAlpha / 255f)));

		Paint levelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

		float centerX = mDimension / 2f + mPadding.left;
		float centerY = mDimension / 2f + mPadding.top;

		float baseRadius = mDimension / 2f;

		float levelRadius = baseRadius * mBatteryLevel / 100f;

		try {
			setLevelBasedColor(levelPaint, centerX, centerY, baseRadius);
		} catch (Throwable t) {
			levelPaint.setColor(Color.BLACK);
		}

		if(mIsCharging && mBatteryLevel < 100)
		{
			if(!mLevelAlphaAnimator.isStarted() && mChargingAnimationEnabled)
				mLevelAlphaAnimator.start();

			levelPaint.setAlpha(round(
					(mChargingAnimationEnabled
						? (int) mLevelAlphaAnimator.getAnimatedValue()
						: 255)
					* mAlpha/255f));
		}
		else
		{
			if (mLevelAlphaAnimator.isStarted())
				mLevelAlphaAnimator.end();

			levelPaint.setAlpha(mAlpha);
		}

		canvas.drawCircle(centerX, centerY, baseRadius, basePaint);
		canvas.drawCircle(centerY, centerY, levelRadius, levelPaint);
	}

	private void setLevelBasedColor(Paint paint, float cx, float cy, float baseRadius) {
		int singleColor = mFGColor;

		paint.setShader(null);
		if (mIsFastCharging && showFastCharging && mBatteryLevel < 100) {
			paint.setColor(fastChargingColor);
			return;
		} else if (mIsCharging && showCharging && mBatteryLevel < 100) {
			paint.setColor(chargingColor);
			return;
		} else if (mIsPowerSaving) {
			paint.setColor(mPowerSaveColor);
			return;
		}

		if (!colorful || mShadeColors == null) {
			for (int i = 0; i < batteryLevels.size(); i++) {
				if (mBatteryLevel <= batteryLevels.get(i)) {
					if (transitColors && i > 0) {
						float range = batteryLevels.get(i) - batteryLevels.get(i - 1);
						float currentPos = mBatteryLevel - batteryLevels.get(i - 1);
						float ratio = currentPos / range;
						singleColor = ColorUtils.blendARGB(batteryColors[i - 1], batteryColors[i], ratio);
					} else {
						singleColor = batteryColors[i];
					}
					break;
				}
			}
			paint.setColor(singleColor);
		} else {
			RadialGradient shader = new RadialGradient(cx, cy, baseRadius, mShadeColors, mShadeLevels, Shader.TileMode.CLAMP);
			paint.setShader(shader);
		}
	}

	@Override
	public void setAlpha(int alpha) {
		if (mAlpha != alpha) {
			mAlpha = alpha;
			invalidateSelf();
		}
	}

	@Override
	public void setColorFilter(@Nullable ColorFilter colorFilter) {
	}

	@Override
	public void setBounds(Rect bounds) {
		super.setBounds(bounds);
		mDimension = Math.max((bounds.height() - mPadding.height()), (bounds.width() - mPadding.width()));
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
	public void setFastCharging(boolean fastCharging) {
		if (mIsFastCharging != fastCharging) {
			mIsFastCharging = fastCharging;
			if (fastCharging) mIsCharging = true;
			invalidateSelf();
		}
	}

	@Override
	public void setCharging(boolean mCharging) {
		if (mCharging != mIsCharging) {
			mIsCharging = mCharging;
			if (!mIsCharging) mIsFastCharging = false;
			invalidateSelf();
		}
	}

	@Override
	public void setBatteryLevel(int mLevel) {
		if (mLevel != mBatteryLevel) {
			mBatteryLevel = mLevel;
			invalidateSelf();
		}
	}

	@Override
	public void setColors(int fgColor, int bgColor, int singleToneColor) {
		mFGColor = fgColor;
		mBGColor = bgColor;
		invalidateSelf();
	}

	@Override
	public void setPowerSaving(boolean powerSaving) {
		if (powerSaving != mIsPowerSaving) {
			mIsPowerSaving = powerSaving;
			invalidateSelf();
		}
	}

	@Override
	public void setChargingAnimationEnabled(boolean enabled) {
		mChargingAnimationEnabled = enabled;

		invalidateSelf();
	}

	@Override
	public void refresh() {
		invalidateSelf();
	}
}