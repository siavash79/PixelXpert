package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

import android.content.Context;
import android.os.VibrationEffect;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.Utils.SystemUtils;
import sh.siava.AOSPMods.XPrefs;
import sh.siava.AOSPMods.XposedModPack;

public class QSHaptic extends XposedModPack
{
	public static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;
	public static boolean QSHapticEnabled = false;
	
	public QSHaptic(Context context)
	{
		super(context);
	}
	
	@Override
	public void updatePrefs(String... Key)
	{
		if(XPrefs.Xprefs == null)
			return;
		QSHapticEnabled = XPrefs.Xprefs.getBoolean("QSHapticEnabled", false);
	}
	
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable
	{
		if(! lpparam.packageName.equals(listenPackage))
			return;
		
		
		Class<?> QSTileImplClass = findClass("com.android.systemui.qs.tileimpl.QSTileImpl", lpparam.classLoader);
		
		findAndHookMethod(QSTileImplClass, "click", View.class, new vibrateFeedback());
		
		findAndHookMethod(QSTileImplClass, "longClick", View.class, new vibrateFeedback());
	}
	
	@Override
	public boolean listensTo(String packageName)
	{
		return listenPackage.equals(packageName);
	}
	
	static class vibrateFeedback extends XC_MethodHook
	{
		@Override
		protected void afterHookedMethod(MethodHookParam param) throws Throwable
		{
			if(! QSHapticEnabled)
				return;
			SystemUtils.vibrate(VibrationEffect.EFFECT_CLICK);
		}
	}
	
	
}
