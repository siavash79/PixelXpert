package sh.siava.pixelxpert.ui.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import sh.siava.pixelxpert.databinding.FragmentIconPackBinding;
import sh.siava.pixelxpert.ui.adapter.IconPackAdapter;
import sh.siava.pixelxpert.utils.IconPackQuery;

public class IconPacks extends BaseFragment implements IconPackQuery.IconPackQueryListener {

	private IconPackQuery mIconPackUtil;
	private FragmentIconPackBinding binding;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		binding = FragmentIconPackBinding.inflate(inflater, container, false);
		return binding.getRoot();
	}

	@Override
	public String getTitle() {
		return "Icok Packs";
	}

	@Override
	public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));

		mIconPackUtil = new IconPackQuery(requireContext());
		mIconPackUtil.addListener(this);
		mIconPackUtil.queryIconPacks();
	}

	@Override
	public void onIconPacksLoaded(HashMap<String, ArrayList<IconPackQuery.ReplacementIcon>> mapping, HashMap<IconPackQuery.IconPack, HashMap<String, ArrayList<IconPackQuery.ReplacementIcon>>> packMapping, List<IconPackQuery.IconPack> packs) {
		Log.d("IconPacks", "onPacksLoaded");
		Log.d("IconPacks", "onPacksLoaded: " + packs.size());
		binding.recyclerView.setAdapter(new IconPackAdapter(packs, packMapping));
		Log.d("IconPacks", "getItemCount: " + binding.recyclerView.getAdapter().getItemCount());
	}

	@Override
	public void onDestroy() {
		mIconPackUtil.removeListener(this);
		super.onDestroy();
	}
}
