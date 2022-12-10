package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.utils.FlexStatusIconContainer;
import sh.siava.AOSPMods.utils.SystemUtils;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class MultiStatusbarRows extends XposedModPack {
	private static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;

	private static boolean systemIconsMultiRow = false;

	public MultiStatusbarRows(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		if (Key.length > 0 && Key[0].equals("systemIconsMultiRow")) {
			boolean newsystemIconsMultiRow = Xprefs.getBoolean("systemIconsMultiRow", false);
			if (newsystemIconsMultiRow != systemIconsMultiRow) {
				SystemUtils.RestartSystemUI();
			}
		}
		systemIconsMultiRow = Xprefs.getBoolean("systemIconsMultiRow", false);
		FlexStatusIconContainer.setSortPlan(Integer.parseInt(Xprefs.getString("systemIconSortPlan", String.valueOf(FlexStatusIconContainer.SORT_CLEAN))));
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(listenPackage)) return;

		Class<?> IconManagerClass = findClass("com.android.systemui.statusbar.phone.StatusBarIconController$IconManager", lpparam.classLoader);

		hookAllConstructors(IconManagerClass, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (!systemIconsMultiRow) return;

				try {
					View linear = (View) param.args[0];

					String id = mContext.getResources().getResourceName(((View) linear.getParent().getParent()).getId()); //helps getting exception if it's in QS
					if (!id.contains("status_bar_end_side_content")) return;

					FlexStatusIconContainer flex = new FlexStatusIconContainer(mContext, lpparam.classLoader);
					flex.setPadding(linear.getPaddingLeft(), 0, linear.getPaddingRight(), 0);

					LinearLayout.LayoutParams flexParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
					flex.setLayoutParams(flexParams);

					flex.setForegroundGravity(Gravity.CENTER_VERTICAL | Gravity.END);

					ViewGroup parent = (ViewGroup) linear.getParent();
					int index = parent.indexOfChild(linear);
					parent.addView(flex, index);
					linear.setVisibility(View.GONE); //remove will crash the system
					param.args[0] = flex;

				} catch (Throwable ignored) {
				}
			}
		});
	}

}

