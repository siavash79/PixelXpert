package sh.siava.AOSPMods.modpacks.systemui;

import static android.media.AudioManager.STREAM_MUSIC;
import static android.service.quicksettings.Tile.STATE_ACTIVE;
import static android.service.quicksettings.Tile.STATE_INACTIVE;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.modpacks.systemui.QSTileGrid.QSHapticEnabled;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.XPLauncher;
import sh.siava.AOSPMods.modpacks.XPrefs;
import sh.siava.AOSPMods.modpacks.XposedModPack;
import sh.siava.AOSPMods.modpacks.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class VolumeTile extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;
	private static final String TARGET_SPEC = "custom(sh.siava.AOSPMods/.utils.VolumeTileService)";

	private int currentPct = 50;
	private static int unMuteVolumePCT = 50;
	private static boolean lightQSHeaderEnabled = false;
	Drawable volumePercentageDrawable = null;
	private Object lastState;

	public VolumeTile(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		lightQSHeaderEnabled = XPrefs.Xprefs.getBoolean("LightQSPanel", false);
		unMuteVolumePCT = XPrefs.Xprefs.getInt("UnMuteVolumePCT", 50);
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(listenPackage)) return;

		volumePercentageDrawable = new PercentageShape();
		volumePercentageDrawable.setAlpha(64);

		Class<?> QSTileViewImplClass = findClass("com.android.systemui.qs.tileimpl.QSTileViewImpl", lpparam.classLoader);

		hookAllMethods(QSTileViewImplClass, "handleStateChanged", new XC_MethodHook() {
			@SuppressLint("DiscouragedApi")
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if(param.args[0] == null) return;

				Object state = param.args[0];
				if (getObjectField(state, "spec").equals(TARGET_SPEC)) {
					SystemUtils.VolumeChangeListener listener = (SystemUtils.VolumeChangeListener) getAdditionalInstanceField(param.thisObject, "volumeChangeListener");

					if(listener == null)
					{
						View thisView = (View) param.thisObject;

						listener = () -> updateVolume(thisView);

						setAdditionalInstanceField(param.thisObject, "volumeChangeListener", listener);

						SystemUtils.registerVolumeChangeListener(listener);

						currentPct = getCurrentVolumePercent();

						thisView.setOnTouchListener(new View.OnTouchListener() {
							float initX = 0;
							float initPct = 0;
							boolean moved = false;

							@SuppressLint({"DiscouragedApi", "ClickableViewAccessibility"})
							@Override
							public boolean onTouch(View view, MotionEvent motionEvent) {
								switch (motionEvent.getAction()) {
									case MotionEvent.ACTION_DOWN: {
										initX = motionEvent.getX();
										initPct = initX / view.getWidth();
										return true;
									}
									case MotionEvent.ACTION_MOVE: {
										float newPct = motionEvent.getX() / view.getWidth();
										float deltaPct = Math.abs(newPct - initPct);
										if (deltaPct > .03f) {
											view.getParent().requestDisallowInterceptTouchEvent(true);
											moved = true;
											currentPct = Math.round(Math.max(Math.min(newPct, 1), 0) * 100f);
											changeVolume(currentPct);
										}
										return true;
									}
									case MotionEvent.ACTION_UP: {
										if (moved) {
											moved = false;
										} else {
											if (QSHapticEnabled) SystemUtils.vibrate(VibrationEffect.EFFECT_CLICK, VibrationAttributes.USAGE_TOUCH);
											toggleMute();
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

			@Override
			protected void afterHookedMethod(MethodHookParam param) {
				if(param.args[0] == null) return;

				Object state = param.args[0];
				if (getObjectField(state, "spec").equals(TARGET_SPEC)) {
					lastState = state;
					LinearLayout tileView = (LinearLayout) param.thisObject;

					currentPct = getCurrentVolumePercent();

					volumePercentageDrawable.setTint(
							(SystemUtils.isDarkMode() || !lightQSHeaderEnabled) && !getObjectField(state, "state").equals(STATE_ACTIVE)
									? Color.WHITE
									: Color.BLACK);

					LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{(Drawable) getObjectField(tileView, "colorBackgroundDrawable"), volumePercentageDrawable});
					tileView.setBackground(layerDrawable);
					updateVolume((View) param.thisObject);
				}
			}
		});
	}

	private void toggleMute() {
		if(currentPct > 0)
		{
			changeVolume(0);
		}
		else
		{
			changeVolume(unMuteVolumePCT);
		}
	}

	private void updateVolume(View thisView) {
		Resources res = mContext.getResources();

		currentPct = getCurrentVolumePercent();

		TextView label = (TextView) getObjectField(thisView, "label");

		@SuppressLint("DiscouragedApi")
		String newLabel = String.format("%s - %s%%",
				res.getText(
						res.getIdentifier(
								"media_output_dialog_accessibility_seekbar",
								"string", mContext.getPackageName())),
				currentPct
		);

		label.setText(newLabel);

		boolean wasActive = getObjectField(thisView, "lastState").equals(STATE_ACTIVE);
		boolean shouldBeActive = currentPct > 0;

		if(wasActive != shouldBeActive)
		{
			setObjectField(lastState, "state", shouldBeActive ? STATE_ACTIVE :STATE_INACTIVE);
			callMethod(thisView, "onStateChanged", lastState);
		}

	}

	private void changeVolume(int currentPct) {
		if(SystemUtils.AudioManager() == null) return;

		int minVol = SystemUtils.AudioManager().getStreamMinVolume(STREAM_MUSIC);
		int maxVol = SystemUtils.AudioManager().getStreamMaxVolume(STREAM_MUSIC);
		int nextVolume = Math.round((maxVol - minVol) * currentPct / 100f) + minVol;

		SystemUtils.AudioManager().setStreamVolume(STREAM_MUSIC, nextVolume, 0);

	}

	private int getCurrentVolumePercent() {
		if(SystemUtils.AudioManager() == null) return 0;

		int currentVol = SystemUtils.AudioManager().getStreamVolume(STREAM_MUSIC);
		int minVol = SystemUtils.AudioManager().getStreamMinVolume(STREAM_MUSIC);
		int maxVol = SystemUtils.AudioManager().getStreamMaxVolume(STREAM_MUSIC);

		return Math.round(100f * (currentVol - minVol) / (maxVol - minVol));
	}

	private class PercentageShape extends Drawable {
		final Drawable shape;

		@SuppressLint({"UseCompatLoadingForDrawables", "DiscouragedApi"})
		private PercentageShape() {
			shape = mContext.getDrawable(mContext.getResources().getIdentifier("qs_tile_background_shape", "drawable", mContext.getPackageName()));
		}

		@Override
		public void setBounds(Rect bounds) {
			shape.setBounds(bounds);
		}

		@Override
		public void setBounds(int a, int b, int c, int d) {
			shape.setBounds(a, b, c, d);
		}

		@Override
		public void draw(@NonNull Canvas canvas) {
			try {
				Bitmap bitmap = Bitmap.createBitmap(Math.round(shape.getBounds().width() * currentPct / 100f), shape.getBounds().height(), Bitmap.Config.ARGB_8888);
				Canvas tempCanvas = new Canvas(bitmap);
				shape.draw(tempCanvas);

				canvas.drawBitmap(bitmap, 0, 0, new Paint());
			}
			catch (Throwable ignored){}
		}

		@Override
		public void setAlpha(int i) {
			shape.setAlpha(i);
		}

		@Override
		public void setColorFilter(@Nullable ColorFilter colorFilter) {
			shape.setColorFilter(colorFilter);
		}

		@Override
		public int getOpacity() {
			return PixelFormat.UNKNOWN;
		}

		@Override
		public void setTint(int t) {
			shape.setTint(t);
		}
	}
}