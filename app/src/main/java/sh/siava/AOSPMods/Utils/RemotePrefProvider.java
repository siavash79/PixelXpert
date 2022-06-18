package sh.siava.AOSPMods.Utils;

import com.crossbowffs.remotepreferences.RemotePreferenceFile;
import com.crossbowffs.remotepreferences.RemotePreferenceProvider;

import sh.siava.AOSPMods.BuildConfig;

public class RemotePrefProvider extends RemotePreferenceProvider
{
	public RemotePrefProvider()
	{
		super(BuildConfig.APPLICATION_ID, new RemotePreferenceFile[]{new RemotePreferenceFile(BuildConfig.APPLICATION_ID + "_preferences", true)});
	}
}
