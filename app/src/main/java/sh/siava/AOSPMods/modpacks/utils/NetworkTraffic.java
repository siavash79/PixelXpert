package sh.siava.AOSPMods.modpacks.utils;

import static android.widget.LinearLayout.VERTICAL;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import sh.siava.AOSPMods.R;
import sh.siava.AOSPMods.modpacks.ResourceManager;
import sh.siava.AOSPMods.modpacks.XPrefs;
import sh.siava.AOSPMods.modpacks.systemui.StatusbarMods;

@SuppressLint("ViewConstructor")
public class NetworkTraffic extends FrameLayout {
	private static final int KB = 1024;
	private static final String symbol = "/s";

	public static final int MODE_SHOW_RXTX = 0;
	public static final int MODE_SHOW_RX = 1;
	public static final int MODE_SHOW_TX = 2;
	public static final int MODE_SHOW_TOTAL = 3;

	//Same on all instances - default values needed in case not set by parents
	private static int autoHideThreshold = 10 * KB; // unit: Bytes/second
	private static int indicatorMode = MODE_SHOW_RXTX;
	private static boolean RXonTop = true; //if download is above upload
	private static int refreshInterval = 1; //seconds
	private static boolean showIcons = true;

	@ColorInt
	private static int downloadColor = Color.GREEN;
	@ColorInt
	private static int uploadColor = Color.RED;
	private static boolean colorTraffic = false;
	private static int opacity = 100;
	private static long lastParamUpdate = 0;

	private final boolean isSBInstance;
	private long lastInstanceParamUpdate = -1;

	public static void setConstants(int refreshInterval, int autoHideThreshold, int indicatorMode, boolean RXonTop, boolean colorTraffic, int downloadColor, int uploadColor, int opacity, boolean showIcons) {
		NetworkTraffic.refreshInterval = refreshInterval;
		NetworkTraffic.autoHideThreshold = autoHideThreshold * KB;
		NetworkTraffic.indicatorMode = indicatorMode;
		NetworkTraffic.RXonTop = RXonTop;
		NetworkTraffic.colorTraffic = colorTraffic;
		NetworkTraffic.downloadColor = downloadColor;
		NetworkTraffic.uploadColor = uploadColor;
		NetworkTraffic.opacity = opacity;
		NetworkTraffic.showIcons = showIcons;

		lastParamUpdate = SystemClock.elapsedRealtime();
	}

	@SuppressLint("StaticFieldLeak")
	private static NetworkTraffic SBInstance = null;
	private static int SBTintColor;

	@SuppressLint("StaticFieldLeak")
	private static NetworkTraffic QSInstance = null;
	private static int QSTintColor;

	private final LinearLayout iconLayout;
	protected boolean mAttached;
	private long totalRxBytes;
	private long totalTxBytes;
	private long lastUpdateTime;
	private final Context mContext;
	private final TextView mTextView;

	private boolean mViewVisible = true, mTrafficVisible = false;
	private final ConnectivityManager mConnectivityManager;

