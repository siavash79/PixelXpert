package sh.siava.AOSPMods.systemui;

import static android.service.quicksettings.Tile.STATE_ACTIVE;
import static android.service.quicksettings.Tile.STATE_INACTIVE;
import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.ResourceManager.resparams;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.VibrationEffect;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.utils.SystemUtils;
import sh.siava.rangesliderpreference.RangeSliderPreference;

@SuppressWarnings("RedundantThrows")
public class QSTileGrid extends XposedModPack {
	public static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;

	private boolean replaced = false;
	private int quick_settings_max_rows = 0, quick_settings_num_columns = 0, quick_qs_panel_max_tiles = 0;
	private static final int NOT_SET = 0;
	private static final int QQS_NOT_SET = 4;

	private static int QSRowQty = NOT_SET;
	private static int QSColQty = NOT_SET;
	private static int QQSTileQty = QQS_NOT_SET;

	private static Float labelSize = null, secondaryLabelSize = null;
	private static int labelSizeUnit = -1, secondaryLabelSizeUnit = -1;

	private static float QSLabelScaleFactor = 1, QSSecondaryLabelScaleFactor = 1;
	private static boolean QRTileInactiveColor = false;

	private static boolean QSHapticEnabled = false;
	private static boolean VerticalQSTile = false;

	public QSTileGrid(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		if (Xprefs == null) return;

		VerticalQSTile = Xprefs.getBoolean("VerticalQSTile", false);

		if(Key.length > 0 && Key[0].equals("VerticalQSTile"))
		{
			SystemUtils.doubleToggleDarkMode();
		}

		QSHapticEnabled = Xprefs.getBoolean("QSHapticEnabled", false);

		QSRowQty = Xprefs.getInt("QSRowQty", NOT_SET);
		QSColQty = Xprefs.getInt("QSColQty", NOT_SET);
		QQSTileQty = Xprefs.getInt("QQSTileQty", QQS_NOT_SET);

		QRTileInactiveColor = Xprefs.getBoolean("QRTileInactiveColor", false);

		try {
			QSLabelScaleFactor = (RangeSliderPreference.getValues(Xprefs, "QSLabelScaleFactor", 0).get(0) + 100) / 100f;
			QSSecondaryLabelScaleFactor = (RangeSliderPreference.getValues(Xprefs, "QSSecondaryLabelScaleFactor", 0).get(0) + 100) / 100f;
		} catch (Exception ignored) {
		}

		setResources();

		if (Key.length > 0 && (Key[0].equals("QSRowQty") || Key[0].equals("QSColQty") || Key[0].equals("QQSTileQty"))) {
			SystemUtils.doubleToggleDarkMode();
		}
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(listenPackage)) return;

		Class<?> TileLayoutClass = findClass("com.android.systemui.qs.TileLayout", lpparam.classLoader);
		Class<?> QSTileViewImplClass = findClass("com.android.systemui.qs.tileimpl.QSTileViewImpl", lpparam.classLoader);
		Class<?> FontSizeUtilsClass = findClass("com.android.systemui.FontSizeUtils", lpparam.classLoader);
		Class<?> QSTileImplClass = findClass("com.android.systemui.qs.tileimpl.QSTileImpl", lpparam.classLoader);

