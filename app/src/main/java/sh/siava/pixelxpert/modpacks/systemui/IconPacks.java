package sh.siava.pixelxpert.modpacks.systemui;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import androidx.core.content.res.ResourcesCompat;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.XposedModPack;

public class IconPacks extends XposedModPack {

	static IDMapping drawableMapping = new IDMapping();
	private final PackageManager p;

	public IconPacks(Context context) {
		super(context);
		p = mContext.getPackageManager();
	}

	@Override
	public void updatePrefs(String... Key) {
		if(Key.length == 0 && drawableMapping.isEmpty()) //only refresh the mapping once at process startup. No more
			drawableMapping = getIDMapping("drawableMapping", "drawable");
	}

	/** @noinspection SameParameterValue*/
	private IDMapping getIDMapping(String prefKey, String type) {
		Mapping prefMapping = getMapping(prefKey);

		IDMapping idMapping = new IDMapping();
		for (String key : prefMapping.keySet()) {
			try {
				OverlayIDName overlayIDName = prefMapping.get(key);
				//noinspection DataFlowIssue
				@SuppressLint("DiscouragedApi")
				OverlayID overlayID = new OverlayID(overlayIDName.packageName, p.getResourcesForApplication(overlayIDName.packageName).getIdentifier(overlayIDName.resName, type, overlayIDName.packageName));
				@SuppressLint("DiscouragedApi")
				int mappingID = mContext.getResources().getIdentifier(key, type, mContext.getPackageName());
				idMapping.put(mappingID, overlayID);
			} catch (Throwable ignored) {}
		}
		return idMapping;
	}
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		//test part - shall go to UI later
/*		Intent i = new Intent("sh.siava.pixelxpert.iconpack");
		List<ResolveInfo> l = p.queryIntentActivities(i, 0);
		log("l " + l.size());
		String packageName = l.get(0).activityInfo.packageName;
		log("name " + packageName);
		Mapping mapping = new Mapping();
		Resources r = p.getResourcesForApplication(packageName);

		mapping = getMappingUI(r, packageName);


		Gson gson = new Gson();
		Xprefs.edit().putString("drawableMapping", gson.toJson(mapping)).commit();*/

		if(drawableMapping.isEmpty()) return; //don't hook into anything if we don't have a mapping

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
		if(drawableMapping.containsKey(id))
		{
			OverlayID overlayID = drawableMapping.get(id);
			//noinspection DataFlowIssue
			return ResourcesCompat.getDrawableForDensity(p.getResourcesForApplication(overlayID.packageName), overlayID.resID, density, theme);
		}
		return null;
	}

	private Drawable getDrawable(int id, Resources.Theme theme) throws Throwable {
		if(drawableMapping.containsKey(id))
		{
			OverlayID overlayID = drawableMapping.get(id);
			//noinspection DataFlowIssue
			return ResourcesCompat.getDrawable(p.getResourcesForApplication(overlayID.packageName), overlayID.resID, theme);
		}
		return null;
	}

	/** @noinspection unused*/ //shall go to UI for release
	private Mapping getMappingUI(Resources r, String packageName) {
		Mapping mapping = new Mapping();

		@SuppressLint("DiscouragedApi")
		String[] replacements = r.getStringArray(r.getIdentifier("mapping_replacement", "array", packageName));
		@SuppressLint("DiscouragedApi")
		String[] drawables = r.getStringArray(r.getIdentifier("mapping_drawable", "array", packageName));

		for(int i = 0; i < replacements.length; i++)
		{
			mapping.put(drawables[i], new OverlayIDName(packageName, replacements[i]));
		}
		return mapping;
	}

	@Override
	public boolean listensTo(String packageName) {
		return true;
	}

	private Mapping getMapping(String key) {
		log(Xprefs.getString(key, ""));
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

	private static class OverlayIDName
	{
		public String resName;
		public String packageName;

		private OverlayIDName(String packageName, String resName)
		{
			this.resName = resName;
			this.packageName = packageName;
		}
	}


	static class IDMapping extends HashMap<Integer, OverlayID>
	{}

	static class Mapping extends HashMap<String, OverlayIDName>
	{}

}
