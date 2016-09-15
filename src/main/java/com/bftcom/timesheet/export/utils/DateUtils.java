package com.bftcom.timesheet.export.utils;

import java.util.Calendar;
import java.util.Date;

/**
 */
public class DateUtils {

    public static Date getStartOfDay(Date date) {
        Calendar start = Calendar.getInstance();
        start.setTime(date);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        return start.getTime();
    }

    public static Date getEndOfDay(Date date) {
        Calendar end = Calendar.getInstance();
        end.setTime(date);
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        //end.set(Calendar.MILLISECOND, 999);
        return end.getTime();
    }

    public static Date getStartOfCurrentMonth() {
        Calendar start = Calendar.getInstance();
        start.setTime(new Date());
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        return start.getTime();
    }

    public static Date getEndOfCurrentMonth() {
        Calendar end = Calendar.getInstance();
        end.setTime(new Date());
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        return end.getTime();
    }
}
