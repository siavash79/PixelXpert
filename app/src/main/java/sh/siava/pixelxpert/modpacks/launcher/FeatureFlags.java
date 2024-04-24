package sh.siava.pixelxpert.modpacks.launcher;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.pixelxpert.modpacks.utils.GoogleMonochromeIconFactory;

@SuppressWarnings("RedundantThrows")
public class FeatureFlags extends XposedModPack {
	private static final String listenPackage = Constants.LAUNCHER_PACKAGE;
	private static final String FACTORY_FIELD_NAME = "mMonochromeIconFactoryPX";

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
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		try {
//			Class<?> FeatureFlags = findClass("com.android.launcher3.config.FeatureFlags", lpParam.classLoader);
			Class<?> LauncherIconsClass = findClass("com.android.launcher3.icons.LauncherIcons", lpParam.classLoader);
			Class<?> ClippedMonoDrawableClass = findClass("com.android.launcher3.icons.BaseIconFactory$ClippedMonoDrawable", lpParam.classLoader);
			hookAllMethods(LauncherIconsClass, "getMonochromeDrawable", new XC_MethodHook() {  //flag doesn't work on A14B3 for some reason
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if(!ForceThemedLauncherIcons) return;

					try
					{
						if(!(param.args[0] instanceof AdaptiveIconDrawable) || ((AdaptiveIconDrawable) param.args[0]).getMonochrome() == null)
						{
							//at this point it's clear that we don't have a monochrome icon
							GoogleMonochromeIconFactory mGoogleMonochromeIconFactory = (GoogleMonochromeIconFactory) getAdditionalInstanceField(param.thisObject, FACTORY_FIELD_NAME);
							if(mGoogleMonochromeIconFactory == null)
							{
								int mIconBitmapSize = getIntField(param.thisObject, "mIconBitmapSize");
								mGoogleMonochromeIconFactory = new GoogleMonochromeIconFactory(ClippedMonoDrawableClass, mIconBitmapSize);
								setAdditionalInstanceField(param.thisObject, FACTORY_FIELD_NAME, mGoogleMonochromeIconFactory);
							}
							param.setResult(mGoogleMonochromeIconFactory.wrap((Drawable) param.args[0]));
						}
					}
					catch (Throwable ignored){}
				}
			});
		}
		catch (Throwable ignored){} //Android 13
	}
}