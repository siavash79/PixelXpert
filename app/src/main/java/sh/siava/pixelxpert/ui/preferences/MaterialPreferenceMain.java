package sh.siava.pixelxpert.ui.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import sh.siava.pixelxpert.R;

public class MaterialPreferenceMain extends Preference {

	private static final int POSITION_TOP = 0;
	private static final int POSITION_DEFAULT = 1;
	private static final int POSITION_SINGLE = 2;
	private static final int POSITION_BOTTOM = 3;

	private int position = POSITION_DEFAULT;

	public MaterialPreferenceMain(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init(context, attrs);
	}

	public MaterialPreferenceMain(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context, attrs);
	}

	public MaterialPreferenceMain(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public MaterialPreferenceMain(@NonNull Context context) {
		super(context);
		initResource();
	}

	private void init(Context context, @Nullable AttributeSet attrs) {
		if (attrs != null) {
			TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MaterialPreferenceMain, 0, 0);
			try {
				position = a.getInt(R.styleable.MaterialPreferenceMain_position, POSITION_DEFAULT);
			} finally {
				a.recycle();
			}
		}
		initResource();
	}

	private void initResource() {
		setLayoutResource(R.layout.custom_preference_main);
	}

	@Override
	public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
		super.onBindViewHolder(holder);

		// Set margin for the first item
		if (holder.getBindingAdapterPosition() == 0) {
			ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
			layoutParams.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getContext().getResources().getDisplayMetrics());
			holder.itemView.setLayoutParams(layoutParams);
		} else {
			if (holder.getBindingAdapter() != null) {
				// Set margin for the last item
				if (holder.getBindingAdapterPosition() == holder.getBindingAdapter().getItemCount() - 1) {
					ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
					layoutParams.bottomMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getContext().getResources().getDisplayMetrics());
					holder.itemView.setLayoutParams(layoutParams);
				}
			}
		}

		// Set background drawable based on the position attribute
		switch (position) {
			case POSITION_TOP:
				holder.itemView.setBackgroundResource(R.drawable.container_top);
				break;
			case POSITION_SINGLE:
				holder.itemView.setBackgroundResource(R.drawable.container_single);
				break;
			case POSITION_BOTTOM:
				holder.itemView.setBackgroundResource(R.drawable.container_bottom);
				break;
			case POSITION_DEFAULT:
			default:
				holder.itemView.setBackgroundResource(R.drawable.container_mid);
				break;
		}
	}
}
