package sh.siava.pixelxpert.modpacks.systemui;

import static android.service.quicksettings.Tile.STATE_ACTIVE;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;
import static sh.siava.pixelxpert.modpacks.utils.SystemUtils.getFlashlightLevel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.pixelxpert.modpacks.utils.SystemUtils;
import sh.siava.pixelxpert.modpacks.utils.TilePercentageDrawable;

@SuppressWarnings("RedundantThrows")
public class FlashLightLevel extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;
	private static boolean leveledFlashTile = false;
	private float currentPct = .5f;
	private static boolean lightQSHeaderEnabled = false;
	TilePercentageDrawable mFlashPercentageDrawable = null;

	public FlashLightLevel(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		leveledFlashTile = Xprefs.getBoolean("leveledFlashTile", false);
		lightQSHeaderEnabled = Xprefs.getBoolean("LightQSPanel", false);
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		if (!lpParam.packageName.equals(listenPackage)) return;

		mFlashPercentageDrawable = new TilePercentageDrawable(mContext);
		mFlashPercentageDrawable.setAlpha(64);

		Class<?> QSTileViewImplClass = findClass("com.android.systemui.qs.tileimpl.QSTileViewImpl", lpParam.classLoader);

		hookAllMethods(QSTileViewImplClass, "handleStateChanged", new XC_MethodHook() {
			@SuppressLint("DiscouragedApi")
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (!leveledFlashTile || !SystemUtils.supportsFlashLevels()) return;

				try {
					Object state = param.args[0];
					if (getObjectField(state, "spec").equals("flashlight")) {
						SystemUtils.ChangeListener listener = (SystemUtils.ChangeListener) getAdditionalInstanceField(param.thisObject, "flashlightLevelListener");

						if (listener == null) {
							View thisView = (View) param.thisObject;

							listener = level -> {
								Resources res = mContext.getResources();

								TextView label = (TextView) getObjectField(thisView, "label");

								String newLabel = String.format("%s - %s%%",
										res.getText(
												res.getIdentifier(
														"quick_settings_flashlight_label",
														"string", mContext.getPackageName())),
										level
								);

								label.setText(newLabel);
							};

							setAdditionalInstanceField(param.thisObject, "flashlightLevelListener", listener);

							SystemUtils.registerFlashlightLevelListener(listener);

							setPct(Xprefs.getFloat("flashPCT", 0.5f));

							thisView.setOnTouchListener(new View.OnTouchListener() {
								float initX = 0;
								float initPct = 0;
								boolean moved = false;

								@SuppressLint({"DiscouragedApi", "ClickableViewAccessibility"})
								@Override
								public boolean onTouch(View view, MotionEvent motionEvent) {
									if (!SystemUtils.supportsFlashLevels() || !leveledFlashTile)
										return false;

									switch (motionEvent.getAction()) {
										case MotionEvent.ACTION_DOWN: {
											initX = motionEvent.getX();
											initPct = initX / view.getWidth();
											return true;
										}
										case MotionEvent.ACTION_MOVE: {
											float deltaMove = Math.abs(initX - motionEvent.getX()) / view.getWidth();

											if (deltaMove > .03f) {
												int newLevel = getFlashlightLevel(motionEvent.getX() / view.getWidth());

												view.getParent().requestDisallowInterceptTouchEvent(true);
												moved = true;
												setPct(newLevel * 1f / SystemUtils.getMaxFlashLevel());
												handleFlashLightClick(false, newLevel);
												SystemUtils.setFlashlightLevel(Math.round(currentPct * 100f));
											}
											return true;
										}
										case MotionEvent.ACTION_UP: {
											if (moved) {
												moved = false;
												Xprefs.edit().putFloat("flashPCT", currentPct).apply();
											} else {
												handleFlashLightClick(true, getFlashlightLevel(currentPct));
												if (QSTileGrid.QSHapticEnabled)
													SystemUtils.vibrate(VibrationEffect.EFFECT_CLICK, VibrationAttributes.USAGE_TOUCH);
											}
											return true;
										}
									}
									return true;
								}
							});
						}
					}
				}
				catch (Throwable ignored){}
			}

			@Override
			protected void afterHookedMethod(MethodHookParam param) {
				if (!leveledFlashTile || !SystemUtils.supportsFlashLevels()) return;

				try {
					Object state = param.args[0];
					if (getObjectField(state, "spec").equals("flashlight")) {
						LinearLayout tileView = (LinearLayout) param.thisObject;

						setPct(Xprefs.getFloat("flashPCT", 0.5f));

						SystemUtils.setFlashlightLevel(Math.round(currentPct * 100f));

						mFlashPercentageDrawable.setTint(
								(SystemUtils.isDarkMode() || !lightQSHeaderEnabled) && !getObjectField(state, "state").equals(STATE_ACTIVE)
										? Color.WHITE
										: Color.BLACK);

						LayerDrawable layerDrawable;
						try { //A14 AP11
							layerDrawable = new LayerDrawable(new Drawable[]{(Drawable) getObjectField(tileView, "backgroundDrawable"), mFlashPercentageDrawable});
						} catch (Throwable ignored) { //Older
							layerDrawable = new LayerDrawable(new Drawable[]{(Drawable) getObjectField(tileView, "colorBackgroundDrawable"), mFlashPercentageDrawable});
						}
						if(layerDrawable == null) return; //something is wrong

						tileView.setBackground(layerDrawable);
					}
				}catch (Throwable ignored){}
			}
		});
	}

	private void setPct(float newVal) {
		currentPct = newVal;
		mFlashPercentageDrawable.setPct(Math.round(newVal * 100));
	}

	private void handleFlashLightClick(boolean toggle, int level) {
		SystemUtils.setFlash(toggle ^ SystemUtils.isFlashOn(), level);
	}
}