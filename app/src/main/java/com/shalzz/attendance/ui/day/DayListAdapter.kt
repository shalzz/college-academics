/*
 * Copyright (c) 2013-2018 Shaleen Jain <shaleen.jain95@gmail.com>
 *
 * This file is part of College Academics.
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

package com.shalzz.attendance.ui.day

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import com.shalzz.attendance.R
import com.shalzz.attendance.data.model.entity.Period
import javax.inject.Inject

class DayListAdapter @Inject
internal constructor() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val mPeriods: SortedList<Period>
    private val callback: SortedListAdapterCallback<Period>

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvSubjectName: TextView = itemView.findViewById(R.id.tvSubjectName)
        var tvTime: TextView = itemView.findViewById(R.id.tvTime)
        var tvTeacher: TextView = itemView.findViewById(R.id.tvTeacher)
        var tvRoom: TextView = itemView.findViewById(R.id.tvRoom)
        var tvMarkedAbsent: TextView = itemView.findViewById(R.id.tvMarkedAbsent)
    }

    init {
        callback = object : SortedListAdapterCallback<Period>(this) {
            override fun compare(o1: Period, o2: Period): Int {
                return o1.startDate!!.compareTo(o2.startDate)
            }

            override fun areContentsTheSame(oldItem: Period, newItem: Period): Boolean {
                if (oldItem.id != newItem.id) {
                    return false
                }
                if (oldItem.date != newItem.date) {
                    return false
                }
                if (oldItem.name != newItem.name) {
                    return false
                }
                if (oldItem.teacher != newItem.teacher) {
                    return false
                }
                if (oldItem.start != newItem.start) {
                    return false
                }
                if (oldItem.end != newItem.end) {
                    return false
                }
                if (oldItem.absent != newItem.absent) {
                    return false
                }
                return oldItem.room == newItem.room
            }

            override fun areItemsTheSame(item1: Period, item2: Period): Boolean {
                return item1.id == item2.id
            }
        }

        mPeriods = SortedList(Period::class.java, callback)
    }

    fun update(periods: List<Period>) {
        mPeriods.beginBatchedUpdates()
        for (i in 0 until mPeriods.size()) {
            val existingObject = mPeriods.get(i)
            var found = false
            for (newObject in periods) {
                if (callback.areItemsTheSame(existingObject, newObject)) {
                    found = true
                    break
                }
            }
            if (!found) {
                mPeriods.remove(existingObject)
            }
        }
        mPeriods.addAll(periods)
        mPeriods.endBatchedUpdates()
    }

    fun clear() {
        mPeriods.clear()
    }


    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): RecyclerView.ViewHolder {
        // create a new view
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_day_item, parent, false)
        return ViewHolder(v)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        val holder = viewHolder as DayListAdapter.ViewHolder
        val period = mPeriods.get(position)
        holder.tvSubjectName.text = period.name
        holder.tvRoom.text = period.room
        holder.tvTeacher.text = period.teacher
        holder.tvTime.text = period.timein12hr
        holder.tvMarkedAbsent.visibility = if (period.absent) View.VISIBLE else View.GONE

    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return mPeriods.size()
    }

}
