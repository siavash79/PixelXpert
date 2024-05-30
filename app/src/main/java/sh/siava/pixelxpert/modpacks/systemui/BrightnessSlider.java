package sh.siava.pixelxpert.modpacks.systemui;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;
import static sh.siava.pixelxpert.modpacks.utils.SystemUtils.isDarkMode;
import static sh.siava.pixelxpert.modpacks.utils.SystemUtils.sleep;
import static sh.siava.pixelxpert.modpacks.utils.toolkit.ReflectionTools.findMethod;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.ResourceManager;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.XposedModPack;

@SuppressWarnings({"RedundantThrows", "unchecked", "rawtypes"})
public class BrightnessSlider extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;
	public static final String SCREEN_BRIGHTNESS_MODE_KEY = "screen_brightness_mode";

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
	private static boolean QSAutoBrightnessToggle = false;
	private final ArrayList<QSAutoBrightnessCallback> mQSAutoBrightnessCallbacks = new ArrayList<>();
	private long mLastAutoToggleClick = 0;
	private static boolean LightQSPanel = false;

	public BrightnessSlider(Context context) {
		super(context);
	}

	private void dataCollected(int id, Class<?> BrightnessMirrorHandlerClass) {
		if (!collectedFields.contains(id)) {
			collectedFields.add(id);
		}

		if (collectedFields.size() == 3) {
			new Thread(() -> {
				try {
					sleep(5000);
					createQQSBrightness(BrightnessMirrorHandlerClass);
				} catch (Throwable ignored) {
				}
			}).start();
		}
	}


	@Override
	public void updatePrefs(String... Key) {

		BrightnessHookEnabled = Xprefs.getBoolean("BrightnessHookEnabled", true);
		QQSBrightnessEnabled = Xprefs.getBoolean("QQSBrightnessEnabled", false);
		QSBrightnessDisabled = Xprefs.getBoolean("QSBrightnessDisabled", false);
		BrightnessSliderOnBottom = Xprefs.getBoolean("BrightnessSlierOnBottom", false);
		QQSBrightnessSupported = Xprefs.getBoolean("QQSBrightnessSupported", true);
		QSAutoBrightnessToggle = Xprefs.getBoolean("QSAutoBrightnessToggle", false);
		LightQSPanel = Xprefs.getBoolean("LightQSPanel", false);

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

		mQSAutoBrightnessCallbacks.forEach(QSAutoBrightnessCallback::onSettingsChanged);
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		if (!BrightnessHookEnabled || !listenPackage.equals(lpParam.packageName)) //master switch
			return;

		Class<?> QSPanelControllerClass = findClass("com.android.systemui.qs.QSPanelController", lpParam.classLoader);
		Class<?> BrightnessMirrorHandlerClass = findClass("com.android.systemui.settings.brightness.BrightnessMirrorHandler", lpParam.classLoader);
		Class<?> QuickQSPanelControllerClass = findClass("com.android.systemui.qs.QuickQSPanelController", lpParam.classLoader);
		Class<?> QSPanelControllerBaseClass = findClass("com.android.systemui.qs.QSPanelControllerBase", lpParam.classLoader);
		Class<?> CentralSurfacesImplClass = findClassIfExists("com.android.systemui.statusbar.phone.CentralSurfacesImpl", lpParam.classLoader);
		Class<?> QSPanelClass = findClass("com.android.systemui.qs.QSPanel", lpParam.classLoader);
		Class<?> BrightnessSliderControllerClass = findClass("com.android.systemui.settings.brightness.BrightnessSliderController", lpParam.classLoader);
		Class<?> BrightnessSliderViewClass = findClass("com.android.systemui.settings.brightness.BrightnessSliderView", lpParam.classLoader);
		DejankUtilsClass = findClass("com.android.systemui.DejankUtils", lpParam.classLoader);
		BrightnessControllerClass = findClass("com.android.systemui.settings.brightness.BrightnessController", lpParam.classLoader);

		hookAllMethods(BrightnessSliderViewClass, "dispatchTouchEvent", new XC_MethodHook() { //responding to QQS slider touch event
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if(param.thisObject.equals(QQSBrightnessSliderView))
				{
					callMethod(QQSBrightnessSliderController, "mirrorTouchEvent", param.args[0]);
				}
			}
		});

		hookAllMethods(BrightnessSliderViewClass, "onFinishInflate", new XC_MethodHook() {
			@SuppressLint("DiscouragedApi")
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				Resources res = mContext.getResources();

				View slider = (View) getObjectField(param.thisObject, "mSlider");
				FrameLayout parent = (FrameLayout) slider.getParent();
				parent.removeView(slider);

				LinearLayout innerLayout = new LinearLayout(mContext);
				LinearLayout.LayoutParams innerLayoutParams = new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
				innerLayout.setLayoutParams(innerLayoutParams);

				parent.addView(innerLayout);

				View toggleView = new View(mContext);
				int toggleSize = res.getDimensionPixelSize(res.getIdentifier("brightness_mirror_height", "dimen", mContext.getPackageName()));
				LinearLayout.LayoutParams toggleViewParams = new LinearLayout.LayoutParams(toggleSize, toggleSize);
				toggleViewParams.weight = 0;
				toggleViewParams.setMarginStart(res.getDimensionPixelSize(res.getIdentifier("qs_tile_margin_horizontal", "dimen", mContext.getPackageName())));
				toggleView.setLayoutParams(toggleViewParams);

				toggleView.setOnClickListener(v -> {
					if(SystemClock.uptimeMillis() > mLastAutoToggleClick + 500) { //falsing prevention
						mLastAutoToggleClick = SystemClock.uptimeMillis();
						toggleAutoBrightness();
					}
				});

				innerLayout.addView(slider);
				innerLayout.addView(toggleView);

				LinearLayout.LayoutParams sliderParams = ((LinearLayout.LayoutParams)slider.getLayoutParams());
				sliderParams.weight = 100;
				sliderParams.width = 1;

				setAutoBrightnessVisibility(toggleView);
				setAutoBrightnessIcon(toggleView);

				mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("screen_brightness_mode"), false, new ContentObserver(new Handler(mContext.getMainLooper())) {
					@Override
					public void onChange(boolean selfChange) {
						setAutoBrightnessIcon(toggleView);
					}
				});

				mQSAutoBrightnessCallbacks.add(() -> setAutoBrightnessVisibility(toggleView));
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
							sleep(500);
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

	private void setAutoBrightnessVisibility(View toggleView) {
		toggleView.setVisibility(
				QSAutoBrightnessToggle
						? View.VISIBLE
						: View.GONE);
	}

	private void toggleAutoBrightness() {
		setAutoBrightness(!isAutoBrightnessEnabled());
	}

	private void setAutoBrightnessIcon(View brightnessToggle) {
		boolean enabled = isAutoBrightnessEnabled();

		OvalShape backgroundShape = new OvalShape();
		ShapeDrawable backgroundDrawable = new ShapeDrawable(backgroundShape);

		backgroundDrawable.setTint(getBTBackgroundColor(enabled));

		Drawable iconDrawable = ResourcesCompat.getDrawable(
				ResourceManager.modRes,
				enabled
						? R.drawable.ic_brightness_auto
						: R.drawable.ic_brightness_manual,
				mContext.getTheme());

		//noinspection DataFlowIssue
		iconDrawable.setTint(getBTIconColor(enabled));

		LayerDrawable toggleDrawable =new LayerDrawable(new Drawable[]{backgroundDrawable, iconDrawable});

		brightnessToggle.setBackground(toggleDrawable);
	}

	public int getBTIconColor(boolean enabled) {
		return (isDarkMode() || !LightQSPanel) == enabled
				? Color.BLACK
				: enabled
					? Color.WHITE //light theme, enabled
					: mContext.getColor(android.R.color.system_accent1_100); //dark, disabled
	}

	public int getBTBackgroundColor(boolean enabled)
	{
		if(isDarkMode() || !LightQSPanel)
		{
			if(enabled)
				return mContext.getColor(android.R.color.system_accent1_100);
			else
				return mContext.getColor(android.R.color.system_neutral1_800);
		}
		else
		{
			if(enabled)
				return mContext.getColor(android.R.color.system_accent1_600);
			else
				return mContext.getColor(android.R.color.system_accent1_10);
		}
	}

	private boolean isAutoBrightnessEnabled() {
		return Settings.System.getInt(mContext.getContentResolver(), SCREEN_BRIGHTNESS_MODE_KEY, 0) == 1;
	}
	private void setAutoBrightness(boolean enabled)
	{
		Settings.System.putInt(mContext.getContentResolver(), SCREEN_BRIGHTNESS_MODE_KEY, enabled ? 1 : 0);
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

		quickQSPanel.post(() -> {
			try { //don't crash systemUI if failed
				View mView = (View) getObjectField(QSPanelController, "mView"); //ViewController#getContext
				QQSBrightnessSliderController = callMethod(brightnessSliderFactory, "create", mView.getContext(), quickQSPanel);

				QQSBrightnessSliderView = (View) getObjectField(QQSBrightnessSliderController, "mView");

				if (!makeBrightnessController(QQSBrightnessSliderController)) {
					return;
				}

				try {
					mBrightnessMirrorHandler = BrightnessMirrorHandlerClass.getConstructors()[0].newInstance(mBrightnessController);
				} catch (Throwable ignored) {}

				addTunable(mTunerService, quickQSPanel, QS_SHOW_BRIGHTNESS);
				setController(mBrightnessMirrorHandler, mBrightnessMirrorController);
				registerCallbacks(mBrightnessController);
//			dumpClass(QQSBrightnessSliderController.getClass());

				findMethod(QQSBrightnessSliderController.getClass(), "^init.*")
						.invoke(QQSBrightnessSliderController);

//        callMethod(mBrightnessController, "checkRestrictionAndSetEnabled"); //apparently not needed

				onQsPanelAttached(mBrightnessMirrorHandler);

				setQQSVisibility();
			}
			catch (Throwable ignored){}
		});
	}

	private boolean makeBrightnessController(Object mBrightnessSliderController) {
		mBrightnessController = null;

		try
		{ //14 QPR1
			mBrightnessController = callMethod(brightnessControllerFactory, "create", mBrightnessSliderController);
		} catch (Throwable ignored){}

		//noinspection ConstantValue
		if(mBrightnessController == null) {
			try { //13 QPR3
				mBrightnessController = BrightnessControllerClass.getConstructors()[0].newInstance(getObjectField(brightnessControllerFactory, "mContext"), mBrightnessSliderController, getObjectField(brightnessControllerFactory, "mUserTracker"), getObjectField(brightnessControllerFactory, "mDisplayTracker"), getObjectField(brightnessControllerFactory, "mMainExecutor"), getObjectField(brightnessControllerFactory, "mBackgroundHandler"));
			} catch (Throwable ignored) {}
		}
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
				Xprefs.edit().putBoolean("QQSBrightnessSupported", true).apply();
			}
			return true;
		}
		else
		{
			if(QQSBrightnessSupported) {
				Xprefs.edit().putBoolean("QQSBrightnessSupported", false).apply();
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

	interface QSAutoBrightnessCallback
	{
		void onSettingsChanged();
	}
}