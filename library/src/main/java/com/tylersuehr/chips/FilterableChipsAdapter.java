package com.tylersuehr.chips;

import android.graphics.PorterDuff;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.tylersuehr.chips.data.Chip;
import com.tylersuehr.chips.data.ChipDataSource;
import com.tylersuehr.chips.data.ChipChangedObserver;

/**
 * Copyright © 2017 Tyler Suehr
 *
 * Used by {@link FilterableRecyclerView} to adapt the filterable chips into
 * views and display them in a linear list-like fashion.
 *
 * This adapter should afford the ability to mFilter the appropriate data source
 * and update the UI accordingly. It should also allow the user to press on a
 * filterable chip item to select it.
 *
 * We should also observe changes to {@link ChipDataSource} to update the UI
 * accordingly.
 *
 * @author Tyler Suehr
 * @version 1.0
 */
class FilterableChipsAdapter
        extends RecyclerView.Adapter<FilterableChipsAdapter.Holder>
        implements ChipChangedObserver, Filterable {
    private final OnFilteredChipClickListener mListener;
    private final ChipDataSource mDataSource;
    private final ChipOptions mOptions;
    private ChipFilter mFilter;


    FilterableChipsAdapter(ChipDataSource chipDataSource,
                           ChipOptions chipOptions,
                           OnFilteredChipClickListener listener) {
        mDataSource = chipDataSource;
        mOptions = chipOptions;
        mListener = listener;

        // Register an observer on chip data source
        mDataSource.addChipChangedObserver(this);
    }

    @Override
    public int getItemCount() {
        return mDataSource.getFilteredChips().size();
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.adapter_filtereable_item, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        final Chip chip = mDataSource.getFilteredChip(position);

        // Set the chip avatar, if possible
        if (chip.getAvatarUri() != null) {
            holder.image.setImageURI(chip.getAvatarUri());
        } else if (chip.getAvatarDrawable() != null) {
            holder.image.setImageDrawable(chip.getAvatarDrawable());
        } else {
            holder.image.setImageBitmap(LetterTileProvider.getInstance(
                    holder.image.getContext()).getLetterTile(chip.getTitle()));
        }

        // Set the chip title
        holder.title.setText(chip.getTitle());
        holder.title.setTypeface(mOptions.typeface);

        // Set the chip subtitle, if possible
        if (chip.getSubtitle() != null) {
            holder.subtitle.setVisibility(View.VISIBLE);
            holder.subtitle.setText(chip.getSubtitle());
            holder.subtitle.setTypeface(mOptions.typeface);
        } else {
            holder.subtitle.setVisibility(View.GONE);
        }

        // Set chip colors from options, if possible
        if (mOptions.filterableListBackgroundColor != null) {
            holder.itemView.getBackground().setColorFilter(mOptions
                    .filterableListBackgroundColor.getDefaultColor(), PorterDuff.Mode.SRC_ATOP);
        }
        if (mOptions.filterableListTextColor != null) {
            holder.title.setTextColor(mOptions.filterableListTextColor);
            holder.subtitle.setTextColor(Utils.alpha(mOptions
                    .filterableListTextColor.getDefaultColor(), 150));
        }
    }

    @Override
    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new ChipFilter();
        }
        return mFilter;
    }

    @Override
    public void onChipDataSourceChanged() {
        notifyDataSetChanged();
    }


    /**
     * Nested inner-subclass of {@link RecyclerView.ViewHolder} to hold
     * references to the views in the filterable list item.
     */
    class Holder extends RecyclerView.ViewHolder implements View.OnClickListener {
        CircleImageView image;
        TextView title, subtitle;

        Holder(View v) {
            super(v);
            v.setOnClickListener(this);
            this.image = v.findViewById(R.id.image);
            this.title = v.findViewById(R.id.title);
            this.subtitle = v.findViewById(R.id.subtitle);
        }

        @Override
        public void onClick(View v) {
            // TODO: POSSIBLE OPTIMIZATION
            // Have takeChip(int) return a Chip object; which can be null checked for callback

            // Take the chip from the filtered chip list
            final Chip chip = mDataSource.getFilteredChip(getAdapterPosition());
            mDataSource.takeChip(chip);

            // Trigger callback with the clicked chip
            mListener.onFilteredChipClick(chip);
        }
    }


    /**
     * Callbacks for filtered chip click events.
     */
    interface OnFilteredChipClickListener {
        void onFilteredChipClick(Chip chip);
    }


    /**
     * Concrete implementation of {@link Filter} to help us mFilter our list of filterable chips.
     *
     * This works by cloning the list of filterable chips, so that the original filterable chips
     * list is retained, and then inclusively filtering the data source filterable chips list.
     *
     * Once the data source filterable chips list is filtered, the adapter will notify data
     * set changes have happened.
     *
     * If the user removes the mFilter (removing all the typed characters), the original list
     * of filterable chips will be added back into the data source filterable chips.
     */
    private final class ChipFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();

            mDataSource.getFilteredChips().clear();
            if (TextUtils.isEmpty(constraint)) {
                mDataSource.getFilteredChips().addAll(mDataSource.getOriginalChips());
            } else {
                final String pattern = constraint.toString().toLowerCase().trim();
                for (Chip chip : mDataSource.getOriginalChips()) {
                    if (chip.getTitle().toLowerCase().contains(pattern)
                            || (chip.getSubtitle() != null && chip.getSubtitle().toLowerCase().replaceAll("\\s", "").contains(pattern))) {
                        mDataSource.getFilteredChips().add(chip);
                    }
                }
            }

            results.values = mDataSource.getFilteredChips();
            results.count = mDataSource.getFilteredChips().size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            notifyDataSetChanged();
        }
    }
}