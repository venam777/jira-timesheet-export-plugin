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
    public static final Float exportPeriod = 5 / 60f;
    public static final Float importPeriod = 2 / 60f;

    public static final String exportJobKey = pluginKey + ".EXPORT";
    public static final String importJobKey = pluginKey + ".IMPORT";

    public static final String exportJobId = exportJobKey + ".0";
    public static final String importJobId = importJobKey + ".0";

    public static final boolean deleteFilesAfterImport = true;

    public static String getExportFileName() {
        return exportDir + "worklog_export_" + new SimpleDateFormat("yyyy-MM-dd_HH.mm.s").format(new Date()) + ".xml";
    }
}
