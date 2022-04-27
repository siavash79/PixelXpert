package sh.siava.AOSPMods;

import de.robv.android.xposed.IXposedHookLoadPackage;

public interface IXposedModPack extends IXposedHookLoadPackage {
    void updatePrefs(String...Key);
    boolean listensTo(String packageName);
}
