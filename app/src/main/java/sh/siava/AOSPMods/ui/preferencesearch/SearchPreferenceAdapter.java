package sh.siava.AOSPMods.ui.preferencesearch;

/*
 * https://github.com/ByteHamster/SearchPreference
 *
 * MIT License
 *
 * Copyright (c) 2018 ByteHamster
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import sh.siava.AOSPMods.R;

class SearchPreferenceAdapter extends RecyclerView.Adapter<SearchPreferenceAdapter.ViewHolder> {
	private List<ListItem> dataset;
	private SearchConfiguration searchConfiguration;
	private SearchClickListener onItemClickListener;


	SearchPreferenceAdapter() {
		dataset = new ArrayList<>();
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		if (viewType == PreferenceItem.TYPE) {
			return new PreferenceViewHolder(
					LayoutInflater.from(parent.getContext()).inflate(
							R.layout.searchpreference_list_item_result, parent, false));
		} else {
			return new HistoryViewHolder(
					LayoutInflater.from(parent.getContext()).inflate(
							R.layout.searchpreference_list_item_history, parent, false));
		}
	}

	@Override
	public void onBindViewHolder(@NonNull final ViewHolder h, final int position) {
		final ListItem listItem = dataset.get(position);
		if (getItemViewType(position) == HistoryItem.TYPE) {
			HistoryViewHolder holder = (HistoryViewHolder) h;
			HistoryItem item = (HistoryItem) listItem;
			holder.term.setText(item.getTerm());
		} else if (getItemViewType(position) == PreferenceItem.TYPE) {
			PreferenceViewHolder holder = (PreferenceViewHolder) h;
			PreferenceItem item = (PreferenceItem) listItem;
			holder.title.setText(item.title);

			if (TextUtils.isEmpty(item.summary) || Objects.equals(item.summary, "%s")) {
				holder.summary.setVisibility(View.GONE);
			} else {
				holder.summary.setVisibility(View.VISIBLE);
				holder.summary.setText(item.summary);
				holder.summary.setAlpha(0.8f);
			}

			if (searchConfiguration.isBreadcrumbsEnabled()) {
				holder.breadcrumbs.setText(item.breadcrumbs);
				holder.breadcrumbs.setAlpha(0.6f);
				holder.summary.setAlpha(0.8f);
			} else {
				holder.breadcrumbs.setVisibility(View.GONE);
				holder.summary.setAlpha(0.6f);
			}

		}

		h.root.setOnClickListener(v -> {
			if (onItemClickListener != null) {
				onItemClickListener.onItemClicked(listItem, h.getBindingAdapterPosition());
			}
		});
	}

	@SuppressLint("NotifyDataSetChanged")
	void setContent(List<ListItem> items) {
		dataset = new ArrayList<>(items);
		this.notifyDataSetChanged();
	}

	@Override
	public int getItemCount() {
		return dataset.size();
	}

	@Override
	public int getItemViewType(int position) {
		return dataset.get(position).getType();
	}

	void setSearchConfiguration(SearchConfiguration searchConfiguration) {
		this.searchConfiguration = searchConfiguration;
	}

	void setOnItemClickListener(SearchClickListener onItemClickListener) {
		this.onItemClickListener = onItemClickListener;
	}

	interface SearchClickListener {
		void onItemClicked(ListItem item, int position);
	}

	static class ViewHolder extends RecyclerView.ViewHolder {
		View root;

		ViewHolder(View v) {
			super(v);
			root = v;
		}
	}

	static class HistoryViewHolder extends ViewHolder {
		TextView term;

		HistoryViewHolder(View v) {
			super(v);
			term = v.findViewById(R.id.term);
		}
	}

	static class PreferenceViewHolder extends ViewHolder {
		TextView title;
		TextView summary;
		TextView breadcrumbs;

		PreferenceViewHolder(View v) {
			super(v);
			title = v.findViewById(R.id.title);
			summary = v.findViewById(R.id.summary);
			breadcrumbs = v.findViewById(R.id.breadcrumbs);
		}
	}
}
