package sh.siava.pixelxpert.modpacks.launcher;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;
import static sh.siava.pixelxpert.modpacks.utils.toolkit.ReflectionTools.hookAllConstructors;

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
	private static boolean ForceThemedLauncherIcons = false;
	private int mIconBitmapSize;
	private GoogleMonochromeIconFactory mGoogleMonochromeIconFactory;

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
							if(mGoogleMonochromeIconFactory == null)
							{
								mGoogleMonochromeIconFactory = new GoogleMonochromeIconFactory(mIconBitmapSize);
							}
							param.setResult(mGoogleMonochromeIconFactory.wrap((Drawable) param.thisObject));
						}
					}
					catch (Throwable ignored){}
				}
			});
		}
		catch (Throwable ignored){} //Android 13
	}
}