package sh.siava.AOSPMods.Utils;

import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

import sh.siava.AOSPMods.BuildConfig;

public class PrefManager {
	private static final String TAG = "Pref Exporter";

	public static boolean exportPrefs(SharedPreferences preferences, final @NonNull OutputStream outputStream) throws IOException {
		ObjectOutputStream objectOutputStream = null;
		try {
			objectOutputStream = new ObjectOutputStream(outputStream);
			objectOutputStream.writeObject(preferences.getAll());
			objectOutputStream.close();
		} catch (IOException e) {
			Log.e(TAG, "Error serializing preferences", BuildConfig.DEBUG ? e : null);
			return false;
		} finally {
			objectOutputStream.close();
			outputStream.close();
		}
		return true;
	}
	
	public static boolean importPath(SharedPreferences sharedPreferences, final @NonNull InputStream inputStream) throws IOException {
		ObjectInputStream objectInputStream = null;
		Map<String, Object> map;
		try {
			objectInputStream = new ObjectInputStream(inputStream);
			map = (Map) objectInputStream.readObject();
		} catch (IOException | ClassNotFoundException e) {
			Log.e(TAG, "Error deserializing preferences", BuildConfig.DEBUG ? e : null);
			return false;
		} finally {
			objectInputStream.close();
			inputStream.close();
		}
		
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.clear();
		
		for (Map.Entry<String, Object> e : map.entrySet()) {
			// Unfortunately, the editor only provides typed setters
			if (e.getValue() instanceof Boolean) {
				editor.putBoolean(e.getKey(), (Boolean)e.getValue());
			} else if (e.getValue() instanceof String) {
				editor.putString(e.getKey(), (String)e.getValue());
			} else if (e.getValue() instanceof Integer) {
				editor.putInt(e.getKey(), (int)e.getValue());
			} else if (e.getValue() instanceof Float) {
				editor.putFloat(e.getKey(), (float)e.getValue());
			} else if (e.getValue() instanceof Long) {
				editor.putLong(e.getKey(), (Long) e.getValue());
			} else if (e.getValue() instanceof Set) {
				editor.putStringSet(e.getKey(), (Set<String>) e.getValue());
			} else {
				throw new IllegalArgumentException("Type " + e.getValue().getClass().getName() + " is unknown");
			}
		}
		return editor.commit();
	}
	
	public static void clearPrefs(SharedPreferences preferences)
	{
		preferences.edit().clear().commit();
	}
}