package sh.siava.pixelxpert.xposed.modpacks.systemui;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static sh.siava.pixelxpert.xposed.XPrefs.Xprefs;
import static sh.siava.pixelxpert.xposed.utils.SystemUtils.idOf;
import static sh.siava.pixelxpert.xposed.utils.toolkit.ObjectTools.tryParseInt;
import static sh.siava.pixelxpert.xposed.utils.toolkit.ReflectionTools.reAddView;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import java.util.Collection;
import java.util.regex.Pattern;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.xposed.ResourceManager;
import sh.siava.pixelxpert.xposed.XposedModPack;
import sh.siava.pixelxpert.xposed.annotations.SystemUIModPack;
import sh.siava.pixelxpert.xposed.utils.toolkit.ReflectedClass;

@SuppressWarnings("RedundantThrows")
@SystemUIModPack
public class NotificationExpander extends XposedModPack {
	private static final int DEFAULT = 0;
	private static final int EXPAND_ALWAYS = 1;
	/** @noinspection unused*/
	private static final int COLLAPSE_ALWAYS = 2;

	public static boolean notificationExpandallHookEnabled = true;
	public static boolean notificationExpandallEnabled = false;
	private static int notificationDefaultExpansion = DEFAULT;
	private Button ExpandBtn, CollapseBtn;
	private FrameLayout FooterView;
	private LinearLayout BtnLayout;
	private View Scroller;
	private Object NotifCollection = null;
	private LinearLayout mDismissContainer;

	public NotificationExpander(Context context) {
		super(context);
	}

	@Override
	public void onPreferenceUpdated(String... Key) {
		notificationExpandallEnabled = Xprefs.getBoolean("notificationExpandallEnabled", false);
		notificationExpandallHookEnabled = Xprefs.getBoolean("notificationExpandallHookEnabled", true);
		notificationDefaultExpansion = tryParseInt(Xprefs.getString("notificationDefaultExpansion", "0"), 0);

		if (Key.length > 0 && Key[0].equals("notificationExpandallEnabled")) {
			updateFooterBtn();
		}
	}

	@Override
	public void onPackageLoaded(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		ReflectedClass NotificationStackScrollLayoutClass = ReflectedClass.of("com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout");
		ReflectedClass FooterViewButtonClass = ReflectedClass.of("com.android.systemui.statusbar.notification.row.FooterViewButton");
		ReflectedClass NotifCollectionClass = ReflectedClass.ofIfPossible("com.android.systemui.statusbar.notification.collection.NotifCollection");
		ReflectedClass NotificationPanelViewControllerClass = ReflectedClass.of("com.android.systemui.shade.NotificationPanelViewController");
		ReflectedClass FooterViewClass = ReflectedClass.of("com.android.systemui.statusbar.notification.footer.ui.view.FooterView");

		//region default notification state
		NotificationPanelViewControllerClass
				.before("notifyExpandingStarted")
				.run(param -> {
					if(notificationDefaultExpansion != DEFAULT)
						expandAll(notificationDefaultExpansion == EXPAND_ALWAYS);
				});
		//endregion

		//Notification Footer, where shortcuts should live
		FooterViewClass
				.after(Pattern.compile("updateColors.*"))
				.run(param -> updateFooterBtn());

		FooterViewClass
				.after("onFinishInflate")
				.run(param -> {
					FooterView = (FrameLayout) param.thisObject;

					FooterView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
						@Override
						public void onViewAttachedToWindow(@NonNull View v) {
							updateFooterBtn();
						}
						@Override
						public void onViewDetachedFromWindow(@NonNull View v) {
						}
					});

					BtnLayout = new LinearLayout(mContext);
					BtnLayout.setClipChildren(false);

					LinearLayout.LayoutParams BtnFrameParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, WRAP_CONTENT);
					BtnFrameParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
					BtnLayout.setLayoutParams(BtnFrameParams);

					reAddView(BtnLayout,FooterView.findViewById(idOf("history_button")));

