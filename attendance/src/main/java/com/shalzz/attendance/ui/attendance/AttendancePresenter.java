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

import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

import com.shalzz.attendance.DatabaseHandler;
import com.shalzz.attendance.injection.ConfigPersistent;
import com.shalzz.attendance.model.remote.Subject;
import com.shalzz.attendance.network.DataAPI;
import com.shalzz.attendance.network.RetrofitException;
import com.shalzz.attendance.ui.base.BasePresenter;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

@ConfigPersistent
class AttendancePresenter extends BasePresenter<AttendanceMvpView>
        implements LoaderManager.LoaderCallbacks<List<Subject>> {

    static final String SUBJECT_FILTER = "subject_filter_text";

    private DatabaseHandler mDb;
    private DataAPI mDataAPI;
    private SubjectAsyncTaskLoader mSubjectAsyncTaskLoader;

    @Inject
    AttendancePresenter(DatabaseHandler db,
                        DataAPI api,
                        SubjectAsyncTaskLoader subjectAsyncTaskLoader) {
        mDb = db;
        mDataAPI = api;
        mSubjectAsyncTaskLoader = subjectAsyncTaskLoader;
    }

    @Override
    public void attachView(AttendanceMvpView mvpView) {
        super.attachView(mvpView);
    }

    @Override
    public void detachView() {
        super.detachView();
    }

    void updateSubjects() {
        Call<List<Subject>> call = mDataAPI.getAttendance();
        call.enqueue(new Callback<List<Subject>>() {
            @Override
            public void onResponse(Call<List<Subject>> call,
                                   Response<List<Subject>> response) {
                List<Subject> subjects = response.body();
                long now = new Date().getTime();
                for (Subject subject : subjects) {
                    mDb.addSubject(subject, now);
                }

                if (mDb.purgeOldSubjects() == 1) {
                    Timber.i("Purging Subjects...");
                    getMvpView().clearSubjects();
                }

                if(!isViewAttached())
                    return;
                getMvpView().addSubjects(subjects);
                getMvpView().showcaseView();
                getMvpView().updateLastSync();
            }

            @Override
            public void onFailure(Call<List<Subject>> call, Throwable t) {
                if(!isViewAttached())
                    return;
                RetrofitException error = (RetrofitException) t;
                if (error.getKind() == RetrofitException.Kind.UNEXPECTED) {
                    Timber.e(t, error.getMessage());
                    getMvpView().showError(error.getMessage());
                }
                else if(mDb.getSubjectCount() > 0 &&
                         (error.getKind() == RetrofitException.Kind.NETWORK ||
                                error.getKind() == RetrofitException.Kind.EMPTY_RESPONSE)) {
                    getMvpView().showRetryError(error.getMessage());
                }
                else {
                    getMvpView().showErrorView(error);
                }
            }
        });
    }

    @Override
    public Loader<List<Subject>> onCreateLoader(int id, Bundle args) {
        String filter = null;
        if(args != null)
            filter = args.getString(SUBJECT_FILTER);
        mSubjectAsyncTaskLoader.setCursorFilter(filter);
        return mSubjectAsyncTaskLoader;
    }

    @Override
    public void onLoadFinished(Loader<List<Subject>> loader, List<Subject> data) {
        String filter = ((SubjectAsyncTaskLoader) loader).mCurFilter;
        if(data.size() == 0 && filter == null) {
            updateSubjects();
        } else {
            checkViewAttached();
            getMvpView().addSubjects(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<Subject>> loader) {
        // Loader reset, throw away our data,
        // unregister any listeners, etc.
        getMvpView().clearSubjects();
        // Of course, unless you use destroyLoader(),
        // this is called when everything is already dying
        // so a completely empty onLoaderReset() is
        // totally acceptable
    }
}
