package sh.siava.pixelxpert.modpacks.launcher;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;
import android.graphics.drawable.AdaptiveIconDrawable;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.pixelxpert.modpacks.utils.GoogleMonochromeIconFactory;

@SuppressWarnings("RedundantThrows")
public class FeatureFlags extends XposedModPack {
	private static final String listenPackage = Constants.LAUNCHER_PACKAGE;
	private static boolean ForceThemedLauncherIcons = false;
	private int mIconBitmapSize;

	public FeatureFlags(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		ForceThemedLauncherIcons = Xprefs.getBoolean("ForceThemedLauncherIcons", false);
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		try {
			Class<?> BaseIconFactoryClass = findClass("com.android.launcher3.icons.BaseIconFactory", lpParam.classLoader);

			hookAllConstructors(BaseIconFactoryClass, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					mIconBitmapSize = getIntField(param.thisObject, "mIconBitmapSize");
				}
			});

			hookAllMethods(AdaptiveIconDrawable.class, "getMonochrome", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					try {
						if(param.getResult() == null && ForceThemedLauncherIcons)
						{
							if(new Throwable().getStackTrace()[4].getMethodName().toLowerCase().contains("override")) //It's from com.android.launcher3.icons.IconProvider.getIconWithOverrides. Monochrome is included
							{
								return;
							}

							GoogleMonochromeIconFactory mono = (GoogleMonochromeIconFactory) getAdditionalInstanceField(param.thisObject, "mMonoFactoryPX");
							if(mono == null)
							{
								mono = new GoogleMonochromeIconFactory((AdaptiveIconDrawable) param.thisObject, mIconBitmapSize);
								setAdditionalInstanceField(param.thisObject, "mMonoFactoryPX", mono);
							}
							param.setResult(mono);
						}
					}
					catch (Throwable ignored){}
				}
			});
		}
		catch (Throwable ignored){} //Android 13
	}
}