package sh.siava.AOSPMods.modpacks.systemui;

import static android.graphics.Color.BLACK;
import static android.graphics.Paint.Style.FILL;
import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;

import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.XPLauncher;
import sh.siava.AOSPMods.modpacks.XPrefs;
import sh.siava.AOSPMods.modpacks.XposedModPack;
import sh.siava.AOSPMods.modpacks.utils.Helpers;
import sh.siava.AOSPMods.modpacks.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class QSThemeManager extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	public static final int STATE_ACTIVE = 2;

	private static boolean lightQSHeaderEnabled = false;
	private static boolean brightnessThickTrackEnabled = false;

	private boolean isDark;
	private Integer colorInactive = null;
	private int colorUnavailable;
	private int colorActive;
	private Drawable lightFooterShape = null;
	private Object mClockViewQSHeader;
	private Object mScrimBehindTint = BLACK;
	private Object unlockedScrimState;

	public QSThemeManager(Context context) {
		super(context);
		if (!listensTo(context.getPackageName())) return;

		lightFooterShape = getFooterShape(context);
		isDark = isDarkMode();
	}

	@Override
	public void updatePrefs(String... Key) {
		if (XPrefs.Xprefs == null) return;

		setLightQSHeader(XPrefs.Xprefs.getBoolean("LightQSPanel", false));
		boolean newbrightnessThickTrackEnabled = XPrefs.Xprefs.getBoolean("BSThickTrackOverlay", false);
		if (newbrightnessThickTrackEnabled != brightnessThickTrackEnabled) {
			brightnessThickTrackEnabled = newbrightnessThickTrackEnabled;
			try {
				applyOverlays(true);
			} catch (Throwable ignored) {
			}
		}
	}

	public void setLightQSHeader(boolean state) {
		if (lightQSHeaderEnabled != state) {
			lightQSHeaderEnabled = state;

			try {
				applyOverlays(true);
			} catch (Throwable ignored) {
			}
		}
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
		if (!lpparam.packageName.equals(listenPackage)) return;

		Class<?> QSTileViewImplClass = findClass("com.android.systemui.qs.tileimpl.QSTileViewImpl", lpparam.classLoader);
		Class<?> ScrimControllerClass = findClass("com.android.systemui.statusbar.phone.ScrimController", lpparam.classLoader);
		Class<?> QSPanelControllerClass = findClass("com.android.systemui.qs.QSPanelController", lpparam.classLoader);
		Class<?> ScrimStateEnum = findClass("com.android.systemui.statusbar.phone.ScrimState", lpparam.classLoader);
		Class<?> QSIconViewImplClass = findClass("com.android.systemui.qs.tileimpl.QSIconViewImpl", lpparam.classLoader);
		Class<?> CentralSurfacesImplClass = findClass("com.android.systemui.statusbar.phone.CentralSurfacesImpl", lpparam.classLoader);
		Class<?> ClockClass = findClass("com.android.systemui.statusbar.policy.Clock", lpparam.classLoader);
		Class<?> QuickStatusBarHeaderClass = findClass("com.android.systemui.qs.QuickStatusBarHeader", lpparam.classLoader);
		Class<?> BrightnessSliderViewClass = findClass("com.android.systemui.settings.brightness.BrightnessSliderView", lpparam.classLoader);

		try {
			Class<?> BatteryStatusChipClass = findClass("com.android.systemui.statusbar.BatteryStatusChip", lpparam.classLoader);
			hookAllMethods(BatteryStatusChipClass, "updateResources", new XC_MethodHook() { //background color of 14's charging chip. Fix for light QS theme situation
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					if (lightQSHeaderEnabled && !isDark)
						((LinearLayout) getObjectField(param.thisObject, "roundedContainer"))
								.getBackground()
								.setTint(colorActive);
				}
			});
		}
		catch (Throwable ignored){} //Android 13 or something

		hookAllMethods(BrightnessSliderViewClass, "onFinishInflate", new XC_MethodHook() { //setting brightness slider background to gray
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(!lightQSHeaderEnabled) return;

				Object slider = getObjectField(param.thisObject, "mSlider");

				LayerDrawable drawable = (LayerDrawable) callMethod(slider, "getProgressDrawable");

				DrawableWrapper sliderBackground = (DrawableWrapper) drawable.findDrawableByLayerId(android.R.id.background);
				sliderBackground.setTint(Color.GRAY);
			}
		});

		unlockedScrimState = Arrays.stream(ScrimStateEnum.getEnumConstants()).filter(c -> c.toString().equals("UNLOCKED")).findFirst().get();

		hookAllMethods(unlockedScrimState.getClass(), "prepare", new XC_MethodHook() { //after brightness adjustment
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(!lightQSHeaderEnabled) return;

				setObjectField(unlockedScrimState, "mBehindTint", mScrimBehindTint);
			}
		});

		hookAllConstructors(QSPanelControllerClass, new XC_MethodHook() { //important to calculate colors specially when material colors change
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				calculateColors();
			}
		});

		try { //13QPR1
			Class<?> QSSecurityFooterClass = findClass("com.android.systemui.qs.QSSecurityFooter", lpparam.classLoader);
			Class<?> QSFgsManagerFooterClass = findClass("com.android.systemui.qs.QSFgsManagerFooter", lpparam.classLoader);
			Class<?> FooterActionsControllerClass = findClass("com.android.systemui.qs.FooterActionsController", lpparam.classLoader);

			hookAllConstructors(QSFgsManagerFooterClass, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					if (!isDark && lightQSHeaderEnabled) {
						((View) getObjectField(param.thisObject, "mNumberContainer"))
								.getBackground()
								.setTint(colorInactive);
						((View) getObjectField(param.thisObject, "mTextContainer"))
								.setBackground(lightFooterShape.getConstantState().newDrawable().mutate()); //original has to be copied to new object otherwise will get affected by view changes
					}
				}
			});

			hookAllConstructors(QSSecurityFooterClass, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					if (!isDark && lightQSHeaderEnabled) {
						((View) getObjectField(param.thisObject, "mView")).setBackground(lightFooterShape.getConstantState().newDrawable().mutate());
					}
				}
			});

			hookAllConstructors(FooterActionsControllerClass, new XC_MethodHook() {
				@SuppressLint("DiscouragedApi")
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					if (!isDark && lightQSHeaderEnabled) {
						Resources res = mContext.getResources();
						ViewGroup view = (ViewGroup) param.args[0];

						view.findViewById(res.getIdentifier("multi_user_switch", "id", mContext.getPackageName())).getBackground().setTint(colorInactive);

						View settings_button_container = view.findViewById(res.getIdentifier("settings_button_container", "id", mContext.getPackageName()));
						settings_button_container.getBackground().setTint(colorInactive);

						((LinearLayout.LayoutParams)
								((ViewGroup) settings_button_container
										.getParent()
								).getLayoutParams()
						).setMarginEnd(0);
					}
				}
			});

			//White QS Clock bug - doesn't seem applicable on 13QPR3 and 14
			hookAllMethods(QuickStatusBarHeaderClass, "onFinishInflate", new XC_MethodHook() {
				@SuppressLint("DiscouragedApi")
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					View thisView = (View) param.thisObject;
					Resources res = mContext.getResources();

					mClockViewQSHeader = thisView.findViewById(res.getIdentifier("clock", "id", mContext.getPackageName()));
				}
			});

			//White QS Clock bug - doesn't seem applicable on 13QPR3 and 14
			hookAllMethods(ClockClass, "onColorsChanged", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					if(lightQSHeaderEnabled && isDark)
					{
						((TextView)mClockViewQSHeader).setTextColor(Color.BLACK);
					}
				}
			});

		}catch (Throwable ignored){ //13QPR2&3
			//QPR3
			Class<?> QSContainerImplClass = findClass("com.android.systemui.qs.QSContainerImpl", lpparam.classLoader);
			Class<?> ShadeHeaderControllerClass = findClassIfExists("com.android.systemui.shade.ShadeHeaderController", lpparam.classLoader);
			if(ShadeHeaderControllerClass == null) //13QPR2
			{
				ShadeHeaderControllerClass = findClass("com.android.systemui.shade.LargeScreenShadeHeaderController", lpparam.classLoader);
			}

			hookAllMethods(ShadeHeaderControllerClass, "onInit", new XC_MethodHook() { //setting colors for some icons
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					View mView = (View) getObjectField(param.thisObject, "mView");

					Object iconManager = getObjectField(param.thisObject, "iconManager");
					Object batteryIcon = getObjectField(param.thisObject, "batteryIcon");
					Object configurationControllerListener = getObjectField(param.thisObject, "configurationControllerListener");
					hookAllMethods(configurationControllerListener.getClass(), "onConfigChanged", new XC_MethodHook() {
						@SuppressLint("DiscouragedApi")
						@Override
						protected void afterHookedMethod(MethodHookParam param) throws Throwable {
							if (!lightQSHeaderEnabled) return;

							int textColor = SettingsLibUtilsProvider.getColorAttrDefaultColor(android.R.attr.textColorPrimary, mContext);

							((TextView) mView.findViewById(mContext.getResources().getIdentifier("clock", "id", mContext.getPackageName()))).setTextColor(textColor);
							((TextView) mView.findViewById(mContext.getResources().getIdentifier("date", "id", mContext.getPackageName()))).setTextColor(textColor);

							callMethod(iconManager, "setTint", textColor);

							for(int i = 1; i <= 3; i ++) {
								String id = String.format("carrier%s", i);

								((TextView) getObjectField(mView.findViewById(mContext.getResources().getIdentifier(id, "id", mContext.getPackageName())), "mCarrierText")).setTextColor(textColor);
								((ImageView) getObjectField(mView.findViewById(mContext.getResources().getIdentifier(id, "id", mContext.getPackageName())), "mMobileSignal")).setImageTintList(ColorStateList.valueOf(textColor));
								((ImageView) getObjectField(mView.findViewById(mContext.getResources().getIdentifier(id, "id", mContext.getPackageName())), "mMobileRoaming")).setImageTintList(ColorStateList.valueOf(textColor));
							}

							callMethod(batteryIcon, "updateColors", textColor, textColor, textColor);
						}
					});
				}
			});

			hookAllMethods(QSContainerImplClass, "updateResources", new XC_MethodHook() { //setting colors for more icons
				@SuppressLint("DiscouragedApi")
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					if (!isDark && lightQSHeaderEnabled) {
						Resources res = mContext.getResources();
						ViewGroup view = (ViewGroup) param.thisObject;

						View settings_button_container = view.findViewById(res.getIdentifier("settings_button_container", "id", mContext.getPackageName()));
						settings_button_container.getBackground().setTint(colorInactive);

						ImageView icon = settings_button_container.findViewById(res.getIdentifier("icon", "id", mContext.getPackageName()));
						icon.setImageTintList(ColorStateList.valueOf(Color.BLACK));

						((FrameLayout.LayoutParams)
								((ViewGroup) settings_button_container
										.getParent()
								).getLayoutParams()
						).setMarginEnd(0);

						ViewGroup parent = (ViewGroup) settings_button_container.getParent();
						for(int i = 0; i < 3; i++) //Security + Foreground services containers
						{
							parent.getChildAt(i).setBackground(lightFooterShape.getConstantState().newDrawable().mutate());
						}
					}
				}
			});
		}

		hookAllMethods(QSIconViewImplClass, "updateIcon", new XC_MethodHook() { //setting color for QS tile icons on light theme
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(lightQSHeaderEnabled
						&& !isDark
						&& getIntField(param.args[1], "state") == STATE_ACTIVE)
				{
					((ImageView) param.args[0])
							.setImageTintList(
									ColorStateList
											.valueOf(colorInactive));
				}
			}
		});

		hookAllConstructors(CentralSurfacesImplClass, new XC_MethodHook() { //SystemUI init
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				applyOverlays(true);
			}
		});

		hookAllConstructors(QSTileViewImplClass, new XC_MethodHook() { //setting tile colors in light theme
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (!lightQSHeaderEnabled || isDark) return;

				setObjectField(param.thisObject, "colorActive", colorActive);
				setObjectField(param.thisObject, "colorInactive", colorInactive);
				setObjectField(param.thisObject, "colorUnavailable", colorUnavailable);

				setObjectField(param.thisObject, "colorLabelActive", Color.WHITE);
			}
		});

		hookAllMethods(CentralSurfacesImplClass, "updateTheme", new XC_MethodHook() { //required to recalculate colors and overlays when dark is toggled
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						applyOverlays(false);
					}
				});

		hookAllMethods(ScrimControllerClass, "updateThemeColors", new XC_MethodHook() { //called when dark toggled or material change.
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				calculateColors();
			}
		});

		hookAllMethods(ScrimControllerClass, "applyState", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (!lightQSHeaderEnabled) return;

				boolean mClipsQsScrim = (boolean) getObjectField(param.thisObject, "mClipsQsScrim");
				if (mClipsQsScrim) {
					setObjectField(param.thisObject, "mBehindTint", mScrimBehindTint);
				}
			}
		});
	}

	private void applyOverlays(boolean force) throws Throwable {
		boolean isCurrentlyDark = isDarkMode();

		if (isCurrentlyDark == isDark && !force) return;

		isDark = isCurrentlyDark;

		calculateColors();

		Helpers.setOverlay("QSLightThemeOverlay", false, true, false);
		Helpers.setOverlay("QSLightThemeBSTOverlay", false, false, false);

		Thread.sleep(50);

		if (lightQSHeaderEnabled && !isCurrentlyDark)
		{
			Helpers.setOverlay("QSLightThemeOverlay", !brightnessThickTrackEnabled, true, false);
			Helpers.setOverlay("QSLightThemeBSTOverlay", brightnessThickTrackEnabled, false, false);
		}
	}

	@SuppressLint("DiscouragedApi")
	private void calculateColors() { //calculating dual-tone QS scrim color and tile colors
		if(!lightQSHeaderEnabled) return;

		mScrimBehindTint =  mContext.getColor(
				isDark
					? android.R.color.system_neutral1_1000
					: android.R.color.system_neutral1_100);

		setObjectField(unlockedScrimState, "mBehindTint", mScrimBehindTint);

		if (!isDark) {
			colorActive = mContext.getColor(android.R.color.system_accent1_600);

			colorInactive = mContext.getColor(android.R.color.system_accent1_10);

			colorUnavailable = applyAlpha(0.3f, colorInactive); //30% opacity of inactive color

			lightFooterShape.setTint(colorInactive);
		}
	}

	private boolean isDarkMode() {
		return SystemUtils.isDarkMode();
	}

	@ColorInt
	public static int applyAlpha(float alpha, int inputColor) {
		alpha *= Color.alpha(inputColor);
		return Color.argb((int) (alpha), Color.red(inputColor), Color.green(inputColor),
				Color.blue(inputColor));
	}

	private static Drawable getFooterShape(Context context) {
		Resources res = context.getResources();
		@SuppressLint("DiscouragedApi") int radius = res.getDimensionPixelSize(res.getIdentifier("qs_security_footer_corner_radius", "dimen", context.getPackageName()));
		float[] radiusF = new float[8];
		for (int i = 0; i < 8; i++) {
			radiusF[i] = radius;
		}
		final ShapeDrawable result = new ShapeDrawable(new RoundRectShape(radiusF, null, null));
		result.getPaint().setStyle(FILL);
		return result;
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}
}