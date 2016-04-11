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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

/** Field names need to be the same
 *  as that of the fields in the
 *  JSON object sent by the REST API,
 *  for {@link Gson} to be able to deserialize it
 *  properly and automatically.
 *
 *  Typical `attendance` JSON object will be of the format:
 *  {
 *      "absent_dates":["","",...],
 *      "id": ##,
 *      "name":"",
 *      "held": ##,
 *      "attended": ##}
 *  }
 *
 *  which is exposed by the api endpoint /api/v1/me/attendance
 *  by the express.js server (upes-api) as of this writing.
 */
public class SubjectModel {

    private DateFormat dayFormat = new SimpleDateFormat("d", Locale.US);
    private DateFormat monthFormat = new SimpleDateFormat("MMM", Locale.US);

	private int id;
	private String name;
	private Float held;
	private Float attended;
	private ArrayList<Date> absent_dates;

	public int getID(){
		return this.id;
	}

	public void setID(int id){
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Float getClassesHeld() {
		return this.held;
	}

	public void setClassesHeld(Float classesHeld) {
		this.held = classesHeld;
	}

	public Float getClassesAttended() {
		return this.attended;
	}

	public void setClassesAttended(Float classesAttended) {
		this.attended = classesAttended;
	}

    public ArrayList<Date> getAbsentDates() {
        return absent_dates;
    }

    public void setAbsentDates(ArrayList<Date> dates) {
        absent_dates = dates;
    }

	public String getAbsentDatesAsString() {
        if(absent_dates.size() == 0)
            return "";

		String datesStr = "";
        String prevMonth = "";
        Collections.sort(absent_dates);
        for (Date date: absent_dates) {
            int day = Integer.parseInt(dayFormat.format(date));
            String month = monthFormat.format(date);
            if(prevMonth.length() == 0) {
                datesStr += month + ": ";
                prevMonth = month;
            }
            else if(!prevMonth.equals(month)) {
                datesStr += "\n" + month + ": ";
                prevMonth = month;
            }
            datesStr += day + DateHelper.getDayOfMonthSuffix(day) + ", ";
        }
        return datesStr.substring(0,datesStr.length()-2);
	}

	public Float getPercentage() {
        if(held > 0f)
            return (float) (Math.round( attended / held * 10000.0 ) / 100.0 );
        return 0.0f;
	}

    @Override
    public String toString() {
        return "SubjectModel{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", held=" + held +
                ", attended=" + attended +
                ", absent_dates=" + absent_dates +
                '}';
    }
}
