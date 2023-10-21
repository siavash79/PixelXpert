package sh.siava.pixelxpert.utils;

import com.crossbowffs.remotepreferences.RemotePreferenceFile;
import com.crossbowffs.remotepreferences.RemotePreferenceProvider;

import sh.siava.pixelxpert.BuildConfig;

public class RemotePrefProvider extends RemotePreferenceProvider {
	public RemotePrefProvider() {
		super(BuildConfig.APPLICATION_ID, new RemotePreferenceFile[]{new RemotePreferenceFile(BuildConfig.APPLICATION_ID + "_preferences", true)});
	}
}
