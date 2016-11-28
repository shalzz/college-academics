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

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.shalzz.attendance.DatabaseHandler;
import com.shalzz.attendance.injection.ApplicationContext;
import com.shalzz.attendance.model.remote.Subject;

import java.util.List;

import javax.inject.Inject;

public class SubjectAsyncTaskLoader extends AsyncTaskLoader<List<Subject>> {

    private DatabaseHandler mDb;
    private List<Subject> mSubjects;

    // If non-null, this is the current filter the user has provided.
    String mCurFilter;

    @Inject
    SubjectAsyncTaskLoader(@ApplicationContext Context context, DatabaseHandler db) {
        super(context);
        mDb = db;
    }

    void setCursorFilter(String filter) {
        mCurFilter = filter;
    }

    @Override
    protected void onStartLoading() {
        if (mSubjects != null) {
            // Use cached data
            deliverResult(mSubjects);
        }
        if (takeContentChanged() || mSubjects == null) {
            // Something has changed or we have no data,
            // so kick off loading it
            forceLoad();
        }
    }

    public List<Subject> loadInBackground() {
        return mDb.getAllSubjects(this, mCurFilter);
    }

    @Override
    public void deliverResult(List<Subject> data) {
        // We’ll save the data for later retrieval
        mSubjects = data;
        // We can do any pre-processing we want here
        // Just remember this is on the UI thread so nothing lengthy!
        super.deliverResult(data);
    }
}
