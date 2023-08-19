package sh.siava.AOSPMods.ui.preferencesearch;

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
import android.content.res.TypedArray;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import sh.siava.AOSPMods.R;
import sh.siava.AOSPMods.SettingsActivity;

public class SearchPreference extends Preference implements View.OnClickListener {
	private final SearchConfiguration searchConfiguration = new SearchConfiguration();
	private String hint = null;

	@SuppressWarnings("unused")
	public SearchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setLayoutResource(R.layout.searchpreference_preference);
		parseAttrs(attrs);
	}

	@SuppressWarnings("unused")
	public SearchPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setLayoutResource(R.layout.searchpreference_preference);
		parseAttrs(attrs);
	}

	@SuppressWarnings("unused")
	public SearchPreference(Context context) {
		super(context);
		setLayoutResource(R.layout.searchpreference_preference);
	}

	private void parseAttrs(AttributeSet attrs) {
		TypedArray a = getContext().obtainStyledAttributes(attrs, new int[]{R.attr.textHint});
		if (a.getText(0) != null) {
			hint = a.getText(0).toString();
			searchConfiguration.setTextHint(a.getText(0).toString());
		}
		a.recycle();
		a = getContext().obtainStyledAttributes(attrs, new int[]{R.attr.textClearHistory});
		if (a.getText(0) != null) {
			searchConfiguration.setTextClearHistory(a.getText(0).toString());
		}
		a.recycle();
		a = getContext().obtainStyledAttributes(attrs, new int[]{R.attr.textNoResults});
		if (a.getText(0) != null) {
			searchConfiguration.setTextNoResults(a.getText(0).toString());
		}
		a.recycle();
	}

	@Override
	public void onBindViewHolder(PreferenceViewHolder holder) {
		EditText searchText = (EditText) holder.findViewById(R.id.search);
		searchText.setFocusable(false);
		searchText.setInputType(InputType.TYPE_NULL);
		searchText.setOnClickListener(this);

		if (hint != null) {
			searchText.setHint(hint);
		}

		holder.findViewById(R.id.search_card).setOnClickListener(this);
		holder.itemView.setOnClickListener(this);
		holder.itemView.setBackgroundColor(0x0);
	}

	@Override
	public void onClick(View view) {
		getSearchConfiguration().showSearchFragment();
		SettingsActivity.backButtonEnabled();
	}

	/**
	 * Returns the search configuration object for this preference
	 *
	 * @return The search configuration
	 */
	public SearchConfiguration getSearchConfiguration() {
		return searchConfiguration;
	}
}
