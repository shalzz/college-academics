package com.shalzz.attendance.data.local;

import com.shalzz.attendance.wrapper.DateHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.room.TypeConverter;

public class Converters {

    @NonNull
    @TypeConverter
    public Date fromString(String databaseValue) {
        return DateHelper.parseDate(databaseValue);
    }

    @NonNull
    @TypeConverter
    public String dateToString(@NonNull Date value) {
        return DateHelper.toTechnicalFormat(value);
    }

    @NonNull
    @TypeConverter
    public static List<Date> fromDateListString(@NonNull String databaseValue) {
        List<Date> dates = new ArrayList<>();
        for(String date : databaseValue.split(",")) {
            dates.add(DateHelper.parseDate(date));
        }
        return dates;
    }

    @NonNull
    @TypeConverter
    public static String dateListToString(@NonNull List<Date> value) {
        String SEPARATOR = ",";
        StringBuilder csvBuilder = new StringBuilder();
        for(Date date : value){
            csvBuilder.append(DateHelper.toTechnicalFormat(date));
            csvBuilder.append(SEPARATOR);
        }

        String csv = csvBuilder.toString();
        //Remove last comma
        csv = csv.substring(0, csv.length() - SEPARATOR.length());

        return csv;
    }
}
