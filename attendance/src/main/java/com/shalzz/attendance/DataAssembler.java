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
import android.content.res.Resources;
import android.os.AsyncTask;

import com.shalzz.attendance.model.Day;
import com.shalzz.attendance.model.ListFooter;
import com.shalzz.attendance.model.ListHeader;
import com.shalzz.attendance.model.Period;
import com.shalzz.attendance.model.Subject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

public class DataAssembler {

    public interface ParseListener {

        void onParseComplete(int result);
        void cancelListener();
    }

    public static class ParseStudentDetails extends AsyncTask<String, Void, Integer> {

        private Context mContext;
        private ParseListener mParseListener;

        public ParseStudentDetails(Context context, ParseListener parseListener) {
            mContext = context;
            mParseListener = parseListener;
        }

        protected Integer doInBackground(String... response) {

            Resources resources = mContext.getResources();
            String session_error_identifier = resources.getString(R.string.session_error_identifier);
            String http_tag_title = resources.getString(R.string.http_tag_title);
            String http_tag_div = resources.getString(R.string.http_tag_div);
            String expired_password = resources.getString(R.string.expired_password);

            Document doc = Jsoup.parse(response[0]);
            Elements tddata = doc.select(mContext.getString(R.string.selector_table_data));

            if(doc.getElementsByTag(http_tag_title).size()==0 ||
                    doc.getElementsByTag(http_tag_title).text().equals(session_error_identifier)) {
                return -1;
            }
            else if(doc.getElementsByClass(http_tag_div).text().equals(expired_password)) {
                return -4;
            }
            else if (tddata != null && tddata.size() > 0) {
                int i = 0;
                ListHeader header = new ListHeader();
                for (Element element : tddata) {
                    if (i == 5)
                        header.setName(element.text());
                    else if (i == 8)
                        header.setFatherName(element.text());
                    else if (i == 11)
                        header.setCourse(element.text());
                    else if (i == 14)
                        header.setSection(element.text());
                    else if (i == 17)
                        header.setRollNo(element.text());
                    else if (i == 20)
                        header.setSAPId(Integer.parseInt(element.text()));
                    ++i;
                }

                DatabaseHandler db = new DatabaseHandler(mContext);
                db.addOrUpdateListHeader(header);
                db.close();
            }
            return 0;
        }

        protected void onPostExecute(Integer result) {
            if(mParseListener != null)
                mParseListener.onParseComplete(result);
        }
    }

    /**
     * Extracts Attendance details from the HTML code
     */
    public static class ParseAttendance extends AsyncTask<String, Void, Integer> {

        private Context mContext;
        private ParseListener mParseListener;

        public ParseAttendance(Context context, ParseListener parseListener) {
            mContext = context;
            mParseListener = parseListener;
        }

        protected Integer doInBackground(String... response) {
            return parseAttendance(response[0],mContext);
        }

        protected void onPostExecute(Integer result) {
            if(mParseListener != null)
                mParseListener.onParseComplete(result);
        }
    }

    public static class ParseTimeTable extends AsyncTask<String, Void, Integer> {

        private Context mContext;
        private ParseListener mParseListener;

        public ParseTimeTable(Context context, ParseListener parseListener) {
            mContext = context;
            mParseListener = parseListener;
        }

        protected Integer doInBackground(String... response) {
            return parseTimetable(response[0],mContext);
        }

        protected void onPostExecute(Integer result) {
            if(mParseListener != null)
                mParseListener.onParseComplete(result);
        }
    }

