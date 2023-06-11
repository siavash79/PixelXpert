package sh.siava.AOSPMods.modpacks.launcher;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.AOSPMods.modpacks.XPrefs.Xprefs;

import android.content.Context;
import android.graphics.drawable.AdaptiveIconDrawable;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.modpacks.Constants;
import sh.siava.AOSPMods.modpacks.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class FeatureFlags extends XposedModPack {
	private static final String listenPackage = Constants.LAUNCHER_PACKAGE;

	private static boolean ForceThemedLauncherIcons = false;

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
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		try {
			Class<?> FeatureFlags = findClass("com.android.launcher3.config.FeatureFlags", lpparam.classLoader);
			Class<?> LauncherIconsClass = findClass("com.android.launcher3.icons.LauncherIcons", lpparam.classLoader);

			hookAllMethods(LauncherIconsClass, "getMonochromeDrawable", new XC_MethodHook() {  //flag doesn't work on A14B3 for some reason
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if(!ForceThemedLauncherIcons) return;

					try
					{
						if(!(param.args[0] instanceof AdaptiveIconDrawable) || ((AdaptiveIconDrawable) param.args[0]).getMonochrome() == null)
						{
							//at this point it's clear that we don't have a monochrome icon
							Object mMonochromeIconFactory = getObjectField(param.thisObject, "mMonochromeIconFactory");
							if(mMonochromeIconFactory == null)
							{
								Class<?> MonochromeIconFactoryClass = findClass("com.android.launcher3.icons.MonochromeIconFactory", lpparam.classLoader);
								int mIconBitmapSize = getIntField(param.thisObject, "mIconBitmapSize");
								mMonochromeIconFactory = MonochromeIconFactoryClass.getConstructors()[0].newInstance(mIconBitmapSize);
								setObjectField(param.thisObject, "mMonochromeIconFactory", mMonochromeIconFactory);
							}
							param.setResult(callMethod(mMonochromeIconFactory, "wrap", param.args[0]));
						}
					}
					catch (Throwable ignored){}
				}
			});

			hookAllMethods(FeatureFlags.getField("ENABLE_APP_CLONING_CHANGES_IN_LAUNCHER").getType(), "get", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						param.setResult(true);
				}
			});

/*			hookAllMethods(FeatureFlags.getField("ENABLE_FORCED_MONO_ICON").getType(), "get", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if (ForceThemedLauncherIcons)
						param.setResult(true);
				}
			});*/
		}
		catch (Throwable ignored){} //Android 13
	}
}