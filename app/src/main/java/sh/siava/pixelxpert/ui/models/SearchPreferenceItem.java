package sh.siava.pixelxpert.ui.models;

import androidx.annotation.StringRes;
import androidx.annotation.XmlRes;

import sh.siava.pixelxpert.utils.ControlledPreferenceFragmentCompat;

public class SearchPreferenceItem {

	private final @XmlRes int xml;
	private final @StringRes int title;
	private final ControlledPreferenceFragmentCompat fragment;

	public SearchPreferenceItem(int xml, int title, ControlledPreferenceFragmentCompat fragment) {
		this.xml = xml;
		this.title = title;
		this.fragment = fragment;
	}

	public int getXml() {
		return xml;
	}

	public int getTitle() {
		return title;
	}

	public ControlledPreferenceFragmentCompat getFragment() {
		return fragment;
	}
}
