package sh.siava.AOSPMods.modpacks.utils;

import android.os.RemoteException;

import com.topjohnwu.superuser.Shell;

import sh.siava.AOSPMods.IRootProviderProxy;
import sh.siava.AOSPMods.modpacks.XPLauncher;

public class ModuleFolderOperations {
	public static void applyFontSettings(boolean customFontsEnabled, boolean GSansOverrideEnabled, String rootPath, boolean runFromApp) {
		new Thread(() -> {
			try {
				if (customFontsEnabled && GSansOverrideEnabled) {
					runRootCommand(String.format("cp %s/data/etcz/fonts.xml %s/system/etc/ && cp %s/data/fontz/GSans/*.ttf %s/system/fonts/ && cp %s/data/productz/etc/fonts_customization.xml.NEW %s/system/product/etc/fonts_customization.xml", rootPath, rootPath, rootPath, rootPath, rootPath, rootPath), runFromApp);
				} else if (customFontsEnabled) {
					runRootCommand(String.format("cp %s/data/etcz/fonts.xml %s/system/etc/ && rm -rf %s/system/fonts/*.ttf && cp %s/data/productz/etc/fonts_customization.xml.OLD %s/system/product/etc/fonts_customization.xml && cp -r %s/data/productz/fonts/* %s/system/product/fonts/", rootPath, rootPath, rootPath, rootPath, rootPath, rootPath, rootPath), runFromApp);
				} else {
					runRootCommand(String.format("rm -rf %s/system/fonts/*.ttf && rm -f %s/system/product/etc/fonts_customization.xml && rm -rf %s/system/product/fonts/* && rm -rf %s/system/etc/fonts.xml", rootPath, rootPath, rootPath, rootPath), runFromApp);
				}
			} catch (Exception ignored) {
			}
		}).start();
	}

	public static void applyVolumeSteps(int volumeStps, String rootPath, boolean runFromApp) {
		if (volumeStps <= 10) {
			runRootCommand("rm -Rf " + rootPath + "/system.prop", runFromApp);
			return;
		}
		runRootCommand("echo ro.config.media_vol_steps=" + volumeStps + " > " + rootPath + "/system.prop", runFromApp);
	}

	private static void runRootCommand(String command, boolean runFromApp)
	{
		try
		{
			if(runFromApp)
			{
				Shell.cmd(command).exec();
			}
			else
			{
				XPLauncher.enqueueProxyCommand(new XPLauncher.ProxyRunnable() {
					@Override
					public void run(IRootProviderProxy proxy) throws RemoteException {
						proxy.runCommand(command);
					}
				});
			}
		}
		catch (Throwable ignored){}
	}
}
