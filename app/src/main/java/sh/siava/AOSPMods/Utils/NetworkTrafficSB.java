package sh.siava.AOSPMods.Utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import sh.siava.AOSPMods.R;
import sh.siava.AOSPMods.XPrefs;
import sh.siava.AOSPMods.systemui.StatusbarMods;

public class NetworkTrafficSB extends LinearLayout {

    public static final String SLOT = "networktraffic";
    private int mVisibleState = -1;
    private boolean mTrafficVisible = true;
    private boolean mSystemIconVisible = true;
    private boolean mKeyguardShowing;
    private NetworkTraffic networkTraffic;
    private ImageView mArrow;
    private static NetworkTrafficSB instance = null;

    public NetworkTrafficSB(Context context) {
        super(context);
        instance = this;

        //instances
        networkTraffic = new NetworkTraffic(context);
        networkTraffic.setMode();

        mArrow = new ImageView(context);
        mArrow.setImageDrawable(XPrefs.modRes.getDrawable(R.drawable.network_traffic_arrows));

        //sizes
        Resources res = context.getResources();

        TypedValue typedValue = new TypedValue();

        res.getValue(res.getIdentifier("status_bar_icon_scale_factor", "dimen", context.getPackageName()), typedValue, true);
        float iconScaleFactor = typedValue.getFloat() * 2;

        int Height = res.getDimensionPixelSize(res.getIdentifier("status_bar_battery_icon_height", "dimen", context.getPackageName()));

        mArrow.setLayoutParams(new LayoutParams((int) (Height * iconScaleFactor / 3), (int) (Height * iconScaleFactor)));

        networkTraffic.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);

        //colors
        setTint(StatusbarMods.clockColor);

        //parents
        this.addView(mArrow);
        this.addView(networkTraffic);
    }

    public void setTintI(int Color)
    {
        networkTraffic.setTintColor(Color);
        mArrow.setColorFilter(Color);
    }

    public static void setTint(int Color)
    {
        if(instance != null)
        {
            instance.setTintI(Color);
        }
    }

    public static void setHideTreshold(int KBs)
    {
        if(instance != null)
        {
            instance.networkTraffic.mAutoHideThreshold = KBs;
        }
    }
}