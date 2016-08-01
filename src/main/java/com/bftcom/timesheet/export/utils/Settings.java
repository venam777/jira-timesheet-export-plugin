package com.bftcom.timesheet.export.utils;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Settings {
    public static final String pluginKey = "jira-timesheet-export-plugin";
    public static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    public static final DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH.mm.s");
    public static final String exportDir = "//pm/exchange/JIRA/import/";
    public static final String importDir = "//pm/exchange/JIRA/proexport/";
    public static final Float exportPeriod = 1f;
    public static final Float importPeriod = 5 / 60f;
    public static String getExportFileName() {
        return exportDir + "worklog_export_" + dateTimeFormat.format(new Date()) + ".xml";
    }
}
