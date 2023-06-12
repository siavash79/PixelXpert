package sh.siava.AOSPMods.modpacks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.core.content.res.ResourcesCompat;

import java.util.Collection;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.R;
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.ResourceManager;
import sh.siava.AOSPMods.modpacks.XPLauncher;
import sh.siava.AOSPMods.modpacks.XPrefs;
import sh.siava.AOSPMods.modpacks.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class NotificationExpander extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	public static boolean notificationExpandallHookEnabled = true;
	public static boolean notificationExpandallEnabled = false;

	private Button ExpandBtn, CollapseBtn;
	private FrameLayout FooterView;
	private FrameLayout BtnLayout;
	private static int fh = 0;
	private Object Scroller;
	private Object NotifCollection = null;

	public NotificationExpander(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		notificationExpandallEnabled = XPrefs.Xprefs.getBoolean("notificationExpandallEnabled", false);
		notificationExpandallHookEnabled = XPrefs.Xprefs.getBoolean("notificationExpandallHookEnabled", true);

		if (Key.length > 0 && Key[0].equals("notificationExpandallEnabled")) {
			updateFooterBtn();
		}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!listenPackage.equals(lpparam.packageName) || !notificationExpandallHookEnabled) return;

		Class<?> FooterViewClass = findClass("com.android.systemui.statusbar.notification.row.FooterView", lpparam.classLoader);
		Class<?> FooterViewButtonClass = findClass("com.android.systemui.statusbar.notification.row.FooterViewButton", lpparam.classLoader);
		Class<?> NotificationStackScrollLayoutClass = findClass("com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout", lpparam.classLoader);
		Class<?> NotifCollectionClass = findClassIfExists("com.android.systemui.statusbar.notification.collection.NotifCollection", lpparam.classLoader);

		//Notification Footer, where shortcuts should live
		hookAllMethods(FooterViewClass, "onFinishInflate", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				FooterView = (FrameLayout) param.thisObject;

				BtnLayout = new FrameLayout(mContext);
				FrameLayout.LayoutParams BtnFrameParams = new FrameLayout.LayoutParams(Math.round(fh * 2.5f), fh);
				BtnFrameParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
				BtnLayout.setLayoutParams(BtnFrameParams);

				ExpandBtn = (Button) FooterViewButtonClass.getConstructor(Context.class).newInstance(mContext);
				BtnLayout.addView(ExpandBtn);

				FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(fh, fh);
				layoutParams.gravity = Gravity.START | Gravity.BOTTOM;
				ExpandBtn.setLayoutParams(layoutParams);

				ExpandBtn.setOnClickListener(v -> expandAll(true));

				CollapseBtn = (Button) FooterViewButtonClass.getConstructor(Context.class).newInstance(mContext);
				BtnLayout.addView(CollapseBtn);

				FrameLayout.LayoutParams lpc = new FrameLayout.LayoutParams(fh, fh);
				lpc.gravity = Gravity.END | Gravity.BOTTOM;
				CollapseBtn.setLayoutParams(lpc);

				CollapseBtn.setOnClickListener(v -> expandAll(false));

				updateFooterBtn();
				FooterView.addView(BtnLayout);
			}
		});

		//theme changed
		hookAllMethods(FooterViewClass, "updateColors", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				updateFooterBtn();
			}
		});


		//grab notification container manager
		if (NotifCollectionClass != null) {
			hookAllConstructors(NotifCollectionClass, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					NotifCollection = param.thisObject;
				}
			});
		}

		//grab notification scroll page
		hookAllConstructors(NotificationStackScrollLayoutClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				Scroller = param.thisObject;
			}
		});
	}


	private void updateFooterBtn() {
		if(FooterView == null) return; //Footer not inflated yet

		View mDismissButton = (View) getObjectField(FooterView, "mManageButton"); //A13

		int fh = mDismissButton.getLayoutParams().height;

		if (fh > 0) { //sometimes it's zero. we don't need that
			NotificationExpander.fh = fh;
		}
		int tc = (int) callMethod(mDismissButton, "getCurrentTextColor");

		Drawable expandArrows = ResourcesCompat.getDrawable(ResourceManager.modRes, R.drawable.ic_expand, mContext.getTheme());
		expandArrows.setTint(tc);
		ExpandBtn.setBackground(expandArrows);

		Drawable collapseArrows = ResourcesCompat.getDrawable(ResourceManager.modRes, R.drawable.ic_collapse, mContext.getTheme());
		collapseArrows.setTint(tc);
		CollapseBtn.setBackground(collapseArrows);

		BtnLayout.setVisibility(notificationExpandallEnabled ? View.VISIBLE : View.GONE);
	}

	public void expandAll(boolean expand) {
		if (NotifCollection == null) return;

		if (!expand) {
			callMethod(
					Scroller,
					"setOwnScrollY",
					/* pisition */0,
					/* animate */ true);
		}

		Collection<Object> entries;
		//noinspection unchecked
		entries = (Collection<Object>) getObjectField(NotifCollection, "mReadOnlyNotificationSet");
		for (Object entry : entries.toArray()) {
			Object row = getObjectField(entry, "row");
			if (row != null) {
				callMethod(row, "setUserExpanded", expand, expand);
			}
		}

	}
}
