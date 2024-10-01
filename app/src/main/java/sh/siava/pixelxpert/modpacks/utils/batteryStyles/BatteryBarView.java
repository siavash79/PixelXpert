package sh.siava.pixelxpert.modpacks.utils.batteryStyles;

import static sh.siava.pixelxpert.modpacks.systemui.BatteryDataProvider.getCurrentLevel;
import static sh.siava.pixelxpert.modpacks.systemui.BatteryDataProvider.isCharging;
import static sh.siava.pixelxpert.modpacks.systemui.BatteryDataProvider.isFastCharging;
import static sh.siava.pixelxpert.modpacks.systemui.BatteryDataProvider.isPowerSaving;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BaseInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.core.graphics.ColorUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import sh.siava.pixelxpert.modpacks.systemui.BatteryDataProvider;
import sh.siava.pixelxpert.modpacks.systemui.StatusbarMods;

public class BatteryBarView extends FrameLayout {
	private static int[] shadeColors;
	private final boolean RTL;
	private static float[] shadeLevels = new float[0];
	private final ShapeDrawable mDrawable = new ShapeDrawable();
	FrameLayout maskLayout;
	private final ImageView chargingIndicatorView;
	private final ImageView chargingIndicatorViewForCenter;
	private boolean colorful = false;
	private int alphaPct = 100;
	private int singleColorTone = Color.WHITE;
	private boolean isCenterBased = false;
	private int barHeight = 10;
	private final ImageView barView;
	private boolean onTop = true;
	@SuppressLint("StaticFieldLeak")
	private static BatteryBarView instance = null;
	private boolean onlyWhileCharging = false;
	private boolean mIsEnabled = true;
	private boolean mIsHidden = false;
	private static List<Float> batteryLevels = Arrays.asList(20f, 40f);
	private static int[] batteryColors = new int[]{Color.RED, Color.YELLOW};
	private static int chargingColor = Color.WHITE;
	private static int fastChargingColor = Color.WHITE;
	private static int powerSaveColor = Color.parseColor("#FFBF00");
	private static boolean indicateCharging = false;
	private static boolean indicateFastCharging = false;
	private static boolean indicatePowerSave = false;
	private static boolean transitColors = false;
	private static boolean animateCharging = false;
	private static final int ANIM_DURATION = 1000;
	private static final int ANIM_DELAY = 2000;
	private final Handler animationHandler = new Handler(Looper.getMainLooper());
	private Runnable chargingAnimationRunnable;

	public static void setStaticColor(List<Float> batteryLevels, int[] batteryColors, boolean indicateCharging, int chargingColor, boolean indicateFastCharging, int fastChargingColor, boolean indicatePowerSave, int powerSaveColor, boolean transitColors, boolean animate) {
		BatteryBarView.transitColors = transitColors;
		BatteryBarView.batteryLevels = batteryLevels;
		BatteryBarView.batteryColors = batteryColors;
		BatteryBarView.chargingColor = chargingColor;
		BatteryBarView.fastChargingColor = fastChargingColor;
		BatteryBarView.indicatePowerSave = indicatePowerSave;
		BatteryBarView.powerSaveColor = powerSaveColor;
		BatteryBarView.indicateCharging = indicateCharging;
		BatteryBarView.indicateFastCharging = indicateFastCharging;
		BatteryBarView.animateCharging = animate;
	}

	public void setOnTop(boolean onTop) {
		this.onTop = onTop;

		refreshLayout();
	}

	public void setOnlyWhileCharging(boolean state) {
		onlyWhileCharging = state;
		refreshLayout();
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		refreshLayout();
	}

	public void refreshLayout() {
		if (!isAttachedToWindow()) return;

		refreshVisibility();

		if (barView.getVisibility() == GONE) return;
		maskLayout.setLayoutParams(maskLayoutParams());
		barView.setLayoutParams(barLayoutParams());
		chargingIndicatorView.setLayoutParams(charginLayoutParams());
		chargingIndicatorViewForCenter.setLayoutParams(charginLayoutParams());

		refreshColors(barView.getWidth(), barView.getHeight());
		mDrawable.invalidateSelf();
	}

	private LayoutParams maskLayoutParams() {
		LayoutParams result = new LayoutParams(Math.round(getWidth() * getCurrentLevel() / 100f), ViewGroup.LayoutParams.MATCH_PARENT);
		result.gravity = (isCenterBased) ? Gravity.CENTER : Gravity.START;
		return result;
	}

