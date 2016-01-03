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

package com.shalzz.attendance;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;
import android.widget.OverScroller;
import android.widget.Scroller;

import com.shalzz.attendance.model.Period;
import com.shalzz.attendance.wrapper.DateHelper;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CalendarItemDecoration extends RecyclerView.ItemDecoration {

    public static final int HORIZONTAL_LIST = LinearLayoutManager.HORIZONTAL;
    public static final int VERTICAL_LIST = LinearLayoutManager.VERTICAL;
    private int mOrientation;
    private List<Period> mPeriods;
    private View mHeaderRow;

    private Calendar mToday;
    private Calendar mStartDate;
    private Paint mTimeTextPaint;
    private float mTimeTextWidth;
    private float mTimeTextHeight;
    private Paint mHeaderTextPaint;
    private float mHeaderTextHeight = 0;
    private GestureDetectorCompat mGestureDetector;
    private OverScroller mScroller;
    private PointF mCurrentOrigin = new PointF(0f, 0f);
    private Paint mHeaderBackgroundPaint;
    private float mWidthPerDay;
    private Paint mDayBackgroundPaint;
    private Paint mHourSeparatorPaint;
    private float mHeaderMarginBottom;
    private Paint mTodayBackgroundPaint;
    private Paint mTodayHeaderTextPaint;
    private Paint mEventBackgroundPaint;
    private float mHeaderColumnWidth = 25;
    private TextPaint mEventTextPaint;
    private Paint mHeaderColumnBackgroundPaint;
    private Scroller mStickyScroller;
    private int mFetchedMonths[] = new int[3];
    private boolean mRefreshEvents = false;
    private float mDistanceY = 0;
    private float mDistanceX = 0;
    private DateHelper.DateTimeInterpreter mDateTimeInterpreter;

    // Attributes and their default values.
    private int mHourHeight = 60;
    private int mColumnGap = 10;
    private int mFirstDayOfWeek = Calendar.MONDAY;
    private int mTextSize = 12;
    private int mHeaderColumnPadding = 10;
    private int mHeaderColumnTextColor = Color.BLACK;
    private int mNumberOfVisibleDays = 3;
    private int mHeaderRowPadding = 10;
    private int mHeaderRowBackgroundColor = Color.WHITE;
    private int mDayBackgroundColor = Color.rgb(245, 245, 245);
    private int mHourSeparatorColor = Color.rgb(230, 230, 230);
    private int mTodayBackgroundColor = Color.rgb(239, 247, 254);
    private int mHourSeparatorHeight = 2;
    private int mTodayHeaderTextColor = Color.rgb(39, 137, 228);
    private int mEventTextSize = 12;
    private int mEventTextColor = Color.BLACK;
    private int mEventPadding = 8;
    private int mHeaderColumnBackgroundColor = Color.LTGRAY;
    private int mDefaultEventColor;
    private boolean mIsFirstDraw = true;
    private int mOverlappingEventGap = 0;
    private int mEventMarginVertical = 0;
    private float mXScrollingSpeed = 1f;
    private Calendar mFirstVisibleDay;
    private Calendar mLastVisibleDay;

    public CalendarItemDecoration(Context context, Date mDate, int orientation) {
        setOrientation(orientation);

        DatabaseHandler db = new DatabaseHandler(context);
        String weekday = DateHelper.getShortWeekday(mDate);
        mPeriods = db.getAllPeriods(weekday);
        init();
    }

    private void init() {
        // Measure settings for time column.
        mTimeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTimeTextPaint.setTextAlign(Paint.Align.RIGHT);
        mTimeTextPaint.setTextSize(mTextSize);
        mTimeTextPaint.setColor(mHeaderColumnTextColor);
        Rect rect = new Rect();
        mTimeTextPaint.getTextBounds("00 PM", 0, "00 PM".length(), rect);
        mTimeTextWidth = mTimeTextPaint.measureText("00 PM");
        mTimeTextHeight = rect.height();
        mHeaderMarginBottom = mTimeTextHeight / 2;

        // Prepare hour separator color paint.
        mHourSeparatorPaint = new Paint();
        mHourSeparatorPaint.setStyle(Paint.Style.STROKE);
        mHourSeparatorPaint.setStrokeWidth(mHourSeparatorHeight);
        mHourSeparatorPaint.setColor(mHourSeparatorColor);

        // Prepare day background color paint.
        mDayBackgroundPaint = new Paint();
        mDayBackgroundPaint.setColor(mDayBackgroundColor);

        // Prepare today background color paint.
        mTodayBackgroundPaint = new Paint();
        mTodayBackgroundPaint.setColor(mTodayBackgroundColor);

        // Prepare event background color.
        mEventBackgroundPaint = new Paint();
        mEventBackgroundPaint.setColor(Color.rgb(174, 208, 238));

        // Prepare header column background color.
        mHeaderColumnBackgroundPaint = new Paint();
        mHeaderColumnBackgroundPaint.setColor(mHeaderColumnBackgroundColor);

        // Set default event color.
        mDefaultEventColor = Color.parseColor("#9fc6e7");
    }

    public void setOrientation(int orientation) {
        if (orientation != HORIZONTAL_LIST && orientation != VERTICAL_LIST) {
            throw new IllegalArgumentException("invalid orientation");
        }
        mOrientation = orientation;
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDraw(c,parent,state);

        // Draw the time column and all the axes/separators.
        drawTimeColumnAndAxes(c, parent);
    }

    private void drawTimeColumnAndAxes(Canvas canvas , RecyclerView parent) {

        // Do not let the view go above/below the limit due to scrolling. Set the max and min limit of the scroll.
        if (mOrientation == VERTICAL_LIST) {
            if (mCurrentOrigin.y - mDistanceY > 0) mCurrentOrigin.y = 0;
            else if (mCurrentOrigin.y - mDistanceY < -(mHourHeight * 24 + mHeaderTextHeight + mHeaderRowPadding * 2 - parent.getHeight()))
                mCurrentOrigin.y = -(mHourHeight * 24 + mHeaderTextHeight + mHeaderRowPadding * 2 - parent.getHeight());
            else mCurrentOrigin.y -= mDistanceY;
        }

        if(mPeriods.size() > 0) {
            // Draw the background color for the header column.
            canvas.drawRect(0, 0, mHeaderColumnWidth, parent.getHeight(), mHeaderColumnBackgroundPaint);
            String startTime = mPeriods.get(0).getStartTime();
            int start = Integer.parseInt(startTime.substring(0, startTime.indexOf(':')));
            int end = start + parent.getChildCount();
            for (int i = 0; i < end; i++) {
                float top = mHeaderTextHeight + mHeaderRowPadding * 2 + mCurrentOrigin.y + mHourHeight * i + mHeaderMarginBottom;

                // Draw the text if its y position is not outside of the visible area. The pivot point of the text is the point at the bottom-right corner.
                String time = getDateTimeInterpreter().interpretTime(start++);
                if (time == null)
                    throw new IllegalStateException("A DateTimeInterpreter must not return null time");
                if (top < parent.getHeight())
                    canvas.drawText(time, mTimeTextWidth + mHeaderColumnPadding, top + mTimeTextHeight, mTimeTextPaint);
            }
        }
    }

    private View getHeaderRow() {
        if(mHeaderRow == null) {
//            mHeaderRow = new View();
        }
        return mHeaderRow;
    }

    /**
     * Get the interpreter which provides the text to show in the header column and the header row.
     * @return The date, time interpreter.
     */
    public DateHelper.DateTimeInterpreter getDateTimeInterpreter() {
        if (mDateTimeInterpreter == null) {
            mDateTimeInterpreter = new DateHelper.DateTimeInterpreter() {
                @Override
                public String interpretDate(Calendar date) {
                    SimpleDateFormat sdf;
                    sdf = new SimpleDateFormat("EEEEE");
                    try{
                        String dayName = sdf.format(date.getTime()).toUpperCase();
                        return String.format("%s %d/%02d", dayName, date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH));
                    }catch (Exception e){
                        e.printStackTrace();
                        return "";
                    }
                }
                @Override
                public String interpretTime(int hour) {
                    String amPm;
                    if (hour >= 0 && hour < 12) amPm = "AM";
                    else amPm = "PM";
                    if (hour == 0) hour = 12;
                    if (hour > 12) hour -= 12;
                    return String.format("%02d\n%s", hour, amPm);
                }
            };
        }
        return mDateTimeInterpreter;
    }
    /**
     * Set the interpreter which provides the text to show in the header column and the header row.
     * @param dateTimeInterpreter The date, time interpreter.
     */
    public void setDateTimeInterpreter(DateHelper.DateTimeInterpreter dateTimeInterpreter){
        this.mDateTimeInterpreter = dateTimeInterpreter;
    }

    @Override
    public void getItemOffsets(Rect outRect, int itemPosition, RecyclerView parent) {
        super.getItemOffsets(outRect, itemPosition, parent);
        outRect.left = Math.round(mHeaderColumnWidth);

        if(itemPosition > 1 ) {
            int diff = 0 ;
            try {
                DateFormat hr24Format = new SimpleDateFormat("HH:mm");
                Date start = hr24Format.parse(mPeriods.get(itemPosition-1).getEndTime());
                Date end = hr24Format.parse(mPeriods.get(itemPosition).getStartTime());
                diff = end.compareTo(start);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if(diff !=0) {
                Log.d("Calender decoration","Diff: "+diff);
                outRect.top = diff * mHourHeight;
            }
        }
    }
}
