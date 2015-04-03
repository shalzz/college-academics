package com.shalzz.attendance.model;

public enum Week {

    SUNDAY ("sun","Sunday"),
    MONDAY ("mon","Monday"),
    TUESDAY ("tue","Tuesday"),
    WEDNESDAY ("wed","Wednesday"),
    THURSDAY ("thur","Thursday"),
    FRIDAY ("fri","Friday"),
    SATURDAY ("sat","Saturday");

    private final String technical;
    private final String proper;

    Week(String t, String p) {
        technical = t;
        proper = p;
    }

    public String getTechnical() {
        return technical;
    }

    public static String getTechnical(int i) {
        return Week.values()[i].getTechnical();
    }

    public String getProper() {
        return proper;
    }

    public static String getProper(int i) {
        return Week.values()[i].getProper();
    }
}