    public static Integer parseAttendance(String response, Context mContext) {

        Resources resources = mContext.getResources();
        String session_error_identifier = resources.getString(R.string.session_error_identifier);
        String unavailable_data_identifier = resources.getString(R.string.unavailable_data_identifier);
        String http_tag_title = resources.getString(R.string.http_tag_title);
        String http_tag_div = resources.getString(R.string.http_tag_div);
        String expired_password = resources.getString(R.string.expired_password);

        DatabaseHandler db = new DatabaseHandler(mContext);

        ArrayList<Float> claHeld = new ArrayList<>();
        ArrayList<Float> claAttended = new ArrayList<>();
        ArrayList<String> abDates = new ArrayList<>();
        ArrayList<String> projPer = new ArrayList<>();
        ArrayList<String> subjectName = new ArrayList<>();
        ArrayList<Float> percentage = new ArrayList<>();

        Document doc = Jsoup.parse(response);

        Elements tddata = doc.select(mContext.getString(R.string.selector_table_data));

        if(doc.getElementsByTag(http_tag_title).size()==0 ||
                doc.getElementsByTag(http_tag_title).text().equals(session_error_identifier)) {
            return -1;
        }
        else if(doc.getElementsByClass(http_tag_div).text().equals(unavailable_data_identifier)) {
            return -2;
        }
        else if(doc.getElementsByClass(http_tag_div).text().equals(expired_password)) {
            return -4;
        }
        else if (tddata != null && tddata.size() > 0)
        {
            int i=0;
            for(Element element : tddata)
            {
                if(i>29)
                {
                    // for subjects
                    if ((i - 30) % 7 == 0) {
                        subjectName.add(element.text());
                    }
                    // for Classes Held
                    else if ((i - 31) % 7 == 0) {
                        claHeld.add(Float.parseFloat(element.text()));
                    }
                    // for Classes attended
                    else if ((i - 32) % 7 == 0) {
                        claAttended.add(Float.parseFloat(element.text()));
                    }
                    // for Dates Absent
                    else if ((i - 33) % 7 == 0) {
                        abDates.add(element.text());
                    }
                    // for attendance percentage
                    else if ((i - 34) % 7 == 0) {
                        percentage.add(Float.parseFloat(element.text()));
                    }
                    // for projected percentage
                    else if ((i - 35) % 7 == 0) {
                        projPer.add(element.text());
                    }
                }
                ++i;
            }

            Elements total = doc.select(mContext.getString(R.string.selector_table_header));
            ListFooter footer = new ListFooter();
            footer.setAttended(Float.parseFloat(total.get(10).text()));
            footer.setHeld(Float.parseFloat(total.get(9).text()));
            footer.setPercentage(Float.parseFloat(total.get(12).text()));
            db.deleteAllSubjects();
            db.addOrUpdateListFooter(footer);

            for(i=0;i<claHeld.size();i++)
            {
                Subject subject = new Subject(i+1,
                        subjectName.get(i),
                        claHeld.get(i),
                        claAttended.get(i),
                        abDates.get(i),
                        percentage.get(i),
                        projPer.get(i));
                db.addSubject(subject);
            }
            db.close();
        }
        return 0;
    }

