package sh.siava.pixelxpert.modpacks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.ShapeDrawable;
import android.widget.ImageView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.XPrefs;
import sh.siava.pixelxpert.modpacks.XposedModPack;

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
		if (UdfpsKeyguardViewClass == null) { //A13
			UdfpsKeyguardViewClass = findClassIfExists("com.android.systemui.biometrics.UdfpsKeyguardView", lpparam.classLoader);
		}
		Class<?> LockIconViewControllerClass = findClass("com.android.keyguard.LockIconViewController", lpparam.classLoader);

		hookAllMethods(LockIconViewControllerClass, "updateIsUdfpsEnrolled", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(transparentBG)
					setObjectField(
							getObjectField(param.thisObject, "mView"),
							"mUseBackground",
							false);
			}
		});

		hookAllConstructors(UdfpsKeyguardViewClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				try {
					hookAllMethods(getObjectField(param.thisObject, "mLayoutInflaterFinishListener").getClass(),
							"onInflateFinished",
							new XC_MethodHook() {
								@Override
								protected void afterHookedMethod(MethodHookParam param1) throws Throwable {
									removeUDFPSBG(param.thisObject);
								}
							});
				} catch (Throwable ignored) {
				}//A13
			}
		});

		hookAllMethods(UdfpsKeyguardViewClass, "onFinishInflate", new XC_MethodHook() { //A13
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				removeUDFPSBG(param.thisObject);
			}
		});

		hookAllMethods(UdfpsKeyguardViewClass,
				"updateColor", new XC_MethodHook() {
					@SuppressLint("DiscouragedApi")
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						if (!transparentBG ||
								!getBooleanField(param.thisObject, "mFullyInflated"))
							return;

						Object mLockScreenFp = getObjectField(param.thisObject, "mLockScreenFp");

						int mTextColorPrimary = SettingsLibUtilsProvider.getColorAttrDefaultColor(
								mContext.getResources().getIdentifier("wallpaperTextColorAccent", "attr", mContext.getPackageName()), mContext);

						setObjectField(param.thisObject, "mTextColorPrimary", mTextColorPrimary);

						callMethod(mLockScreenFp, "invalidate");
						param.setResult(null);
					}
				});
	}

	private void removeUDFPSBG(Object object) {
		if (!transparentBG) return;

		ImageView mBgProtection = (ImageView) getObjectField(object, "mBgProtection");
		mBgProtection.setImageDrawable(new ShapeDrawable());
	}
}
