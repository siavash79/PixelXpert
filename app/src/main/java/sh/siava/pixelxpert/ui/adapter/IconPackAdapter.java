package sh.siava.pixelxpert.ui.adapter;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import sh.siava.pixelxpert.PixelXpert;
import sh.siava.pixelxpert.databinding.ViewItemIconPackBinding;
import sh.siava.pixelxpert.utils.IconPackQuery;
import sh.siava.pixelxpert.utils.IconPackUtil;

public class IconPackAdapter extends RecyclerView.Adapter<IconPackAdapter.ViewHolder> {

	private final List<IconPackQuery.IconPack> mPacks;
	private final HashMap<IconPackQuery.IconPack, HashMap<String, ArrayList<IconPackQuery.ReplacementIcon>>> mPacksMapping;

	public IconPackAdapter(List<IconPackQuery.IconPack> packs, HashMap<IconPackQuery.IconPack, HashMap<String, ArrayList<IconPackQuery.ReplacementIcon>>> packMapping) {
		mPacks = packs;
		mPacksMapping = packMapping;
		Log.d("IconPackAdapter", "IconPackAdapter: " + packs.size());
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		Log.d("IconPackAdapter", "onCreateViewHolder called");
		ViewItemIconPackBinding binding = ViewItemIconPackBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
		return new ViewHolder(binding);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		IconPackQuery.IconPack pack = mPacks.get(position);
		Log.d("IconPackAdapter", "onBindViewHolder: " + pack.mPackageName);
		holder.bind(pack, mPacksMapping.get(pack));
	}

	@Override
	public int getItemCount() {
		return mPacks.size();
	}

	public static class ViewHolder extends RecyclerView.ViewHolder {

		private final ViewItemIconPackBinding binding;

		public ViewHolder(ViewItemIconPackBinding itemView) {
			super(itemView.getRoot());
			binding = itemView;
		}

		public void bind(IconPackQuery.IconPack pack, HashMap<String, ArrayList<IconPackQuery.ReplacementIcon>> mapping) {
			Log.d("IconPackAdapter", "bind: " + pack.mPackageName);
			binding.packAuthor.setText(pack.mAuthor);
			binding.packName.setText(pack.mName);
			binding.packNameRef.setText(pack.mPackageName);
			Log.d("IconPackAdapter", "binding " + pack.toString());
			if (pack.mReplacementRes.length > 0) {
				binding.icon1.setImageDrawable(getDrawableFromPack(pack.mReplacementRes[0], pack.mPackageName));
				binding.icon2.setImageDrawable(getDrawableFromPack(pack.mReplacementRes[1], pack.mPackageName));
				binding.icon3.setImageDrawable(getDrawableFromPack(pack.mReplacementRes[2], pack.mPackageName));
				binding.icon4.setImageDrawable(getDrawableFromPack(pack.mReplacementRes[3], pack.mPackageName));
			}
		}

		private Drawable getDrawableFromPack(String resId, String pkgName) {
			try {
				Resources r = PixelXpert.getmPackageManager().getResourcesForApplication(pkgName);
				return ResourcesCompat.getDrawable(
					r,
					r.getIdentifier(resId, "drawable", pkgName),
					binding.icon1.getContext().getTheme()
				);
			} catch (PackageManager.NameNotFoundException e) {
				Log.e("IconPackAdapter", "getDrawableFromPack: ", e);
			}
			return null;
		}

	}

}