    public static Integer parseTimetable(String response, Context mContext) {

        Resources resources = mContext.getResources();
        String session_error_identifier = resources.getString(R.string.session_error_identifier);
        String unavailable_timetable_identifier = resources.getString(R.string.unavailable_timetable_identifier);
        String http_tag_title = resources.getString(R.string.http_tag_title);
        String http_tag_div = resources.getString(R.string.http_tag_div);
        String expired_password = resources.getString(R.string.expired_password);

        DatabaseHandler db = new DatabaseHandler(mContext);

        Document doc = Jsoup.parse(response);
        Elements thdata = doc.select(mContext.getString(R.string.selector_table_header));

        ArrayList<String> time = new ArrayList<>();
        ArrayList<String> mon = new ArrayList<>();
        ArrayList<String> tue = new ArrayList<>();
        ArrayList<String> wed = new ArrayList<>();
        ArrayList<String> thur = new ArrayList<>();
        ArrayList<String> fri = new ArrayList<>();
        ArrayList<String> sat = new ArrayList<>();
        String dayNames[] = {"mon","tue","wed","thur","fri","sat"};
        ArrayList<ArrayList<String>> days = new ArrayList<>();
        days.add(mon);
        days.add(tue);
        days.add(wed);
        days.add(thur);
        days.add(fri);
        days.add(sat);

        if(doc.getElementsByTag(http_tag_title).size()==0 ||
                doc.getElementsByTag(http_tag_title).text().equals(session_error_identifier)) {
            return -1;
        }
        else if(doc.getElementsByClass(http_tag_div).text().equals(unavailable_timetable_identifier)) {
            return -3;
        }
        else if(doc.getElementsByClass(http_tag_div).text().equals(expired_password)) {
            return -4;
        }
        else if (thdata != null && thdata.size() > 0) {
            int i=0;
            for(Element element : thdata)
            {
                if(i>8)
                {
                    // get time
                    if ((i - 9) % 7 == 0) {
                        time.add(element.html());
                    }
                    // periods on mon
                    if ((i - 10) % 7 == 0) {
                        mon.add(element.html());
                    }
                    // periods on tue
                    if ((i - 11) % 7 == 0) {
                        tue.add(element.html());
                    }
                    // periods on wed
                    if ((i - 12) % 7 == 0) {
                        wed.add(element.html());
                    }
                    // periods on thur
                    if ((i - 13) % 7 == 0) {
                        thur.add(element.html());
                    }
                    // periods on fri
                    if ((i - 14) % 7 == 0) {
                        fri.add(element.html());
                    }
                    // periods on sat
                    if ((i - 15) % 7 == 0) {
                        sat.add(element.html());
                    }
                }
                ++i;
            }

            db.deleteAllPeriods();
            for(int j=0;j<days.size();j++)
            {
                ArrayList<String> dayofweek = days.get(j);
                Day day = new Day();
                for(i=0;i<time.size();i++)
                {
                    String[] parts = dayofweek.get(i).split("<br />");
                    int index = time.get(i).indexOf("-");
                    String start = time.get(i).substring(0,index);
                    String end = time.get(i).substring(index+1);
                    while(i+1<time.size() && dayofweek.get(i).equals(dayofweek.get(i + 1))) {
                        index = time.get(i+1).indexOf("-");
                        end = time.get(i+1).substring(index+1);
                        i++;
                    }
                    Period period = new Period();
                    Period period1 = new Period();
                    if(!dayofweek.get(i).equals("***")) {
                        if(parts.length==7) {
                            String batch = parts[0].substring(parts[0].indexOf('-')+1);
                            period.setTeacher(parts[1].replaceAll("&amp;", "&"));
                            period.setSubjectName(parts[2].replaceAll("&amp;", "&"));
                            period.setRoom(parts[3].split("<hr")[0].replaceAll("&amp;", "&"));
                            period.setBatch(batch);
                            batch = parts[3].split("<hr")[1].substring(parts[3].split("<hr")[1].indexOf('-')+1);
                            period1.setTeacher(parts[4].replaceAll("&amp;", "&"));
                            period1.setSubjectName(parts[5].replaceAll("&amp;", "&"));
                            period1.setRoom(parts[6].replaceAll("&amp;", "&"));
                            period1.setBatch(batch);
                        }
                        else if (!parts[0].isEmpty()) {
                            String batch = parts[0].substring(parts[0].indexOf('-')+1);
                            period.setTeacher(parts[1].replaceAll("&amp;", "&"));
                            period.setSubjectName(parts[2].replaceAll("&amp;", "&"));
                            period.setRoom(parts[3].replaceAll("&amp;", "&"));
                            period.setBatch(batch);
                        }
                        else {
                            period.setTeacher(parts[1].replaceAll("&amp;", "&"));
                            period.setSubjectName(parts[2].replaceAll("&amp;", "&"));
                            period.setRoom(parts[3].replaceAll("&amp;", "&"));
                        }
                    }
                    period.setDay(dayNames[j]);
                    period.setTime(start, end);
                    period1.setDay(dayNames[j]);
                    period1.setTime(start, end);
                    int count = day.getCount();
                    if(count>0 ) {
                        Period period0 = day.getPeriod(count - 1);
                        if(period.isEqual(period0) || period1.isEqual(period0)) {
                            if (period.isEqual(period0))
                                period0.setTime(period0.getStartTime(), period.getEndTime());
                            if (period1.isEqual(period0))
                                period0.setTime(period0.getStartTime(), period1.getEndTime());
                            continue;
                        }
                    }
                    if(!period.getSubjectName().isEmpty()) {
                        day.addPeriod(period);
                    }
                    if(parts.length==7) {
                        day.addPeriod(period1);
                    }
                }
                db.addDay(day);
            }
            db.close();
        }
        return 0;
    }

}
