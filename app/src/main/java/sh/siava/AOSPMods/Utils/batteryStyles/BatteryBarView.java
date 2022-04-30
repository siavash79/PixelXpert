package sh.siava.AOSPMods.Utils.batteryStyles;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.core.graphics.ColorUtils;

import java.util.Locale;

public class BatteryBarView extends FrameLayout {
	private static int[] shadeColors;
	private final boolean RTL;
	private static float[] shadeLevels = new float[0];
	private final ShapeDrawable mDrawable = new ShapeDrawable();
	FrameLayout maskLayout;
	private boolean colorful = false;
	private int alphaPct = 100;
	private int singleColorTone = Color.WHITE;
	private boolean isCenterBased = false;
	private int barHeight = 10;
	private final ImageView barView;
	private int batteryPCT;
	private boolean onTop = true;
	private static int initialLevel = 0;
	private static boolean initialCharging = false;
	@SuppressLint("StaticFieldLeak")
	private static BatteryBarView instance = null;
	private static boolean isCharging = false;
	private static boolean isFastCharging = false;
	private boolean onlyWhileCharging = false;
	private boolean isEnabled = true;
	private boolean isHidden = false;
	private static float[] batteryLevels = new float[]{20f,40f};
	private static int[] batteryColors = new int[]{Color.RED, Color.YELLOW};
	private static int chargingColor = Color.WHITE;
	private static int fastChargingColor = Color.WHITE;
	private static boolean indicateCharging = false;
	private static boolean indicateFastCharging = false;
	private static boolean transitColors = false;
	
	public static void setStaticColor(float[] batteryLevels, int[] batteryColors, boolean indicateCharging, int chargingColor, boolean indicateFastCharging, int fastChargingColor, boolean transitColors) {
		BatteryBarView.transitColors = transitColors;
		BatteryBarView.batteryLevels = batteryLevels;
		BatteryBarView.batteryColors = batteryColors;
		BatteryBarView.chargingColor = chargingColor;
		BatteryBarView.fastChargingColor = fastChargingColor;
		BatteryBarView.indicateCharging = indicateCharging;
		BatteryBarView.indicateFastCharging = indicateFastCharging;
	}
	
	public void setOnTop(boolean onTop)
	{
		this.onTop = onTop;
		
		refreshLayout();
	}
	
	public void setOnlyWhileCharging(boolean state)
	{
		onlyWhileCharging = state;
		refreshLayout();
	}
	
	public void setBatteryLevel(int level, boolean charging)
	{
		isCharging = charging;
		if(!charging) setIsFastCharging(false);
		batteryPCT = level;
		refreshLayout();
	}

	@Override
	public void onAttachedToWindow()
	{
		super.onAttachedToWindow();
		refreshLayout();
	}
	
	public void refreshLayout()
	{
		if(!isAttachedToWindow()) return;
		
		refreshVisibility();
		
		if(barView.getVisibility() == GONE) return;
		maskLayout.setLayoutParams(maskLayoutParams());
		barView.setLayoutParams(barLayoutParams());
		
		refreshColors(barView.getWidth(), barView.getHeight());
		mDrawable.invalidateSelf();
	}
	
	private FrameLayout.LayoutParams maskLayoutParams() {
		FrameLayout.LayoutParams result = new FrameLayout.LayoutParams(Math.round(getWidth()*batteryPCT/100f), ViewGroup.LayoutParams.MATCH_PARENT);
		result.gravity = (isCenterBased) ? Gravity.CENTER : Gravity.START;
		return  result;
	}
	
