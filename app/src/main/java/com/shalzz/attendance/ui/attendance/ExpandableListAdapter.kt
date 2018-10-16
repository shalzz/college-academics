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

package com.shalzz.attendance.ui.attendance

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import butterknife.BindView
import butterknife.ButterKnife
import com.shalzz.attendance.BuildConfig
import com.shalzz.attendance.R
import com.shalzz.attendance.data.model.ListFooter
import com.shalzz.attendance.data.model.entity.Subject
import com.shalzz.attendance.injection.ApplicationContext
import com.shalzz.attendance.utils.Miscellaneous
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class ExpandableListAdapter @Inject
internal constructor(@param:ApplicationContext private val mContext: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val mResources: Resources = mContext.resources
    private val mBitmap: Bitmap
    private val mExpandedTranslationZ: Float

    //our items
    private val mSubjects: SortedList<Subject>
    private var mFooter: ListFooter? = null
    private val headers = ArrayList<View>()
    private val footers = ArrayList<View>()

    /**
     * Tracks the row which was previously expanded.  Used so that the closure of a
     * previously expanded call log entry can be animated on rebind.
     */
    private var mPreviouslyExpanded = NONE_EXPANDED

    private var mExpandedId = NONE_EXPANDED

    private var mSubjectItemExpandedListener: SubjectItemExpandedListener? = null

    /**
     * The onClickListener used to expand or collapse the action buttons section for a call log
     * entry.
     */
    private val mExpandCollapseListener = View.OnClickListener {
        view ->  handleRowExpanded(view, true /* animate */, false /* forceExpand */)
    }

    /** Interface used to inform a parent UI element that a list item has been expanded.  */
    interface SubjectItemExpandedListener {
        /**
         * @param view The [View] that represents the item that was clicked
         * on.
         */
        fun onItemExpanded(view: View)

        /**
         * Retrieves the call log view for the specified call Id.  If the view is not currently
         * visible, returns null.
         *
         * @param callId The call Id.
         * @return The call log view.
         */
        fun getViewForCallId(callId: Long): View?
    }

    init {
        mExpandedTranslationZ = mResources.getDimension(R.dimen.atten_view_expanded_elevation)
        mBitmap = BitmapFactory.decodeResource(mResources, R.drawable.alert)

        mSubjects = SortedList(Subject::class.java,
                object : SortedListAdapterCallback<Subject>(this) {
                    override fun compare(o1: Subject, o2: Subject): Int {
                        return o1.name.compareTo(o2.name)
                    }

                    override fun areContentsTheSame(oldItem: Subject, newItem: Subject): Boolean {
                        if (oldItem.id != newItem.id) {
                            return false
                        }
                        if (oldItem.name != newItem.name) {
                            return false
                        }
                        if (java.lang.Float.compare(oldItem.attended, newItem.attended) != 0) {
                            return false
                        }
                        if (java.lang.Float.compare(oldItem.held, newItem.held) != 0) {
                            return false
                        }
                        if (oldItem.absent_dates != null && newItem.absent_dates == null) {
                            return false
                        }
                        if (oldItem.absent_dates == null && newItem.absent_dates != null) {
                            return false
                        }
                        return if (oldItem.absent_dates == null && newItem.absent_dates == null) {
                            true
                        } else
                            oldItem.absentDatesAsString == newItem.absentDatesAsString
                    }

                    override fun areItemsTheSame(item1: Subject, item2: Subject): Boolean {
                        return item1.id == item2.id
                    }
                })
    }

    fun setCallback(callback: SubjectItemExpandedListener) {
        mSubjectItemExpandedListener = callback
    }

    fun addAll(subjects: List<Subject>) {
        mSubjects.addAll(subjects)
        notifyDataSetChanged()
    }

    fun clear() {
        if (BuildConfig.DEBUG)
            Timber.i("Data set cleared.")
        mSubjects.clear()
    }

    fun updateFooter(footer: ListFooter) {
        if (footer != mFooter) {
            mFooter = footer
            notifyItemChanged(itemCount - 1)
        }
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class GenericViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder
    (itemView) {
        internal var position = -1

        @BindView(R.id.tvSubj)
        lateinit var subject: TextView
        @BindView(R.id.tvPercent)
        lateinit var percentage: TextView
        @BindView(R.id.tvClasses)
        lateinit var classes: TextView
        @BindView(R.id.pbPercent)
        lateinit var percent: ProgressBar

        //child views
        var childView: RelativeLayout? = null
        var tvAbsent: TextView? = null
        var tvReach: TextView? = null
        var ivAlert: ImageView? = null

        init {
            ButterKnife.bind(this, itemView)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        //if our position is one of our items (this comes from getItemViewType(int position) below)
        return if (viewType == TYPE_ITEM) {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.list_attend_card,
                    parent, false)
            GenericViewHolder(v)
            //else we have a header/footer
        } else {
            //create a new framelayout, or inflate from a resource
            val frameLayout = FrameLayout(parent.context)
            //make sure it fills the space
            frameLayout.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
            HeaderFooterViewHolder(frameLayout)
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        //check what type of view our position is
        when {
            position < headers.size -> {
                val v = headers[position]
                //add our view to a header view and display it
                prepareHeaderFooter(holder as HeaderFooterViewHolder, v)
            }
            position >= headers.size + mSubjects.size() -> {
                val v = footers[position - mSubjects.size() - headers.size]
                //add our view to a footer view and display it
                prepareHeaderFooter(holder as HeaderFooterViewHolder, v)
            }
            else -> //it's one of our items, display as required
                prepareGeneric(holder as GenericViewHolder, position - headers.size)
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
    private fun inflateChildView(view: View) {
        val views = view.tag as GenericViewHolder

        val stub = view.findViewById<ViewStub>(R.id.subject_details_stub)
        if (stub != null) {
            views.childView = stub.inflate() as RelativeLayout
        } else
            views.childView = views.itemView.findViewById(R.id.subTree)

        // child view
        val childView = views.childView
        views.tvAbsent = childView!!.findViewById(R.id.tvAbsent)
        views.tvReach = childView.findViewById(R.id.tvReach)
        views.ivAlert = childView.findViewById(R.id.imageView1)

        bindChildView(views, views.position)
    }

    /**
     * Toggles the expansion state tracked for the row identified by rowId and returns
     * the new expansion state.  Assumes that only a single row will be expanded at any
     * one point and tracks the current and previous expanded item.
     *
     * @param rowId The row Id associated with the row to expand/collapse.
     * @return True where the row is now expanded, false otherwise.
     */
    private fun toggleExpansion(rowId: Long): Boolean {
        return if (isExpanded(rowId)) {
            mPreviouslyExpanded = NONE_EXPANDED
            mExpandedId = NONE_EXPANDED
            false
        } else {
            // Collapse the previous row
            mPreviouslyExpanded = mExpandedId

            // Expanding a row.
            mExpandedId = rowId
            true
        }
    }

    /**
     * Determines if a row with the given Id is expanded.
     * @param rowId The row Id.
     * @return True if the row is expanded.
     */
    private fun isExpanded(rowId: Long): Boolean {
        return mExpandedId == rowId
    }

    /**
     * Expands or collapses the view.
     *
     * @param view The call log entry parent view.
     * @param isExpanded The new expansion state of the view.
     */
    private fun expandOrCollapseChildView(view: View, isExpanded: Boolean) {
        val views = view.tag as GenericViewHolder

        if (isExpanded) {
            // Inflate the view stub if necessary.
            inflateChildView(view)

            views.childView!!.visibility = View.VISIBLE
            views.childView!!.alpha = 1.0f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                view.translationZ = mExpandedTranslationZ
                view.isActivated = true
            }
        } else {

            // When recycling a view, it is possible the actionsView ViewStub was previously
            // inflated so we should hide it in this case.
            if (views.childView != null) {
                views.childView!!.visibility = View.GONE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                view.translationZ = 0f
                view.isActivated = false
            }
        }
    }

    /**
     * Manages the state changes for the UI interaction where a row is expanded.
     *
     * @param view The view that was tapped
     * @param animate Whether or not to animate the expansion/collapse
     * @param forceExpand Whether or not to force the row into an expanded state regardless
     * of its previous state
     */
    private fun handleRowExpanded(view: View, animate: Boolean, forceExpand: Boolean) {
        val views = view.tag as GenericViewHolder

        if (forceExpand && isExpanded(views.position.toLong())) {
            return
        }

        // Hide or show the actions view.
        val expanded = toggleExpansion(views.position.toLong())

        // Trigger loading of the viewstub and visual expand or collapse.
        expandOrCollapseChildView(view, expanded)

        // Animate the expansion or collapse.
        if (mSubjectItemExpandedListener != null) {
            if (animate) {
                mSubjectItemExpandedListener!!.onItemExpanded(view)
            }

            // Animate the collapse of the previous item if it is still visible on screen.
            if (mPreviouslyExpanded != NONE_EXPANDED) {
                val previousItem = mSubjectItemExpandedListener!!.getViewForCallId(
                        mPreviouslyExpanded)

                if (previousItem != null) {
                    expandOrCollapseChildView(previousItem, false)
                    if (animate) {
                        mSubjectItemExpandedListener!!.onItemExpanded(previousItem)
                    }
                }
                mPreviouslyExpanded = NONE_EXPANDED
            }
        }
    }

    private fun bindChildView(holder: GenericViewHolder, position: Int) {

        val tvAbsent = holder.tvAbsent
        val tvReach = holder.tvReach
        val ivAlert = holder.ivAlert

        val held = mSubjects.get(position).held.toInt()
        val attend = mSubjects.get(position).attended.toInt()
        val percent = Math.round(mSubjects.get(position).getPercentage())

        if (mSubjects.get(position).absentDatesAsString.isEmpty()) {
            tvAbsent!!.text = mResources.getText(R.string.atten_list_days_absent_null)
        } else {
            tvAbsent!!.text = mResources.getString(R.string.atten_list_days_absent,
                    mSubjects.get(position).absentDatesAsString)
        }

        if (percent < 67 && held != 0) {
            val x = 2 * held - 3 * attend
            if (x == 0) {
                tvReach!!.visibility = View.GONE
                ivAlert!!.visibility = View.GONE
                ivAlert.setImageBitmap(null)
            } else {
                tvReach!!.text = mResources.getQuantityString(R.plurals.tv_classes_to_67, x, x)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    tvReach.setTextColor(mResources.getColor(R.color.attend, mContext.theme))
                } else {
                    tvReach.setTextColor(mResources.getColor(R.color.attend))
                }
                tvReach.visibility = View.VISIBLE
                ivAlert!!.visibility = View.VISIBLE
                ivAlert.setImageBitmap(mBitmap)
            }
        } else if (percent < 75 && held != 0) {
            val x = 3 * held - 4 * attend
            if (x == 0) {
                tvReach!!.visibility = View.GONE
                ivAlert!!.visibility = View.GONE
                ivAlert.setImageBitmap(null)
            } else {
                tvReach!!.text = mResources.getQuantityString(R.plurals.tv_classes_to_75, x, x)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    tvReach.setTextColor(mResources.getColor(R.color.attend, mContext.theme))
                } else {
                    tvReach.setTextColor(mResources.getColor(R.color.attend))
                }
                tvReach.visibility = View.VISIBLE
                ivAlert!!.visibility = View.VISIBLE
                ivAlert.setImageBitmap(mBitmap)
            }
        } else {
            val x = 4 * attend / 3 - held
            if (x == 0) {
                tvReach!!.visibility = View.GONE
            } else {
                tvReach!!.text = mResources.getQuantityString(R.plurals.tv_miss_classes, x, x)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    tvReach.setTextColor(mResources.getColor(R.color.skip, mContext.theme))
                } else {
                    tvReach.setTextColor(mResources.getColor(R.color.skip))
                }
                tvReach.visibility = View.VISIBLE
            }
            ivAlert!!.visibility = View.GONE
            ivAlert.setImageBitmap(null)
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        //make sure the adapter knows to look for all our items, headers, and footers
        return headers.size + mSubjects.size() + footers.size
    }

    private fun prepareHeaderFooter(vh: HeaderFooterViewHolder, view: View) {
        //empty out our FrameLayout and replace with our header/footer
        vh.base.removeAllViews()
        if (view.parent != null) {
            (view.parent as ViewGroup).removeView(view)
        }
        vh.base.addView(view)

        if (mFooter == null)
            return
        val percent = mFooter!!.getPercentage()

        /** --------footer--------  */
        view.visibility = View.VISIBLE
        val tvPercent = view.findViewById<TextView>(R.id.tvTotalPercent)
        val tvClasses = view.findViewById<TextView>(R.id.tvClass)
        val pbPercent = view.findViewById<ProgressBar>(R.id.pbTotalPercent)
        tvPercent.text = mResources.getString(R.string.atten_list_percentage,
                mFooter!!.getPercentage())
        tvClasses.text = mResources.getString(R.string.atten_list_attended_upon_held,
                mFooter!!.attended.toInt(),
                mFooter!!.held.toInt())
        pbPercent.progress = percent!!.toInt()
        val d = pbPercent.progressDrawable
        d.level = percent.toInt() * 100
        /** ------------------------ */
    }

    private fun prepareGeneric(holder: GenericViewHolder, position: Int) {
        holder.position = position
        holder.itemView.tag = holder

        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        val percent = mSubjects.get(position).getPercentage()
        holder.subject.text = Miscellaneous.capitalizeString(mSubjects.get(position).name)
        holder.percentage.text = mResources.getString(R.string.atten_list_percentage,
                mSubjects.get(position).getPercentage())
        holder.classes.text = mResources.getString(R.string.atten_list_attended_upon_held,
                mSubjects.get(position).attended.toInt(),
                mSubjects.get(position).held.toInt())
        val d = holder.percent.progressDrawable
        if (percent > 0f)
            d.level = percent.toInt() * 100
        else
            d.level = 1
        holder.percent.progressDrawable = d
        holder.percent.progress = percent.toInt()

        // In the call log, expand/collapse an actions section for the call log entry when
        // the primary view is tapped.
        holder.itemView.setOnClickListener(this.mExpandCollapseListener)

        // Restore expansion state of the row on rebind.  Inflate the actions ViewStub if required,
        // and set its visibility state accordingly.
        expandOrCollapseChildView(holder.itemView, isExpanded(position.toLong()))
    }

    override fun getItemViewType(position: Int): Int {
        //check what type our position is, based on the assumption that the order is headers > items > footers
        if (position < headers.size) {
            return TYPE_HEADER
        } else if (position >= headers.size + mSubjects.size()) {
            return TYPE_FOOTER
        }
        return TYPE_ITEM
    }

    //add a header to the adapter
    fun addHeader(header: View) {
        if (!headers.contains(header)) {
            headers.add(header)
            //animate
            notifyItemInserted(headers.size - 1)
        }
    }

    //remove a header from the adapter
    fun removeHeader(header: View) {
        if (headers.contains(header)) {
            //animate
            notifyItemRemoved(headers.indexOf(header))
            headers.remove(header)
        }
    }

    //add a footer to the adapter
    fun addFooter(footer: View) {
        if (!footers.contains(footer)) {
            footers.add(footer)
            //animate
            notifyItemInserted(headers.size + mSubjects.size() + footers.size - 1)
        }
    }

    //remove a footer from the adapter
    fun removeFooter(footer: View) {
        if (footers.contains(footer)) {
            //animate
            notifyItemRemoved(headers.size + mSubjects.size() + footers.indexOf(footer))
            footers.remove(footer)
        }
    }

    //our header/footer RecyclerView.ViewHolder is just a FrameLayout
    class HeaderFooterViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var base: FrameLayout = itemView as FrameLayout

    }

    companion object {

        private const val TYPE_HEADER = 111
        private const val TYPE_FOOTER = 222
        private const val TYPE_ITEM = 333

        /** Constant used to indicate no row is expanded.  */
        private const val NONE_EXPANDED: Long = -1
    }
}