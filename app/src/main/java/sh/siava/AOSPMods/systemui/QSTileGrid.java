package sh.siava.AOSPMods.systemui;

import static sh.siava.AOSPMods.ResourceManager.resparams;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.content.Context;

import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.Utils.SystemUtils;
import sh.siava.AOSPMods.XposedModPack;

public class QSTileGrid extends XposedModPack {
    public static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;

    private boolean replaced = false;
    private int quick_settings_max_rows = 0, quick_settings_num_columns = 0, quick_qs_panel_max_tiles = 0;
    private static final int NOT_SET = 0;
    private static final int QQS_NOT_SET = 4;

    private static int QSRowQty = NOT_SET;
    private static int QSColQty = NOT_SET;
    private static int QQSTileQty = QQS_NOT_SET;

    public QSTileGrid(Context context) { super(context); }

    @Override
    public void updatePrefs(String... Key) {
        if(Xprefs == null) return;

        QSRowQty = Xprefs.getInt("QSRowQty", NOT_SET);
        QSColQty = Xprefs.getInt("QSColQty", NOT_SET);
        QQSTileQty = Xprefs.getInt("QQSTileQty", QQS_NOT_SET);

        setResources();

        if(Key.length > 0 && (Key[0].equals("QSRowQty") || Key[0].equals("QSColQty") || Key[0].equals("QQSTileQty")))
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
        XC_InitPackageResources.InitPackageResourcesParam ourResparam = resparams.get(listenPackage);

        if(ourResparam == null) return;

        try {
            if (quick_settings_max_rows == 0) {
                quick_settings_num_columns = mContext.getResources().getInteger(mContext.getResources().getIdentifier("quick_settings_num_columns", "integer", mContext.getPackageName()));
                quick_settings_max_rows = mContext.getResources().getInteger(mContext.getResources().getIdentifier("quick_settings_max_rows", "integer", mContext.getPackageName()));
                quick_qs_panel_max_tiles = mContext.getResources().getInteger(mContext.getResources().getIdentifier("quick_qs_panel_max_tiles", "integer", mContext.getPackageName()));
            }

            if(replaced || QQSTileQty != QQS_NOT_SET)
            {
                ourResparam.res.setReplacement(ourResparam.packageName, "integer", "quick_qs_panel_max_tiles", QQSTileQty == QQS_NOT_SET ? quick_qs_panel_max_tiles : QQSTileQty);
                replaced = true;
            }

            if (replaced || QSColQty != NOT_SET) {
                ourResparam.res.setReplacement(ourResparam.packageName, "integer", "quick_settings_num_columns", QSColQty == NOT_SET ? quick_settings_num_columns : QSColQty);
                replaced = true;
            }

            if (replaced || QSRowQty != NOT_SET) {
                ourResparam.res.setReplacement(ourResparam.packageName, "integer", "quick_settings_max_rows", QSRowQty == NOT_SET ? quick_settings_max_rows : QSRowQty);
                replaced = true;
            }
        }catch (Throwable ignored){}
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
}