					ExpandBtn = (Button) FooterViewButtonClass.getClazz().getConstructor(Context.class).newInstance(mContext);
					Drawable expandArrows = ResourcesCompat.getDrawable(ResourceManager.modRes, R.drawable.ic_expand, mContext.getTheme());
					ExpandBtn.setForeground(expandArrows);

					BtnLayout.addView(ExpandBtn);


					mDismissContainer = new LinearLayout(mContext);
					reAddView(mDismissContainer, FooterView.findViewById(idOf("dismiss_text")));

					BtnLayout.addView(mDismissContainer);

					ExpandBtn.setOnClickListener(v -> expandAll(true));

					CollapseBtn = (Button) FooterViewButtonClass.getClazz().getConstructor(Context.class).newInstance(mContext);
					Drawable collapseArrows = ResourcesCompat.getDrawable(ResourceManager.modRes, R.drawable.ic_collapse, mContext.getTheme());
					CollapseBtn.setForeground(collapseArrows);


					BtnLayout.addView(CollapseBtn);

					reAddView(BtnLayout,FooterView.findViewById(idOf("settings_button")));

					CollapseBtn.setOnClickListener(v -> expandAll(false));

					updateFooterBtn();
					FooterView.addView(BtnLayout);
				});

		//theme changed
		FooterViewClass
				.after("updateColors")
				.run(param -> updateFooterBtn());

		//grab notification container manager
		if (NotifCollectionClass.getClazz() != null) {
			NotifCollectionClass
					.afterConstruction()
					.run(param -> NotifCollection = param.thisObject);
		}

		//grab notification scroll page
		NotificationStackScrollLayoutClass
				.afterConstruction()
				.run(param -> Scroller = (View) param.thisObject);
	}

	private void updateFooterBtn() {
		if(FooterView == null) return; //Footer not inflated yet

		FooterView.post(() -> {
			try {
				Button clearAllButton = BtnLayout.findViewById(idOf("dismiss_text"));

				clearAllButton.getLayoutParams().width = -1;

				LinearLayout.LayoutParams dismissContainerParams = new LinearLayout.LayoutParams(0, WRAP_CONTENT);
				dismissContainerParams.weight = 1;
				mDismissContainer.setLayoutParams(dismissContainerParams);

				//must clone a fully new drawable for each to avoid effects conflict
				//noinspection DataFlowIssue
				ExpandBtn.setBackground(clearAllButton.getBackground().getConstantState().newDrawable());
				CollapseBtn.setBackground(clearAllButton.getBackground().getConstantState().newDrawable());

				int textColor =  clearAllButton.getCurrentTextColor();
				ExpandBtn.getForeground().setTint(textColor);
				CollapseBtn.getForeground().setTint(textColor);

				setLayouParams(ExpandBtn, true);
				setLayouParams(CollapseBtn, false);

				ExpandBtn.setVisibility(notificationExpandallEnabled ? VISIBLE : GONE);
				CollapseBtn.setVisibility(ExpandBtn.getVisibility());
			}
			catch (Throwable ignored){}
		});
	}

	private void setLayouParams(Button button, boolean marginToLeft) {
		int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, mContext.getResources().getDisplayMetrics());

		LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(size, size);
		btnParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
		int margin = (int)
				TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
						8,
						mContext.getResources().getDisplayMetrics());

		if(marginToLeft)
			btnParams.setMarginStart(margin);
		else
			btnParams.setMarginEnd(margin);

		button.post(() -> button.setLayoutParams(btnParams));
	}

	public void expandAll(boolean expand) {
		if (NotifCollection == null) return;

		Collection<Object> entries;
		//noinspection unchecked
		entries = (Collection<Object>) getObjectField(NotifCollection, "mReadOnlyNotificationSet");
		for (Object entry : entries.toArray()) {
			Object row = getObjectField(entry, "row");
			if (row != null) {
				setRowExpansion(row, expand);
			}
		}

		if (!expand) {
			Scroller.requestLayout();
		}

	}

	private void setRowExpansion(Object row, boolean expand) {
		callMethod(row, "setUserExpanded", expand, true);
	}
}
