package sh.siava.pixelxpert.modpacks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;
import static sh.siava.pixelxpert.modpacks.utils.toolkit.ColorUtils.getColorAttrDefaultColor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.ShapeDrawable;
import android.widget.ImageView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class UDFPSManager extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;
	private static boolean transparentBG = false;
	private static boolean transparentFG = false;

	public UDFPSManager(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		if (Xprefs == null) return;
		transparentBG = Xprefs.getBoolean("fingerprint_circle_hide", false);
		transparentFG = Xprefs.getBoolean("fingerprint_icon_hide", false);
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) {
		if (!lpParam.packageName.equals(listenPackage)) return;

		Class<?> UdfpsKeyguardViewClass = findClassIfExists("com.android.systemui.biometrics.UdfpsKeyguardViewLegacy", lpParam.classLoader); //A4B3
		if (UdfpsKeyguardViewClass == null) { //A13
			UdfpsKeyguardViewClass = findClassIfExists("com.android.systemui.biometrics.UdfpsKeyguardView", lpParam.classLoader);
		}
		Class<?> LockIconViewControllerClass = findClass("com.android.keyguard.LockIconViewController", lpParam.classLoader);

		hookAllMethods(LockIconViewControllerClass, "updateIsUdfpsEnrolled", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(transparentBG) {
					setObjectField(
							getObjectField(param.thisObject, "mView"),
							"mUseBackground",
							false);

					callMethod(getObjectField(param.thisObject, "mView"), "updateColorAndBackgroundVisibility");
				}
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
									removeUDFPSGraphics(param.thisObject);
								}
							});
				} catch (Throwable ignored) {
				}//A13
			}
		});

		hookAllMethods(UdfpsKeyguardViewClass, "onFinishInflate", new XC_MethodHook() { //A13
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				removeUDFPSGraphics(param.thisObject);
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

						int mTextColorPrimary = getColorAttrDefaultColor(
								mContext,
								mContext.getResources().getIdentifier("wallpaperTextColorAccent", "attr", mContext.getPackageName()));

						setObjectField(param.thisObject, "mTextColorPrimary", mTextColorPrimary);

						callMethod(mLockScreenFp, "invalidate");
						param.setResult(null);
					}
				});
	}

	private void removeUDFPSGraphics(Object object) {
		try
		{
			if (transparentBG) {
				ImageView mBgProtection = (ImageView) getObjectField(object, "mBgProtection");
				mBgProtection.setImageDrawable(new ShapeDrawable());
			}

			if (transparentFG) {
				ImageView mLockScreenFp = (ImageView) getObjectField(object, "mLockScreenFp");
				mLockScreenFp.setImageDrawable(new ShapeDrawable());
				
				ImageView mAodFp = (ImageView) getObjectField(object, "mAodFp");
				mAodFp.setImageDrawable(new ShapeDrawable());
			}
		}
		catch (Throwable ignored){}
	}
}
