package sh.siava.pixelxpert.modpacks;

import android.content.Context;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public abstract class XposedModPack {
	protected Context mContext;

	public XposedModPack(Context context) {
		mContext = context;
	}

	public abstract void updatePrefs(String... Key);

	public abstract void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable;

	public abstract boolean listensTo(String packageName);
}