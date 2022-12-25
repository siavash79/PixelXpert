package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getFloatField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.core.graphics.ColorUtils;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.utils.Helpers;
import sh.siava.AOSPMods.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class QSThemeManager extends XposedModPack {
	public static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;

	public static final int STATE_UNAVAILABLE = 0;
	public static final int STATE_INACTIVE = 1;
	public static final int STATE_ACTIVE = 2;

	private static boolean lightQSHeaderEnabled = false;
	private static boolean dualToneQSEnabled = false;
	private static boolean brightnessThickTrackEnabled = false;

	private Object mBehindColors;
	private boolean wasDark;
	private Class<?> UtilsClass = null;
	private Integer colorInactive = null;
	private int colorUnavailable;
	private int colorActive;
	private Drawable lightFooterShape = null;
	private Object mClockViewQSHeader;

	public QSThemeManager(Context context) {
		super(context);
		if (!listensTo(context.getPackageName())) return;

		lightFooterShape = makeFooterShape();
		wasDark = getIsDark();
	}

	@Override
	public void updatePrefs(String... Key) {
		if (Xprefs == null) return;

		dualToneQSEnabled = Xprefs.getBoolean("dualToneQSEnabled", false);
		Helpers.setOverlay("QSDualToneOverlay", dualToneQSEnabled, true, false);

		setLightQSHeader(Xprefs.getBoolean("LightQSPanel", false));
		boolean newbrightnessThickTrackEnabled = Xprefs.getBoolean("BSThickTrackOverlay", false);
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
		UtilsClass = findClass("com.android.settingslib.Utils", lpparam.classLoader);
		Class<?> FragmentHostManagerClass = findClass("com.android.systemui.fragments.FragmentHostManager", lpparam.classLoader);
		Class<?> ScrimControllerClass = findClass("com.android.systemui.statusbar.phone.ScrimController", lpparam.classLoader);
		Class<?> GradientColorsClass = findClass("com.android.internal.colorextraction.ColorExtractor$GradientColors", lpparam.classLoader);
//		Class<?> QSPanelControllerClass = findClass("com.android.systemui.qs.QSPanelController", lpparam.classLoader);
//		Class<?> QuickQSPanelControllerClass = findClass("com.android.systemui.qs.QuickQSPanelController", lpparam.classLoader);
		Class<?> InterestingConfigChangesClass = findClass("com.android.settingslib.applications.InterestingConfigChanges", lpparam.classLoader);
		Class<?> ScrimStateEnum = findClass("com.android.systemui.statusbar.phone.ScrimState", lpparam.classLoader);
		Class<?> QSIconViewImplClass = findClass("com.android.systemui.qs.tileimpl.QSIconViewImpl", lpparam.classLoader);
		Class<?> CentralSurfacesImplClass = findClass("com.android.systemui.statusbar.phone.CentralSurfacesImpl", lpparam.classLoader);
		Class<?> QSSecurityFooterClass = findClass("com.android.systemui.qs.QSSecurityFooter", lpparam.classLoader);
		Class<?> QSFgsManagerFooterClass = findClass("com.android.systemui.qs.QSFgsManagerFooter", lpparam.classLoader);
		Class<?> FooterActionsControllerClass = findClass("com.android.systemui.qs.FooterActionsController", lpparam.classLoader);
		Class<?> ClockClass = findClass("com.android.systemui.statusbar.policy.Clock", lpparam.classLoader);
		Class<?> QuickStatusBarHeaderClass = findClass("com.android.systemui.qs.QuickStatusBarHeader", lpparam.classLoader);

		hookAllMethods(QSIconViewImplClass, "updateIcon", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(lightQSHeaderEnabled
						&& !getIsDark()
						&& getIntField(param.args[1], "state") == STATE_ACTIVE)
				{
					((ImageView) param.args[0])
							.setImageTintList(
									ColorStateList
											.valueOf(colorInactive));
				}
			}
		});

		hookAllMethods(QSIconViewImplClass, "setIcon", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (lightQSHeaderEnabled
						&& !getIsDark())
				{
					if (param.args[0] instanceof ImageView
							&& getIntField(param.args[1], "state") == STATE_ACTIVE)
					{
						setObjectField(param.thisObject, "mTint", colorInactive);
					}
				}
			}
		});

		//White QS Clock bug
		hookAllMethods(QuickStatusBarHeaderClass, "onFinishInflate", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				mClockViewQSHeader = getObjectField(param.thisObject, "mClockView");
			}
		});

		//While QS Clock bug
		hookAllMethods(ClockClass, "onColorsChanged", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(lightQSHeaderEnabled && !SystemUtils.isDarkMode())
				{
					((TextView)mClockViewQSHeader).setTextColor(Color.BLACK);
				}
			}
		});

		hookAllConstructors(FooterActionsControllerClass, new XC_MethodHook() {
			@SuppressLint("DiscouragedApi")
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (!wasDark && lightQSHeaderEnabled) {
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

		hookAllConstructors(QSFgsManagerFooterClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (!wasDark && lightQSHeaderEnabled) {
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
				if (!wasDark && lightQSHeaderEnabled) {
					((View) getObjectField(param.thisObject, "mView")).setBackground(lightFooterShape.getConstantState().newDrawable().mutate());
				}
			}
		});

		hookAllConstructors(CentralSurfacesImplClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				applyOverlays(true);
			}
		});
		hookAllMethods(QSTileViewImplClass, "getLabelColorForState", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				int state = (int) param.args[0];

				if (!lightQSHeaderEnabled || wasDark || state < 2) return;
				param.setResult(Color.WHITE);
			}
		});

		hookAllMethods(QSTileViewImplClass, "getBackgroundColorForState", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (!lightQSHeaderEnabled || wasDark) return;
				if (colorInactive == null) {
					calculateColors();
				}
				int state = (int) param.args[0];
				switch (state) {
					case STATE_UNAVAILABLE:
						param.setResult(colorUnavailable);
						break;
					case STATE_INACTIVE:
						param.setResult(colorInactive);
						break;
					case STATE_ACTIVE:
						param.setResult(colorActive);
						break;
				}
			}
		});

		hookAllMethods(QSIconViewImplClass, "getIconColorForState", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (!wasDark && lightQSHeaderEnabled && ((int) param.args[1]) == 2) {
					param.setResult(Color.WHITE);
				}
			}
		});

		hookAllMethods(CentralSurfacesImplClass,
				"updateTheme", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						applyOverlays(false);
					}
				});

		try {
			mBehindColors = GradientColorsClass.newInstance();
		} catch (Exception ignored) {
		}

		hookAllMethods(ScrimControllerClass,
				"updateScrims", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						if (!dualToneQSEnabled) return;

						Object mScrimBehind = getObjectField(param.thisObject, "mScrimBehind");
						boolean mBlankScreen = (boolean) getObjectField(param.thisObject, "mBlankScreen");
						float alpha = getFloatField(mScrimBehind, "mViewAlpha");
						boolean animateBehindScrim = alpha != 0 && !mBlankScreen;

						callMethod(mScrimBehind, "setColors", mBehindColors, animateBehindScrim);
					}
				});

		hookAllMethods(ScrimControllerClass, "updateThemeColors", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				calculateColors();
			}
		});

		hookAllMethods(ScrimControllerClass,
				"updateThemeColors", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						if (!dualToneQSEnabled) return;

//                        Object mScrimBehind = getObjectField(param.thisObject, "mScrimBehind");

						@SuppressLint("DiscouragedApi") ColorStateList states = (ColorStateList) callStaticMethod(UtilsClass,
								"getColorAttr",
								mContext.getResources().getIdentifier("android:attr/colorSurfaceHeader", "attr", listenPackage),
								mContext);
						int surfaceBackground = states.getDefaultColor();

						@SuppressLint("DiscouragedApi") ColorStateList accentStates = (ColorStateList) callStaticMethod(UtilsClass, "getColorAttr", mContext.getResources().getIdentifier("colorAccent", "attr", "android"), mContext);
						int accent = accentStates.getDefaultColor();

						callMethod(mBehindColors, "setMainColor", surfaceBackground);
						callMethod(mBehindColors, "setSecondaryColor", accent);

						double contrast = ColorUtils.calculateContrast((int) callMethod(mBehindColors, "getMainColor"), Color.WHITE);

						callMethod(mBehindColors, "setSupportsDarkText", contrast > 4.5);
					}
				});

