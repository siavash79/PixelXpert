package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import android.content.Context;
import android.graphics.Point;
import android.view.View;
import android.view.ViewGroup;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XPrefs;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class GestureNavbarManager extends XposedModPack
{
	public static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;
	public static float widthFactor = 1f;
	//region Back gesture
	private static float backGestureHeightFractionLeft = 1f; // % of screen height. can be anything between 0 to 1
	private static float backGestureHeightFractionRight = 1f; // % of screen height. can be anything between 0 to 1
	private static boolean leftEnabled = true;
	//endregion
	private static boolean rightEnabled = true;
	//region pill size
	private static int GesPillHeightFactor = 100;
	//region pill color
	private static boolean navPillColorAccent = false;
	//endregion
	private Object mNavigationBarInflaterView = null;
	private int mLightColor, mDarkColor; //original navbar colors
	//endregion
	
	public GestureNavbarManager(Context context)
	{
		super(context);
	}
	
	public void updatePrefs(String... Key)
	{
		if(XPrefs.Xprefs == null)
			return;
		
		//region Back gesture
		leftEnabled = XPrefs.Xprefs.getBoolean("BackFromLeft", true);
		rightEnabled = XPrefs.Xprefs.getBoolean("BackFromRight", true);
		backGestureHeightFractionLeft = XPrefs.Xprefs.getInt("BackLeftHeight", 100) / 100f;
		backGestureHeightFractionRight = XPrefs.Xprefs.getInt("BackRightHeight", 100) / 100f;
		//endregion
		
		//region pill size
		widthFactor = XPrefs.Xprefs.getInt("GesPillWidthModPos", 50) * .02f;
		GesPillHeightFactor = XPrefs.Xprefs.getInt("GesPillHeightFactor", 100);
		
		if(Key.length > 0) {
			switch(Key[0]) {
				case "GesPillWidthModPos":
				case "GesPillHeightFactor":
					refreshNavbar();
					break;
			}
		}
		//endregion
		
		//region pill color
		navPillColorAccent = XPrefs.Xprefs.getBoolean("navPillColorAccent", false);
		//endregion
	}
	
	//region pill size
	private void refreshNavbar()
	{
		try {
			callMethod(mNavigationBarInflaterView, "clearViews");
			Object defaultLayout = callMethod(mNavigationBarInflaterView, "getDefaultLayout");
			callMethod(mNavigationBarInflaterView, "inflateLayout", defaultLayout);
		} catch(Throwable ignored) {
		}
	}
	//endregion
	
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable
	{
		if(! lpparam.packageName.equals(listenPackage))
			return;
		
		Class<?> NavigationBarInflaterViewClass = findClass("com.android.systemui.navigationbar.NavigationBarInflaterView", lpparam.classLoader);
		Class<?> NavigationHandleClass = findClass("com.android.systemui.navigationbar.gestural.NavigationHandle", lpparam.classLoader);
		Class<?> EdgeBackGestureHandlerClass = findClass("com.android.systemui.navigationbar.gestural.EdgeBackGestureHandler", lpparam.classLoader);
		
		//region Back gesture
		hookAllMethods(EdgeBackGestureHandlerClass, "isWithinInsets", new XC_MethodHook()
		{
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable
			{
				Point mDisplaySize = (Point) getObjectField(param.thisObject, "mDisplaySize");
				boolean isLeftSide = (int) (int) param.args[0] < (mDisplaySize.x / 3);
				if((isLeftSide && ! leftEnabled) || (! isLeftSide && ! rightEnabled)) {
					param.setResult(false);
					return;
				}
				
				int mEdgeHeight = isLeftSide ? Math.round(mDisplaySize.y * backGestureHeightFractionLeft) : Math.round(mDisplaySize.y * backGestureHeightFractionRight);
				
				if(mEdgeHeight != 0 && (int) param.args[1] < (mDisplaySize.y - (float) getObjectField(param.thisObject, "mBottomGestureHeight") - mEdgeHeight)) {
					param.setResult(false);
				}
			}
		});
		//endregion
		
		//region pill color
		hookAllConstructors(NavigationHandleClass, new XC_MethodHook()
		{
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable
			{
				//Let's remember the original colors
				mLightColor = getIntField(param.thisObject, "mLightColor");
				mDarkColor = getIntField(param.thisObject, "mDarkColor");
			}
		});
		
		hookAllMethods(NavigationHandleClass, "setDarkIntensity", new XC_MethodHook()
		{
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable
			{
				setObjectField(param.thisObject, "mLightColor", (navPillColorAccent) ? mContext.getResources()
				                                                                               .getColor(android.R.color.system_accent1_200, mContext.getTheme()) : mLightColor);
				setObjectField(param.thisObject, "mDarkColor", (navPillColorAccent) ? mContext.getResources()
				                                                                              .getColor(android.R.color.system_accent1_600, mContext.getTheme()) : mDarkColor);
			}
		});
		//endregion
		
		//region pill size
		hookAllMethods(NavigationHandleClass, "onDraw", new XC_MethodHook()
		{
			int mRadius = 0;
			
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable
			{
				if(GesPillHeightFactor != 100) {
					mRadius = getIntField(param.thisObject, "mRadius");
					setObjectField(param.thisObject, "mRadius", Math.round(mRadius * GesPillHeightFactor / 100f));
				}
			}
			
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable
			{
				if(mRadius > 0) {
					setObjectField(param.thisObject, "mRadius", mRadius);
				}
			}
		});
		
		
		hookAllConstructors(NavigationBarInflaterViewClass, new XC_MethodHook()
		{
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable
			{
				mNavigationBarInflaterView = param.thisObject;
				refreshNavbar();
			}
		});
		
		hookAllMethods(NavigationBarInflaterViewClass, "createView", new XC_MethodHook()
		{
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable
			{
				if(widthFactor != 1f) {
					String button = (String) callMethod(param.thisObject, "extractButton", param.args[0]);
					if(! button.equals("home_handle"))
						return;
					
					View result = (View) param.getResult();
					ViewGroup.LayoutParams resultLayoutParams = result.getLayoutParams();
					resultLayoutParams.width = Math.round(resultLayoutParams.width * widthFactor);
					result.setLayoutParams(resultLayoutParams);
				}
			}
		});
		//endregion
		
	}
	
	@Override
	public boolean listensTo(String packageName)
	{
		return listenPackage.equals(packageName);
	}
}
