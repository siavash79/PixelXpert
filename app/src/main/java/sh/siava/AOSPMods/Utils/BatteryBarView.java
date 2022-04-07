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
		
		if(onlyWhileCharging && !isCharging) {
			barView.setVisibility(GONE);
			return;
		}
		barView.setVisibility(VISIBLE);
		maskLayout.setLayoutParams(maskLayoutParams());
		barView.setLayoutParams(barLayoutParams());
		XposedBridge.log("bw,bh " + barView.getWidth() + ","+ barView.getHeight());
		XposedBridge.log("mw,mh " + maskLayout.getWidth() + ","+ maskLayout.getHeight());
		XposedBridge.log("w,h " + getWidth() + ","+ getHeight());
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
		
		mDrawable.setColor(singleColorTone);
		mDrawable.setAlpha(alphaPct);
		mDrawable.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
		mDrawable.setShape(GradientDrawable.RECTANGLE);
		
		
		barView = new ImageView(mContext);
		barView.setImageDrawable(mDrawable);
		
		maskLayout = new FrameLayout(mContext);

		maskLayout.addView(barView);
		maskLayout.setClipChildren(true);
		
		this.addView(maskLayout);
		this.setClipChildren(true);
		refreshLayout();
	}
	
	private FrameLayout.LayoutParams barLayoutParams() {
		FrameLayout.LayoutParams result = new FrameLayout.LayoutParams(getWidth(), barHeight);

		result.gravity = (isCenterBased) ? Gravity.CENTER : Gravity.START;
		
		result.gravity |= (onTop) ? Gravity.TOP : Gravity.BOTTOM;
		
		return  result;
	}
	
	private void setBarHeight(int height)
	{
		barHeight = height;
		refreshLayout();
	}
	
	public void setColorful(boolean colorful) {
		this.colorful = colorful;
		if(colorful)
		{
			setCenterBased(isCenterBased);
		}
		else
		{
			mDrawable.setColor(singleColorTone);
		}
	}
	
	public void setSingleColorTone(int colorTone)
	{
		this.singleColorTone = colorTone;
		
		if(!colorful)
		{
			mDrawable.setColor(singleColorTone);
		}
	}
	
	public void setCenterBased(boolean centerBased)
	{
		isCenterBased = centerBased;
		if(centerBased)
		{
			colors = new int[] {Color.GREEN,Color.GREEN,Color.YELLOW,Color.RED,Color.YELLOW,Color.GREEN,Color.GREEN};
		}
		else
		{
			colors = new int[] {Color.RED,Color.YELLOW,Color.GREEN,Color.GREEN};
		}
		if(colorful)
		{
			mDrawable.setColors(colors);
		}
		refreshLayout();
	}
	
	public void setAlphaPct(int alphaPct) {
		this.alphaPct = alphaPct;
		mDrawable.setAlpha(alphaPct);
	}
	
	public void setRTL(boolean RTL)
	{
		this.RTL = RTL;
		mDrawable.setOrientation((RTL) ? GradientDrawable.Orientation.RIGHT_LEFT : GradientDrawable.Orientation.LEFT_RIGHT);
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
