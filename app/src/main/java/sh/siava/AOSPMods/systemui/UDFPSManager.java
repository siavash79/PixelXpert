package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.widget.ImageView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class UDFPSManager extends XposedModPack {
	private static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;
	private static boolean transparentBG = false;

	public UDFPSManager(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		if (Xprefs == null) return;
		transparentBG = Xprefs.getBoolean("fingerprint_circle_hide", false);
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
		if (!lpparam.packageName.equals(listenPackage)) return;

		Class<?> UtilsClass = findClass("com.android.settingslib.Utils", lpparam.classLoader);
		Class<?> UdfpsKeyguardViewClass = findClass("com.android.systemui.biometrics.UdfpsKeyguardView", lpparam.classLoader);
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
		if (Build.VERSION.SDK_INT == 33) { //A13
			hookAllMethods(UdfpsKeyguardViewClass, "updateBurnInOffsets", FPCircleTransparenter);
			hookAllMethods(UdfpsKeyguardViewClass, "onFinishInflate", FPCircleTransparenter);


			hookAllMethods(LockIconViewClass,
					"updateIcon", new XC_MethodHook() {
						@Override
						protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
							setObjectField(param.thisObject, "mUseBackground", false);
						}
					});
		} else { //A12
			hookAllMethods(UdfpsKeyguardViewClass, "updateAlpha", FPCircleTransparenter);

			hookAllMethods(LockIconViewClass,
					"setUseBackground", new XC_MethodHook() {
						@Override
						protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
							if (!transparentBG) return;
							param.args[0] = false;
						}
					});
		}

		hookAllMethods(UdfpsKeyguardViewClass,
				"updateColor", new XC_MethodHook() {
					@SuppressLint("DiscouragedApi")
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						if (!transparentBG) return;

						Object mLockScreenFp = getObjectField(param.thisObject, "mLockScreenFp");

						if (mLockScreenFp == null) return;

						int mTextColorPrimary;
						try { //13 R18 Pixel: int,context!!
							mTextColorPrimary = (int) callStaticMethod(UtilsClass, "getColorAttrDefaultColor",
									mContext.getResources().getIdentifier("wallpaperTextColorAccent", "attr", mContext.getPackageName()), mContext);
						}
						catch (Throwable ignored) //prior ones: context, int
						{
							mTextColorPrimary = (int) callStaticMethod(UtilsClass, "getColorAttrDefaultColor", mContext,
									mContext.getResources().getIdentifier("wallpaperTextColorAccent", "attr", mContext.getPackageName()));
						}

						setObjectField(param.thisObject, "mTextColorPrimary", mTextColorPrimary);

						callMethod(mLockScreenFp, "invalidate");
						param.setResult(null);
					}
				});
	}
}
