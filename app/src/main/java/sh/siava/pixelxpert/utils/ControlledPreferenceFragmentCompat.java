package sh.siava.pixelxpert.utils;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import sh.siava.pixelxpert.R;

public abstract class ControlledPreferenceFragmentCompat extends PreferenceFragmentCompat {
	public ExtendedSharedPreferences mPreferences;
	private final OnSharedPreferenceChangeListener changeListener = (sharedPreferences, key) -> updateScreen(key);

	protected boolean isBackButtonEnabled() {
		return true;
	}

	public boolean getBackButtonEnabled() {
		return isBackButtonEnabled();
	}

	public abstract String getTitle();

	public abstract int getLayoutResource();

	protected int getDefaultThemeResource() {
		return R.style.PrefsThemeToolbar;
	}

	public int getThemeResource() {
		return getDefaultThemeResource();
	}

	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		inflater.getContext().setTheme(getThemeResource());
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		AppCompatActivity baseContext = (AppCompatActivity) getContext();
		Toolbar toolbar = view.findViewById(R.id.toolbar);

		if (baseContext != null) {
			if (toolbar != null) {
				baseContext.setSupportActionBar(toolbar);
				toolbar.setTitle(getTitle());
			}
			if (baseContext.getSupportActionBar() != null) {
				baseContext.getSupportActionBar().setDisplayHomeAsUpEnabled(getBackButtonEnabled());
			}
		}
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		getPreferenceManager().setStorageDeviceProtected();
		setPreferencesFromResource(getLayoutResource(), rootKey);
	}

	@NonNull
	@Override
	public RecyclerView.Adapter<?> onCreateAdapter(@NonNull PreferenceScreen preferenceScreen) {
		mPreferences = ExtendedSharedPreferences.from(getDefaultSharedPreferences(requireContext().createDeviceProtectedStorageContext()));

		mPreferences.registerOnSharedPreferenceChangeListener(changeListener);

		updateScreen(null);

		return super.onCreateAdapter(preferenceScreen);
	}

	@Override
	public void onDestroy() {
		if (mPreferences != null) {
			mPreferences.unregisterOnSharedPreferenceChangeListener(changeListener);
		}
		super.onDestroy();
	}

	public void updateScreen(String key) {
		PreferenceHelper.setupAllPreferences(this.getPreferenceScreen());
	}
}
