package sh.siava.AOSPMods;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.modpacks.ResourceManager;
import sh.siava.AOSPMods.modpacks.XPLauncher;

public class XPEntry implements IXposedHookZygoteInit, IXposedHookInitPackageResources, IXposedHookLoadPackage {
	ResourceManager ResourceManager = new ResourceManager();
	XPLauncher XPLauncher = new XPLauncher();
	@Override
	public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam initPackageResourcesParam) throws Throwable {
		ResourceManager.handleInitPackageResources(initPackageResourcesParam);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable
	{
		XPLauncher.handleLoadPackage(loadPackageParam);
	}

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		ResourceManager.initZygote(startupParam);
	}
}
