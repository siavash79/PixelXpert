package sh.siava.pixelxpert.modpacks.systemui;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;
import static sh.siava.pixelxpert.modpacks.systemui.BatteryDataProvider.BATTERY_STATUS_DISCHARGING;
import static sh.siava.pixelxpert.modpacks.systemui.BatteryDataProvider.isCharging;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.pixelxpert.modpacks.utils.StringFormatter;
import sh.siava.pixelxpert.modpacks.utils.SystemUtils;

@SuppressWarnings("RedundantThrows")
public class QSFooterManager extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;
	private static boolean customQSFooterTextEnabled = false;
	private static String customText = "";
	private Object QSFooterView;
	private final StringFormatter stringFormatter = new StringFormatter();

	private TextView mChargingIndicator;
	private static boolean ChargingInfoOnQSEnabled = false;

	public void updatePrefs(String... Key) {
		if (Xprefs == null) return;
		customQSFooterTextEnabled = Xprefs.getBoolean("QSFooterMod", false);
		customText = Xprefs.getString("QSFooterText", "");

		ChargingInfoOnQSEnabled = Xprefs.getBoolean("ChargingInfoOnQSEnabled", true);

		setQSFooterText();
		setChargingIndicatorVisibility();
	}

	public QSFooterManager(Context context) {
		super(context);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		stringFormatter.registerCallback(this::setQSFooterText);

		Class<?> QSFooterViewClass = findClass("com.android.systemui.qs.QSFooterView", lpparam.classLoader);
		Class<?> QSContainerImplClass = findClass("com.android.systemui.qs.QSContainerImpl", lpparam.classLoader);

		hookAllConstructors(QSFooterViewClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				QSFooterView = param.thisObject;
			}
		});

		hookAllMethods(QSFooterViewClass,
				"setBuildText", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						if (!customQSFooterTextEnabled) return;
						setQSFooterText();
					}
				});

		hookAllMethods(QSContainerImplClass, "onFinishInflate", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				createQSBatteryIndicator((FrameLayout)param.thisObject);

			}
		});
	}

	private void createQSBatteryIndicator(FrameLayout QSContainerImpl) {
		Resources res = mContext.getResources();

		@SuppressLint("DiscouragedApi")
		LinearLayout QSPanelView = QSContainerImpl.findViewById(res.getIdentifier("quick_settings_panel", "id", mContext.getPackageName()));

		mChargingIndicator = new TextView(mContext);
		mChargingIndicator.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);

		mChargingIndicator.setTextColor(SystemUtils.isDarkMode() ? WHITE : BLACK);
		mChargingIndicator.setMaxLines(2);

		QSPanelView.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
			@Override
			public void onChildViewAdded(View parent, View child) {
				if(QSPanelView.indexOfChild(mChargingIndicator) < QSPanelView.getChildCount() - 1)
				{
					QSPanelView.removeView(mChargingIndicator);
					QSPanelView.addView(mChargingIndicator);
				}
			}

			@Override
			public void onChildViewRemoved(View parent, View child) {}
		});

		mChargingIndicator.setText(KeyguardMods.getPowerIndicationString());
		setChargingIndicatorVisibility();

		BatteryDataProvider.registerStatusCallback((batteryStatus, batteryStatusIntent) -> {
			if(ChargingInfoOnQSEnabled && batteryStatus != BATTERY_STATUS_DISCHARGING)
			{
				mChargingIndicator.setText(KeyguardMods.getPowerIndicationString());
			}
			setChargingIndicatorVisibility();
		});
	}

	private void setQSFooterText() {
		try {
			if (customQSFooterTextEnabled) {
				TextView mBuildText = (TextView) getObjectField(QSFooterView, "mBuildText");

				setObjectField(QSFooterView,
						"mShouldShowBuildText",
						customText.trim().length() > 0);

				mBuildText.post(() -> {
					mBuildText.setText(stringFormatter.formatString(customText));
					mBuildText.setSelected(true);
				});
			} else {
				callMethod(QSFooterView,
						"setBuildText");
			}
		} catch (Throwable ignored) {
		} //probably not initiated yet
	}

	public void setChargingIndicatorVisibility()
	{
		if(mChargingIndicator != null) {
			mChargingIndicator.setVisibility(ChargingInfoOnQSEnabled && isCharging()
					? VISIBLE
					: GONE);
		}
	}


	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}
}
