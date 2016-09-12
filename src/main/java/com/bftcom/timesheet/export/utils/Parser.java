package com.bftcom.timesheet.export.utils;

import com.bftcom.timesheet.export.entity.WorklogData;

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
        mass = mass.replaceAll(", ", ",");
        return mass.substring(mass.indexOf('[') + 1, mass.lastIndexOf(']')).split(",");
    }

    public static String parseWorklogComment(String dirtyComment) {
        String prefix = "| Статус: ";
        String status1 = prefix + WorklogData.NOT_VIEWED_STATUS;
        String status2 = prefix + WorklogData.APPROVED_STATUS;
        String status3 = prefix + WorklogData.REJECTED_STATUS;
        if (dirtyComment.endsWith(status1) ) {
            return dirtyComment.substring(0, dirtyComment.indexOf(status1));
        } else if (dirtyComment.endsWith(status2)) {
            return dirtyComment.substring(0, dirtyComment.indexOf(status2));
        } else if (dirtyComment.contains(status3)) {//contains не ошибка: если статус отклонено, то после статуса может идти причина отклонения
            return dirtyComment.substring(0, dirtyComment.indexOf(status3));
        }
        return dirtyComment;
    }

    public static boolean parseBoolean(Object stringOrBoolean, boolean defaultParam) {
        if (stringOrBoolean == null || (stringOrBoolean instanceof String && stringOrBoolean.equals(""))) return defaultParam;
        if (stringOrBoolean instanceof Boolean) {
            return (boolean) stringOrBoolean;
        }
        if (stringOrBoolean instanceof String) {
            return ((String) stringOrBoolean).equalsIgnoreCase("true");
        }
        return defaultParam;
    }

}