/*		hookAllConstructors(QSTileViewImplClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (!lightQSHeaderEnabled) return;

				@SuppressLint("DiscouragedApi") Object colorActive = callStaticMethod(UtilsClass, "getColorAttrDefaultColor",
						mContext.getResources().getIdentifier("android:attr/colorAccent", "attr", lpparam.packageName),
						mContext);

				setObjectField(param.thisObject, "colorActive", colorActive);
			}
		});*/

		hookAllMethods(ScrimControllerClass, "applyState", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (!lightQSHeaderEnabled) return;

				boolean mClipsQsScrim = (boolean) getObjectField(param.thisObject, "mClipsQsScrim");
				if (mClipsQsScrim) {
					setObjectField(param.thisObject, "mBehindTint", Color.TRANSPARENT);
				}
			}
		});

		Object[] constants = ScrimStateEnum.getEnumConstants();
		for (Object constant : constants) {
			String enumVal = constant.toString();
			switch (enumVal) {
				case "KEYGUARD":
					hookAllMethods(constant.getClass(),
							"prepare", new XC_MethodHook() {
								@Override
								protected void afterHookedMethod(MethodHookParam param) throws Throwable {
									if (!lightQSHeaderEnabled) return;
									boolean mClipQsScrim = (boolean) getObjectField(param.thisObject, "mClipQsScrim");
									if (mClipQsScrim) {
										Object mScrimBehind = getObjectField(param.thisObject, "mScrimBehind");
										int mTintColor = getIntField(mScrimBehind, "mTintColor");
										if (mTintColor != Color.TRANSPARENT) {
											setObjectField(mScrimBehind, "mTintColor", Color.TRANSPARENT);
											callMethod(mScrimBehind, "updateColorWithTint", false);
										}

										callMethod(mScrimBehind, "setViewAlpha", 1f);
									}
								}
							});
					break;
				case "BOUNCER":
					hookAllMethods(constant.getClass(),
							"prepare", new XC_MethodHook() {
								@Override
								protected void afterHookedMethod(MethodHookParam param) throws Throwable {
									if (!lightQSHeaderEnabled) return;

									setObjectField(param.thisObject, "mBehindTint", Color.TRANSPARENT);
								}
							});
					break;
				case "SHADE_LOCKED":
					hookAllMethods(constant.getClass(),
							"prepare", new XC_MethodHook() {
								@Override
								protected void afterHookedMethod(MethodHookParam param) throws Throwable {
									if (!lightQSHeaderEnabled) return;

									setObjectField(param.thisObject, "mBehindTint", Color.TRANSPARENT);
									boolean mClipQsScrim = (boolean) getObjectField(param.thisObject, "mClipQsScrim");
									if (mClipQsScrim) {
										Object mScrimBehind = getObjectField(param.thisObject, "mScrimBehind");
										int mTintColor = getIntField(mScrimBehind, "mTintColor");
										if (mTintColor != Color.TRANSPARENT) {
											setObjectField(mScrimBehind, "mTintColor", Color.TRANSPARENT);
											callMethod(mScrimBehind, "updateColorWithTint", false);
										}

										callMethod(mScrimBehind, "setViewAlpha", 1f);
									}
								}
							});
					hookAllMethods(constant.getClass(),
							"getBehindTint", new XC_MethodHook() {
								@Override
								protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
									if (!lightQSHeaderEnabled) return;
									param.setResult(Color.TRANSPARENT);
								}
							});
					break;

				case "UNLOCKED":
					hookAllMethods(constant.getClass(),
							"prepare", new XC_MethodHook() {
								@Override
								protected void afterHookedMethod(MethodHookParam param) throws Throwable {
									if (!lightQSHeaderEnabled) return;

									setObjectField(param.thisObject, "mBehindTint", Color.TRANSPARENT);

									Object mScrimBehind = getObjectField(param.thisObject, "mScrimBehind");
									int mTintColor = getIntField(mScrimBehind, "mTintColor");
									if (mTintColor != Color.TRANSPARENT) {
										setObjectField(mScrimBehind, "mTintColor", Color.TRANSPARENT);
										callMethod(mScrimBehind, "updateColorWithTint", false);
									}
									callMethod(mScrimBehind, "setViewAlpha", 1f);
								}
							});
					break;
			}
		}

		hookAllConstructors(FragmentHostManagerClass, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				setObjectField(param.thisObject,
						"mConfigChanges",
						InterestingConfigChangesClass.getDeclaredConstructor(int.class).newInstance(0x40000000 | 0x0004 | 0x0100 | 0x80000000 | 0x0200));
			}
		});

	}

	private void applyOverlays(boolean force) throws Throwable {
		boolean isDark = getIsDark();

		if (isDark == wasDark && !force) return;
		wasDark = isDark;

		calculateColors();

		Helpers.setOverlay("QSLightThemeOverlay", false, true, false);
		Helpers.setOverlay("QSLightThemeBSTOverlay", false, false, false);

		Thread.sleep(50);

		if (lightQSHeaderEnabled && !isDark) {

			Helpers.setOverlay("QSLightThemeOverlay", !brightnessThickTrackEnabled, true, false);
			Helpers.setOverlay("QSLightThemeBSTOverlay", brightnessThickTrackEnabled, false, false);
		}
	}

	@SuppressLint("DiscouragedApi")
	private void calculateColors() {
		Resources res = mContext.getResources();
		colorActive = res.getColor(
				res.getIdentifier("android:color/system_accent1_600", "color", listenPackage),
				mContext.getTheme());

		colorInactive = res.getColor(
				res.getIdentifier("android:color/system_accent1_10", "color", listenPackage),
				mContext.getTheme());

		colorUnavailable = applyAlpha(0.3f, colorInactive);

		lightFooterShape.setTint(colorInactive);
	}

	private boolean getIsDark() {
		return (mContext.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_YES) == Configuration.UI_MODE_NIGHT_YES;
	}

	@ColorInt
	public static int applyAlpha(float alpha, int inputColor) {
		alpha *= Color.alpha(inputColor);
		return Color.argb((int) (alpha), Color.red(inputColor), Color.green(inputColor),
				Color.blue(inputColor));
	}

	private Drawable makeFooterShape() {
		@SuppressLint("DiscouragedApi") int radius = mContext.getResources().getDimensionPixelSize(mContext.getResources().getIdentifier("qs_security_footer_corner_radius", "dimen", mContext.getPackageName()));
		float[] radiusF = new float[8];
		for (int i = 0; i < 8; i++) {
			radiusF[i] = radius;
		}
		final ShapeDrawable result = new ShapeDrawable(new RoundRectShape(radiusF, null, null));
		result.getPaint().setStyle(Paint.Style.FILL);
		return result;
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}
}