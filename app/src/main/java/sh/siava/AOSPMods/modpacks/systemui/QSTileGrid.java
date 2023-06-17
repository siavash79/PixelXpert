package sh.siava.AOSPMods.modpacks.systemui;

import static android.service.quicksettings.Tile.STATE_ACTIVE;
import static android.service.quicksettings.Tile.STATE_INACTIVE;
import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.modpacks.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.ResourceManager;
import sh.siava.AOSPMods.modpacks.XPLauncher;
import sh.siava.AOSPMods.modpacks.XposedModPack;
import sh.siava.AOSPMods.modpacks.utils.SystemUtils;
import sh.siava.rangesliderpreference.RangeSliderPreference;

@SuppressWarnings("RedundantThrows")
public class QSTileGrid extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	private boolean replaced = false;
	private int quick_settings_max_rows_l = 0, quick_settings_num_columns_l = 0, quick_qs_panel_max_tiles_l = 0;
	private int quick_settings_max_rows_p = 0, quick_settings_num_columns_p = 0, quick_qs_panel_max_tiles_p = 0;

	private static final int NOT_SET = 0;
	private static final int QS_COL_NOT_SET = 1;
	private static final int QQS_NOT_SET = 4;

	private static int QSRowQty = NOT_SET;
	private static int QSColQty = QS_COL_NOT_SET;
	private static int QQSTileQty = QQS_NOT_SET;

	private static int QSRowQtyL = NOT_SET;
	private static int QSColQtyL = QS_COL_NOT_SET;
	private static int QQSTileQtyL = QQS_NOT_SET;

	private static Float labelSize = null, secondaryLabelSize = null;
	private static int labelSizeUnit = -1, secondaryLabelSizeUnit = -1;

	private static float QSLabelScaleFactor = 1, QSSecondaryLabelScaleFactor = 1;
	private static boolean QRTileInactiveColor = false;

	protected static boolean QSHapticEnabled = false;
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
		QSColQty = Xprefs.getInt("QSColQty", QS_COL_NOT_SET);
		if(QSColQty < QS_COL_NOT_SET) QSColQty = QS_COL_NOT_SET;
		QQSTileQty = Xprefs.getInt("QQSTileQty", QQS_NOT_SET);

		QSRowQtyL = Xprefs.getInt("QSRowQtyL", NOT_SET);
		QSColQtyL = Xprefs.getInt("QSColQtyL", QS_COL_NOT_SET);
		if(QSColQtyL < QS_COL_NOT_SET) QSColQtyL = QS_COL_NOT_SET;
		QQSTileQtyL = Xprefs.getInt("QQSTileQtyL", QQS_NOT_SET);

		QRTileInactiveColor = Xprefs.getBoolean("QRTileInactiveColor", false);

		try {
			QSLabelScaleFactor = (RangeSliderPreference.getValues(Xprefs, "QSLabelScaleFactor", 0).get(0) + 100) / 100f;
			QSSecondaryLabelScaleFactor = (RangeSliderPreference.getValues(Xprefs, "QSSecondaryLabelScaleFactor", 0).get(0) + 100) / 100f;
		} catch (Exception ignored) {
		}

		setResources(mContext.getResources().getConfiguration());

		if (Key.length > 0 && (Key[0].equals("QSRowQty") || Key[0].equals("QSColQty") || Key[0].equals("QQSTileQty") || Key[0].equals("QSRowQtyL") || Key[0].equals("QSColQtyL") || Key[0].equals("QQSTileQtyL"))) {
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
		Class<?> QSFactoryImplClass = findClass("com.android.systemui.qs.tileimpl.QSFactoryImpl", lpparam.classLoader);
		Class<?> SystemUIApplicationClass = findClass("com.android.systemui.SystemUIApplication", lpparam.classLoader);

		try {
			if(findClassIfExists("com.android.systemui.qs.tiles.WifiTile", lpparam.classLoader) == null)
				Xprefs
						.edit()
						.putBoolean("InternetTileModEnabled", false)
						.apply();
		}
		catch (Throwable ignored){}

		hookAllMethods(SystemUIApplicationClass, "onConfigurationChanged", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				setResources((Configuration) param.args[0]); //rotation detection
			}
		});

		//used to enable dual wifi/cell tiles for 13
		hookAllMethods(QSFactoryImplClass, "createTile", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if(param.args[0].equals("wifi_AOSPMods"))
				{
					param.args[0] = "wifi";
				}
				if(param.args[0].equals("cell_AOSPMods"))
				{
					param.args[0] = "cell";
				}
			}
		});


		XC_MethodHook vibrateCallback = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (QSHapticEnabled) SystemUtils.vibrate(VibrationEffect.EFFECT_CLICK, VibrationAttributes.USAGE_TOUCH);
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
			@SuppressLint("DiscouragedApi")
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
						callStaticMethod(FontSizeUtilsClass,
								"updateFontSize",
								mContext.getResources().getIdentifier("qs_tile_text_size", "dimen", mContext.getPackageName()),
								getObjectField(param.thisObject, "label"));

						callStaticMethod(FontSizeUtilsClass,
								"updateFontSize",
								mContext.getResources().getIdentifier("qs_tile_text_size", "dimen", mContext.getPackageName()),
								getObjectField(param.thisObject, "secondaryLabel"));

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
				Context context = (Context) getObjectField(param.thisObject, "mContext");
				boolean isLandscape = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
				if (!isLandscape && QSColQty != QS_COL_NOT_SET) {
					param.args[0] = QSColQty;
				}
				else if (isLandscape && QSColQtyL != QS_COL_NOT_SET)
				{
					param.args[0] = QSColQtyL;
				}
			}
		});

		setResources(mContext.getResources().getConfiguration());
	}

	private void fixPaddingVerticalLayout(LinearLayout parent) {
		Resources res = mContext.getResources();

		@SuppressLint("DiscouragedApi") int padding = res.getDimensionPixelSize(
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

	@SuppressLint("DiscouragedApi")
	private void setResources(Configuration conf) {
//		if(true) return;
		XC_InitPackageResources.InitPackageResourcesParam ourResparam = ResourceManager.resparams.get(listenPackage);

		boolean isLandscape = conf.orientation == Configuration.ORIENTATION_LANDSCAPE;
//		Context context = mContext.createConfigurationContext(conf);

		if (ourResparam == null) return;

		try {
			if (quick_settings_max_rows_l == 0) {
				conf.orientation = Configuration.ORIENTATION_LANDSCAPE;
				Context landscapeContext = mContext.createConfigurationContext(conf);
				quick_settings_num_columns_l = landscapeContext.getResources().getInteger(landscapeContext.getResources().getIdentifier("quick_settings_num_columns", "integer", landscapeContext.getPackageName()));
				quick_settings_max_rows_l = landscapeContext.getResources().getInteger(landscapeContext.getResources().getIdentifier("quick_settings_max_rows", "integer", landscapeContext.getPackageName()));
				quick_qs_panel_max_tiles_l = landscapeContext.getResources().getInteger(landscapeContext.getResources().getIdentifier("quick_qs_panel_max_tiles", "integer", landscapeContext.getPackageName()));

				conf.orientation = Configuration.ORIENTATION_PORTRAIT;
				Context portraitContext = mContext.createConfigurationContext(conf);
				quick_settings_num_columns_p = portraitContext.getResources().getInteger(portraitContext.getResources().getIdentifier("quick_settings_num_columns", "integer", portraitContext.getPackageName()));
				quick_settings_max_rows_p = portraitContext.getResources().getInteger(portraitContext.getResources().getIdentifier("quick_settings_max_rows", "integer", portraitContext.getPackageName()));
				quick_qs_panel_max_tiles_p = portraitContext.getResources().getInteger(portraitContext.getResources().getIdentifier("quick_qs_panel_max_tiles", "integer", portraitContext.getPackageName()));

				if(isLandscape)
				{
					conf.orientation = Configuration.ORIENTATION_LANDSCAPE;
				}
			}

			if (replaced || QQSTileQty != QQS_NOT_SET || QQSTileQtyL != QQS_NOT_SET) {
				if(isLandscape) {
					ourResparam.res.setReplacement(ourResparam.packageName, "integer", "quick_qs_panel_max_tiles", QQSTileQtyL == QQS_NOT_SET ? quick_qs_panel_max_tiles_l : QQSTileQtyL);
				}
				else
				{
					ourResparam.res.setReplacement(ourResparam.packageName, "integer", "quick_qs_panel_max_tiles", QQSTileQty == QQS_NOT_SET ? quick_qs_panel_max_tiles_p : QQSTileQty);
				}
				replaced = true;
			}

			if (replaced || QSColQty != QS_COL_NOT_SET || QSColQtyL != QS_COL_NOT_SET) {
				if(isLandscape)
				{
					ourResparam.res.setReplacement(ourResparam.packageName, "integer", "quick_settings_num_columns", QSColQtyL == QS_COL_NOT_SET ? quick_settings_num_columns_l : QSColQtyL);
				}
				else
				{
					ourResparam.res.setReplacement(ourResparam.packageName, "integer", "quick_settings_num_columns", QSColQty == QS_COL_NOT_SET ? quick_settings_num_columns_p : QSColQty);
				}
				replaced = true;
			}

			if (replaced || QSRowQty != NOT_SET || QSRowQtyL != NOT_SET) {
				if(isLandscape)
				{
					ourResparam.res.setReplacement(ourResparam.packageName, "integer", "quick_settings_max_rows", QSRowQtyL == NOT_SET ? quick_settings_max_rows_l : QSRowQtyL);
				}
				else
				{
					ourResparam.res.setReplacement(ourResparam.packageName, "integer", "quick_settings_max_rows", QSRowQty == NOT_SET ? quick_settings_max_rows_p : QSRowQty);
				}
				replaced = true;
			}
		} catch (Throwable ignored) {
		}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}
}