package sh.siava.pixelxpert.ui.fragments;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import sh.siava.pixelxpert.R;

abstract class BaseFragment extends Fragment {

	protected boolean isBackButtonEnabled() {
		return true;
	}

	public boolean getBackButtonEnabled() {
		return isBackButtonEnabled();
	}

	public abstract String getTitle();

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
}
