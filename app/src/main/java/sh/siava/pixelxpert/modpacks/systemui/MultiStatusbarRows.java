package sh.siava.pixelxpert.modpacks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedHelpers.findClass;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.XPrefs;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.pixelxpert.modpacks.utils.FlexStatusIconContainer;
import sh.siava.pixelxpert.modpacks.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class MultiStatusbarRows extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	private static boolean systemIconsMultiRow = false;

	public MultiStatusbarRows(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		if (Key.length > 0 && Key[0].equals("systemIconsMultiRow")) { //WHY we check the old value? because if prefs is empty it will fill it up and count an unwanted change
			boolean newsystemIconsMultiRow = XPrefs.Xprefs.getBoolean("systemIconsMultiRow", false);
			if (newsystemIconsMultiRow != systemIconsMultiRow) {
				SystemUtils.killSelf();
			}
		}
		systemIconsMultiRow = XPrefs.Xprefs.getBoolean("systemIconsMultiRow", false);
		FlexStatusIconContainer.setSortPlan(Integer.parseInt(XPrefs.Xprefs.getString("systemIconSortPlan", String.valueOf(FlexStatusIconContainer.SORT_CLEAN))));
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
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
					View linearStatusbarIconContainer = (View) param.args[0];

					String id = mContext.getResources().getResourceName(((View) linearStatusbarIconContainer.getParent().getParent()).getId()); //helps getting exception if it's in QS
					if (!id.contains("status_bar_end_side_content")) return;

					FlexStatusIconContainer flex = new FlexStatusIconContainer(mContext, lpparam.classLoader, linearStatusbarIconContainer);
					flex.setPadding(linearStatusbarIconContainer.getPaddingLeft(), 0, linearStatusbarIconContainer.getPaddingRight(), 0);

					LinearLayout.LayoutParams flexParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
					flex.setLayoutParams(flexParams);

					flex.setForegroundGravity(Gravity.CENTER_VERTICAL | Gravity.END);

					ViewGroup parent = (ViewGroup) linearStatusbarIconContainer.getParent();
					int index = parent.indexOfChild(linearStatusbarIconContainer);
					parent.addView(flex, index);
					parent.getLayoutParams().height = LinearLayout.LayoutParams.MATCH_PARENT;
					linearStatusbarIconContainer.setVisibility(View.GONE); //remove will crash the system
					param.args[0] = flex;

				} catch (Throwable ignored) {
				}
			}
		});
	}

}

