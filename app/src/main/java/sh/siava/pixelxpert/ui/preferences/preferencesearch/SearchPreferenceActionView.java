package sh.siava.pixelxpert.ui.preferences.preferencesearch;

/*
 * https://github.com/ByteHamster/SearchPreference
 *
 * MIT License
 *
 * Copyright (c) 2018 ByteHamster
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.FragmentManager;

public class SearchPreferenceActionView extends SearchView {
	protected SearchPreferenceFragment searchFragment;
	protected SearchConfiguration searchConfiguration = new SearchConfiguration();
	protected AppCompatActivity activity;

	public SearchPreferenceActionView(Context context) {
		super(context);
		initView();
	}

	public SearchPreferenceActionView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	public SearchPreferenceActionView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initView();
	}

	private void initView() {
		searchConfiguration.setSearchBarEnabled(false);
		setOnQueryTextListener(new OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				return false;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				if (searchFragment != null) {
					searchFragment.setSearchTerm(newText);
				}
				return true;
			}
		});
		setOnQueryTextFocusChangeListener((v, hasFocus) -> {
			if (hasFocus && (searchFragment == null || !searchFragment.isVisible())) {
				searchFragment = searchConfiguration.showSearchFragment();
				searchFragment.setHistoryClickListener(entry -> setQuery(entry, false));
			}
		});
	}

	@SuppressWarnings("unused")
	public SearchConfiguration getSearchConfiguration() {
		return searchConfiguration;
	}

	/**
	 * Hides the search fragment
	 *
	 * @return true if it was hidden, so the calling activity should not go back itself.
	 */
	@SuppressWarnings("unused")
	public boolean cancelSearch() {
		setQuery("", false);

		boolean didSomething = false;
		if (!isIconified()) {
			setIconified(true);
			didSomething = true;
		}
		if (searchFragment != null && searchFragment.isVisible()) {
			removeFragment();
			didSomething = true;
		}
		return didSomething;
	}

	protected void removeFragment() {
		if (searchFragment.isVisible()) {
			FragmentManager fm = activity.getSupportFragmentManager();
			fm.beginTransaction().remove(searchFragment).commit();
			fm.popBackStack(SearchPreferenceFragment.TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}
	}

	@SuppressWarnings("unused")
	public void setActivity(AppCompatActivity activity) {
		searchConfiguration.setActivity(activity);
		this.activity = activity;
	}
}
