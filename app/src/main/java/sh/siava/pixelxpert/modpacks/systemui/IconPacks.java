package sh.siava.pixelxpert.modpacks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import androidx.core.content.res.ResourcesCompat;

import com.google.gson.Gson;

import java.util.HashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.XposedModPack;

public class IconPacks extends XposedModPack {

	static Mapping drawableMapping = new Mapping();
	private final PackageManager p;

	public IconPacks(Context context) {
		super(context);
		p = mContext.getPackageManager();
	}

	@Override
	public void updatePrefs(String... Key) {
		drawableMapping = getMapping("drawableMapping");
	}
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {

/*		Intent i = new Intent("sh.siava.pixelxpert.iconpack");
		List<ResolveInfo> l = p.queryIntentActivities(i, 0);
		log("l " + l.size());
		String packageName = l.get(0).activityInfo.packageName;
		log("name " + packageName);
		HashMap<Integer, OverlayID> mapping = new HashMap<>();
		Resources r = p.getResourcesForApplication(packageName);

		mapping = getMapping(r, packageName);

		HashMap<Integer, OverlayID> finalMapping = mapping;
		Gson gson = new Gson();
		Xprefs.edit().putString("drawableMapping", gson.toJson(finalMapping)).commit();*/

		findAndHookMethod(Resources.class, "getDrawable", int.class, Resources.Theme.class, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				Drawable drawable = getDrawable((int)param.args[0], (Resources.Theme) param.args[1]);
				if(drawable != null)
				{
					param.setResult(drawable);
				}
			}
		});

		findAndHookMethod(Resources.class, "getDrawable", int.class, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				Drawable drawable = getDrawable((int)param.args[0], mContext.getTheme());
				if(drawable != null)
				{
					param.setResult(drawable);
				}
			}
		});

		findAndHookMethod(Resources.class, "getDrawableForDensity", int.class, int.class, Resources.Theme.class, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				Drawable drawable = getDrawableForDensity((int)param.args[0], (int)param.args[1], (Resources.Theme) param.args[2]);
				if(drawable != null)
				{
					param.setResult(drawable);
				}

			}
		});

		findAndHookMethod(Resources.class, "getDrawableForDensity", int.class, int.class, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				Drawable drawable = getDrawableForDensity((int)param.args[0], (int)param.args[1], mContext.getTheme());
				if(drawable != null)
				{
					param.setResult(drawable);
				}

			}
		});


/*		hookAllMethods(Resources.class, "getDrawable", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				int id = (int) param.args[0];
				if(drawableMapping.containsKey(String.valueOf(id)))
				{
					OverlayID overlayID = drawableMapping.get(String.valueOf(id));
					Drawable drawable = ResourcesCompat.getDrawable(p.getResourcesForApplication(overlayID.packageName), overlayID.resID, mContext.getTheme());
					if(drawable != null)
						param.setResult(drawable);
				}
			}
		});*/
	}

	private Drawable getDrawableForDensity(int id, int density, Resources.Theme theme) throws Throwable {
		if(drawableMapping.containsKey(String.valueOf(id)))
		{
			OverlayID overlayID = drawableMapping.get(String.valueOf(id));
			return ResourcesCompat.getDrawableForDensity(p.getResourcesForApplication(overlayID.packageName), overlayID.resID, density, theme);
		}
		return null;
	}

	private Drawable getDrawable(int id, Resources.Theme theme) throws Throwable {
		if(drawableMapping.containsKey(String.valueOf(id)))
		{
			OverlayID overlayID = drawableMapping.get(String.valueOf(id));
			return ResourcesCompat.getDrawable(p.getResourcesForApplication(overlayID.packageName), overlayID.resID, theme);
		}
		return null;
	}

/*	private HashMap<Integer, OverlayID> getMapping(Resources r, String packageName) {
		HashMap<Integer, OverlayID> mapping = new HashMap<>();
		String[] replacements = r.getStringArray(r.getIdentifier("mapping_replacement", "array", packageName));
		String[] drawables = r.getStringArray(r.getIdentifier("mapping_drawable", "array", packageName));

		for(int i = 0; i < replacements.length; i++)
		{
			int repid = r.getIdentifier(replacements[i], "drawable", packageName);
			int drawid = mContext.getResources().getIdentifier(drawables[i], "drawable", mContext.getPackageName());
			mapping.put(drawid, new OverlayID(packageName, repid));
		}
		return mapping;
	}*/

	@Override
	public boolean listensTo(String packageName) {
		return true;
	}

	private Mapping getMapping(String key) {
		return new Gson()
				.fromJson(
						Xprefs.getString(key, ""),
						Mapping.class);
	}

	private static class OverlayID
	{
		public int resID;
		public String packageName;

		private OverlayID(String packageName, int resID)
		{
			this.resID = resID;
			this.packageName = packageName;
		}
	}

	static class Mapping extends HashMap<String, OverlayID>
	{}
}
