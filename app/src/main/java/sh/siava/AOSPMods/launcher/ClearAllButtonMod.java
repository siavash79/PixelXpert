package sh.siava.AOSPMods.launcher;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findMethodBestMatch;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.core.content.res.ResourcesCompat;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.R;
import sh.siava.AOSPMods.XPrefs;
import sh.siava.AOSPMods.XposedModPack;
import sh.siava.AOSPMods.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class ClearAllButtonMod extends XposedModPack {
	private static final String listenPackage = AOSPMods.LAUNCHER_PACKAGE;

	private Object recentView;
	private static boolean RecentClearAllReposition = false;
	private ImageView clearAllIcon;

	public ClearAllButtonMod(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		if(Key.length > 0 && Key[0].equals("RecentClearAllReposition"))
		{
			android.os.Process.killProcess(android.os.Process.myPid());
		}

		RecentClearAllReposition = Xprefs.getBoolean("RecentClearAllReposition", false);
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(listenPackage)) return;

		Class<?> OverviewActionsViewClass = findClass("com.android.quickstep.views.OverviewActionsView", lpparam.classLoader);
		Class<?> RecentsViewClass = findClass("com.android.quickstep.views.RecentsView", lpparam.classLoader);
		Method dismissAllTasksMethod = findMethodBestMatch(RecentsViewClass, "dismissAllTasks", View.class);

		hookAllConstructors(RecentsViewClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				recentView = param.thisObject;
				log(recentView.getClass().getName());
			}
		});

		hookAllMethods(RecentsViewClass, "setColorTint", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(!RecentClearAllReposition) return;

				clearAllIcon.getDrawable().setTintList(getThemedColor(mContext));
				//clearAllButton.getBackground().setTintList(getThemedColor(mContext));
			}
		});

		hookAllMethods(OverviewActionsViewClass, "onFinishInflate", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if(!RecentClearAllReposition) return;

				FrameLayout clearAllButton = new FrameLayout(mContext);

				clearAllIcon = new ImageView(mContext);
				clearAllIcon.setImageDrawable(ResourcesCompat.getDrawable(XPrefs.modRes, R.drawable.ic_clear_all, mContext.getTheme()));
				clearAllButton.addView(clearAllIcon);

				clearAllIcon.getDrawable().setTintList(getThemedColor(mContext));

				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

				@SuppressLint("DiscouragedApi")
				LinearLayout buttonsLayout =  ((View)param.thisObject).findViewById(mContext.getResources().getIdentifier("action_buttons", "id", mContext.getOpPackageName()));

				buttonsLayout.setVerticalGravity(Gravity.CENTER_VERTICAL);
				int margins = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 25, Resources.getSystem().getDisplayMetrics());

				params.rightMargin = params.leftMargin = margins;

				clearAllButton.setLayoutParams(params);
				clearAllButton.setOnClickListener(v -> {
					if(recentView != null)
					{
						try {
							dismissAllTasksMethod.invoke(recentView, v);
						} catch (Throwable ignored) {}
					}
				});

				clearAllButton.setPadding(margins/2,0,margins/2,0);
				buttonsLayout.addView(clearAllButton, buttonsLayout.getChildCount() - 2);
			}
		});
	}

	public static ColorStateList getThemedColor(Context context) {
		return getSystemAttrColor(context, SystemUtils.isDarkMode()
				? android.R.attr.textColorPrimaryInverse
				: android.R.attr.textColorPrimary);
	}

	public static ColorStateList getSystemAttrColor(Context context, int attr) {
		TypedArray a = context.obtainStyledAttributes(new int[] { attr });
		ColorStateList color = a.getColorStateList(a.getIndex(0));
		a.recycle();
		return color;
	}
}