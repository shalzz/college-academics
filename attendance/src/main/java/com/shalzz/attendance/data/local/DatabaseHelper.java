package com.shalzz.attendance.data.local;

import android.database.sqlite.SQLiteDatabase;

import com.shalzz.attendance.data.model.AbsentDate;
import com.shalzz.attendance.data.model.ListFooter;
import com.shalzz.attendance.data.model.Period;
import com.shalzz.attendance.data.model.Subject;
import com.shalzz.attendance.data.model.User;
import com.shalzz.attendance.wrapper.DateHelper;
import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;
import com.squareup.sqldelight.SqlDelightStatement;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * Helper Class for SQLite database
 *
 * @author shalzz
 */
@Singleton
public class DatabaseHelper {

    private final BriteDatabase mDb;

    @Inject
    public DatabaseHelper(DbOpenHelper dbOpenHelper) {
        SqlBrite sqlBrite = new SqlBrite.Builder().build();
        mDb = sqlBrite.wrapDatabaseHelper(dbOpenHelper, Schedulers.io());
        mDb.setLoggingEnabled(true);
    }

    public BriteDatabase getBriteDb() {
        return mDb;
    }

    public Observable<Subject> setSubjects(final Collection<Subject> newSubjects) {
        return Observable.create(subscriber -> {
            if (subscriber.isUnsubscribed()) return;

            try (BriteDatabase.Transaction transaction = mDb.newTransaction()) {
                mDb.delete(Subject.TABLE_NAME, null);
                for (Subject subject : newSubjects) {
                    long result = mDb.insert(Subject.TABLE_NAME,
                            Subject.FACTORY.marshal(subject).asContentValues(),
                            SQLiteDatabase.CONFLICT_REPLACE);

                    // Store the dates in another table corresponding to the same id
                    for (Date date : subject.absent_dates()) {
                        mDb.insert(AbsentDate.TABLE_NAME,
                                AbsentDate.FACTORY.marshal()
                                        .subject_id(subject.id())
                                        .absent_date(date)
                                        .asContentValues());
                    }
                    if (result >= 0) subscriber.onNext(subject);
                }
                transaction.markSuccessful();
                subscriber.onCompleted();
            }
        });
    }

    public Observable<List<Subject>> getSubjects(String filter) {
        SqlDelightStatement query = Subject.FACTORY.select_like_name(filter);
        return mDb.createQuery(query.tables, query.statement, query.args)
                .mapToList(Subject.MAPPER::map);
    }

    /**
     * Gets a list of subjects that are marked absent on a particular date
     *
     * @param date The date
     * @return A list of Id's of subjects marked as absent
     */
    public Observable<List<Integer>> getAbsentSubjects(Date date) {
        SqlDelightStatement query = AbsentDate.FACTORY.select_absent_subjects(date);
        return mDb.createQuery(query.tables, query.statement, query.args)
                .mapToList(cursor -> AbsentDate.MAPPER.map(cursor).subject_id());
    }

    public Observable<Period> addPeriods(final Collection<Period> newPeriods) {
        return Observable.create(subscriber -> {
            if (subscriber.isUnsubscribed()) return;

            try (BriteDatabase.Transaction transaction = mDb.newTransaction()) {
                mDb.delete(Period.TABLE_NAME, null);
                for (Period period : newPeriods) {
                    long result = mDb.insert(Period.TABLE_NAME,
                            Period.FACTORY.marshal(period).asContentValues(),
                            SQLiteDatabase.CONFLICT_REPLACE);
                    if (result >= 0) subscriber.onNext(period);
                }
                transaction.markSuccessful();
                subscriber.onCompleted();
            }
        });
    }

    public Observable<List<Period>> getPeriods(Date date) {
        SqlDelightStatement query = Period.FACTORY.select_by_week_day(DateHelper.getShortWeekday(date));
        return mDb.createQuery(query.tables, query.statement, query.args)
                .mapToList(Period.MAPPER::map);
    }

    public Observable<User> addUser(User user) {
        return Observable.create(subscriber -> {
            if (subscriber.isUnsubscribed()) return;
            long result = mDb.insert(User.TABLE_NAME, User.FACTORY.marshal(user).asContentValues(),
                    SQLiteDatabase.CONFLICT_REPLACE);
            if (result >= 0) subscriber.onNext(user);
            subscriber.onCompleted();
        });
    }

    public Observable<User> getUser() {
        SqlDelightStatement query = User.FACTORY.select_all();
        return mDb.createQuery(query.tables, query.statement, query.args)
                .mapToOne(User.MAPPER::map);
    }

    public Observable<ListFooter> getListFooter() {
        SqlDelightStatement query = Subject.FACTORY.select_total();
        return mDb.createQuery(query.tables, query.statement, query.args)
                .mapToOne(cursor -> ListFooter.builder()
                        .setAttended(cursor.getFloat(0))
                        .setHeld(cursor.getFloat(1))
                        .build());
    }

    public Observable<Integer> getSubjectCount() {
        SqlDelightStatement query = Subject.FACTORY.select_count();
        return mDb.createQuery(query.tables, query.statement, query.args)
                .mapToOne(cursor -> cursor.getInt(0));
    }

    public Observable<Integer> getPeriodCount() {
        SqlDelightStatement query = Period.FACTORY.select_count();
        return mDb.createQuery(query.tables, query.statement, query.args)
                .mapToOne(cursor -> cursor.getInt(0));
    }

    public Observable<Integer> getUserCount() {
        SqlDelightStatement query = User.FACTORY.select_count();
        return mDb.createQuery(query.tables, query.statement, query.args)
                .mapToOne(cursor -> cursor.getInt(0));
    }

    /**
     * Delete All Rows
     * */
    public void resetTables(){
        mDb.delete(Subject.TABLE_NAME, null);
        mDb.delete(Period.TABLE_NAME, null);
        mDb.delete(User.TABLE_NAME, null);
        mDb.delete(AbsentDate.TABLE_NAME, null);
    }
}
