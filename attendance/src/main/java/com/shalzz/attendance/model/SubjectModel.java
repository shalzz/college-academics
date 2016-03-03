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

import com.shalzz.attendance.wrapper.DateHelper;

import java.util.ArrayList;
import java.util.Collections;
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
        this.absent_dates = dates;
    }

	public String getAbsentDatesAsString() {
		String datesStr = "";
        for(int i = 0; i < absent_dates.size() ; i++) {
            Collections.sort(absent_dates);
			datesStr += DateHelper.formatToTechnicalFormat(absent_dates.get(i));
            if(i!= absent_dates.size()-1)
                datesStr += ", ";
		}
		return datesStr;
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
