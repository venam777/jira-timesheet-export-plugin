package com.bftcom.timesheet.export.utils;


import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Settings {
    //========== static immutable plugin settings ============
    public static final String pluginKey = "jira-timesheet-export-plugin";
    public static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    public static final DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH.mm.s");
    public static final String exportJobKey = pluginKey + ".EXPORT";
    public static final String importJobKey = pluginKey + ".IMPORT";

    public static final String exportJobId = exportJobKey + ".0";
    public static final String importJobId = importJobKey + ".0";

    public static final boolean deleteFilesAfterImport = true;
    public static String importEncoding = "windows-1251";//UTF-8
    //========================================================

    private static Map<String, Object> defaultParams = new HashMap<>();
    static {
        defaultParams.put("exportPeriod", 60 / 60f);
        defaultParams.put("importPeriod", 30 / 60f);
        defaultParams.put("exportDir", "/mnt/pm/import/");
        defaultParams.put("importDir", "/mnt/pm/proexport/");
//        defaultParams.put("exportDir", "//pm/exchange/JIRA/import/");
//        defaultParams.put("importDir", "//pm/exchange/JIRA/proexport/");
        defaultParams.put("includeAllProjects", true);
        defaultParams.put("projects", "[]");
        defaultParams.put("includeAllUsers", true);
        defaultParams.put("users", "[]");
        defaultParams.put("includeAllStatuses", false);
    }

    private static PluginSettingsFactory factory;

    public static <T> T get(String key) {
        check();
        PluginSettings settings = factory.createSettingsForKey(pluginKey);
        return (T) settings.get(key);
    }

    public static <T> T getDefault(String key) {
        return (T) defaultParams.get(key);
    }

    public static void put(String key, Object value) {
        check();
        PluginSettings settings = factory.createSettingsForKey(pluginKey);
        settings.put(key, value != null ? (value instanceof String ? value : value.toString()) : null);
    }

    public static void saveDefaultSettings() {
        check();
        for (String key : defaultParams.keySet()) {
            put(key, defaultParams.get(key));
        }
    }

    public static String getExportFileName() {
        return get("exportDir") + "worklog_export_" + new SimpleDateFormat("yyyy-MM-dd_HH.mm.s").format(new Date()) + ".xml";
    }

    private static void check() {
        if (factory == null) {
            throw new IllegalStateException("Settings must be initialized by calling init(factory) before using!");
        }
    }

    public static void init(PluginSettingsFactory pluginSettingsFactory) {
        factory = pluginSettingsFactory;
        saveDefaultSettings();
    }
}
