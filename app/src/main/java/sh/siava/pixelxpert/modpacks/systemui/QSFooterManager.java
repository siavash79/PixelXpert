package sh.siava.pixelxpert.modpacks.systemui;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getFloatField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.view.Gravity;
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
	private Object QSFV;
	private final StringFormatter stringFormatter = new StringFormatter();

	private TextView mChargingIndicator;
	private boolean mCharging = false;
	private boolean mQSOpen = false;
	private static boolean ChargingInfoOnQSEnabled = false;
	private LinearLayout mQSFooterContainer;


	public void updatePrefs(String... Key) {
		if (Xprefs == null) return;
		customQSFooterTextEnabled = Xprefs.getBoolean("QSFooterMod", false);
		customText = Xprefs.getString("QSFooterText", "");

		ChargingInfoOnQSEnabled = Xprefs.getBoolean("ChargingInfoOnQSEnabled", true);

		if(!ChargingInfoOnQSEnabled)
			mQSOpen = false;

		setQSFooterText();
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
				QSFV = param.thisObject;
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

		hookAllMethods(QSContainerImplClass, "updateExpansion", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(ChargingInfoOnQSEnabled)
				{
					mQSOpen = getFloatField(param.thisObject, "mQsExpansion") != 0;
					setChargingIndicatorVisibility();
				}
			}
		});

		hookAllMethods(QSContainerImplClass, "updateResources", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mQSFooterContainer.getLayoutParams();
				lp.leftMargin = -1*getIntField(param.thisObject, "mTilesPageMargin");
				lp.rightMargin = lp.leftMargin;
			}
		});

	}

	private void createQSBatteryIndicator(FrameLayout QSContainerImpl) {
		Resources res = mContext.getResources();

		@SuppressLint("DiscouragedApi")
		View footerActionsView = QSContainerImpl.findViewById(res.getIdentifier("qs_footer_actions", "id", mContext.getPackageName()));

		mQSFooterContainer = new LinearLayout(mContext);
		mQSFooterContainer.setOrientation(LinearLayout.VERTICAL);

		mChargingIndicator = new TextView(mContext);
		mChargingIndicator.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
		mChargingIndicator.setVisibility(GONE);

		mChargingIndicator.setTextColor(SystemUtils.isDarkMode() ? WHITE : BLACK);
		mChargingIndicator.setMaxLines(2);

		ViewGroup footerActionsParent = (ViewGroup)footerActionsView.getParent();
		footerActionsParent.removeView(footerActionsView);

		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		lp.gravity = Gravity.BOTTOM;

		mQSFooterContainer.addView(mChargingIndicator);
		mQSFooterContainer.addView(footerActionsView);

		QSContainerImpl.addView(mQSFooterContainer);
		mQSFooterContainer.setLayoutParams(lp);

		mChargingIndicator.setText(KeyguardMods.getPowerIndicationString());

		BatteryInfoUpdateIndicator.registerCallback(batteryStatus -> {
			if(ChargingInfoOnQSEnabled && batteryStatus != BatteryInfoUpdateIndicator.BATTERY_STATUS_DISCHARGING)
			{
				mChargingIndicator.setText(KeyguardMods.getPowerIndicationString());
				mCharging = true;
			}
			else
			{
				mCharging = false;
			}
			setChargingIndicatorVisibility();
		});

	}

	private void setQSFooterText() {
		try {
			if (customQSFooterTextEnabled) {
				TextView mBuildText = (TextView) getObjectField(QSFV, "mBuildText");

				setObjectField(QSFV,
						"mShouldShowBuildText",
						customText.trim().length() > 0);

				mBuildText.post(() -> {
					mBuildText.setText(stringFormatter.formatString(customText));
					mBuildText.setSelected(true);
				});
			} else {
				callMethod(QSFV,
						"setBuildText");
			}
		} catch (Throwable ignored) {
		} //probably not initiated yet
	}

	public void setChargingIndicatorVisibility()
	{
		mChargingIndicator.setVisibility(mCharging && mQSOpen
				? VISIBLE
				: GONE);
	}


	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}
}
