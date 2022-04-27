package sh.siava.AOSPMods.Utils;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import java.text.DecimalFormat;

/*
 *
 * Seeing how an Integer object in java requires at least 16 Bytes, it seemed awfully wasteful
 * to only use it for a single boolean. 32-bits is plenty of room for what we need it to do.
 *
 */
public class NetworkTraffic extends androidx.appcompat.widget.AppCompatTextView {

    private static NetworkTraffic instance = null;
    private static final int KB = 1024;
    private static final int MB = KB * KB;
    private static final int GB = MB * KB;
    private static final String symbol = "/S";

    protected boolean mIsEnabled;
    protected boolean mAttached;
    private long totalRxBytes;
    private long totalTxBytes;
    private long lastUpdateTime;
    protected int mAutoHideThreshold;
    protected int mTintColor;
    protected int mLocation;
    private int mRefreshInterval = 1;
    private int mIndicatorMode = 0;
    private Context mContext;

    protected boolean mVisible = true;
    private ConnectivityManager mConnectivityManager;

    private Handler mTrafficHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            long timeDelta = SystemClock.elapsedRealtime() - lastUpdateTime;

            if (timeDelta < mRefreshInterval * 1000 * .95) {
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
                setText("");
                setVisibility(View.GONE);
                mVisible = false;
            } else if (shouldShowUpload(rxData, txData, timeDelta)) {
                // Show information for uplink if it's called for

                SpannableStringBuilder output = new SpannableStringBuilder().append(formatOutput(timeDelta, rxData, symbol)).append("\n").append(formatOutput(timeDelta, txData, symbol));

                // Update view if there's anything new to show
                if (output != getText()) {
                    setText(output);
                }
                makeVisible();
            } else {
                // Add information for downlink if it's called for
                CharSequence output = formatOutput(timeDelta, rxData, symbol);

                // Update view if there's anything new to show
                if (output != getText()) {
                    setText(output);
                }
                makeVisible();
            }

