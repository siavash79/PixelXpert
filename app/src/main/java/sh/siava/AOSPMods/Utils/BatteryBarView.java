package sh.siava.AOSPMods.Utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import de.robv.android.xposed.XposedBridge;

public class BatteryBarView extends FrameLayout {
	private GradientDrawable mDrawable = new GradientDrawable();
	FrameLayout maskLayout;
	private Context mContext;
	private boolean colorful = false;
	private int alphaPct = 100;
	private int singleColorTone = Color.WHITE;
	private boolean RTL = false;
	private boolean isCenterBased = false;
	private int[] colors;
	private int barHeight = 10;
	private ImageView barView;
	private int batteryPCT = 0;
	private boolean onTop = true;
	private static int initialLevel = 0;
	private static boolean initialCharging = false;
	private static BatteryBarView instance = null;
	private boolean isCharging = false;
	private boolean onlyWhileCharging = false;
	private boolean isEnabled = true;
	private boolean isHidden = false;
	
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
	}
	
	private FrameLayout.LayoutParams maskLayoutParams() {
		FrameLayout.LayoutParams result = new FrameLayout.LayoutParams(Math.round(getWidth()*batteryPCT/100f), ViewGroup.LayoutParams.MATCH_PARENT);
		result.gravity = (isCenterBased) ? Gravity.CENTER : Gravity.START;
		return  result;
	}
	
	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		super.onSizeChanged(w,h, oldw, oldh);
		refreshLayout();
	}
	
	private BatteryBarView(Context context){
		super(context);
		instance = this;
		this.mContext = context;
		this.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT));
		
		batteryPCT = initialLevel;
		isCharging = initialCharging;
		
		mDrawable.setShape(GradientDrawable.RECTANGLE);
		this.setSingleColorTone(singleColorTone);
		this.setAlphaPct(alphaPct);
		this.setRTL(false);
		
		barView = new ImageView(mContext);
		barView.setImageDrawable(mDrawable);
		
		maskLayout = new FrameLayout(mContext);

		maskLayout.addView(barView);
		maskLayout.setClipChildren(true);
		
		this.addView(maskLayout);
		this.setClipChildren(true);
		this.setRTL(this.getLayoutDirection() == LAYOUT_DIRECTION_RTL);
		refreshLayout();
	}
	
	@Override
	public void setLayoutDirection(int direction)
	{
		super.setLayoutDirection(direction);
		this.setRTL(direction == LAYOUT_DIRECTION_RTL);
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
		setCenterBased(isCenterBased);
	}
	
	public void setSingleColorTone(int colorTone)
	{
		this.singleColorTone = colorTone;
		setCenterBased(isCenterBased);
	}
	
	public void setCenterBased(boolean centerBased)
	{
		isCenterBased = centerBased;
		
		if(colorful)
		{
			if(centerBased)
			{
				colors = new int[] {Color.GREEN,Color.GREEN,Color.YELLOW,Color.RED,Color.YELLOW,Color.GREEN,Color.GREEN};
			}
			else
			{
				colors = new int[] {Color.RED,Color.YELLOW,Color.GREEN,Color.GREEN};
			}
		}
		else
		{
			colors = new int[] {singleColorTone, singleColorTone};
		}
		mDrawable.setColors(colors);
		
		refreshLayout();
	}
	
	public void setAlphaPct(int alphaPct) {
		this.alphaPct = alphaPct;
		mDrawable.setAlpha(Math.round(alphaPct*2.55f));
	}
	
	public void setRTL(boolean RTL)
	{
		this.RTL = RTL;
		mDrawable.setOrientation((RTL) ? GradientDrawable.Orientation.RIGHT_LEFT : GradientDrawable.Orientation.LEFT_RIGHT);
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
			XposedBridge.log("received update");
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
	public static void refreshInstance()
	{
		if(instance != null)
		{
			instance.refreshLayout();
		}
	}
	public static boolean hasInstance()
	{
		return (instance != null);
	}
}
