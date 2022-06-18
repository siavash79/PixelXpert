package sh.siava.AOSPMods.systemui;

import static sh.siava.AOSPMods.ResourceManager.SysUIresparam;

import android.content.Context;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.Utils.SystemUtils;
import sh.siava.AOSPMods.XPrefs;
import sh.siava.AOSPMods.XposedModPack;

public class QSTileGrid extends XposedModPack {
    public static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;

    private boolean replaced = false;
    private int originalRow = 0, originalCol = 0;
    private static final int NOT_SET = 0;

    public static int QSRowQty = NOT_SET;
    public static int QSColQty = NOT_SET;

    public QSTileGrid(Context context) { super(context); }

    @Override
    public void updatePrefs(String... Key) {
        if(XPrefs.Xprefs == null) return;

        QSRowQty = XPrefs.Xprefs.getInt("QSRowQty", NOT_SET);
        QSColQty = XPrefs.Xprefs.getInt("QSColQty", NOT_SET);

        setResources();

        if(Key.length > 0 && (Key[0].equals("QSRowQty") || Key[0].equals("QSColQty")))
        {
            SystemUtils.doubleToggleDarkMode();
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;

        setResources();
    }

    private void setResources()
    {
        try {
            if (originalRow == 0) {
                SysUIresparam.res.setReplacement(SysUIresparam.res.getIdentifier("quick_qs_panel_max_tiles", "integer", SysUIresparam.res.getPackageName()), 10);

                originalCol = mContext.getResources().getInteger(mContext.getResources().getIdentifier("quick_settings_num_columns", "integer", mContext.getPackageName()));
                originalRow = mContext.getResources().getInteger(mContext.getResources().getIdentifier("quick_settings_max_rows", "integer", mContext.getPackageName()));
            }

            if (replaced || QSColQty != NOT_SET) {
                SysUIresparam.res.setReplacement(SysUIresparam.packageName, "integer", "quick_settings_num_columns", QSColQty == NOT_SET ? originalCol : QSColQty);
                replaced = true;
            }

            if (replaced || QSRowQty != NOT_SET) {
                SysUIresparam.res.setReplacement(SysUIresparam.packageName, "integer", "quick_settings_max_rows", QSRowQty == NOT_SET ? originalRow : QSRowQty);
                replaced = true;
            }
        }catch (Throwable ignored){}
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
}
