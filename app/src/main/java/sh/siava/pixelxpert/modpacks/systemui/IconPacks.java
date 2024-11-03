package sh.siava.pixelxpert.modpacks.systemui;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import androidx.core.content.res.ResourcesCompat;

import com.google.gson.Gson;


import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.pixelxpert.utils.ThemePackMapping.IDMapping;
import sh.siava.pixelxpert.utils.ThemePackMapping.Mapping;
import sh.siava.pixelxpert.utils.ThemePackMapping.OverlayID;
import sh.siava.pixelxpert.utils.ThemePackMapping.OverlayIDName;

public class IconPacks extends XposedModPack {

	static IDMapping drawableMapping = new IDMapping();
	private final PackageManager p;

	public IconPacks(Context context) {
		super(context);
		p = mContext.getPackageManager();
	}

	@Override
	public void updatePrefs(String... Key) {
		if(Key.length == 0 && drawableMapping.isEmpty()) { //only refresh the mapping once at process startup. No more
			log("updating");
			drawableMapping = getIDMapping("drawableMapping", "drawable");
			Gson g = new Gson();
			log(g.toJson(drawableMapping));
		}
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
				String[] keyParts = key.split(":");
				String resName = keyParts[keyParts.length - 1];
				String sourcePackage = keyParts.length > 1 ? keyParts[0] : mContext.getPackageName();
				@SuppressLint("DiscouragedApi")
				int mappingID = mContext.getResources().getIdentifier(resName, type, sourcePackage);
				if(mappingID != 0) {
					idMapping.put(mappingID, overlayID);
				}
			} catch (Throwable ignored) {}
		}
		return idMapping;
	}
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
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
	}

	private Drawable getDrawableForDensity(int id, int density, Resources.Theme theme) throws Throwable {
		if(drawableMapping.containsKey(id))
		{
			log("available - density");
			OverlayID overlayID = drawableMapping.get(id);
			//noinspection DataFlowIssue
			return ResourcesCompat.getDrawableForDensity(p.getResourcesForApplication(overlayID.packageName), overlayID.resID, density, theme);
		}
		return null;
	}

	private Drawable getDrawable(int id, Resources.Theme theme) throws Throwable {
		if(drawableMapping.containsKey(id))
		{
			log("available");
			OverlayID overlayID = drawableMapping.get(id);
			return ResourcesCompat.getDrawable(p.getResourcesForApplication(overlayID.packageName), overlayID.resID, theme);
		}
		return null;
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
}
