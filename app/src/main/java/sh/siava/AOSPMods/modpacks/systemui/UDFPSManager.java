package sh.siava.AOSPMods.modpacks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.ImageView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.XPLauncher;
import sh.siava.AOSPMods.modpacks.XPrefs;
import sh.siava.AOSPMods.modpacks.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class UDFPSManager extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;
	private static boolean transparentBG = false;

	public UDFPSManager(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		if (XPrefs.Xprefs == null) return;
		transparentBG = XPrefs.Xprefs.getBoolean("fingerprint_circle_hide", false);
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
		if (!lpparam.packageName.equals(listenPackage)) return;

		Class<?> UdfpsKeyguardViewClass = findClassIfExists("com.android.systemui.biometrics.UdfpsKeyguardViewLegacy", lpparam.classLoader); //A4B3
		if(UdfpsKeyguardViewClass == null)
		{ //A13
			UdfpsKeyguardViewClass = findClassIfExists("com.android.systemui.biometrics.UdfpsKeyguardView", lpparam.classLoader);
		}
		Class<?> LockIconViewClass = findClass("com.android.keyguard.LockIconView", lpparam.classLoader);


		XC_MethodHook FPCircleTransparenter = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (!transparentBG) return;

				try {
					ImageView mBgProtection = (ImageView) getObjectField(param.thisObject, "mBgProtection");
					mBgProtection.setImageAlpha(0);
				} catch (Throwable ignored) {
				} //if (!mFullyInflated) A13
			}
		};

		hookAllMethods(UdfpsKeyguardViewClass, "updateBurnInOffsets", FPCircleTransparenter);
		hookAllMethods(UdfpsKeyguardViewClass, "onFinishInflate", FPCircleTransparenter);


		hookAllMethods(LockIconViewClass,
				"updateIcon", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						setObjectField(param.thisObject, "mUseBackground", false);
					}
				});

		hookAllMethods(UdfpsKeyguardViewClass,
				"updateColor", new XC_MethodHook() {
					@SuppressLint("DiscouragedApi")
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						if (!transparentBG) return;

						Object mLockScreenFp = getObjectField(param.thisObject, "mLockScreenFp");

						if (mLockScreenFp == null) return;

						int mTextColorPrimary = SettingsLibUtilsProvider.getColorAttrDefaultColor(
								mContext.getResources().getIdentifier("wallpaperTextColorAccent", "attr", mContext.getPackageName()), mContext);


						setObjectField(param.thisObject, "mTextColorPrimary", mTextColorPrimary);

						callMethod(mLockScreenFp, "invalidate");
						param.setResult(null);
					}
				});
	}
}