	@SuppressWarnings("SpellCheckingInspection")
	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		refreshLayout();
	}

	private void startChargingAnimation() {
		if (chargingAnimationRunnable == null) {
			chargingAnimationRunnable = new Runnable() {
				@Override
				public void run() {
					animateChargingIndicator();
					animationHandler.postDelayed(this, ANIM_DELAY);
				}
			};
			animationHandler.post(chargingAnimationRunnable);
		}
	}

	private void stopChargingAnimation() {
		animationHandler.removeCallbacks(chargingAnimationRunnable);

		post(() -> {
			chargingIndicatorView.setVisibility(GONE);
			chargingIndicatorView.clearAnimation();

			chargingIndicatorViewForCenter.setVisibility(GONE);
			chargingIndicatorViewForCenter.clearAnimation();
		});

		chargingAnimationRunnable = null;
	}

	private void animateChargingIndicator() {
		int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;

		chargingIndicatorView.post(() -> chargingIndicatorView.setVisibility(VISIBLE));
		int secondaryVisibility = (isCenterBased) ? GONE : VISIBLE;
		chargingIndicatorViewForCenter.post(() -> chargingIndicatorViewForCenter.setVisibility(secondaryVisibility));

		float startX, startXCenter, endX;

		if (RTL) {
			startX = 0;
			startXCenter = getWidth();
			endX = getWidth() - Math.round(getWidth() * getCurrentLevel() / 100f);
		} else {
			startX = screenWidth;
			startXCenter = 0;
			endX = Math.round(getWidth() * getCurrentLevel() / 100f);
		}

		if (isCenterBased) endX = getWidth() * (1 - ((100 - getCurrentLevel()) / 200f));

		BaseInterpolator interpolator = new AccelerateInterpolator();

		TranslateAnimation animation = new TranslateAnimation(startX, endX, 0, 0);
		animation.setDuration(ANIM_DURATION);
		animation.setInterpolator(interpolator);
		chargingIndicatorView.startAnimation(animation);

		if (isCenterBased) {
			TranslateAnimation animationCenter = new TranslateAnimation(startXCenter, getWidth() - endX, 0, 0);
			animationCenter.setDuration(ANIM_DURATION);
			animationCenter.setInterpolator(interpolator);
			chargingIndicatorViewForCenter.startAnimation(animationCenter);
		}
	}

	public BatteryBarView(Context context) {
		super(context);
		instance = this;
		this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		mDrawable.setShape(new RectShape());
		this.setSingleColorTone(singleColorTone);
		this.setAlphaPct(alphaPct);

		barView = new ImageView(context);
		barView.setImageDrawable(mDrawable);

		chargingIndicatorView = new ImageView(context);
		chargingIndicatorView.setLayoutParams(new LayoutParams(20, barHeight));
		chargingIndicatorView.setBackgroundColor(singleColorTone);

		chargingIndicatorViewForCenter = new ImageView(context);
		chargingIndicatorViewForCenter.setLayoutParams(new LayoutParams(20, barHeight));
		chargingIndicatorViewForCenter.setBackgroundColor(singleColorTone);

		maskLayout = new FrameLayout(context);
		maskLayout.addView(barView);
		maskLayout.setClipChildren(true);

		this.addView(maskLayout);
		this.addView(chargingIndicatorView);
		this.addView(chargingIndicatorViewForCenter);
		this.setClipChildren(true);

		RTL = (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == LAYOUT_DIRECTION_RTL);

		StatusbarMods.registerClockVisibilityCallback(this::setVisible);

		refreshLayout();

		BatteryBarView instance = this;
		BatteryDataProvider.registerInfoCallback(() -> instance.post(instance::refreshLayout));
	}
	@Override
	public void setLayoutDirection(int direction) {
		super.setLayoutDirection(direction);
	}

	private LayoutParams barLayoutParams() {
		LayoutParams result = new LayoutParams(getWidth(), barHeight);

		result.gravity = (isCenterBased) ? Gravity.CENTER : Gravity.START;

		result.gravity |= (onTop) ? Gravity.TOP : Gravity.BOTTOM;

		return result;
	}

	private LayoutParams charginLayoutParams() {
		DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
		float dp = 4f;
		int pixels = (int) (metrics.density * dp + 0.5f);
		LayoutParams result = new LayoutParams(pixels, barHeight);

		result.gravity = (RTL) ? Gravity.END : Gravity.START;
		result.gravity |= (onTop) ? Gravity.TOP : Gravity.BOTTOM;

		return result;
	}

	public void setBarHeight(int height) {
		barHeight = height;
		refreshLayout();
	}

	public void setColorful(boolean colorful) {
		this.colorful = colorful;
		refreshLayout();
	}

	public void setSingleColorTone(int colorTone) {
		this.singleColorTone = colorTone;
		refreshLayout();
	}

	public void refreshColors(int lenX, int lenY) {
		if (lenX == 0) return; //we're not ready yet
		refreshShadeColors();
		Paint mPaint = mDrawable.getPaint();
		mPaint.setShader(null);
		mDrawable.setIntrinsicWidth(lenX);
		mDrawable.setIntrinsicHeight(lenY);
		if (isFastCharging() && indicateFastCharging) //fast charging color
		{
			chargingIndicatorView.setBackgroundColor(fastChargingColor);
			chargingIndicatorViewForCenter.setBackgroundColor(fastChargingColor);
			mPaint.setColor(fastChargingColor);
		} else if (isCharging() && indicateCharging) //normal charging color
		{
			chargingIndicatorView.setBackgroundColor(chargingColor);
			chargingIndicatorViewForCenter.setBackgroundColor(chargingColor);
			mPaint.setColor(chargingColor);
		} else if (isPowerSaving() && indicatePowerSave) //power saving color
		{
			mPaint.setColor(powerSaveColor);
		} else if (!colorful || shadeColors == null) {                    //not charging color
			for (int i = 0; i < batteryLevels.size(); i++) {
				if (getCurrentLevel() <= batteryLevels.get(i)) {
					if (transitColors && i > 0) {
						float range = batteryLevels.get(i) - batteryLevels.get(i - 1);
						float currentPos = getCurrentLevel() - batteryLevels.get(i - 1);
						float ratio = currentPos / range;
						mPaint.setColor(ColorUtils.blendARGB(batteryColors[i - 1], batteryColors[i], ratio));
					} else {
						mPaint.setColor(batteryColors[i]);
					}
					return;
				}
			}
			mPaint.setColor(singleColorTone);
		} else                                    //it's colorful
		{
			float cX = isCenterBased ? lenX / 2f : ((RTL) ? lenX : 0);
			float cY = isCenterBased ? lenY / 2f : ((RTL) ? lenY : 0);
			float radius = isCenterBased ? lenX / 2f : lenX;

			RadialGradient colorfulShader = new RadialGradient(cX, cY, radius, shadeColors, shadeLevels, Shader.TileMode.CLAMP);
			mPaint.setShader(colorfulShader);
		}
	}

	private static void refreshShadeColors() {
		if (batteryColors == null || batteryLevels.isEmpty()) return;

		shadeColors = new int[batteryLevels.size() * 2 + 2];
		shadeLevels = new float[shadeColors.length];
		float prev = 0;
		for (int i = 0; i < batteryLevels.size(); i++) {
			float rangeLength = batteryLevels.get(i) - prev;
			shadeLevels[2 * i] = (prev + rangeLength * .3f) / 100;
			shadeColors[2 * i] = batteryColors[i];

			shadeLevels[2 * i + 1] = (batteryLevels.get(i) - rangeLength * .3f) / 100;
			shadeColors[2 * i + 1] = batteryColors[i];

			prev = batteryLevels.get(i);
		}

		shadeLevels[shadeLevels.length - 2] = (batteryLevels.get(batteryLevels.size() - 1) + (100 - batteryLevels.get(batteryLevels.size() - 1)) * .3f) / 100;
		shadeColors[shadeColors.length - 2] = Color.GREEN;
		shadeLevels[shadeLevels.length - 1] = 1f;
		shadeColors[shadeColors.length - 1] = Color.GREEN;
	}


	public void setAlphaPct(int alphaPct) {
		this.alphaPct = alphaPct;
		mDrawable.setAlpha(Math.round(alphaPct * 2.55f));
	}

	public void setEnabled(boolean enabled) {
		this.mIsEnabled = enabled;
		refreshVisibility();
	}

	public void setVisible(boolean visible) {
		this.mIsHidden = !visible;
		refreshVisibility();
	}

	private void refreshVisibility() {
		if (!mIsEnabled || mIsHidden || (onlyWhileCharging && !isCharging())) {
			barView.setVisibility(GONE);
			stopChargingAnimation();
		} else {
			barView.setVisibility(VISIBLE);

			if(isCharging() && animateCharging) {
				startChargingAnimation();
			} else {
				stopChargingAnimation();
			}
		}
	}

	public static BatteryBarView getInstance(Context context) {
		if (instance != null) return instance;
		return new BatteryBarView(context);
	}

	public static BatteryBarView getInstance() {
		return instance;
	}

	public static boolean hasInstance() {
		return (instance != null);
	}

	public void setCenterBased(boolean bbSetCentered) {
		isCenterBased = bbSetCentered;
	}
}
