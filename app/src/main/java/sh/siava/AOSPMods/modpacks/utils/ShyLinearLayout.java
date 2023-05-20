package sh.siava.AOSPMods.modpacks.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;

public class ShyLinearLayout extends LinearLayout {

	public ShyLinearLayout(Context context) {
		super(context);
		setVisibility(GONE);
	}

	public ShyLinearLayout(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	public ShyLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setLayoutParams(new LinearLayoutCompat.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,1));
	}

	@Override
	public void onViewAdded(View v) {
		super.onViewAdded(v);

		setVisibility(VISIBLE);
	}

	@Override
	public void onViewRemoved(View v) {
		super.onViewRemoved(v);

		if (getChildCount() == 0) {
			setVisibility(GONE);
		}
	}
}