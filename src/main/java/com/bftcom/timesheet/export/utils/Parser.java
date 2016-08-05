package com.bftcom.timesheet.export.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Parser {

    public static Date parseDate(Object dateOrString, Date defaultParam) {
        if (dateOrString == null) return defaultParam;
        if (dateOrString instanceof Date) return (Date) dateOrString;
        try {
            return Settings.dateFormat.parse((String) dateOrString);
        } catch (ParseException e) {
            e.printStackTrace();
            return defaultParam;
        }
    }

    public static String formatDate(Date date) {
        return Settings.dateFormat.format(date);
    }

    public static String formatDateTime(Date date) {
        return Settings.dateTimeFormat.format(date);
    }

    public static Float parseFloat(Object floatOrString, Float defaultParam) {
        if (floatOrString == null) return defaultParam;
        if (floatOrString instanceof Float) return (Float) floatOrString;
        try {
            return Float.parseFloat((String) floatOrString);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return defaultParam;
        }
    }

    public static String[] parseArray(String mass) {
        if (mass == null || mass.equals("") || mass.equals("null")) return new String[0];
        return mass.substring(mass.indexOf('['), mass.lastIndexOf(']')).split(",");
    }

}
