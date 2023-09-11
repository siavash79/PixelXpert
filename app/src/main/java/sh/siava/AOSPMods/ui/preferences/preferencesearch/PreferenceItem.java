package sh.siava.AOSPMods.ui.preferences.preferencesearch;

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

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.XmlRes;

import org.apache.commons.text.similarity.FuzzyScore;

import java.util.ArrayList;
import java.util.Locale;

public class PreferenceItem extends ListItem implements Parcelable {
	public static final Creator<PreferenceItem> CREATOR = new Creator<>() {
		@Override
		public PreferenceItem createFromParcel(Parcel in) {
			return new PreferenceItem(in);
		}

		@Override
		public PreferenceItem[] newArray(int size) {
			return new PreferenceItem[size];
		}
	};
	static final int TYPE = 2;
	@SuppressLint("ConstantLocale")
	private static final FuzzyScore fuzzyScore = new FuzzyScore(Locale.getDefault());
	String title;
	String summary;
	String key;
	String entries;
	String breadcrumbs;
	String keywords;
	ArrayList<String> keyBreadcrumbs = new ArrayList<>();
	int resId;
	private float lastScore = 0;
	private String lastKeyword = null;

	PreferenceItem() {
	}

	private PreferenceItem(Parcel in) {
		this.title = in.readString();
		this.summary = in.readString();
		this.key = in.readString();
		this.breadcrumbs = in.readString();
		this.keywords = in.readString();
		this.resId = in.readInt();
	}

	boolean hasData() {
		return title != null || summary != null;
	}

	boolean matchesFuzzy(String keyword) {
		return getScore(keyword) > 0.3;
	}

	boolean matches(String keyword) {
		Locale locale = Locale.getDefault();
		return getInfo().toLowerCase(locale).contains(keyword.toLowerCase(locale));
	}

	float getScore(String keyword) {
		if (TextUtils.isEmpty(keyword)) {
			return 0;
		} else if (TextUtils.equals(lastKeyword, keyword)) {
			return lastScore;
		}
		String info = getInfo();

		float score = fuzzyScore.fuzzyScore(info, "ø" + keyword);
		float maxScore = (keyword.length() + 1) * 3 - 2; // First item can not get +2 bonus score

		lastScore = score / maxScore;
		lastKeyword = keyword;
		return lastScore;
	}

	private String getInfo() {
		StringBuilder infoBuilder = new StringBuilder();
		if (!TextUtils.isEmpty(title)) {
			infoBuilder.append("ø").append(title);
		}
		if (!TextUtils.isEmpty(summary)) {
			infoBuilder.append("ø").append(summary);
		}
		if (!TextUtils.isEmpty(entries)) {
			infoBuilder.append("ø").append(entries);
		}
		if (!TextUtils.isEmpty(breadcrumbs)) {
			infoBuilder.append("ø").append(breadcrumbs);
		}
		if (!TextUtils.isEmpty(keywords)) {
			infoBuilder.append("ø").append(keywords);
		}
		return infoBuilder.toString();
	}

	@SuppressWarnings("unused")
	public PreferenceItem withKey(String key) {
		this.key = key;
		return this;
	}

	@SuppressWarnings("unused")
	public PreferenceItem withSummary(String summary) {
		this.summary = summary;
		return this;
	}

	@SuppressWarnings("unused")
	public PreferenceItem withTitle(String title) {
		this.title = title;
		return this;
	}

	@SuppressWarnings("unused")
	public PreferenceItem withEntries(String entries) {
		this.entries = entries;
		return this;
	}

	@SuppressWarnings("unused")
	public PreferenceItem withKeywords(String keywords) {
		this.keywords = keywords;
		return this;
	}

	@SuppressWarnings("unused")
	public PreferenceItem withResId(@XmlRes Integer resId) {
		this.resId = resId;
		return this;
	}

	/**
	 * @param breadcrumb The breadcrumb to add
	 * @return For chaining
	 */
	@SuppressWarnings("unused")
	public PreferenceItem addBreadcrumb(String breadcrumb) {
		this.breadcrumbs = Breadcrumb.concat(this.breadcrumbs, breadcrumb);
		return this;
	}

	@NonNull
	@Override
	public String toString() {
		return "PreferenceItem: " + title + " " + summary + " " + key;
	}

	@Override
	public int getType() {
		return TYPE;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int i) {
		parcel.writeString(title);
		parcel.writeString(summary);
		parcel.writeString(key);
		parcel.writeString(breadcrumbs);
		parcel.writeString(keywords);
		parcel.writeInt(resId);
	}

}
