package sh.siava.AOSPMods.modpacks.systemui;

import static android.graphics.Bitmap.Config;
import static android.graphics.Bitmap.createBitmap;
import static android.media.AudioManager.STREAM_MUSIC;
import static android.os.VibrationAttributes.USAGE_TOUCH;
import static android.os.VibrationEffect.EFFECT_CLICK;
import static android.service.quicksettings.Tile.STATE_ACTIVE;
import static android.service.quicksettings.Tile.STATE_INACTIVE;
import static android.view.View.GONE;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.modpacks.systemui.QSTileGrid.QSHapticEnabled;
import static sh.siava.AOSPMods.modpacks.utils.SystemUtils.AudioManager;
import static sh.siava.AOSPMods.modpacks.utils.SystemUtils.isDarkMode;
import static sh.siava.AOSPMods.modpacks.utils.SystemUtils.registerVolumeChangeListener;
import static sh.siava.AOSPMods.modpacks.utils.SystemUtils.unregisterVolumeChangeListener;
import static sh.siava.AOSPMods.modpacks.utils.SystemUtils.vibrate;

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
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.XPLauncher;
import sh.siava.AOSPMods.modpacks.XPrefs;
import sh.siava.AOSPMods.modpacks.XposedModPack;
import sh.siava.AOSPMods.modpacks.utils.SystemUtils.VolumeChangeListener;

@SuppressWarnings({"RedundantThrows", "ConstantConditions"})
public class VolumeTile extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;
	private static final String TARGET_SPEC = "custom(sh.siava.AOSPMods/.utils.VolumeTileService)";

	private int currentPct = 50;
	private static int unMuteVolumePCT = 50;
	private static boolean lightQSHeaderEnabled = false;
	Drawable volumePercentageDrawable = null;
	private static int minVol = -1;
	private static int maxVol = -1;
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
		if (!lpparam.packageName.equals(listenPackage) || AudioManager() == null) return;

		minVol = AudioManager().getStreamMinVolume(STREAM_MUSIC);
		maxVol = AudioManager().getStreamMaxVolume(STREAM_MUSIC);

		volumePercentageDrawable = new PercentageShape();
		volumePercentageDrawable.setAlpha(64);

		Class<?> QSTileViewImplClass = findClass("com.android.systemui.qs.tileimpl.QSTileViewImpl", lpparam.classLoader);
		Class<?> QSPanelControllerBaseClass = findClass("com.android.systemui.qs.QSPanelControllerBase", lpparam.classLoader);
		Class<?> QSTileImplClass = findClass("com.android.systemui.qs.tileimpl.QSTileImpl", lpparam.classLoader);

		hookAllMethods(QSTileImplClass, "removeCallback", new XC_MethodHook() { //removing dead tiles from callbacks
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				VolumeChangeListener volumeChangeListener = (VolumeChangeListener) getAdditionalInstanceField(param.thisObject, "volumeChangeListener");

				if(volumeChangeListener != null)
					unregisterVolumeChangeListener(volumeChangeListener);
			}
		});

		hookAllMethods(QSPanelControllerBaseClass, "setTiles", new XC_MethodHook() { //finding and setting up volume tiles
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				((ArrayList<?>) getObjectField(param.thisObject, "mRecords")).forEach(record ->
				{
					Object tile = getObjectField(record, "tile");

					if (TARGET_SPEC.equals((String) getObjectField(tile, "mTileSpec"))) {
						View tileView = (View) getObjectField(record, "tileView");

						setupTile(tile, tileView);

						handleVolumeChanged(tileView);
					}
				});
			}
		});

		hookAllMethods(QSTileViewImplClass, "handleStateChanged", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) {
				try {
					if (getAdditionalInstanceField(param.thisObject, "mParentTile") != null) {
						updateTileView((LinearLayout) param.thisObject, (int)getObjectField(param.args[0] /* QSTile.State */, "state"));
					}
				}catch (Throwable ignored){}
			}
		});
	}

	private void setupTile(Object tile, View tileView) {
		setAdditionalInstanceField(tileView, "mParentTile", tile);

		setVolumeChangeListener(tile, tileView);

		setTouchListener(tileView);
	}

	private void setVolumeChangeListener(Object tile, View tileView) {
		VolumeChangeListener listener = () -> handleVolumeChanged(tileView);

		setAdditionalInstanceField(tile, "volumeChangeListener", listener);

		registerVolumeChangeListener(listener);
	}

	private void setTouchListener(View tileView) {
		tileView.setOnTouchListener(new View.OnTouchListener() {
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
						float deltaPct = abs(newPct - initPct);
						if (deltaPct > .03f) {
							view.getParent().requestDisallowInterceptTouchEvent(true);
							moved = true;
							currentPct = round(max(min(newPct, 1), 0) * 100f);
							changeVolume(currentPct);
						}
						return true;
					}
					case MotionEvent.ACTION_UP: {
						if (moved) {
							moved = false;
						} else {
							if (QSHapticEnabled)
								vibrate(EFFECT_CLICK, USAGE_TOUCH);
							toggleMute();
						}
						return true;
					}
				}
				return true;
			}
		});
	}

	private void updateTileView(LinearLayout tileView, int state) {
		Resources res = mContext.getResources();

		volumePercentageDrawable.setTint(
				(isDarkMode() || !lightQSHeaderEnabled) && state != STATE_ACTIVE
						? Color.WHITE
						: Color.BLACK);

		LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{(Drawable) getObjectField(tileView, "colorBackgroundDrawable"), volumePercentageDrawable});
		tileView.setBackground(layerDrawable);

		TextView label = (TextView) getObjectField(tileView, "label");

		@SuppressLint("DiscouragedApi")
		String newLabel = String.format("%s - %s%%",
				res.getText(
						res.getIdentifier(
								"media_output_dialog_accessibility_seekbar",
								"string", mContext.getPackageName())),
				currentPct
		);

		label.setText(newLabel);

		//We don't need the chevron icon on the right side
		((View) getObjectField(tileView, "chevronView"))
				.setVisibility(GONE);
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

	private void handleVolumeChanged(View thisView) {
		new Thread(() -> {
			Object parentTile = getAdditionalInstanceField(thisView, "mParentTile");

			currentPct = getCurrentVolumePercent();

			Object mTile = getObjectField(parentTile, "mTile");

			int currentState = (int) getObjectField(mTile, "mState");
			int newState = currentPct > 0 ? STATE_ACTIVE : STATE_INACTIVE;

			if(currentState != newState)
			{
				setObjectField(mTile, "mState", newState);
				callMethod(parentTile, "refreshState");
			}
			thisView.post(() -> updateTileView((LinearLayout) thisView, newState));
		}).start();
	}

	private void changeVolume(int currentPct) {
		AudioManager().setStreamVolume(
				STREAM_MUSIC,
				round((maxVol - minVol) * currentPct / 100f) + minVol,
				0 /* don't show UI */);
	}

	private int getCurrentVolumePercent() {
		int currentVol = AudioManager().getStreamVolume(STREAM_MUSIC);

		return round(100f * (currentVol - minVol) / (maxVol - minVol));
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
				Bitmap bitmap = createBitmap(round(shape.getBounds().width() * currentPct / 100f), shape.getBounds().height(), Config.ARGB_8888);
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