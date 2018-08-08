/*
 * Copyright (c) 2013-2016 Shaleen Jain <shaleen.jain95@gmail.com>
 *
 * This file is part of UPES Academics.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shalzz.attendance.ui.attendance;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.shalzz.attendance.BuildConfig;
import com.shalzz.attendance.R;
import com.shalzz.attendance.data.model.ListFooter;
import com.shalzz.attendance.data.model.Subject;
import com.shalzz.attendance.injection.ApplicationContext;
import com.shalzz.attendance.utils.Miscellaneous;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;
import androidx.recyclerview.widget.SortedListAdapterCallback;
import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class ExpandableListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context mContext;
    private Resources mResources;
    private Bitmap mBitmap;
    private float mExpandedTranslationZ;

    //our items
    private final SortedList<Subject> mSubjects;
    private ListFooter mFooter;
    private List<View> headers = new ArrayList<>();
    private List<View> footers = new ArrayList<>();

    private static final int TYPE_HEADER = 111;
    private static final int TYPE_FOOTER = 222;
    private static final int TYPE_ITEM = 333;

    /** Constant used to indicate no row is expanded. */
    private static final long NONE_EXPANDED = -1;

    /**
     * Tracks the row which was previously expanded.  Used so that the closure of a
     * previously expanded call log entry can be animated on rebind.
     */
    private long mPreviouslyExpanded = NONE_EXPANDED;

    private long mExpandedId = NONE_EXPANDED;

    private SubjectItemExpandedListener mSubjectItemExpandedListener;

    /** Interface used to inform a parent UI element that a list item has been expanded. */
    public interface SubjectItemExpandedListener {
        /**
         * @param view The {@link View} that represents the item that was clicked
         *         on.
         */
        void onItemExpanded(View view);

        /**
         * Retrieves the call log view for the specified call Id.  If the view is not currently
         * visible, returns null.
         *
         * @param callId The call Id.
         * @return The call log view.
         */
        View getViewForCallId(long callId);
    }

    @Inject
    ExpandableListAdapter(@ApplicationContext Context context) {
        mContext = context;
        mResources = context.getResources();
        mExpandedTranslationZ = mResources.getDimension(R.dimen.atten_view_expanded_elevation);
        mBitmap = BitmapFactory.decodeResource(mResources,R.drawable.alert);

        mSubjects = new SortedList<>(Subject.class,
                new SortedListAdapterCallback<Subject>(this) {
                    @Override
                    public int compare(Subject o1, Subject o2) {
                        return o1.name().compareTo(o2.name());
                    }

                    @SuppressWarnings("SimplifiableIfStatement")
                    @Override
                    public boolean areContentsTheSame(Subject oldItem, Subject newItem) {
                        if(oldItem.id() != newItem.id()) {
                            return false;
                        }
                        if(!oldItem.name().equals(newItem.name())) {
                            return false;
                        }
                        if(Float.compare(oldItem.attended(), newItem.attended()) != 0) {
                            return false;
                        }
                        if(Float.compare(oldItem.held(), newItem.held()) != 0 ) {
                            return false;
                        }
                        if (oldItem.absent_dates() != null && newItem.absent_dates() == null) {
                            return false;
                        }
                        if (oldItem.absent_dates() == null && newItem.absent_dates() != null) {
                            return false;
                        }
                        if (oldItem.absent_dates() == null && newItem.absent_dates() == null) {
                            return true;
                        }
                        else
                            return oldItem.absent_dates().equals(newItem.absent_dates());
                    }

                    @Override
                    public boolean areItemsTheSame(Subject item1, Subject item2) {
                        return item1.id() == item2.id();
                    }
                });
    }

    void setCallback(SubjectItemExpandedListener callback) {
        mSubjectItemExpandedListener = callback;
    }

    void addAll(List<Subject> subjects) {
        mSubjects.addAll(subjects);
        notifyDataSetChanged();
    }

    public int getSubjectCount() {
        return mSubjects.size();
    }

    void clear() {
        if(BuildConfig.DEBUG)
            Timber.i("Data set cleared.");
        mSubjects.clear();
    }

    void updateFooter(ListFooter footer) {
        if(!footer.equals(mFooter)) {
            mFooter = footer;
            notifyItemChanged(getItemCount()-1);
        }
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class GenericViewHolder extends RecyclerView.ViewHolder {
        int position = -1;

        @BindView(R.id.tvSubj) TextView subject;
        @BindView(R.id.tvPercent) TextView percentage;
        @BindView(R.id.tvClasses) TextView classes;
        @BindView(R.id.pbPercent) ProgressBar percent;

        //child views
        RelativeLayout childView;
        TextView tvAbsent;
        TextView tvReach;
        ImageView ivAlert;

        GenericViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this,itemView);
        }

    }

    /**
     * The onClickListener used to expand or collapse the action buttons section for a call log
     * entry.
     */
    private final View.OnClickListener mExpandCollapseListener =
            v -> handleRowExpanded(v, true /* animate */, false /* forceExpand */);

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        //if our position is one of our items (this comes from getItemViewType(int position) below)
        if(viewType == TYPE_ITEM) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_attend_card,
                    parent, false);
            return new GenericViewHolder(v);
            //else we have a header/footer
        }else{
            //create a new framelayout, or inflate from a resource
            FrameLayout frameLayout = new FrameLayout(parent.getContext());
            //make sure it fills the space
            frameLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            return new HeaderFooterViewHolder(frameLayout);
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        //check what type of view our position is
        if(position < headers.size()){
            View v = headers.get(position);
            //add our view to a header view and display it
            prepareHeaderFooter((HeaderFooterViewHolder) holder, v);
        }else if(position >= headers.size() + mSubjects.size()){
            View v = footers.get(position-mSubjects.size()-headers.size());
            //add our view to a footer view and display it
            prepareHeaderFooter((HeaderFooterViewHolder) holder, v);
        }else {
            //it's one of our items, display as required
            prepareGeneric((GenericViewHolder) holder , position-headers.size());
        }

    }

    /**
     * Configures the action buttons in the expandable actions ViewStub.  The ViewStub is not
     * inflated during initial binding, so click handlers, tags and accessibility text must be set
     * here, if necessary.
     *
     * @param view The call log list item view.
     */
    @SuppressLint("WrongViewCast")
    private void inflateChildView(final View view) {
        final GenericViewHolder views = (GenericViewHolder) view.getTag();

        ViewStub stub = view.findViewById(R.id.subject_details_stub);
        if (stub != null) {
            views.childView = (RelativeLayout) stub.inflate();
        }
        else
            views.childView = views.itemView.findViewById(R.id.subTree);

        // child view
        View childView = views.childView;
        views.tvAbsent = childView.findViewById(R.id.tvAbsent);
        views.tvReach = childView.findViewById(R.id.tvReach);
        views.ivAlert = childView.findViewById(R.id.imageView1);

        bindChildView(views,views.position);
    }

    /**
     * Toggles the expansion state tracked for the row identified by rowId and returns
     * the new expansion state.  Assumes that only a single row will be expanded at any
     * one point and tracks the current and previous expanded item.
     *
     * @param rowId The row Id associated with the row to expand/collapse.
     * @return True where the row is now expanded, false otherwise.
     */
    private boolean toggleExpansion(long rowId) {
        if (isExpanded(rowId)) {
            mPreviouslyExpanded = NONE_EXPANDED;
            mExpandedId = NONE_EXPANDED;
            return false;
        } else {
            // Collapse the previous row
            mPreviouslyExpanded = mExpandedId;

            // Expanding a row.
            mExpandedId = rowId;
            return true;
        }
    }

    /**
     * Determines if a row with the given Id is expanded.
     * @param rowId The row Id.
     * @return True if the row is expanded.
     */
    private boolean isExpanded(long rowId) {
        return mExpandedId == rowId;
    }

    /**
     * Expands or collapses the view.
     *
     * @param view The call log entry parent view.
     * @param isExpanded The new expansion state of the view.
     */
    private void expandOrCollapseChildView(View view, boolean isExpanded) {
        final GenericViewHolder views = (GenericViewHolder) view.getTag();

        if (isExpanded) {
            // Inflate the view stub if necessary.
            inflateChildView(view);

            views.childView.setVisibility(View.VISIBLE);
            views.childView.setAlpha(1.0f);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                view.setTranslationZ(mExpandedTranslationZ);
                view.setActivated(true);
            }
        } else {

            // When recycling a view, it is possible the actionsView ViewStub was previously
            // inflated so we should hide it in this case.
            if (views.childView != null) {
                views.childView.setVisibility(View.GONE);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                view.setTranslationZ(0);
                view.setActivated(false);
            }
        }
    }

    /**
     * Manages the state changes for the UI interaction where a row is expanded.
     *
     * @param view The view that was tapped
     * @param animate Whether or not to animate the expansion/collapse
     * @param forceExpand Whether or not to force the row into an expanded state regardless
     *        of its previous state
     */
    private void handleRowExpanded(View view, boolean animate, boolean forceExpand) {
        final GenericViewHolder views = (GenericViewHolder) view.getTag();

        if (forceExpand && isExpanded(views.position)) {
            return;
        }

        // Hide or show the actions view.
        boolean expanded = toggleExpansion(views.position);

        // Trigger loading of the viewstub and visual expand or collapse.
        expandOrCollapseChildView(view, expanded);

        // Animate the expansion or collapse.
        if (mSubjectItemExpandedListener != null) {
            if (animate) {
                mSubjectItemExpandedListener.onItemExpanded(view);
            }

            // Animate the collapse of the previous item if it is still visible on screen.
            if (mPreviouslyExpanded != NONE_EXPANDED) {
                View previousItem = mSubjectItemExpandedListener.getViewForCallId(
                        mPreviouslyExpanded);

                if (previousItem != null) {
                    expandOrCollapseChildView(previousItem, false);
                    if (animate) {
                        mSubjectItemExpandedListener.onItemExpanded(previousItem);
                    }
                }
                mPreviouslyExpanded = NONE_EXPANDED;
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void bindChildView(GenericViewHolder holder, int position) {

        TextView tvAbsent = holder.tvAbsent;
        TextView tvReach = holder.tvReach;
        ImageView ivAlert = holder.ivAlert;

        int held = Float.valueOf(mSubjects.get(position).held()).intValue();
        int attend = Float.valueOf(mSubjects.get(position).attended()).intValue();
        int percent = Math.round(mSubjects.get(position).getPercentage());

        if (mSubjects.get(position).absent_dates() == null) {
            tvAbsent.setText(mResources.getText(R.string.atten_list_days_absent_null));
        } else {
            tvAbsent.setText(mResources.getString(R.string.atten_list_days_absent,
                    mSubjects.get(position).getAbsentDatesAsString()));
        }

        if (percent<67 && held!=0) {
            int x = (2*held) - (3*attend);
            if(x == 0) {
                tvReach.setVisibility(View.GONE);
                ivAlert.setVisibility(View.GONE);
                ivAlert.setImageBitmap(null);
            } else {
                tvReach.setText(mResources.getQuantityString(R.plurals.tv_classes_to_67,x,x));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    tvReach.setTextColor(mResources.getColor(R.color.attend, mContext.getTheme()));
                } else {
                    tvReach.setTextColor(mResources.getColor(R.color.attend));
                }
                tvReach.setVisibility(View.VISIBLE);
                ivAlert.setVisibility(View.VISIBLE);
                ivAlert.setImageBitmap(mBitmap);
            }
        }
        else if(percent<75 && held!=0) {
            int x = (3*held) - (4*attend);
            if(x == 0) {
                tvReach.setVisibility(View.GONE);
                ivAlert.setVisibility(View.GONE);
                ivAlert.setImageBitmap(null);
            } else {
                tvReach.setText(mResources.getQuantityString(R.plurals.tv_classes_to_75, x, x));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    tvReach.setTextColor(mResources.getColor(R.color.attend, mContext.getTheme()));
                } else {
                    tvReach.setTextColor(mResources.getColor(R.color.attend));
                }
                tvReach.setVisibility(View.VISIBLE);
                ivAlert.setVisibility(View.VISIBLE);
                ivAlert.setImageBitmap(mBitmap);
            }
        } else {
            int x = ((4*attend)/3)-held;
            if(x == 0) {
                tvReach.setVisibility(View.GONE);
            } else {
                tvReach.setText(mResources.getQuantityString(R.plurals.tv_miss_classes, x, x));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    tvReach.setTextColor(mResources.getColor(R.color.skip, mContext.getTheme()));
                } else {
                    tvReach.setTextColor(mResources.getColor(R.color.skip));
                }
                tvReach.setVisibility(View.VISIBLE);
            }
            ivAlert.setVisibility(View.GONE);
            ivAlert.setImageBitmap(null);
        }
    }

    @Override
    public long getItemId(int position) {
        return (long) position;
    }

    @Override
    public int getItemCount() {
        //make sure the adapter knows to look for all our items, headers, and footers
        return headers.size() + mSubjects.size() + footers.size();
    }

    private void prepareHeaderFooter(HeaderFooterViewHolder vh, View view){
        //empty out our FrameLayout and replace with our header/footer
        vh.base.removeAllViews();
        if(view.getParent() != null) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
        vh.base.addView(view);

        if(mFooter == null)
            return;
        Float percent = mFooter.getPercentage();

        /** --------footer-------- */
        view.setVisibility(View.VISIBLE);
        TextView tvPercent = view.findViewById(R.id.tvTotalPercent);
        TextView tvClasses = view.findViewById(R.id.tvClass);
        ProgressBar pbPercent = view.findViewById(R.id.pbTotalPercent);
        tvPercent.setText(mResources.getString(R.string.atten_list_percentage,
                mFooter.getPercentage()));
        tvClasses.setText(mResources.getString(R.string.atten_list_attended_upon_held,
                mFooter.getAttended().intValue(),
                mFooter.getHeld().intValue()));
        pbPercent.setProgress(percent.intValue());
        Drawable d = pbPercent.getProgressDrawable();
        d.setLevel(percent.intValue() * 100);
        /** ------------------------*/
    }

    private void prepareGeneric(GenericViewHolder holder, int position){
        holder.position = position;
        holder.itemView.setTag(holder);

        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        Float percent = mSubjects.get(position).getPercentage();
        holder.subject.setText(Miscellaneous.capitalizeString(mSubjects.get(position).name()));
        holder.percentage.setText(mResources.getString(R.string.atten_list_percentage,
                mSubjects.get(position).getPercentage()));
        holder.classes.setText(mResources.getString(R.string.atten_list_attended_upon_held,
                Float.valueOf(mSubjects.get(position).attended()).intValue(),
                Float.valueOf(mSubjects.get(position).held()).intValue()));
        Drawable d = holder.percent.getProgressDrawable();
        if(percent > 0f)
            d.setLevel(percent.intValue()*100);
        else
            d.setLevel(1);
        holder.percent.setProgressDrawable(d);
        holder.percent.setProgress(percent.intValue());

        // In the call log, expand/collapse an actions section for the call log entry when
        // the primary view is tapped.
        holder.itemView.setOnClickListener(this.mExpandCollapseListener);

        // Restore expansion state of the row on rebind.  Inflate the actions ViewStub if required,
        // and set its visibility state accordingly.
        expandOrCollapseChildView(holder.itemView, isExpanded(position));
    }

    @Override
    public int getItemViewType(int position) {
        //check what type our position is, based on the assumption that the order is headers > items > footers
        if(position < headers.size()){
            return TYPE_HEADER;
        }else if(position >= headers.size() + mSubjects.size()){
            return TYPE_FOOTER;
        }
        return TYPE_ITEM;
    }

    //add a header to the adapter
    public void addHeader(View header){
        if(!headers.contains(header)){
            headers.add(header);
            //animate
            notifyItemInserted(headers.size()-1);
        }
    }

    //remove a header from the adapter
    public void removeHeader(View header){
        if(headers.contains(header)){
            //animate
            notifyItemRemoved(headers.indexOf(header));
            headers.remove(header);
        }
    }

    //add a footer to the adapter
    public void addFooter(View footer){
        if(!footers.contains(footer)){
            footers.add(footer);
            //animate
            notifyItemInserted(headers.size()+mSubjects.size()+footers.size()-1);
        }
    }

    //remove a footer from the adapter
    public void removeFooter(View footer){
        if(footers.contains(footer)) {
            //animate
            notifyItemRemoved(headers.size()+mSubjects.size()+footers.indexOf(footer));
            footers.remove(footer);
        }
    }

    //our header/footer RecyclerView.ViewHolder is just a FrameLayout
    public static class HeaderFooterViewHolder extends RecyclerView.ViewHolder{
        FrameLayout base;

        HeaderFooterViewHolder(View itemView) {
            super(itemView);
            this.base = (FrameLayout) itemView;
        }
    }
}