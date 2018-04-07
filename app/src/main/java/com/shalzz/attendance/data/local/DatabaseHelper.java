package com.shalzz.attendance.data.local;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.SupportSQLiteOpenHelper;
import android.arch.persistence.db.SupportSQLiteOpenHelper.Configuration;
import android.arch.persistence.db.framework.FrameworkSQLiteOpenHelperFactory;
import android.content.Context;

import com.shalzz.attendance.data.model.AbsentDate;
import com.shalzz.attendance.data.model.ListFooter;
import com.shalzz.attendance.data.model.Period;
import com.shalzz.attendance.data.model.Subject;
import com.shalzz.attendance.data.model.User;
import com.shalzz.attendance.injection.ApplicationContext;
import com.shalzz.attendance.model.SubjectModel;
import com.shalzz.attendance.wrapper.DateHelper;
import com.squareup.sqlbrite3.BriteDatabase;
import com.squareup.sqlbrite3.SqlBrite;
import com.squareup.sqldelight.SqlDelightQuery;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.arch.persistence.db.SupportSQLiteOpenHelper.Factory;

/**
 * Helper Class for SQLite database
 *
 * @author shalzz
 */
@Singleton
public class DatabaseHelper {

    private final BriteDatabase mDb;
    private final Subject.InsertSubject insertSubject;
    private final Subject.DeleteAll deleteAllSubjects;
    private final AbsentDate.InsertAbsentDate insertAbsentDate;
    private final Period.InsertPeriod insertPeriod;
    private final Period.DeleteByDate deleteByDate;
    private final User.InsertUser insertUser;
    private final User.DeleteAll deleteAllUsers;

    @Inject
    public DatabaseHelper(@ApplicationContext Context context) {
        SqlBrite sqlBrite = new SqlBrite.Builder()
                .logger(message -> Timber.tag("Database").v(message))
                .build();

        Configuration configuration = Configuration.builder(context)
                .name(DbOpenHelper.DATABASE_NAME)
                .callback(new DbOpenHelper())
                .build();
        Factory factory = new FrameworkSQLiteOpenHelperFactory();
        SupportSQLiteOpenHelper helper = factory.create(configuration);
//        helper.setWriteAheadLoggingEnabled(true);
        mDb = sqlBrite.wrapDatabaseHelper(helper, Schedulers.io());
        mDb.setLoggingEnabled(true);

        SupportSQLiteDatabase db = mDb.getWritableDatabase();
        insertSubject = new SubjectModel.InsertSubject(db);
        deleteAllSubjects = new SubjectModel.DeleteAll(db);
        insertAbsentDate = new AbsentDate.InsertAbsentDate(db, AbsentDate.FACTORY);
        insertPeriod = new Period.InsertPeriod(db);
        deleteByDate = new Period.DeleteByDate(db);
        insertUser = new User.InsertUser(db);
        deleteAllUsers = new User.DeleteAll(db);
    }

    public Observable<Subject> setSubjects(final Collection<Subject> newSubjects) {
        return Observable.create(subscriber -> {
            if (subscriber.isDisposed()) return;

            try (BriteDatabase.Transaction transaction = mDb.newTransaction()) {
                mDb.executeUpdateDelete(deleteAllSubjects.getTable(), deleteAllSubjects);
                for (Subject subject : newSubjects) {
                    insertSubject.bind(subject.id(),
                            subject.name(),
                            subject.attended(),
                            subject.held());
                    long result = mDb.executeInsert(insertSubject.getTable(), insertSubject);

                    // Store the dates in another table corresponding to the same id
                    if (subject.absent_dates() != null) {
                        for (Date date : subject.absent_dates()) {
                            insertAbsentDate.bind(subject.id(), date);
                            mDb.executeInsert(insertAbsentDate.getTable(), insertAbsentDate);
                        }
                    }
                    if (result >= 0) subscriber.onNext(subject);
                }
                transaction.markSuccessful();
                subscriber.onComplete();
            }
        });
    }

