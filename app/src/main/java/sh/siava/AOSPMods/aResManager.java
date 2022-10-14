package sh.siava.AOSPMods;

import android.content.res.XModuleResources;
import android.util.SparseIntArray;

import de.robv.android.xposed.callbacks.XC_InitPackageResources;

public abstract class aResManager {
	protected String MODULE_PATH;
	protected XC_InitPackageResources.InitPackageResourcesParam resparam;
	protected XModuleResources modRes;

	protected static SparseIntArray mappedResource = new SparseIntArray();

	public aResManager(String MODULE_PATH, XC_InitPackageResources.InitPackageResourcesParam resparam, XModuleResources modRes) {
		this.MODULE_PATH = MODULE_PATH;
		this.resparam = resparam;
		this.modRes = modRes;
	}

	public abstract void hookResources();

	protected void mapResource(int res) {
		mappedResource.put(res, resparam.res.addResource(modRes, res));
	}

}