	@SuppressWarnings("SpellCheckingInspection")
	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		super.onSizeChanged(w,h, oldw, oldh);
		refreshLayout();
	}
	
	public BatteryBarView(Context context){
		super(context);
		instance = this;
		this.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT));

		batteryPCT = initialLevel;
		isCharging = initialCharging;
		
		mDrawable.setShape(new RectShape());
		this.setSingleColorTone(singleColorTone);
		this.setAlphaPct(alphaPct);
		
		barView = new ImageView(context);
		barView.setImageDrawable(mDrawable);
		
		maskLayout = new FrameLayout(context);

		maskLayout.addView(barView);
		maskLayout.setClipChildren(true);
		
		this.addView(maskLayout);
		this.setClipChildren(true);
		
		RTL=(TextUtils.getLayoutDirectionFromLocale(Locale.getDefault())==LAYOUT_DIRECTION_RTL);
		
		refreshLayout();
	}
	
	@Override
	public void setLayoutDirection(int direction)
	{
		super.setLayoutDirection(direction);
	}
	
	private FrameLayout.LayoutParams barLayoutParams() {
		FrameLayout.LayoutParams result = new FrameLayout.LayoutParams(getWidth(), barHeight);

		result.gravity = (isCenterBased) ? Gravity.CENTER : Gravity.START;
		
		result.gravity |= (onTop) ? Gravity.TOP : Gravity.BOTTOM;
		
		return  result;
	}
	
	public void setBarHeight(int height)
	{
		barHeight = height;
		refreshLayout();
	}
	
	public void setColorful(boolean colorful) {
		this.colorful = colorful;
		refreshLayout();
	}
	
	public void setSingleColorTone(int colorTone)
	{
		this.singleColorTone = colorTone;
		refreshLayout();
	}
	
	public void refreshColors(int lenX, int lenY)
	{
		if(lenX == 0) return; //we're not ready yet
		refreshShadeColors();
		Paint mPaint = mDrawable.getPaint();
		mPaint.setShader(null);
		mDrawable.setIntrinsicWidth(lenX);
		mDrawable.setIntrinsicHeight(lenY);
		if(isFastCharging && indicateFastCharging) //fast charging color
		{
			mPaint.setColor(fastChargingColor);
		}
		else if(isCharging && indicateCharging) //normal charging color
		{
			mPaint.setColor(chargingColor);
		}
		else if(!colorful || shadeColors == null) {                    //not charging color
			for (int i = 0; i < batteryLevels.length; i++) {
				if (batteryPCT <= batteryLevels[i]) {
					if(transitColors && i > 0)
					{
						float range = batteryLevels[i] - batteryLevels[i-1];
						float currentPos = batteryPCT - batteryLevels[i-1];
						float ratio = currentPos/range;
						mPaint.setColor(ColorUtils.blendARGB(batteryColors[i-1], batteryColors[i], ratio));
					}
					else {
						mPaint.setColor(batteryColors[i]);
					}
					return;
				}
			}
			mPaint.setColor(singleColorTone);
		}
		else                                    //it's colorful
		{
			float cX = isCenterBased ? lenX /2f : ((RTL) ? lenX : 0);
			float cY = isCenterBased ? lenY /2f : ((RTL) ? lenY : 0);
			float radius = isCenterBased ? lenX /2f : lenX;
			
			RadialGradient colorfulShader = new RadialGradient(cX, cY, radius, shadeColors, shadeLevels, Shader.TileMode.CLAMP);
			mPaint.setShader(colorfulShader);
		}
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
	
	
	public void setAlphaPct(int alphaPct) {
		this.alphaPct = alphaPct;
		mDrawable.setAlpha(Math.round(alphaPct*2.55f));
	}
	
	public void setEnabled(boolean enabled)
	{
		this.isEnabled = enabled;
		refreshVisibility();
	}
	public void setVisible(boolean visible)
	{
		this.isHidden = !visible;
		refreshVisibility();
	}
	
	private void refreshVisibility() {
		if(!isEnabled || isHidden || (onlyWhileCharging && !isCharging)) {
			barView.setVisibility(GONE);
		}
		else {
			barView.setVisibility(VISIBLE);
		}
	}
	
	
	public static void setStaticLevel(int level, boolean charging)
	{
		initialLevel = level;
		initialCharging = charging;
		if(instance != null)
		{
			instance.setBatteryLevel(level, charging);
		}
	}
	
	public static BatteryBarView getInstance(Context context)
	{
		if(instance != null) return instance;
		return new BatteryBarView(context);
	}
	public static BatteryBarView getInstance()
	{
		return instance;
	}
	public static boolean hasInstance()
	{
		return (instance != null);
	}
	
	public static void setIsFastCharging(boolean isFast)
	{
		if(isFast != isFastCharging) {
			isFastCharging = isFast;
			if(isFast) isCharging = true;
			if(hasInstance()) {
				instance.refreshLayout();
			}
		}
	}
	
	public void setCenterBased(boolean bbSetCentered) {
		isCenterBased = bbSetCentered;
	}
}
