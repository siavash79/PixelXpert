package sh.siava.pixelxpert.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IconPackQuery {

	private static final String TAG = "IconPackQuery";
	private Context mContext;
	private PackageManager mPackageManager;
	private List<IconPack> mIconPacks = new ArrayList<>();
	private HashMap<String, ArrayList<ReplacementIcon>> mResourceMapping;
	private HashMap<IconPack, HashMap<String, ArrayList<ReplacementIcon>>> mIconPackMapping;
	private final Set<IconPackQueryListener> listeners = ConcurrentHashMap.newKeySet();
	private final ExecutorService executorService = Executors.newCachedThreadPool();

	public interface IconPackQueryListener {
		default void onIconPacksLoaded(HashMap<String, ArrayList<ReplacementIcon>>  mapping, HashMap<IconPack, HashMap<String, ArrayList<ReplacementIcon>>> packMapping, List<IconPack> packs) {}
	}

	public void addListener(IconPackQueryListener listener) {
		listeners.add(listener);
	}

	public void removeListener(IconPackQueryListener listener) {
		listeners.remove(listener);
	}

	public IconPackQuery(@NonNull Context context) {
		mContext = context;
		mPackageManager = mContext.getPackageManager();
	}

	public void queryIconPacks() {
		executorService.submit(this::queryMappingInternal);
	}

	private void queryMappingInternal() {
		mResourceMapping = new ResourceMapping();
		mIconPackMapping = new IconPackMapping();
		mIconPacks.clear();

		List<ResolveInfo> activities = mPackageManager.queryIntentActivities(new Intent("sh.siava.pixelxpert.iconpack"), 0);

		for (ResolveInfo activity : activities) {
			try {
				String packageName = activity.activityInfo.packageName;
				String packName = activity.activityInfo.name.replaceAll(String.format("^%s\\.", packageName), "");
				String packLabel = String.valueOf(activity.activityInfo.loadLabel(mPackageManager));
				PackageInfo p = mPackageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA);
				String author = p.applicationInfo.metaData.getString("packauthor");

				Resources packResources = mPackageManager.getResourcesForApplication(packageName);

				@SuppressLint("DiscouragedApi")
				String[] packData = packResources.getStringArray(packResources.getIdentifier(packName, "array", packageName));

				int resNameArrayID = getStringArrayID(packData[1], packResources, packageName);
				int replacementNameArrayID = getStringArrayID(packData[2], packResources, packageName);

				String[] resNames = packResources.getStringArray(resNameArrayID);
				String[] replacements = packResources.getStringArray(replacementNameArrayID);

				IconPack iconPack = new IconPack(packageName, packLabel, author, resNames, replacements);
				mIconPacks.add(iconPack);

				for (int i = 0; i < resNames.length; i++) {
					if (mIconPackMapping instanceof IconPackMapping mapping) {
						(mapping).add(iconPack, resNames[i], replacements[i]);
					} else if (mResourceMapping instanceof ResourceMapping mapping) {
						(mapping).add(resNames[i], new ReplacementIcon(iconPack, replacements[i]));
					}
				}
			} catch (Exception e) {
				Log.e(TAG, "queryIconPacks: ", e);
			}
		}

		listeners.forEach(listener -> listener.onIconPacksLoaded(mResourceMapping, mIconPackMapping , mIconPacks));
	}

	@SuppressLint("DiscouragedApi")
	private int getStringArrayID(String resName, Resources packResources, String packageName) {
		return packResources.getIdentifier(resName, "array", packageName);
	}

	public static class IconPack {
		public String mName;
		public String mAuthor;
		public String mPackageName;
		public String[] mOriginalRes;
		public String[] mReplacementRes;

		public IconPack(String packageName, String name, String author, String[] originalRes, String[] replacementRes) {
			mName = name;
			mAuthor = author;
			mPackageName = packageName;
			mOriginalRes = originalRes;
			mReplacementRes = replacementRes;
		}

		@NonNull
		@Override
		public String toString() {
			return "IconPack{" +
				"mName='" + mName + '\'' +
				", mAuthor='" + mAuthor + '\'' +
				", mPackageName='" + mPackageName + '\'' +
				", mOriginalRes=" + Arrays.toString(mOriginalRes) +
				", mReplacementRes=" + Arrays.toString(mReplacementRes) +
				'}';
		}

	}

	public class ReplacementIcon {
		public IconPack mIconPack;
		public String mReplacementRes;

		public ReplacementIcon(IconPack iconPack, String replacementRes) {
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

	public class ResourceMapping extends HashMap<String, ArrayList<ReplacementIcon>> {
		/**
		 * @noinspection DataFlowIssue
		 */
		public void add(String originalRes, ReplacementIcon replacementIcon) {
			if (!containsKey(originalRes)) {
				put(originalRes, new ArrayList<>());
			}
			get(originalRes).add(replacementIcon);
		}
	}

	public class IconPackMapping extends HashMap<IconPack, HashMap<String, ArrayList<ReplacementIcon>>> {
		/**
		 * @noinspection DataFlowIssue
		 */
		public void add(IconPack iconPack, String resName, String replacementName) {
			if (!containsKey(iconPack)) {
				put(iconPack, new HashMap<>());
			}
			HashMap<String, ArrayList<ReplacementIcon>> thisPack = get(iconPack);

			if (!thisPack.containsKey(resName)) {
				thisPack.put(resName, new ArrayList<>());
			}
			thisPack.get(resName).add(new ReplacementIcon(iconPack, replacementName));
		}
	}
}