	private final Handler mTrafficHandler = new Handler(Looper.myLooper()) {
		@Override
		public void handleMessage(Message msg) {
			long timeDelta = SystemClock.elapsedRealtime() - lastUpdateTime;


			if (timeDelta < refreshInterval * 1000L) {
				if (msg.what != 1) {
					// we just updated the view, nothing further to do
					return;
				}
				if (timeDelta < 1) {
					// Can't div by 0 so make sure the value displayed is minimal
					timeDelta = Long.MAX_VALUE;
				}
			}
			lastUpdateTime = SystemClock.elapsedRealtime();

			// Calculate the data rate from the change in total bytes and time
			long newTotalRxBytes = TrafficStats.getTotalRxBytes();
			long newTotalTxBytes = TrafficStats.getTotalTxBytes();
			long rxData = newTotalRxBytes - totalRxBytes;
			long txData = newTotalTxBytes - totalTxBytes;


			if (shouldHide(rxData, txData, timeDelta)) {
				hide(true);
			} else {
				SpannableStringBuilder output = null;
				switch (indicatorMode) {
					case MODE_SHOW_RXTX:
						SpannableStringBuilder RXOutput = formatOutput(timeDelta, rxData, (colorTraffic) ? downloadColor : null);

						SpannableStringBuilder TXOutput = formatOutput(timeDelta, txData, (colorTraffic) ? uploadColor : null);
						output = (RXonTop) ?
								RXOutput.append("\n").append(TXOutput) :
								TXOutput.append("\n").append(RXOutput);
						break;
					case MODE_SHOW_TOTAL:
						output = formatOutput(timeDelta, rxData + txData, null);
						break;
					case MODE_SHOW_RX:
						output = formatOutput(timeDelta, rxData, (colorTraffic) ? downloadColor : null);
						break;
					case MODE_SHOW_TX:
						output = formatOutput(timeDelta, txData, (colorTraffic) ? uploadColor : null);
						break;
				}
				mTextView.setText(output);
				makeVisible(true);
			}

			// Post delayed message to refresh in ~1000ms
			totalRxBytes = newTotalRxBytes;
			totalTxBytes = newTotalTxBytes;
			clearHandlerCallbacks();
			mTrafficHandler.postDelayed(mRunnable, refreshInterval * 1000L);
		}

		private SpannableStringBuilder formatOutput(long timeDelta, long data, @Nullable @ColorInt Integer textColor) {
			long speed = (long) (data / (timeDelta / 1000F));

			return Helpers.getHumanizedBytes(speed, .7f, (indicatorMode == MODE_SHOW_RXTX) ? " " : "\n", symbol, textColor);
		}

		private boolean shouldHide(long rxData, long txData, long timeDelta) {
			long speedRx = (long) (rxData / (timeDelta / 1000f));
			long speedTx = (long) (txData / (timeDelta / 1000f));

			boolean lowSpeed = false;
			switch (indicatorMode) {
				case MODE_SHOW_RXTX:
					lowSpeed = (speedRx < autoHideThreshold &&
							speedTx < autoHideThreshold);
					break;
				case MODE_SHOW_RX:
					lowSpeed = speedRx < autoHideThreshold;
					break;
				case MODE_SHOW_TX:
					lowSpeed = speedTx < autoHideThreshold;
					break;
				case MODE_SHOW_TOTAL:
					lowSpeed = (speedRx + speedTx) < autoHideThreshold;
					break;
			}

			return !getConnectAvailable() || lowSpeed;
		}
	};

	public static NetworkTraffic getInstance(Context context, boolean onStatusbar) {
		NetworkTraffic instance = (onStatusbar) ? SBInstance : QSInstance;
		if (instance == null) {
			new NetworkTraffic(context, onStatusbar);
		}
		return (onStatusbar) ? SBInstance : QSInstance;
	}

	private void hide(boolean trafficRelated) {
		if (!trafficRelated) {
			mViewVisible = false;
		} else {
			mTrafficVisible = false;
		}
		mTextView.setText("");
		setVisibility(View.GONE);
	}

	protected void makeVisible(boolean trafficRelated) {
		if (trafficRelated) mTrafficVisible = true;
		else mViewVisible = true;

		if (!mTrafficVisible || !mViewVisible) return;

		if (lastInstanceParamUpdate < lastParamUpdate) {
			this.setAlpha(2.55f * opacity);
			setIndicatorMode();
			mTextView.setGravity(Gravity.CENTER);
			mTextView.setMaxLines(2);
			setSpacingAndFonts();
			updateTrafficDrawable();
			lastInstanceParamUpdate = lastParamUpdate;
		}
		setVisibility(View.VISIBLE);
	}

	private NetworkTraffic(Context context, boolean onStatusbar) {
		this(context, null, onStatusbar);
	}

	private NetworkTraffic(Context context, AttributeSet attrs, boolean onStatusbar) {
		this(context, attrs, 0, onStatusbar);
	}

