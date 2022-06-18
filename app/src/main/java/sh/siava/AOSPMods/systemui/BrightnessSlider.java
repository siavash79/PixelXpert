package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XPrefs;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class BrightnessSlider extends XposedModPack
{
	private static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;
	@SuppressLint("StaticFieldLeak")
	private static View QQSbrightnessSliderView = null;
	private static boolean BrightnessSlierOnBottom = false;
	private static boolean BrightnessHookEnabled = true;
	private static boolean QQSBrightnessEnabled = false;
	private static boolean QSBrightnessDisabled = false;
	private Object mBrightnessMirrorHandlerController = null;
	private Object brightnessSliderFactory = null;
	private Object brightnessControllerFactory = null;
	private Object QQSBrightnessSliderController = null;
	private View QSbrightnessSliderView = null;
	private Object BrightnessMirrorController = null;
	private Object QS, QQS;
	private ViewGroup QSParent;
	
	public BrightnessSlider(Context context)
	{
		super(context);
	}
	
	@Override
	public void updatePrefs(String... Key)
	{
		
		BrightnessHookEnabled = XPrefs.Xprefs.getBoolean("BrightnessHookEnabled", true);
		QQSBrightnessEnabled = XPrefs.Xprefs.getBoolean("QQSBrightnessEnabled", false);
		QSBrightnessDisabled = XPrefs.Xprefs.getBoolean("QSBrightnessDisabled", false);
		BrightnessSlierOnBottom = XPrefs.Xprefs.getBoolean("BrightnessSlierOnBottom", false);
		
		if(QSBrightnessDisabled)
			QQSBrightnessEnabled = false; //if there's no slider, then .......
		
		if(Key.length > 0) {
			switch(Key[0]) {
				case "QQSBrightnessEnabled":
				case "QSBrightnessDisabled":
				case "BrightnessSlierOnBottom":
					setQSVisibility();
					setQQSVisibility();
					break;
			}
		}
	}
	
	private void setQSVisibility()
	{
		try {
			QSParent.removeView(QSbrightnessSliderView);
		} catch(Exception ignored) {
		}
		try {
			if(! QSBrightnessDisabled) {
				Object mView = getObjectField(QS, "mView");
				callMethod(mView, "setBrightnessView", QSbrightnessSliderView);
			}
		} catch(Exception ignored) {
		}
	}
	
	private void setQQSVisibility()
	{
		if(QQSbrightnessSliderView == null)
			return;
		try {
			if(QQSBrightnessEnabled) {
				callMethod(QQS, "setBrightnessView", QQSbrightnessSliderView);
			} else {
				((ViewGroup) QQSbrightnessSliderView.getParent()).removeView(QQSbrightnessSliderView);
			}
		} catch(Exception ignored) {
		}
	}
	
	@Override
	public boolean listensTo(String packageName)
	{
		return listenPackage.equals(packageName);
	}
	
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable
	{
		if(! BrightnessHookEnabled || ! listenPackage.equals(lpparam.packageName)) //master switch
			return;
		
		Class<?> QuickQSPanelClass = findClass("com.android.systemui.qs.QuickQSPanel", lpparam.classLoader);
		Class<?> QSPanelControllerClass = findClass("com.android.systemui.qs.QSPanelController", lpparam.classLoader);
		Class<?> BrightnessMirrorHandlerClass = findClass("com.android.systemui.settings.brightness.BrightnessMirrorHandler", lpparam.classLoader);
		Class<?> QSPanelClass = findClass("com.android.systemui.qs.QSPanel", lpparam.classLoader);
		
		//Stealing info from Main QS
		hookAllMethods(QSPanelControllerClass, "setBrightnessMirror", new XC_MethodHook()
		{
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable
			{
				BrightnessMirrorController = param.args[0];
			}
		});
		
		hookAllMethods(QSPanelClass, "setBrightnessView", new XC_MethodHook()
		{
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable
			{
				if(! BrightnessSlierOnBottom)
					return;
				
				try {
					View v = (View) param.args[0];
					ViewGroup parent = (ViewGroup) v.getParent();
					parent.removeView(v);
					parent.addView(v, 1);
					setBottomSliderMargins(v);
				} catch(Exception ignored) {
				}
			}
		});
		
		hookAllMethods(QSPanelClass, "initialize", new XC_MethodHook()
		{
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable
			{
				setQSVisibility();
				setQQSVisibility();
			}
		});
		
		hookAllConstructors(QSPanelControllerClass, new XC_MethodHook()
		{
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable
			{
				QS = param.thisObject;
				brightnessControllerFactory = param.args[12];
				brightnessSliderFactory = param.args[13];
				Object mBrightnessSliderController = getObjectField(param.thisObject, "mBrightnessSliderController");
				QSbrightnessSliderView = (View) callMethod(mBrightnessSliderController, "getRootView");
				QSParent = (ViewGroup) QSbrightnessSliderView.getParent();
				
				setQSVisibility();
			}
		});
		//End Stealing info from Main QS
		
		//Making new Brightness Slider in QQS using stolen info
		hookAllConstructors(QuickQSPanelClass, new XC_MethodHook()
		{
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable
			{
				QQS = param.thisObject;
				if(BrightnessMirrorController == null)
					return;
				
				//Create new Slider
				QQSBrightnessSliderController = callMethod(brightnessSliderFactory, "create", mContext, param.thisObject);
				
				//Place it to QQS
				QQSbrightnessSliderView = (View) callMethod(QQSBrightnessSliderController, "getRootView");
				
				//Creating controller and handler
				Object mBrightnessController;
				try {
					mBrightnessController = callMethod(brightnessControllerFactory, "create", QQSBrightnessSliderController);
				} catch(Throwable e) //some custom roms added icon into signature. like ArrowOS
				{
					ImageView icon = (ImageView) callMethod(QQSBrightnessSliderController, "getIconView");
					mBrightnessController = callMethod(brightnessControllerFactory, "create", icon, QQSBrightnessSliderController);
				}
				mBrightnessMirrorHandlerController = BrightnessMirrorHandlerClass.getConstructors()[0].newInstance(mBrightnessController);
				callMethod(mBrightnessMirrorHandlerController, "setController", BrightnessMirrorController);
				
				//initialization
				callMethod(QQSBrightnessSliderController, "init");
				callMethod(mBrightnessController, "registerCallbacks");
				callMethod(mBrightnessController, "checkRestrictionAndSetEnabled");
				callMethod(mBrightnessMirrorHandlerController, "onQsPanelAttached");
				
				setQQSVisibility();
			}
		});
	}
	
	//swapping top and bottom margins of slider
	private void setBottomSliderMargins(View slider)
	{
		if(slider != null) {
			Resources res = mContext.getResources();
			ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) slider.getLayoutParams();
			lp.topMargin = res.getDimensionPixelSize(res.getIdentifier("qs_brightness_margin_bottom", "dimen", mContext.getPackageName()));
			lp.bottomMargin = res.getDimensionPixelSize(res.getIdentifier("qs_brightness_margin_top", "dimen", mContext.getPackageName()));
			slider.setLayoutParams(lp); //TODO: shall we really re-set it?
		}
	}
}
