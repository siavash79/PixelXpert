package sh.siava.AOSPMods.modpacks;

import android.content.res.Resources;

import java.util.HashMap;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;

@SuppressWarnings("RedundantThrows")
public class ResourceManager implements IXposedHookInitPackageResources, IXposedHookZygoteInit {

	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	private String MODULE_PATH;
	public final static HashMap<String, XC_InitPackageResources.InitPackageResourcesParam> resparams = new HashMap<>();
	public static Resources modRes;

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		MODULE_PATH = startupParam.modulePath;
	}

	@Override
	public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable
	{
		resparams.put(resparam.packageName, resparam);
	}
}
