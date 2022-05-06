package sh.siava.AOSPMods;

import android.content.Context;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public abstract class IXposedModPack {
    protected Context mContext;
    public IXposedModPack(Context context)
    {
        mContext = context;
    }
    public abstract void updatePrefs(String...Key);
    public abstract boolean listensTo(String packageName);
    public abstract void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable;
}