    public Observable<List<Subject>> getSubjects(String filter) {
        filter = filter == null ? "" : filter;
        filter = '%' + filter + '%';
        SqlDelightQuery query = Subject.FACTORY.selectLikeName(filter);
        return mDb.createQuery(query.getTables(), query)
                .mapToList(Subject.MAPPER::map);
    }

    /**
     * Gets a list of subjects that are marked absent on a particular date
     *
     * @param date The date
     * @return A list of Id's of subjects marked as absent
     */
    public Observable<List<Integer>> getAbsentSubjects(Date date) {
        SqlDelightQuery query = AbsentDate.FACTORY.selectAbsentSubjects(date);
        return mDb.createQuery(query.getTables(), query)
                .mapToList(cursor -> AbsentDate.MAPPER.map(cursor).subject_id());
    }

    public Observable<Period> addPeriods(final List<Period> newPeriods) {
        return Observable.create(subscriber -> {
            if (subscriber.isDisposed()) return;
            if (newPeriods.isEmpty()) return;

            try (BriteDatabase.Transaction transaction = mDb.newTransaction()) {
                deleteByDate.bind(newPeriods.get(0).date());
                mDb.executeUpdateDelete(deleteByDate.getTable(), deleteByDate);
                for (Period period : newPeriods) {
                    insertPeriod.bind(period.id(),
                            period.name(),
                            period.teacher(),
                            period.room(),
                            period.batchid(),
                            period.batch(),
                            period.start(),
                            period.end(),
                            period.absent(),
                            period.date());
                    long result = mDb.executeInsert(insertPeriod.getTable(), insertPeriod);
                    if (result >= 0) subscriber.onNext(period);
                }
                transaction.markSuccessful();
                subscriber.onComplete();
            }
        });
    }

    public Observable<List<Period>> getPeriods(Date date) {
        SqlDelightQuery query = Period.FACTORY.selectByDate(DateHelper.formatToTechnicalFormat(date));
        return mDb.createQuery(query.getTables(), query)
                .mapToList(Period.MAPPER::map);
    }

    public Observable<User> addUser(User user) {
        return Observable.create(subscriber -> {
            if (subscriber.isDisposed()) return;
            insertUser.bind(user.id(),
                    user.roll_number(),
                    user.name(),
                    user.course(),
                    user.phone(),
                    user.email());
            long result = mDb.executeInsert(insertUser.getTable(), insertUser);
            if (result >= 0) subscriber.onNext(user);
            subscriber.onComplete();
        });
    }

    public Observable<User> getUser() {
        SqlDelightQuery query = User.FACTORY.selectAll();
        return mDb.createQuery(query.getTables(), query)
                .mapToOne(User.MAPPER::map);
    }

    public Observable<ListFooter> getListFooter() {
        SqlDelightQuery query = Subject.FACTORY.selectTotal();
        return mDb.createQuery(query.getTables(), query)
                .mapToOne(cursor -> ListFooter.builder()
                        .setAttended(cursor.getFloat(0))
                        .setHeld(cursor.getFloat(1))
                        .build());
    }

    public Observable<Integer> getSubjectCount() {
        SqlDelightQuery query = Subject.FACTORY.selectCount();
        return mDb.createQuery(query.getTables(), query)
                .mapToOne(cursor -> cursor.getInt(0));
    }

    public Observable<Integer> getPeriodCount(Date day) {
        SqlDelightQuery query =
                Period.FACTORY.selectCountByDate(DateHelper.formatToTechnicalFormat(day));
        return mDb.createQuery(query.getTables(), query)
                .mapToOne(cursor -> cursor.getInt(0));
    }

    public Observable<Integer> getUserCount() {
        SqlDelightQuery query = User.FACTORY.selectCount();
        return mDb.createQuery(query.getTables(), query)
                .mapToOne(cursor -> cursor.getInt(0));
    }

    /**
     * Delete All Rows
     * */
    public void resetTables(){
        mDb.executeUpdateDelete(deleteAllSubjects.getTable(), deleteAllSubjects);
        mDb.delete(Period.TABLE_NAME, null);
        mDb.executeUpdateDelete(deleteAllUsers.getTable(), deleteAllUsers);
        mDb.delete(AbsentDate.TABLE_NAME, null);
    }
}
