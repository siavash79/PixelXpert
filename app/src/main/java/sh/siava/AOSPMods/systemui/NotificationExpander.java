package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.core.content.res.ResourcesCompat;

import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.R;
import sh.siava.AOSPMods.XPrefs;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class NotificationExpander extends XposedModPack {
	public static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;
	
	public static boolean notificationExpandallHookEnabled = true;
	public static boolean notificationExpandallEnabled = false;

	private static Object NotificationEntryManager;
	private Button ExpandBtn, CollapseBtn;
	private FrameLayout FooterView;
	private FrameLayout BtnLayout;
	private static int fh = 0;
	private Object Scroller;
	
	public NotificationExpander(Context context) { super(context); }
	
	@Override
	public void updatePrefs(String... Key) {
		notificationExpandallEnabled = Xprefs.getBoolean("notificationExpandallEnabled", false);
		notificationExpandallHookEnabled = Xprefs.getBoolean("notificationExpandallHookEnabled", true);

		if(Key.length > 0 && Key[0].equals("notificationExpandallEnabled"))
		{
			updateFooterBtn();
		}
	}
	
	@Override
	public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }
	
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if(!listenPackage.equals(lpparam.packageName) || !notificationExpandallHookEnabled) return;
		
		Class<?> NotificationEntryManagerClass = findClass("com.android.systemui.statusbar.notification.NotificationEntryManager", lpparam.classLoader);
		Class<?> FooterViewClass = findClass("com.android.systemui.statusbar.notification.row.FooterView", lpparam.classLoader);
		Class<?> FooterViewButtonClass = findClass("com.android.systemui.statusbar.notification.row.FooterViewButton", lpparam.classLoader);
		Class<?> NotificationStackScrollLayoutClass = findClass("com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout", lpparam.classLoader);

		//Notification Footer, where shortcuts should live
		hookAllMethods(FooterViewClass, "onFinishInflate", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				FooterView = (FrameLayout) param.thisObject;

				BtnLayout = new FrameLayout(mContext);
				FrameLayout.LayoutParams BtnFrameParams = new FrameLayout.LayoutParams(Math.round(fh*2.5f), fh);
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
		hookAllConstructors(NotificationEntryManagerClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				NotificationEntryManager = param.thisObject;
			}
		});
		
		//grab notification scroll page
		hookAllConstructors(NotificationStackScrollLayoutClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				Scroller = param.thisObject;
			}
		});
	}
	
	
	private void updateFooterBtn()
	{
		View mDismissButton;
		try {
			mDismissButton = (View) getObjectField(FooterView, "mManageButton"); //A13
		}
		catch(Throwable ignored)
		{
			mDismissButton = (View) getObjectField(FooterView, "mDismissButton"); //A12
		}

		int fh = mDismissButton.getLayoutParams().height;

		if(fh > 0) { //sometimes it's zero. we don't need that
			NotificationExpander.fh = fh;
		}
		int tc = (int) callMethod(mDismissButton, "getCurrentTextColor");

		Drawable expandArrows = ResourcesCompat.getDrawable(XPrefs.modRes, R.drawable.exapnd_icon, mContext.getTheme());
		expandArrows.setTint(tc);
		ExpandBtn.setBackground(expandArrows);
		
		Drawable collapseArrows = ResourcesCompat.getDrawable(XPrefs.modRes, R.drawable.collapse_icon, mContext.getTheme());
		collapseArrows.setTint(tc);
		CollapseBtn.setBackground(collapseArrows);
		
		BtnLayout.setVisibility(notificationExpandallEnabled ? View.VISIBLE : View.GONE);
	}
	
	public void expandAll(boolean expand)
	{
		if(NotificationEntryManager == null) return;
		
		if(!expand) {
			if(android.os.Build.VERSION.SDK_INT == 33) { //A13
				callMethod(
						Scroller,
						"setOwnScrollY",
						/* pisition */0,
						/* animate */ true);
			}
			else
			{
				callMethod(Scroller, "resetScrollPosition");
			}
		}
		
		android.util.ArraySet<?> entries = (android.util.ArraySet<?>) getObjectField(NotificationEntryManager, "mAllNotifications");
		
		for(Object entry : entries.toArray())
		{
			callMethod(entry, "setUserExpanded", expand, expand);
		}
	}
}