		XC_MethodHook vibrateCallback = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (QSHapticEnabled) SystemUtils.vibrate(VibrationEffect.EFFECT_CLICK);
			}
		};

		hookAllMethods(QSTileImplClass, "click", vibrateCallback);
		hookAllMethods(QSTileImplClass, "longClick", vibrateCallback);

		hookAllMethods(QSTileViewImplClass, "onLayout", new XC_MethodHook() { //dimension is hard-coded in the layout file. can reset anytime without prior notice. So we set them at layout stage
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				setLabelSizes(param);
			}
		});

		Class<?> QRCodeScannerTileClass = findClassIfExists("com.android.systemui.qs.tiles.QRCodeScannerTile", lpparam.classLoader);

		if (QRCodeScannerTileClass != null) {
			hookAllMethods(QRCodeScannerTileClass, "handleUpdateState", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					if (!QRTileInactiveColor) return;

					if (getObjectField(param.args[0], "state").equals(STATE_ACTIVE)) {
						setObjectField(param.args[0], "state", STATE_INACTIVE);
					}
				}
			});
		}

		hookAllMethods(QSTileViewImplClass, "onConfigurationChanged", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(VerticalQSTile)
					fixPaddingVerticalLayout((LinearLayout) param.thisObject);
			}
		});

		hookAllConstructors(QSTileViewImplClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				try {
					if(VerticalQSTile) {
						LinearLayout thisQSTileView = (LinearLayout) param.thisObject;

						thisQSTileView.setGravity(Gravity.CENTER);
						thisQSTileView.setOrientation(LinearLayout.VERTICAL);

						((TextView) getObjectField(param.thisObject, "label"))
								.setGravity(Gravity.CENTER_HORIZONTAL);
						((TextView) getObjectField(param.thisObject, "secondaryLabel"))
								.setGravity(Gravity.CENTER_HORIZONTAL);

						LinearLayout horizontalLinearLayout = new LinearLayout(mContext);
						horizontalLinearLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

						LinearLayout labelContainer = (LinearLayout) getObjectField(param.thisObject, "labelContainer");
						thisQSTileView.removeView(labelContainer);
						horizontalLinearLayout.addView(labelContainer);

						labelContainer.setGravity(Gravity.CENTER_HORIZONTAL);

						thisQSTileView.removeView((View) getObjectField(param.thisObject, "sideView"));

						fixPaddingVerticalLayout(thisQSTileView);

						thisQSTileView.addView(horizontalLinearLayout);
					}

					if (labelSize == null) { //we need initial font sizes
						if (Build.VERSION.SDK_INT == 33) {
							callStaticMethod(FontSizeUtilsClass,
									"updateFontSize",
									mContext.getResources().getIdentifier("qs_tile_text_size", "dimen", mContext.getPackageName()),
									getObjectField(param.thisObject, "label"));

							callStaticMethod(FontSizeUtilsClass,
									"updateFontSize",
									mContext.getResources().getIdentifier("qs_tile_text_size", "dimen", mContext.getPackageName()),
									getObjectField(param.thisObject, "secondaryLabel"));


						} else {
							callStaticMethod(FontSizeUtilsClass,
									"updateFontSize",
									getObjectField(param.thisObject, "label"),
									mContext.getResources().getIdentifier("qs_tile_text_size", "dimen", mContext.getPackageName()));

							callStaticMethod(FontSizeUtilsClass,
									"updateFontSize",
									getObjectField(param.thisObject, "secondaryLabel"),
									mContext.getResources().getIdentifier("qs_tile_text_size", "dimen", mContext.getPackageName()));
						}

						TextView label = (TextView) getObjectField(param.thisObject, "label");
						TextView secondaryLabel = (TextView) getObjectField(param.thisObject, "secondaryLabel");

						labelSizeUnit = label.getTextSizeUnit();
						labelSize = label.getTextSize();

						secondaryLabelSizeUnit = secondaryLabel.getTextSizeUnit();
						secondaryLabelSize = secondaryLabel.getTextSize();
					}
				} catch (Throwable ignored) {
				}
			}
		});

		// when media is played, system reverts tile cols to default value of 2. handling it:
		hookAllMethods(TileLayoutClass, "setMaxColumns", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (QSColQty != NOT_SET) {
					param.args[0] = QSColQty;
				}
			}
		});

		setResources();
	}

	private void fixPaddingVerticalLayout(LinearLayout parent) {
		Resources res = mContext.getResources();

		int padding = res.getDimensionPixelSize(
				res.getIdentifier(
						"qs_tile_padding",
						"dimen",
						mContext.getPackageName()
				)
		);

		parent.setPadding(padding,padding,padding,padding);

		((LinearLayout.LayoutParams) ((LinearLayout) getObjectField(parent, "labelContainer"))
				.getLayoutParams())
				.setMarginStart(0);
	}

	private void setLabelSizes(XC_MethodHook.MethodHookParam param) {
		try {
			if (QSLabelScaleFactor != 1) {
				((TextView) getObjectField(param.thisObject, "label")).setTextSize(labelSizeUnit, labelSize * QSLabelScaleFactor);
			}

			if (QSSecondaryLabelScaleFactor != 1) {
				((TextView) getObjectField(param.thisObject, "secondaryLabel")).setTextSize(secondaryLabelSizeUnit, secondaryLabelSize * QSSecondaryLabelScaleFactor);
			}
		} catch (Throwable ignored) {
		}
	}

	private void setResources() {
		XC_InitPackageResources.InitPackageResourcesParam ourResparam = resparams.get(listenPackage);

		if (ourResparam == null) return;

		try {
			if (quick_settings_max_rows == 0) {
				quick_settings_num_columns = mContext.getResources().getInteger(mContext.getResources().getIdentifier("quick_settings_num_columns", "integer", mContext.getPackageName()));
				quick_settings_max_rows = mContext.getResources().getInteger(mContext.getResources().getIdentifier("quick_settings_max_rows", "integer", mContext.getPackageName()));
				quick_qs_panel_max_tiles = mContext.getResources().getInteger(mContext.getResources().getIdentifier("quick_qs_panel_max_tiles", "integer", mContext.getPackageName()));
			}

			if (replaced || QQSTileQty != QQS_NOT_SET) {
				ourResparam.res.setReplacement(ourResparam.packageName, "integer", "quick_qs_panel_max_tiles", QQSTileQty == QQS_NOT_SET ? quick_qs_panel_max_tiles : QQSTileQty);
				replaced = true;
			}

			if (replaced || QSColQty != NOT_SET) {
				ourResparam.res.setReplacement(ourResparam.packageName, "integer", "quick_settings_num_columns", QSColQty == NOT_SET ? quick_settings_num_columns : QSColQty);
				replaced = true;
			}

			if (replaced || QSRowQty != NOT_SET) {
				ourResparam.res.setReplacement(ourResparam.packageName, "integer", "quick_settings_max_rows", QSRowQty == NOT_SET ? quick_settings_max_rows : QSRowQty);
				replaced = true;
			}
		} catch (Throwable ignored) {
		}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}
}