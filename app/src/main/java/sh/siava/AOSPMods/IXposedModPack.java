package sh.siava.AOSPMods;

import android.content.Context;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public interface IXposedModPack {
    void updatePrefs(String...Key);
    boolean listensTo(String packageName);
    void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam, Context context) throws Throwable;
}
