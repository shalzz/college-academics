/*
 * Copyright (c) 2014 Shaleen Jain <shaleen.jain95@gmail.com>
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

package com.shalzz.attendance.model;

import com.bugsnag.android.Bugsnag;
import com.shalzz.attendance.wrapper.DateHelper;

import java.text.ParseException;

public class Period {

	// private variables;
	private String name = "";
	private String teacher = "";
	private String room = "";
	private String start;
	private String end;
	private String day;
    private String batch = "NULL";

	public Period() {

	}

    public Period(String day) {
        this.day = day;
    }

	Period (String name, String room, String teacher, String start, String end, String day) {
		this.name = name;
		this.room = room;
		this.teacher = teacher;
		this.start = start;
		this.end = end;
		this.day = day;
	}

	public String getSubjectName() {
		return name;
	}

	public String getRoom() {
		return room;
	}

	public String getTeacher() {
		return teacher;
	}

	public String getStartTime() {
		return start;
	}

	public String getEndTime() {
		return end;
	}

	public String getTime() {
		return start + "-" + end ;
	}

    public String getTimein12hr() throws ParseException {
        String timeRange , mStart, mEnd;
        mStart = DateHelper.to12HrFormat(start);
        mEnd = DateHelper.to12HrFormat(end);

        // Remove leading zero's
        mStart = mStart.startsWith("0") ? mStart.substring(1) : mStart;
        mEnd = mEnd.startsWith("0") ? mEnd.substring(1) : mEnd;

        // If a range shares a common AM/PM, append only on the end of the range. (Material Guideline)
        int sl = mStart.length(), el = mEnd.length();
        if(mStart.substring(sl-2).equals(mEnd.substring(el-2)))
            timeRange =mStart.substring(0,sl-3) + "-" + mEnd ;
        else
            timeRange =  mStart + "-" + mEnd ;

        return timeRange;
    }

	public String getDay() {
		return day;
	}

    public String getBatch() {
        return batch;
    }

	public void setSubjectName(String name) {
		if(name.equals("***"))
			name = "";
		this.name = name;
	}

	public void setRoom(String room) {
		this.room = room;
	}

	public void setTeacher(String teacher) {
		this.teacher = capitalizeString(teacher);
	}

	public void setTime(String start, String end) {
        // always store time in 24 hour format
        try {
            this.start = DateHelper.to24HrFormat(start);
            this.end = DateHelper.to24HrFormat(end);
        } catch (ParseException e) {
            Bugsnag.notify(e);
            e.printStackTrace();
        }
	}

	public void setDay(String day ) {
		this.day = day;
	}

    public boolean isEqual(Period period) {
        return this.name.equals(period.getSubjectName()) &&
                this.room.equals(period.getRoom()) &&
                this.teacher.equals(period.getTeacher()) &&
                this.start.equals(period.getEndTime()) &&
                this.day.equals(period.getDay());
    }

    public void setBatch(String batch) {
        this.batch =  batch;
    }
    
    public String capitalizeString(String name) {
        char[] chars = name.toLowerCase().toCharArray();
        boolean found = false;
        for (int i = 0; i < chars.length; i++) {
            if (!found && Character.isLetter(chars[i])) {
                chars[i] = Character.toUpperCase(chars[i]);
                found = true;
            } else if (Character.isWhitespace(chars[i]) || chars[i]=='.' || chars[i]=='\'') { // You can add other chars here
                found = false;
            }
        }
        return String.valueOf(chars);
    }
}
