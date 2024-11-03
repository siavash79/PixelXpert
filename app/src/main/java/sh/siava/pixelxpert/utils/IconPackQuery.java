package sh.siava.pixelxpert.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class IconPackQuery {
	private static final String TAG = "IconPackQuery";
	Context mContext;
	PackageManager mPackageManager;
	public IconPackQuery(@NonNull Context context)
	{
		mContext = context;
		mPackageManager = mContext.getPackageManager();
	}

	public IconPackMapping queryIconPackMapping()
	{
		return (IconPackMapping) queryMappingInternal(true);
	}

	public ResourceMapping queryResourceMapping()
	{
		return (ResourceMapping) queryMappingInternal(false);
	}

	private HashMap<?,?> queryMappingInternal(boolean queryIconPack)
	{
		HashMap<?,?> mapping = queryIconPack ? new IconPackMapping() : new ResourceMapping();

		List<ResolveInfo> activities = mPackageManager.queryIntentActivities(new Intent("sh.siava.pixelxpert.iconpack"), 0);

		for(ResolveInfo activity : activities)
		{
			try {
				String packageName = activity.activityInfo.packageName;
				String packName = activity.activityInfo.name.replaceAll(String.format("^%s\\.", packageName),"");
				String packLabel = String.valueOf(activity.activityInfo.loadLabel(mPackageManager));

				Resources packResources = mPackageManager.getResourcesForApplication(packageName);

				@SuppressLint("DiscouragedApi")
				String[] packData = packResources.getStringArray(packResources.getIdentifier(packName, "array", packageName));
				IconPack iconPack = new IconPack(packageName, packLabel, null);

				int resNameArrayID = getStringArrayID(packData[0], packResources, packageName);
				int replacementNameArrayID = getStringArrayID(packData[1], packResources, packageName);

				String[] resNames = packResources.getStringArray(resNameArrayID);
				String[] replacements = packResources.getStringArray(replacementNameArrayID);

				for (int i = 0; i < resNames.length; i++) {
					if(mapping instanceof IconPackMapping)
					{
						((IconPackMapping) mapping).add(iconPack, resNames[i], replacements[i]);
					}
					else
					{
						((ResourceMapping) mapping).add(resNames[i], new ReplacementIcon(iconPack, replacements[i]));
					}
				}
			} catch (Exception e)
			{
				Log.e(TAG, "queryIconPacks: ", e);
			}
		}

		return mapping;
	}

	@SuppressLint("DiscouragedApi")
	private int getStringArrayID(String resName, Resources packResources, String packageName) {
		return packResources.getIdentifier(resName, "array", packageName);
	}

	public static class IconPack
	{
		public String mName;
		public String mAuthor;
		public String mPackageName;
		public IconPack(String packageName, String name, String author)
		{
			mName = name;
			mAuthor = author;
			mPackageName = packageName;
		}
	}

	public class ReplacementIcon
	{
		public IconPack mIconPack;
		public String mReplacementRes;
		public ReplacementIcon(IconPack iconPack, String replacementRes)
		{
			mIconPack = iconPack;
			mReplacementRes = replacementRes;
		}
		public Drawable getDrawable() throws PackageManager.NameNotFoundException {
			Resources packResources = mPackageManager.getResourcesForApplication(mIconPack.mPackageName);

			@SuppressLint("DiscouragedApi")
			int resID = packResources.getIdentifier(mReplacementRes, "drawable", mIconPack.mPackageName);
			return ResourcesCompat.getDrawable(packResources, resID, mContext.getTheme());
		}
	}

	public class ResourceMapping extends HashMap<String, ArrayList<ReplacementIcon>>
	{
		public void add(String originalRes, ReplacementIcon replacementIcon)
		{
			if(!containsKey(originalRes))
			{
				put(originalRes, new ArrayList<>());
			}
			//noinspection DataFlowIssue
			get(originalRes).add(replacementIcon);
		}
	}

	public class IconPackMapping extends HashMap<IconPack, HashMap<String, ArrayList<ReplacementIcon>>>{
		/** @noinspection DataFlowIssue*/
		public void add(IconPack iconPack, String resName, String replacementName)
		{
			if(!containsKey(iconPack))
			{
				put(iconPack, new HashMap<>());
			}
			HashMap<String, ArrayList<ReplacementIcon>> thisPack = get(iconPack);

			if(!thisPack.containsKey(resName))
			{
				thisPack.put(resName, new ArrayList<>());
			}
			thisPack.get(resName).add(new ReplacementIcon(iconPack, replacementName));
		}
	}
}