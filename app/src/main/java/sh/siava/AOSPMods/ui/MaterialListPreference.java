package sh.siava.AOSPMods.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceViewHolder;

import sh.siava.AOSPMods.R;

public class MaterialListPreference extends ListPreference {

	public MaterialListPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		initResource();
	}

	public MaterialListPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initResource();
	}

	public MaterialListPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		initResource();
	}

	public MaterialListPreference(@NonNull Context context) {
		super(context);
		initResource();
	}

	private void initResource() {
		setLayoutResource(R.layout.custom_preference_list);
	}

	@Override
	public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
		super.onBindViewHolder(holder);

		if (holder.getBindingAdapterPosition() == 0) {
			ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
			layoutParams.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getContext().getResources().getDisplayMetrics());
			holder.itemView.setLayoutParams(layoutParams);
		} else {
			if (holder.getBindingAdapter() != null) {
				if (holder.getBindingAdapterPosition() == holder.getBindingAdapter().getItemCount() - 1) {
					ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
					layoutParams.bottomMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getContext().getResources().getDisplayMetrics());
					holder.itemView.setLayoutParams(layoutParams);
				}
			}
		}
	}
}