            // Post delayed message to refresh in ~1000ms
            totalRxBytes = newTotalRxBytes;
            totalTxBytes = newTotalTxBytes;
            clearHandlerCallbacks();
            mTrafficHandler.postDelayed(mRunnable, mRefreshInterval * 1000);
        }


        private SpannableStringBuilder formatOutput(long timeDelta, long data, String symbol) {
            long speed = (long)(data / (timeDelta / 1024F));

            return formatDecimal(speed);
        }

        private SpannableStringBuilder formatDecimal(long speed) {
            DecimalFormat decimalFormat;
            String unit;
            String formatSpeed;
            SpannableString spanUnitString;
            SpannableString spanSpeedString;

            if (speed >= GB) {
                unit = "GB";
                decimalFormat = new DecimalFormat("0.00");
                formatSpeed =  decimalFormat.format(speed / (float)GB);
            } else if (speed >= 100 * MB) {
                decimalFormat = new DecimalFormat("000");
                unit = "MB";
                formatSpeed =  decimalFormat.format(speed / (float)MB);
            } else if (speed >= 10 * MB) {
                decimalFormat = new DecimalFormat("00.0");
                unit = "MB";
                formatSpeed =  decimalFormat.format(speed / (float)MB);
            } else if (speed >= MB) {
                decimalFormat = new DecimalFormat("0.00");
                unit = "MB";
                formatSpeed =  decimalFormat.format(speed / (float)MB);
            } else if (speed >= 100 * KB) {
                decimalFormat = new DecimalFormat("000");
                unit = "KB";
                formatSpeed =  decimalFormat.format(speed / (float)KB);
            } else if (speed >= 10 * KB) {
                decimalFormat = new DecimalFormat("00.0");
                unit = "KB";
                formatSpeed =  decimalFormat.format(speed / (float)KB);
            } else {
                decimalFormat = new DecimalFormat("0.00");
                unit = "KB";
                formatSpeed = decimalFormat.format(speed / (float)KB);
            }
            spanSpeedString = new SpannableString(formatSpeed);
            spanSpeedString.setSpan(getSpeedRelativeSizeSpan(), 0, (formatSpeed).length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            spanUnitString = new SpannableString(unit + symbol);
            spanUnitString.setSpan(getUnitRelativeSizeSpan(), 0, (unit + symbol).length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return new SpannableStringBuilder().append(spanSpeedString).append(" ").append(spanUnitString);
        }

        private boolean shouldHide(long rxData, long txData, long timeDelta) {
            long speedRxKB = (long)(rxData / (timeDelta / 1024f)) / KB;
            long speedTxKB = (long)(txData / (timeDelta / 1024f)) / KB;
            return !getConnectAvailable() ||
                    (speedRxKB < mAutoHideThreshold &&
                            speedTxKB < mAutoHideThreshold);
        }

        private boolean shouldShowUpload(long rxData, long txData, long timeDelta) {
            long speedRxKB = (long)(rxData / (timeDelta / 1024f)) / KB;
            long speedTxKB = (long)(txData / (timeDelta / 1024f)) / KB;

            if (mIndicatorMode == 0) {
                return (speedTxKB > speedRxKB);
            } else if (mIndicatorMode == 2) {
                return true;
            } else {
                return false;
            }
        }
    };

    protected boolean restoreViewQuickly() {
        return getConnectAvailable() && mAutoHideThreshold == 0;
    }

    protected void makeVisible() {
        boolean show = mLocation == 1;
        setVisibility(show ? View.VISIBLE
                : View.GONE);
        mVisible = show;
    }

    /*
     *  @hide
     */
    public NetworkTraffic(Context context) {
        this(context, null);
    }

    /*
     *  @hide
     */
    public NetworkTraffic(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /*
     *  @hide
     */
    public NetworkTraffic(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if(context == null) return;
        mContext = context;
        final Resources resources = getResources();
        setMode();
        Handler mHandler = new Handler();
        mConnectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        update();

        setSpacingAndFonts();
        instance = this;
    }

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
    public void setVisibility(int visibility)
    {
        super.setVisibility(visibility);
        LinearLayout parent = ((LinearLayout)this.getParent());
        for(int i = 0; i< parent.getChildCount(); i++)
        {
            View v = parent.getChildAt(i);
            if(!v.equals(this)) v.setVisibility(visibility);
        }
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

    protected RelativeSizeSpan getSpeedRelativeSizeSpan() {
        return new RelativeSizeSpan(1f);
    }

    protected RelativeSizeSpan getUnitRelativeSizeSpan() {
        return new RelativeSizeSpan(0.70f);
    }

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mTrafficHandler.sendEmptyMessage(0);
        }
    };

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
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
        NetworkInfo network = (mConnectivityManager != null) ? mConnectivityManager.getActiveNetworkInfo() : null;
        return network != null;
    }

    protected void update() {
        if (mIsEnabled) {
            if (mAttached) {
                totalRxBytes = TrafficStats.getTotalRxBytes();
                lastUpdateTime = SystemClock.elapsedRealtime();
                mTrafficHandler.sendEmptyMessage(1);
            }
            return;
        } else {
            clearHandlerCallbacks();
        }
        setVisibility(View.GONE);
        mVisible = false;
    }

    public void setMode() {
        mIsEnabled = true;
        mLocation = 1;
        mIndicatorMode = 2;
        mAutoHideThreshold = 10;
        mRefreshInterval = 1;

        setGravity(Gravity.CENTER);
        setMaxLines(2);
        setSpacingAndFonts();
        updateTrafficDrawable();
//        setVisibility(View.GONE);
//        mVisible = false;
    }

    private void clearHandlerCallbacks() {
        mTrafficHandler.removeCallbacks(mRunnable);
        mTrafficHandler.removeMessages(0);
        mTrafficHandler.removeMessages(1);
    }

    protected void updateTrafficDrawable() {

        setTextColor(mTintColor);
        setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
    }

    protected void setSpacingAndFonts() {
        String txtFont = getResources().getString(getResources().getIdentifier("android:string/config_headlineFontFamily", "string", mContext.getOpPackageName()));
        setTypeface(Typeface.create(txtFont, Typeface.BOLD));
        setLineSpacing(0.85f, 0.85f);
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
    }

    public void onDensityOrFontScaleChanged() {
        setSpacingAndFonts();
        update();
    }

    public void setTintColor(int color) {
        if(mTintColor != color) {
            mTintColor = color;
            updateTrafficDrawable();
        }
    }

}