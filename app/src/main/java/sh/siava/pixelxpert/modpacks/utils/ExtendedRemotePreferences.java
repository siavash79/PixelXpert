package sh.siava.pixelxpert.modpacks.utils;

import android.content.Context;

import com.crossbowffs.remotepreferences.RemotePreferences;

import java.util.List;

import sh.siava.rangesliderpreference.RangeSliderPreference;

public class ExtendedRemotePreferences extends RemotePreferences {
	public ExtendedRemotePreferences(Context context, String authority, String prefFileName) {
		super(context, authority, prefFileName);
	}

	public ExtendedRemotePreferences(Context context, String authority, String prefFileName, boolean strictMode) {
		super(context, authority, prefFileName, strictMode);
	}

	public int getSliderInt(String key, int defaultVal)
	{
		return RangeSliderPreference.getSingleIntValue(this, key, defaultVal);
	}

	public float getSliderFloat(String key, float defaultVal)
	{
		return RangeSliderPreference.getSingleFloatValue(this, key, defaultVal);
	}

	public List<Float> getSliderValues(String key, float defaultValue)
	{
		return RangeSliderPreference.getValues(this, key, defaultValue);
	}
}
