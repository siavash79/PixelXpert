package sh.siava.AOSPMods.modpacks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.XPLauncher;
import sh.siava.AOSPMods.modpacks.XPrefs;
import sh.siava.AOSPMods.modpacks.XposedModPack;

@SuppressWarnings({"RedundantThrows", "unchecked", "rawtypes"})
public class BrightnessSlider extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	private Object brightnessSliderFactory = null;
	private Object brightnessControllerFactory = null;

	private static boolean BrightnessSliderOnBottom = false;
	private static boolean BrightnessHookEnabled = true;
	private static boolean QQSBrightnessEnabled = false;
	private static boolean QSBrightnessDisabled = false;
	private static boolean QQSBrightnessSupported = true;
	private Object QQSPC;
	private static final String QS_SHOW_BRIGHTNESS = "qs_show_brightness";
	private Object mBrightnessMirrorController;
	private Object mTunerService;
	private Object QSPanelController;
	private final ArrayList<Integer> collectedFields = new ArrayList<>();
	private Object mBrightnessController;
	private Object mBrightnessMirrorHandler;
	private View QQSBrightnessSliderView;
	static Class<?> BrightnessControllerClass = null;
	static Class<?> DejankUtilsClass = null;

	private boolean duringSliderPlacement = false;
	private Object QQSBrightnessSliderController;

	public BrightnessSlider(Context context) {
		super(context);
	}

	private void dataCollected(int id, Class<?> BrightnessMirrorHandlerClass) {
		if (!collectedFields.contains(id)) {
			collectedFields.add(id);
		}
		if (collectedFields.size() == 3) {
			try {
				createQQSBrightness(BrightnessMirrorHandlerClass);
			} catch (Throwable ignored) {}
		}
	}

	@Override
	public void updatePrefs(String... Key) {

		BrightnessHookEnabled = XPrefs.Xprefs.getBoolean("BrightnessHookEnabled", true);
		QQSBrightnessEnabled = XPrefs.Xprefs.getBoolean("QQSBrightnessEnabled", false);
		QSBrightnessDisabled = XPrefs.Xprefs.getBoolean("QSBrightnessDisabled", false);
		BrightnessSliderOnBottom = XPrefs.Xprefs.getBoolean("BrightnessSlierOnBottom", false);
		QQSBrightnessSupported = XPrefs.Xprefs.getBoolean("QQSBrightnessSupported", true);

		if (QSBrightnessDisabled) QQSBrightnessEnabled = false; //if there's no slider, then .......

		if (Key.length > 0) {
			switch (Key[0]) {
				case "QQSBrightnessEnabled":
				case "QSBrightnessDisabled":
				case "BrightnessSlierOnBottom":
					setQSVisibility();
					setQQSVisibility();
					break;
			}
		}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!BrightnessHookEnabled || !listenPackage.equals(lpparam.packageName)) //master switch
			return;

		Class<?> QSPanelControllerClass = findClass("com.android.systemui.qs.QSPanelController", lpparam.classLoader);
		Class<?> BrightnessMirrorHandlerClass = findClass("com.android.systemui.settings.brightness.BrightnessMirrorHandler", lpparam.classLoader);
		Class<?> QuickQSPanelControllerClass = findClass("com.android.systemui.qs.QuickQSPanelController", lpparam.classLoader);
		Class<?> QSPanelControllerBaseClass = findClass("com.android.systemui.qs.QSPanelControllerBase", lpparam.classLoader);
		Class<?> CentralSurfacesImplClass = findClassIfExists("com.android.systemui.statusbar.phone.CentralSurfacesImpl", lpparam.classLoader);
		Class<?> QSPanelClass = findClass("com.android.systemui.qs.QSPanel", lpparam.classLoader);
		Class<?> BrightnessSliderControllerClass = findClass("com.android.systemui.settings.brightness.BrightnessSliderController", lpparam.classLoader);
		Class<?> BrightnessSliderViewClass = findClass("com.android.systemui.settings.brightness.BrightnessSliderView", lpparam.classLoader);
		DejankUtilsClass = findClass("com.android.systemui.DejankUtils", lpparam.classLoader);
		BrightnessControllerClass = findClass("com.android.systemui.settings.brightness.BrightnessController", lpparam.classLoader);

		hookAllMethods(BrightnessSliderViewClass, "dispatchTouchEvent", new XC_MethodHook() { //responding to QQS slider touch event
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if(param.thisObject.equals(QQSBrightnessSliderView))
				{
					callMethod(QQSBrightnessSliderController, "mirrorTouchEvent", param.args[0]);
				}
			}
		});

		hookAllMethods(BrightnessSliderControllerClass, "onViewDetached", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if(duringSliderPlacement) //we're re-locating the slider. Don't remove the listeners
					param.setResult(null);
			}
		});

		hookAllConstructors(CentralSurfacesImplClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				new Thread(() -> {
					try {
						while (mBrightnessMirrorController == null) {
							//noinspection BusyWait
							Thread.sleep(500);
							mBrightnessMirrorController = getObjectField(param.thisObject, "mBrightnessMirrorController");
						}
						dataCollected(2, BrightnessMirrorHandlerClass);
					} catch (Throwable ignored) {
					}
				}).start();
			}
		});

		hookAllMethods(QSPanelClass, "setBrightnessViewMargin", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				setSliderMargins((View) getObjectField(param.thisObject, "mBrightnessView"));
			}
		});

		hookAllMethods(QSPanelControllerBaseClass, "setTiles", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				setQSVisibility();
				setQQSVisibility();
			}
		});

		hookAllConstructors(QSPanelControllerClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				QSPanelController = param.thisObject;

				brightnessControllerFactory = param.args[11];
				brightnessSliderFactory = param.args[12];
				mTunerService = getObjectField(param.thisObject, "mTunerService");

				dataCollected(0, BrightnessMirrorHandlerClass);

				setQSVisibility();
			}
		});

		hookAllConstructors(QuickQSPanelControllerClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
			}
		});

		hookAllMethods(QuickQSPanelControllerClass, "onInit", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				QQSPC = param.thisObject;

				dataCollected(1, BrightnessMirrorHandlerClass);
			}
		});

		hookAllMethods(QuickQSPanelControllerClass, "onViewAttached", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
			}
		});

		hookAllMethods(QuickQSPanelControllerClass, "onViewDetached", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				try {
					callMethod(mBrightnessMirrorHandler, "onQsPanelDettached");
					callMethod(mBrightnessController, "unregisterCallbacks");
				} catch (Throwable ignored) {
				}
			}
		});

		hookAllMethods(QuickQSPanelControllerClass, "setTiles", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
			}
		});
	}

	//swapping top and bottom margins of slider
	@SuppressLint("DiscouragedApi")
	private void setSliderMargins(View slider) {
		if (slider != null) {
			Resources res = mContext.getResources();
			ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) slider.getLayoutParams();

			int top = res.getDimensionPixelSize(
					res.getIdentifier("qs_brightness_margin_top", "dimen", mContext.getPackageName()));
			int bottom = res.getDimensionPixelSize(
					res.getIdentifier("qs_brightness_margin_bottom", "dimen", mContext.getPackageName()));

			lp.topMargin = (BrightnessSliderOnBottom)
					? bottom
					: top;
			lp.bottomMargin = (BrightnessSliderOnBottom)
					? top
					: bottom;
			slider.setLayoutParams(lp);
		}
	}

	private void setBrightnessView(ViewGroup QSPanelView, @NonNull View brightnessView, boolean isQQS) { //Classloader can't find the method by reflection
		View mBrightnessView = (View) getObjectField(QSPanelView, "mBrightnessView");
		duringSliderPlacement = true;

		if (mBrightnessView != null) {
			QSPanelView.removeView(mBrightnessView);
			setObjectField(QSPanelView, "mMovableContentStartIndex", getIntField(QSPanelView, "mMovableContentStartIndex") - 1);
		}
		try {
			((ViewGroup) brightnessView.getParent()).removeView(brightnessView);
		} catch (Throwable ignored) {
		}

		QSPanelView.addView(brightnessView, BrightnessSliderOnBottom
				? isQQS
					? 2
					: 1
				: 0);

		setObjectField(QSPanelView, "mBrightnessView", brightnessView);

		setSliderMargins(brightnessView);

		setObjectField(QSPanelView, "mMovableContentStartIndex", getIntField(QSPanelView, "mMovableContentStartIndex") + 1);
		duringSliderPlacement = false;
	}

	private void setQSVisibility() {
		try {
			ViewGroup mView = (ViewGroup) getObjectField(QSPanelController, "mView");
			mView.post(() -> {
				View mBrightnessView = (View) getObjectField(mView, "mBrightnessView");
				try {
					if (QSBrightnessDisabled) {
						mView.removeView(mBrightnessView);
					} else {
						setBrightnessView(mView, mBrightnessView, false);
					}
				} catch (Exception ignored) {
				}
			});
		} catch (Throwable ignored) {
		}

	}

	private void setQQSVisibility() {
		if (mBrightnessMirrorHandler == null) return; //Brightness slider isn't made
		ViewGroup QuickQSPanel = (ViewGroup) getObjectField(QQSPC, "mView");

		if (QuickQSPanel == null || QQSBrightnessSliderView == null) return;
		QuickQSPanel.post(() -> {
			try {
				if (QQSBrightnessEnabled) {
					setBrightnessView(QuickQSPanel, QQSBrightnessSliderView, true);
				} else {
					((ViewGroup) QQSBrightnessSliderView.getParent()).removeView(QQSBrightnessSliderView);
				}
			} catch (Exception ignored) {
			}
		});
	}

	private void createQQSBrightness(Class<?> BrightnessMirrorHandlerClass) throws Throwable {
		ViewGroup quickQSPanel = (ViewGroup) getObjectField(QQSPC, "mView");

		QQSBrightnessSliderController = callMethod(brightnessSliderFactory, "create", mContext, quickQSPanel);

		QQSBrightnessSliderView = (View) getObjectField(QQSBrightnessSliderController, "mView");

		if(!makeBrightnessController(QQSBrightnessSliderController))
		{
			return;
		}

		mBrightnessMirrorHandler = BrightnessMirrorHandlerClass.getConstructors()[0].newInstance(mBrightnessController);

		addTunable(mTunerService, quickQSPanel, QS_SHOW_BRIGHTNESS);
		setController(mBrightnessMirrorHandler, mBrightnessMirrorController);
		registerCallbacks(mBrightnessController);
		callMethod(QQSBrightnessSliderController, "init");

//        callMethod(mBrightnessController, "checkRestrictionAndSetEnabled"); //apparently not needed

		onQsPanelAttached(mBrightnessMirrorHandler);

		setQQSVisibility();
	}

	private boolean makeBrightnessController(Object mBrightnessSliderController) {
		mBrightnessController = null;
		try
		{ //13 QPR3
			mBrightnessController = BrightnessControllerClass.getConstructors()[0].newInstance(getObjectField(brightnessControllerFactory, "mContext"), mBrightnessSliderController, getObjectField(brightnessControllerFactory, "mUserTracker"),getObjectField(brightnessControllerFactory, "mDisplayTracker"), getObjectField(brightnessControllerFactory, "mMainExecutor"), getObjectField(brightnessControllerFactory, "mBackgroundHandler"));
		} catch (Throwable ignored){}

		if(mBrightnessController == null) {
			try { //13 QPR2
				mBrightnessController = BrightnessControllerClass.getConstructors()[0].newInstance(getObjectField(brightnessControllerFactory, "mContext"), mBrightnessSliderController, getObjectField(brightnessControllerFactory, "mUserTracker"), getObjectField(brightnessControllerFactory, "mMainExecutor"), getObjectField(brightnessControllerFactory, "mBackgroundHandler"));
			} catch (Throwable ignored) {}
		}
		if(mBrightnessController == null) {
			try { //13 QPR1
				mBrightnessController = BrightnessControllerClass.getConstructors()[0].newInstance(getObjectField(brightnessControllerFactory, "mContext"), mBrightnessSliderController, getObjectField(brightnessControllerFactory, "mBroadcastDispatcher"), getObjectField(brightnessControllerFactory, "mBackgroundHandler"));
			} catch (Throwable ignored) {}
		}

		if(mBrightnessController == null) {
			try { //some custom roms added icon into signature. like ArrowOS
				ImageView icon = (ImageView) callMethod(mBrightnessSliderController, "getIconView");
				mBrightnessController = callMethod(brightnessControllerFactory, "create", icon, mBrightnessSliderController);
			} catch (Throwable ignored) {}
		}

		if(mBrightnessController != null) {
			if (!QQSBrightnessSupported) {
				XPrefs.Xprefs.edit().putBoolean("QQSBrightnessSupported", true).apply();
			}
			return true;
		}
		else
		{
			if(QQSBrightnessSupported) {
				XPrefs.Xprefs.edit().putBoolean("QQSBrightnessSupported", false).apply();
			}
			return false;
		}
	}

	private static void onQsPanelAttached(Object mBrightnessMirrorHandler) {
		Object mirrorController = getObjectField(mBrightnessMirrorHandler, "mirrorController");
		if (mirrorController != null) {
			callMethod(mirrorController, "addCallback", getObjectField(mBrightnessMirrorHandler, "brightnessMirrorListener"));
		}
	}

	private static void registerCallbacks(Object mBrightnessController) {
		callMethod(getObjectField(mBrightnessController, "mBackgroundHandler"), "post", getObjectField(mBrightnessController, "mStartListeningRunnable"));
	}

	private static void setController(Object mirrorHandler, Object mirrorController) {
		Object currentMirrorController = getObjectField(mirrorHandler, "mirrorController");
		if (currentMirrorController != null) {
			callMethod(currentMirrorController, "removeCallback", getObjectField(mirrorHandler, "brightnessMirrorListener"));
		}
		setObjectField(mirrorHandler, "mirrorController", mirrorController);
		if (mirrorController != null) {
			Object listener = getObjectField(mirrorHandler, "brightnessMirrorListener"); //BrightnessMirrorController#Addcalback
			if (listener != null) {
				@SuppressWarnings("rawtypes") ArraySet mBrightnessMirrorListeners = (ArraySet) getObjectField(mirrorController, "mBrightnessMirrorListeners");
				//noinspection unchecked
				mBrightnessMirrorListeners.add(listener);
			}
//			callMethod(getObjectField(mirrorHandler, "brightnessController"), "setMirror", mirrorController); //R8 removed - using custom method below
			setMirror(getObjectField(mirrorHandler, "brightnessController"), mirrorController);
		}
	}

	private static void setMirror(Object brightnessController, Object mirrorController) //Brightness controller#setMirror
	{
		Object brightnessSliderController = getObjectField(brightnessController, "mControl");
		setObjectField(brightnessSliderController, "mMirrorController", mirrorController);
		Object toggleSlider = getObjectField(mirrorController, "mToggleSliderController");

		setObjectField(brightnessSliderController, "mMirror", toggleSlider);

		if(toggleSlider != null)
		{
			Object mView = getObjectField(toggleSlider, "mView");
			Object slider = getObjectField(mView, "mSlider");
			callMethod(toggleSlider, "setMax", callMethod(slider, "getMax"));
			callMethod(toggleSlider, "setValue", callMethod(slider, "getProgress"));
		}
	}

	private void addTunable(Object service, Object tunable, @SuppressWarnings("SameParameterValue") String key) {
		ConcurrentHashMap<String, Set<Object>> mTunableLookup = (ConcurrentHashMap<String, Set<Object>>) getObjectField(service, "mTunableLookup");
		if (!mTunableLookup.containsKey(key)) {
			mTunableLookup.put(key, new ArraySet());
		}
		mTunableLookup.get(key).add(tunable);

		Uri uri = Settings.Secure.getUriFor(key);

		ArrayMap<Uri, String> mListeningUris = (ArrayMap<Uri, String>) getObjectField(service, "mListeningUris");
		if (!mListeningUris.containsKey(uri)) {
			mListeningUris.put(uri, key);
			callMethod(getObjectField(service, "mContentResolver"), "registerContentObserver", uri, false, getObjectField(service, "mObserver"), getObjectField(service, "mCurrentUser"));
		}
		// Send the first state.
		Runnable runnable = () -> callStaticMethod(Settings.Secure.class,
				"getStringForUser", getObjectField(service, "mContentResolver"), key, getObjectField(service, "mCurrentUser"));
		String value = (String) callStaticMethod(DejankUtilsClass, "whitelistIpcs", runnable);
		callMethod(tunable, "onTuningChanged", key, value);
	}
}