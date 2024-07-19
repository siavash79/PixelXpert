package sh.siava.pixelxpert.modpacks.systemui;

import static android.service.quicksettings.Tile.STATE_ACTIVE;
import static android.service.quicksettings.Tile.STATE_INACTIVE;
import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.findMethodExactIfExists;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

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
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.pixelxpert.modpacks.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class QSTileGrid extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

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

	private int updateFontSizeMethodType = 0;

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

		QSRowQty = Xprefs.getSliderInt( "QSRowQty", NOT_SET);
		QSColQty = Xprefs.getSliderInt( "QSColQty", QS_COL_NOT_SET);
		if(QSColQty < QS_COL_NOT_SET) QSColQty = QS_COL_NOT_SET;
		QQSTileQty = Xprefs.getSliderInt( "QQSTileQty", QQS_NOT_SET);

		QSRowQtyL = Xprefs.getSliderInt( "QSRowQtyL", NOT_SET);
		QSColQtyL = Xprefs.getSliderInt( "QSColQtyL", QS_COL_NOT_SET);
		if(QSColQtyL < QS_COL_NOT_SET) QSColQtyL = QS_COL_NOT_SET;
		QQSTileQtyL = Xprefs.getSliderInt( "QQSTileQtyL", QQS_NOT_SET);

		QRTileInactiveColor = Xprefs.getBoolean("QRTileInactiveColor", false);

		QSLabelScaleFactor = (Xprefs.getSliderFloat( "QSLabelScaleFactor", 0) + 100) / 100f;
		QSSecondaryLabelScaleFactor = (Xprefs.getSliderFloat( "QSSecondaryLabelScaleFactor", 0) + 100) / 100f;

		if (Key.length > 0 && (Key[0].equals("QSRowQty") || Key[0].equals("QSColQty") || Key[0].equals("QQSTileQty") || Key[0].equals("QSRowQtyL") || Key[0].equals("QSColQtyL") || Key[0].equals("QQSTileQtyL"))) {
			SystemUtils.doubleToggleDarkMode();
		}
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		if (!lpParam.packageName.equals(listenPackage)) return;

		Class<?> QSTileViewImplClass = findClass("com.android.systemui.qs.tileimpl.QSTileViewImpl", lpParam.classLoader);
		Class<?> FontSizeUtilsClass = findClass("com.android.systemui.FontSizeUtils", lpParam.classLoader);
		Class<?> QSTileImplClass = findClass("com.android.systemui.qs.tileimpl.QSTileImpl", lpParam.classLoader);
		Class<?> QSFactoryImplClass = findClass("com.android.systemui.qs.tileimpl.QSFactoryImpl", lpParam.classLoader);
		Class<?> QuickQSPanelControllerClass = findClass("com.android.systemui.qs.QuickQSPanelController", lpParam.classLoader);
		Class<?> QuickQSPanelClass =findClass("com.android.systemui.qs.QuickQSPanel", lpParam.classLoader);
		Class<?> TileAdapterClass = findClass("com.android.systemui.qs.customize.TileAdapter", lpParam.classLoader);
		Class<?> SideLabelTileLayoutClass = findClass("com.android.systemui.qs.SideLabelTileLayout", lpParam.classLoader);

		Class<?> TileLayoutClass = findClassIfExists("com.android.systemui.qs.TileLayout", lpParam.classLoader);
		if(TileLayoutClass == null) //new versions have merged tile layout to sidelable
		{
			TileLayoutClass = SideLabelTileLayoutClass;
		}

		if(findMethodExactIfExists(FontSizeUtilsClass,
				"updateFontSize",
				TextView.class, int.class)
				!= null)
		{
			updateFontSizeMethodType = 1;
		}

		final int tileLayoutHookSize = hookAllMethods(TileLayoutClass, "updateResources", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(getQSCols() != QS_COL_NOT_SET || getQSRows() != NOT_SET)
				{
					param.setResult(updateTileLayoutResources(param.thisObject));
				}
			}
		}).size();

		hookAllMethods(SideLabelTileLayoutClass, "updateResources", new XC_MethodHook() {
			@SuppressLint("DiscouragedApi")
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				Resources res = mContext.getResources();
				int QSRows = getQSRows();
				if(QSRows == NOT_SET)
				{
					QSRows = res.getInteger(res.getIdentifier("quick_settings_max_rows", "integer", mContext.getPackageName()));
				}
				setObjectField(param.thisObject, "mMaxAllowedRows", Math.max(1, QSRows));

				if(tileLayoutHookSize == 0) {
					if (getQSCols() != QS_COL_NOT_SET || getQSRows() != NOT_SET) {
						param.setResult(updateTileLayoutResources(param.thisObject));
					}
				}
			}
		});

		hookAllConstructors(TileAdapterClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(getQSCols() != QS_COL_NOT_SET)
				{
					setObjectField(param.thisObject, "mNumColumns", getQSCols());
				}
			}
		});
		hookAllConstructors(QuickQSPanelClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				int maxTiles = getQQSMaxTiles();
				if(maxTiles != QQS_NOT_SET)
					setObjectField(param.thisObject, "mMaxTiles", getQQSMaxTiles());
			}
		});

		hookAllMethods(QuickQSPanelControllerClass, "onConfigurationChanged", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				int maxTiles = getQQSMaxTiles();
				if(maxTiles != QQS_NOT_SET)
				{
					param.setResult(null); //replacing the original method now
					if(maxTiles != getIntField(getObjectField(param.thisObject, "mView"), "mMaxTiles"))
					{
						setObjectField(getObjectField(param.thisObject, "mView"), "mMaxTiles", maxTiles);
						callMethod(param.thisObject, "setTiles");
					}
					callMethod(param.thisObject, "updateMediaExpansion");
				}
			}
		});

		try {
			if(findClassIfExists("com.android.systemui.qs.tiles.WifiTile", lpParam.classLoader) == null)
				Xprefs
						.edit()
						.putBoolean("InternetTileModEnabled", false)
						.apply();
		}
		catch (Throwable ignored){}

		//used to enable dual wifi/cell tiles for 13
		hookAllMethods(QSFactoryImplClass, "createTile", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if(param.args[0].equals("wifi_PixelXpert"))
				{
					param.args[0] = "wifi";
				}
				if(param.args[0].equals("cell_PixelXpert"))
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

		Class<?> QRCodeScannerTileClass = findClassIfExists("com.android.systemui.qs.tiles.QRCodeScannerTile", lpParam.classLoader);

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
						updateFontSize(FontSizeUtilsClass,
								getObjectField(param.thisObject, "label"),
								mContext.getResources().getIdentifier("qs_tile_text_size", "dimen", mContext.getPackageName()));

						updateFontSize(FontSizeUtilsClass,
								getObjectField(param.thisObject, "secondaryLabel"),
								mContext.getResources().getIdentifier("qs_tile_text_size", "dimen", mContext.getPackageName()));

						TextView label = (TextView) getObjectField(param.thisObject, "label");
						TextView secondaryLabel = (TextView) getObjectField(param.thisObject, "secondaryLabel");

						labelSizeUnit = label.getTextSizeUnit();
						labelSize = label.getTextSize();

						secondaryLabelSizeUnit = secondaryLabel.getTextSizeUnit();
						secondaryLabelSize = secondaryLabel.getTextSize();
					}
				} catch (Throwable ignored) {}
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
	}

	private void updateFontSize(Class<?> FontSizeUtilsClass, Object textView, int resId)
	{
		if(updateFontSizeMethodType == 1)
		{
			callStaticMethod(FontSizeUtilsClass,
					"updateFontSize",
					textView,
					resId);
		}
		else
		{
			callStaticMethod(FontSizeUtilsClass,
					"updateFontSize",
					resId,
					textView);
		}
	}

	@SuppressLint("DiscouragedApi")
	private boolean updateTileLayoutResources(Object thisObject) {
		final Resources res = mContext.getResources();
		int QSCols = getQSCols();
		if(QSCols == QS_COL_NOT_SET)
		{
			QSCols = res.getInteger(res.getIdentifier("quick_settings_num_columns", "integer", mContext.getPackageName()));
		}

		int QSRows = getQSRows();
		if(QSRows == NOT_SET)
		{
			QSRows = res.getInteger(res.getIdentifier("quick_settings_max_rows", "integer", mContext.getPackageName()));
		}

		setObjectField(thisObject, "mResourceColumns", Math.max(1, QSCols));
		setObjectField(thisObject, "mMaxAllowedRows", Math.max(1, QSRows));

		int oldColumns = getIntField(thisObject, "mColumns");
		int finalColumns = Math.min(Math.max(1, QSCols), getIntField(thisObject, "mMaxColumns"));
		setObjectField(thisObject, "mColumns", finalColumns);

		if (oldColumns != finalColumns) {
			((View)thisObject).requestLayout();
			return true;
		}
		return false;
	}

	private int getQQSMaxTiles() {
		return mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
				? QQSTileQty
				: QQSTileQtyL;
	}

	private int getQSCols() {
		return mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
				? QSColQty
				: QSColQtyL;
	}

	private int getQSRows() {
		return mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
				? QSRowQty
				: QSRowQtyL;
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
		} catch (Throwable ignored) {}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}
}