	private NetworkTraffic(Context context, AttributeSet attrs, int defStyle, boolean onStatusbar) {
		super(context, attrs, defStyle);
		mContext = context;
		mTextView = new TextView(mContext);
		iconLayout = new LinearLayout(mContext);
		iconLayout.setOrientation(VERTICAL);
		iconLayout.setGravity(Gravity.CENTER_VERTICAL);
		iconLayout.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
		LinearLayout contentLayout = new LinearLayout(mContext);
		contentLayout.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL));
		this.addView(contentLayout);
		contentLayout.addView(iconLayout);
		contentLayout.addView(mTextView);
		mConnectivityManager = SystemUtils.ConnectivityManager();

		isSBInstance = onStatusbar;
		if (onStatusbar) {
			SBInstance = this;
			setTintColor(StatusbarMods.clockColor, true);
			StatusbarMods.registerClockVisibilityCallback(visible -> {
				if (visible) makeVisible(false);
				else hide(false);
			});
		} else {
			QSInstance = this;
		}
	}

	private void setIndicatorMode() {
		Resources res = ResourceManager.modRes;

		TypedValue typedValue = new TypedValue();
		mContext.getResources().getValue(mContext.getResources().getIdentifier("status_bar_icon_scale_factor", "dimen", mContext.getPackageName()), typedValue, true);
		float iconScaleFactor = typedValue.getFloat() * .65f;

		int Height = mContext.getResources().getDimensionPixelSize(mContext.getResources().getIdentifier("status_bar_battery_icon_height", "dimen", mContext.getPackageName()));
		ImageView iconR = new ImageView(mContext);
		iconR.setImageDrawable(ResourcesCompat.getDrawable(res, R.drawable.ic_chevron_down, mContext.getTheme()));
		iconR.setLayoutParams(new LinearLayout.LayoutParams((int) (Height * iconScaleFactor), (int) (Height * iconScaleFactor)));

		ImageView iconT = new ImageView(mContext);
		iconT.setImageDrawable(ResourcesCompat.getDrawable(res, R.drawable.ic_chevron_up, mContext.getTheme()));
		iconT.setLayoutParams(new LinearLayout.LayoutParams((int) (Height * iconScaleFactor), (int) (Height * iconScaleFactor)));

		iconLayout.removeAllViews();
		if(showIcons) {
			switch (indicatorMode) {
				case MODE_SHOW_RXTX:
					iconLayout.addView(iconT);
					iconLayout.addView(iconR, (RXonTop) ? 0 : 1);
					break;
				case MODE_SHOW_RX:
					iconLayout.addView(iconR);
					break;
				case MODE_SHOW_TX:
					iconLayout.addView(iconT);
					break;
			}
		}
		mTextView.setTextAlignment((indicatorMode == MODE_SHOW_RXTX) ? View.TEXT_ALIGNMENT_TEXT_END : View.TEXT_ALIGNMENT_CENTER);
		int iconPadding = Math.round(Height * iconScaleFactor / 4);
		iconR.setPadding(0, (RXonTop) ? 0 : iconPadding, 0, (RXonTop) ? iconPadding : 0);
		iconT.setPadding(0, (RXonTop) ? iconPadding : 0, 0, (RXonTop) ? 0 : iconPadding);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (!mAttached) {
			mAttached = true;
			IntentFilter filter = new IntentFilter();
			filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
			mContext.registerReceiver(mIntentReceiver, filter, null, getHandler());
		}
		update();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		clearHandlerCallbacks();
		if (mAttached) {
			mContext.unregisterReceiver(mIntentReceiver);
			mAttached = false;
		}
	}

	private final Runnable mRunnable = () -> mTrafficHandler.sendEmptyMessage(0);

	private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@SuppressWarnings("deprecation")
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action == null) return;
			if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
				update();
			}
		}
	};

	@SuppressLint("MissingPermission")
	private boolean getConnectAvailable() {
		Network network = (mConnectivityManager != null) ? mConnectivityManager.getActiveNetwork() : null;
		return network != null;
	}

	public void update() {
		if (mAttached) {
			totalRxBytes = TrafficStats.getTotalRxBytes();
			lastUpdateTime = SystemClock.elapsedRealtime();
			mTrafficHandler.sendEmptyMessage(1);
		}
	}

	private void clearHandlerCallbacks() {
		mTrafficHandler.removeCallbacks(mRunnable);
		mTrafficHandler.removeMessages(0);
		mTrafficHandler.removeMessages(1);
	}

	protected void updateTrafficDrawable() {
		int color = (isSBInstance) ? SBTintColor : QSTintColor;
		for (int i = 0; i < iconLayout.getChildCount(); i++) {
			try {
				((ImageView) iconLayout.getChildAt(i)).setColorFilter(color);
			} catch (Exception ignored) {
			}
		}
		mTextView.setTextColor(color);
		mTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
	}

	protected void setSpacingAndFonts() {
		String txtFont = getResources().getString(getResources().getIdentifier("android:string/config_headlineFontFamily", "string", mContext.getOpPackageName()));
		mTextView.setTypeface(Typeface.create(txtFont, Typeface.BOLD));
		mTextView.setLineSpacing(0.85f, 0.85f);
		mTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
	}

	public static void setTintColor(int color, boolean isSBInstance) {
		if ((SBTintColor != color && isSBInstance) || (QSTintColor != color && !isSBInstance)) {
			if (isSBInstance) {
				SBTintColor = color;
				if (SBInstance != null) {
					SBInstance.updateTrafficDrawable();
				}
			} else {
				QSTintColor = color;
				if (QSInstance != null) {
					QSInstance.updateTrafficDrawable();
				}
			}
		}
	}

	public static class trafficStyle extends CharacterStyle {
		private final int textColor;

		public trafficStyle(@ColorInt int textColor) {
			this.textColor = textColor;
		}

		@Override
		public void updateDrawState(TextPaint textPaint) {
			textPaint.setShadowLayer(textPaint.getTextSize() / 17, 0, 0, Color.BLACK);
			textPaint.setColor(textColor);
		}
	}
}