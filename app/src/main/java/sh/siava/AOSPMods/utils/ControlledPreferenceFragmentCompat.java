package sh.siava.AOSPMods.utils;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

public abstract class ControlledPreferenceFragmentCompat extends PreferenceFragmentCompat {
	public SharedPreferences mPreferences;
	private final OnSharedPreferenceChangeListener changeListener = (sharedPreferences, key) -> updateScreen(key);

	public abstract String getTitle();
	public abstract int getLayoutResource();

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		getPreferenceManager().setStorageDeviceProtected();
		setPreferencesFromResource(getLayoutResource(), rootKey);
	}

	@NonNull
	@Override
	public RecyclerView.Adapter<?> onCreateAdapter(@NonNull PreferenceScreen preferenceScreen)
	{
		mPreferences = getDefaultSharedPreferences(requireContext().createDeviceProtectedStorageContext());

		mPreferences.registerOnSharedPreferenceChangeListener(changeListener);

		updateScreen(null);

		return super.onCreateAdapter(preferenceScreen);
	}

	@Override
	public void onResume() {
		super.onResume();
		requireActivity().setTitle(getTitle());
	}

	@Override
	public void onDestroy()
	{
		if (mPreferences != null) {
			mPreferences.unregisterOnSharedPreferenceChangeListener(changeListener);
		}
		super.onDestroy();
	}

	public void updateScreen(String key)
	{
		PreferenceHelper.setupAllPreferences(this.getPreferenceScreen());
	}
}
