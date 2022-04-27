package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.core.content.res.ResourcesCompat;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.IXposedModPack;
import sh.siava.AOSPMods.R;
import sh.siava.AOSPMods.XPrefs;

public class NotificationExpander implements IXposedModPack {
	public static final String listenPackage = "com.android.systemui";
	
	public static boolean notificationExpandallHookEnabled = true;
	public static boolean notificationExpandallEnabled = false;

	private static Object NotificationEntryManager;
	private Context mContext;
	private Button ExpandBtn, CollapseBtn;
	private FrameLayout FooterView;
	private FrameLayout BtnLayout;
	private static int fh = 0;
	private Object Scroller;
	
	@Override
	public void updatePrefs(String... Key) {
		notificationExpandallEnabled = XPrefs.Xprefs.getBoolean("notificationExpandallEnabled", false);
		notificationExpandallHookEnabled = XPrefs.Xprefs.getBoolean("notificationExpandallHookEnabled", true);

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
		
		Class<?> NotificationEntryManagerClass = XposedHelpers.findClass("com.android.systemui.statusbar.notification.NotificationEntryManager", lpparam.classLoader);
		Class<?> FooterViewClass = XposedHelpers.findClass("com.android.systemui.statusbar.notification.row.FooterView", lpparam.classLoader);
		Class<?> FooterViewButtonClass = XposedHelpers.findClass("com.android.systemui.statusbar.notification.row.FooterViewButton", lpparam.classLoader);
		Class<?> NotificationStackScrollLayoutControllerClass = XposedHelpers.findClass("com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayoutController", lpparam.classLoader);
		
		//Notification Footer, where shortcuts should live
		XposedBridge.hookAllMethods(FooterViewClass, "onFinishInflate", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				FooterView = (FrameLayout) param.thisObject;
				mContext = (Context) XposedHelpers.callMethod(param.thisObject, "getContext");
				
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
		XposedBridge.hookAllMethods(FooterViewClass, "updateColors", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				updateFooterBtn();
			}
		});
		
		
		//grab notification container manager
		XposedBridge.hookAllConstructors(NotificationEntryManagerClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				NotificationEntryManager = param.thisObject;
			}
		});
		
		//grab notification scroll page
		XposedBridge.hookAllConstructors(NotificationStackScrollLayoutControllerClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				Scroller = param.thisObject;
			}
		});
	}
	
	
	private void updateFooterBtn()
	{
		Object mDismissButton = XposedHelpers.getObjectField(FooterView, "mDismissButton");
		
		int fh = (int) XposedHelpers.callMethod(mDismissButton, "getHeight");
		
		if(fh > 0) { //sometimes it's zero. we don't need that
			NotificationExpander.fh = fh;
		}

		int tc = (int) XposedHelpers.callMethod(mDismissButton, "getCurrentTextColor");
		
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
			XposedHelpers.callMethod(Scroller, "resetScrollPosition");
		}
		
		android.util.ArraySet<?> entries = (android.util.ArraySet<?>) XposedHelpers.getObjectField(NotificationEntryManager, "mAllNotifications");
		
		for(Object entry : entries.toArray())
		{
			XposedHelpers.callMethod(entry, "setUserExpanded", expand, expand);
		}
	}
}
