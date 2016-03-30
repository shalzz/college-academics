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

package com.shalzz.attendance.model;

import com.google.gson.Gson;
import com.shalzz.attendance.wrapper.DateHelper;

import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

public class PeriodModel {

    /** Field names need to be the same
     *  as that of the fields in the
     *  JSON object sent by the REST API,
     *  for {@link Gson} to be able to deserialize it properly.
     *
     *  Typical `period` JSON object will be of the format:
     *  {
     *      "id": ##,
     *      "day": "",
     *      "name": "",
     *      "start": "",
     *      "end": "",
     *      "teacher": "",
     *      "room": "",
     *      "batch": ""
     *  }
     *
     *  which is exposed by the api endpoint /api/v1/me/timetable
     *  by the express.js server (upes-api) as of this writing.
     */
	private int id;
	private String name;
	private String teacher;
	private String room;
	private String start;
	private String end;
	private String day;
    private String batch;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

	public Date getStartDate() {
        Date d = null;
        try {
            d = DateHelper.hr24Format.parse(start);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return d;
	}

	public Date getEndDate() {
        Date d = null;
        try {
            d = DateHelper.hr24Format.parse(end);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return d;
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
		this.name = name;
	}

	public void setRoom(String room) {
		this.room = room;
	}

	public void setTeacher(String teacher) {
		this.teacher = capitalizeString(teacher);
	}

    public void setStart(String start) {
        this.start = start;
    }

    public void setEnd(String end) {
        this.end = end;
    }

	public void setTime(String start, String end) {
        // always store time in 24 hour format
        try {
            this.start = DateHelper.to24HrFormat(start);
            this.end = DateHelper.to24HrFormat(end);
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

	public void setDay(String day ) {
		this.day = day;
	}

    public void setBatch(String batch) {
        this.batch =  batch;
    }
    
    public String capitalizeString(String name) {
        char[] chars = name.toLowerCase(Locale.getDefault()).toCharArray();
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

	@Override
	public String toString() {
		return "PeriodModel{" +
				"id=" + id +
				", name='" + name + '\'' +
				", teacher='" + teacher + '\'' +
				", room='" + room + '\'' +
				", start='" + start + '\'' +
				", start='" + getStartDate().getTime() + '\'' +
				", end='" + end + '\'' +
				", end='" + getEndDate().getTime() + '\'' +
				", day='" + day + '\'' +
				", batch='" + batch + '\'' +
				'}';
	}
}
