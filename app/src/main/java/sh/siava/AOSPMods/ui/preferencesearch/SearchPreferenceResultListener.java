package sh.siava.AOSPMods.ui.preferencesearch;

/*
 * Modified from AOSPMods
 * https://github.com/ByteHamster/SearchPreference
 */

import androidx.annotation.NonNull;

public interface SearchPreferenceResultListener {
	void onSearchResultClicked(@NonNull SearchPreferenceResult result);
}
