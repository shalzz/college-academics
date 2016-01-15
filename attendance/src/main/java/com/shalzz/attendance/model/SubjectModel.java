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

package com.shalzz.attendance.model;

import com.shalzz.attendance.wrapper.DateHelper;

import java.util.Arrays;
import java.util.Date;

/**
 * Model class for subjects.
 * @author shalzz
 *
 */
public class SubjectModel {

	// private variables;
	private int id;
	private String name;
	private Float held;
	private Float attended;
	private Date absent_dates[];

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

	public String getAbsentDates() {
		String dates = "";
        for(int i=0; i < absent_dates.length ; i++) {
            if(absent_dates[i] == null) continue;
			dates += DateHelper.formatToTechnicalFormat(absent_dates[i]);
            if(i!=absent_dates.length-1)
                dates += ", ";
		}
		return dates;
	}

	public void setAbsentDates(String absentDatesStr) {
		String dates[] = absentDatesStr.split(",");
        absent_dates = new Date[dates.length];
		for(int i=0; i < dates.length ; i++) {
			absent_dates[i] = DateHelper.parseDate(dates[i]);
		}
	}

	public Float getPercentage() {
        return (float) (Math.round( attended / held * 10000.0 ) / 100.0 ) ;
	}

    @Override
    public String toString() {
        return "SubjectModel{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", held=" + held +
                ", attended=" + attended +
                ", absent_dates=" + Arrays.toString(absent_dates) +
                '}';
    }
}
