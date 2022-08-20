package sh.siava.AOSPMods.Utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

public class ShyLinearLayout extends LinearLayout {

    public ShyLinearLayout(Context context) {
        super(context);
    }

    public ShyLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ShyLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressWarnings("unused")
    public ShyLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void onViewAdded(View v)
    {
        super.onViewAdded(v);

        setVisibility(VISIBLE);
    }

    @Override
    public void onViewRemoved(View v)
    {
        super.onViewRemoved(v);

        if(getChildCount() == 0)
        {
            setVisibility(GONE);
        }
    }
}