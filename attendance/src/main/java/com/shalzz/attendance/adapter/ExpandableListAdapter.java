/*
 * Copyright (c) 2013-2015 Shaleen Jain <shaleen.jain95@gmail.com>
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

package com.shalzz.attendance.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.shalzz.attendance.DatabaseHandler;
import com.shalzz.attendance.R;
import com.shalzz.attendance.model.ListFooter;
import com.shalzz.attendance.model.Subject;
import com.shalzz.attendance.wrapper.MyVolley;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class ExpandableListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context mContext;
    private Resources mResources;
    private final List<Long> mExpandedIds = new ArrayList<>();
    private float mExpandedTranslationZ;
    private int mLimit = -1;

    //our items
    private List<Subject> mSubjects;
    //headers
    List<View> headers = new ArrayList<>();
    //footers
    List<View> footers = new ArrayList<>();

    public static final int TYPE_HEADER = 111;
    public static final int TYPE_FOOTER = 222;
    public static final int TYPE_ITEM = 333;

    /** Constant used to indicate no row is expanded. */
    private static final long NONE_EXPANDED = -1;

    /**
     * Tracks the call log row which was previously expanded.  Used so that the closure of a
     * previously expanded call log entry can be animated on rebind.
     */
    private long mPreviouslyExpanded = NONE_EXPANDED;


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

    public ExpandableListAdapter(Context context,List<Subject> subjects,
                                 SubjectItemExpandedListener subjectItemExpandedListener) {
        if (subjects == null) {
            throw new IllegalArgumentException(
                    "Data set must not be null");
        }
        mContext = context;
        mResources = MyVolley.getMyResources();
        mSubjects = subjects;
        mSubjectItemExpandedListener = subjectItemExpandedListener;
        mExpandedTranslationZ = mResources.getDimension(R.dimen.atten_view_expanded_elevation);
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class GenericViewHolder extends RecyclerView.ViewHolder {
        public int position = -1;

        @InjectView(R.id.tvSubj) TextView subject;
        @InjectView(R.id.tvPercent) TextView percentage;
        @InjectView(R.id.tvClasses) TextView classes;
        @InjectView(R.id.pbPercent) ProgressBar percent;

        //child views
        public RelativeLayout childView;
        public TextView tvAbsent;
        public TextView tvReach;
        public ImageView ivAlert;

        public GenericViewHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this,itemView);
        }

    }

    /**
     * The onClickListener used to expand or collapse the action buttons section for a call log
     * entry.
     */
    private final View.OnClickListener mExpandCollapseListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            handleRowExpanded(v, true /* animate */, false /* forceExpand */);
        }
    };

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        //if our position is one of our items (this comes from getItemViewType(int position) below)
        if(viewType == TYPE_ITEM) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_attend_card, parent, false);
            return new GenericViewHolder(v);
            //else we have a header/footer
        }else{
            //create a new framelayout, or inflate from a resource
            FrameLayout frameLayout = new FrameLayout(parent.getContext());
            //make sure it fills the space
            frameLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
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
    private void inflateChildView(final View view) {
        final GenericViewHolder views = (GenericViewHolder) view.getTag();

        ViewStub stub = (ViewStub) view.findViewById(R.id.subject_details_stub);
        if (stub != null) {
            views.childView = (RelativeLayout) stub.inflate();
        }
        else
            views.childView = (RelativeLayout) views.itemView.findViewById(R.id.subTree);

        // child view
        View childView = views.childView;
        views.tvAbsent = (TextView) childView.findViewById(R.id.tvAbsent);
        views.tvReach = (TextView) childView.findViewById(R.id.tvReach);
        views.ivAlert = (ImageView) childView.findViewById(R.id.imageView1);

        bindChildView(views,views.position);
    }

    /**
     * Toggles the expansion state tracked for the call log row identified by rowId and returns
     * the new expansion state.  Assumes that only a single call log row will be expanded at any
     * one point and tracks the current and previous expanded item.
     *
     * @param rowId The row Id associated with the call log row to expand/collapse.
     * @return True where the row is now expanded, false otherwise.
     */
    private boolean toggleExpansion(long rowId) {
        if (isExpanded(rowId)) {
            // Collapsing currently expanded row.
            mExpandedIds.remove(rowId);

            return false;
        } else {
            // Expanding a row.
            mExpandedIds.add(rowId);

            // Collapse a view if limit reached
            boolean shouldCollapseOther =  mLimit > 0 && mExpandedIds.size() > mLimit;
            if(shouldCollapseOther) {
                mPreviouslyExpanded = mExpandedIds.get(0);
                mExpandedIds.remove(mPreviouslyExpanded);
            }
            else
                mPreviouslyExpanded = NONE_EXPANDED;
            return true;
        }
    }

    /**
     * Determines if a call log row with the given Id is expanded.
     * @param rowId The row Id of the call.
     * @return True if the row is expanded.
     */
    private boolean isExpanded(long rowId) {
        return mExpandedIds.contains(rowId);
    }

    /**
     * Expands or collapses the view containing the CALLBACK, VOICEMAIL and DETAILS action buttons.
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
                Log.d("adapter", "elev:" + view.getElevation() + " trans:" + view.getTranslationZ());
            }
        } else {

            // When recycling a view, it is possible the actionsView ViewStub was previously
            // inflated so we should hide it in this case.
            if (views.childView != null) {
                views.childView.setVisibility(View.GONE);
            }
            // TODO: fix elevation
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                view.setTranslationZ(0);
                Log.d("adapter", "elev:" + view.getElevation() + " trans:" + view.getTranslationZ());
            }
        }
    }

    /**
     * Manages the state changes for the UI interaction where a call log row is expanded.
     *
     * @param view The view that was tapped
     * @param animate Whether or not to animate the expansion/collapse
     * @param forceExpand Whether or not to force the call log row into an expanded state regardless
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

    public void bindChildView(GenericViewHolder holder, int position) {

        TextView tvAbsent = holder.tvAbsent;
        TextView tvReach = holder.tvReach;
        ImageView ivAlert = holder.ivAlert; // TODO: use a bitmap reference

        int held = mSubjects.get(position).getClassesHeld().intValue();
        int attend = mSubjects.get(position).getClassesAttended().intValue();
        float percent = mSubjects.get(position).getPercentage();

        tvAbsent.setText("Days Absent: " + mSubjects.get(position).getAbsentDates());

        if (percent<67 && held!=0) {
            int x = (2*held) - (3*attend);
            if(x == 0) {
                tvReach.setVisibility(View.GONE);
                ivAlert.setVisibility(View.GONE);
            } else {
                tvReach.setText(mResources.getQuantityString(R.plurals.tv_classes_to_67,x,x));
                tvReach.setTextColor(mResources.getColor(R.color.holo_orange_light));
                tvReach.setVisibility(View.VISIBLE);
                ivAlert.setVisibility(View.VISIBLE);
            }
        }
        else if(percent<75 && held!=0) {
            int x = (3*held) - (4*attend);
            if(x == 0) {
                tvReach.setVisibility(View.GONE);
                ivAlert.setVisibility(View.GONE);
            } else {
                tvReach.setText(mResources.getQuantityString(R.plurals.tv_classes_to_75, x, x));
                tvReach.setTextColor(mResources.getColor(R.color.holo_orange_light));
                tvReach.setVisibility(View.VISIBLE);
                ivAlert.setVisibility(View.VISIBLE);
            }
        } else {
            int x = ((4*attend)/3)-held;
            if(x == 0) {
                tvReach.setVisibility(View.GONE);
            } else {
                tvReach.setText(mResources.getQuantityString(R.plurals.tv_miss_classes, x, x));
                tvReach.setTextColor(mResources.getColor(R.color.holo_green_light));
                tvReach.setVisibility(View.VISIBLE);
            }
            ivAlert.setVisibility(View.GONE);
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
        vh.base.addView(view);

        DatabaseHandler db = new DatabaseHandler(mContext);
        ListFooter listfooter = db.getListFooter();
        Float percent = listfooter.getPercentage();

        /** --------footer-------- */
        TextView tvPercent = (TextView) view.findViewById(R.id.tvTotalPercent);
        TextView tvClasses = (TextView) view.findViewById(R.id.tvClass);
        ProgressBar pbPercent = (ProgressBar) view.findViewById(R.id.pbTotalPercent);
        tvPercent.setText(listfooter.getPercentage()+"%");
        tvClasses.setText(listfooter.getAttended().intValue() + "/" + listfooter.getHeld().intValue());
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
        holder.subject.setText(mSubjects.get(position).getName());
        holder.percentage.setText(mSubjects.get(position).getPercentage().toString()+"%");
        holder.classes.setText(mSubjects.get(position).getClassesAttended().intValue() + "/"
                + mSubjects.get(position).getClassesHeld().intValue());
        Drawable d = holder.percent.getProgressDrawable();
        d.setLevel(percent.intValue()*100);
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

        public HeaderFooterViewHolder(View itemView) {
            super(itemView);
            this.base = (FrameLayout) itemView;
        }
    }

    /**
     * Set the maximum number of items allowed to be expanded. When the
     * (limit+1)th item is expanded, the first expanded item will collapse.
     *
     * @param limit the maximum number of items allowed to be expanded. Use <= 0
     * for no limit.
     */
    public void setLimit(final int limit) {
        mLimit = limit;
        mExpandedIds.clear();
        notifyDataSetChanged();
    }

    public void setDataSet(List<Subject> subjects) {
        if(subjects != null) {
            mSubjects = subjects;
            notifyDataSetChanged();
        }
    }
}