package sh.siava.pixelxpert.ui.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceViewHolder;

import sh.siava.pixelxpert.R;

public class MaterialPreferenceMain extends Preference {

	public MaterialPreferenceMain(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		initResource();
	}

	public MaterialPreferenceMain(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initResource();
	}

	public MaterialPreferenceMain(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		initResource();
	}

	public MaterialPreferenceMain(@NonNull Context context) {
		super(context);
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

		setBackgroundResource(holder);
	}

	private void setBackgroundResource(PreferenceViewHolder holder) {
		PreferenceGroup parent = getParent();

		if (parent != null) {
			int itemCount = parent.getPreferenceCount();
			int position = getOrder();

			if (itemCount == 1) {
				holder.itemView.setBackgroundResource(R.drawable.container_single);
			} else if (itemCount > 1) {
				if (position == 0) {
					holder.itemView.setBackgroundResource(R.drawable.container_top);
				} else if (position == itemCount - 1) {
					holder.itemView.setBackgroundResource(R.drawable.container_bottom);
				} else {
					holder.itemView.setBackgroundResource(R.drawable.container_mid);
				}
			}
		}
	}
}
