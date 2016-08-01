package com.bftcom.timesheet.export;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.event.api.*;
import com.atlassian.event.api.EventListener;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.plugin.event.events.PluginEnabledEvent;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.bftcom.timesheet.export.utils.Parser;
import com.bftcom.timesheet.export.utils.Settings;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;


@Component
public class EntryPoint {

    protected EventPublisher eventPublisher;
    protected WorklogEventListener worklogEventListener;
    protected PluginSettingsFactory pluginSettingsFactory;

    @Inject
    public EntryPoint(@ComponentImport EventPublisher eventPublisher, @ComponentImport ActiveObjects activeObjects,
                      @ComponentImport PluginSettingsFactory pluginSettingsFactory) {
        this.eventPublisher = eventPublisher;
        WorklogDataDao dao = new WorklogDataDao(activeObjects);
        WorklogExporter.createInstance(dao);
        WorklogImporter.createInstance(dao);
        this.pluginSettingsFactory = pluginSettingsFactory;
        worklogEventListener = new WorklogEventListener(dao);
        saveDefaultPluginSettings();
    }

    @PostConstruct
    private void initComponent() {
        eventPublisher.register(this);
    }

    @PreDestroy
    private void cleanUp() {
        eventPublisher.unregister(this);
    }

    @EventListener
    public void onPluginEnabled(PluginEnabledEvent event) {
        if (event.getPlugin().getName().equals("jira-timesheet-export-plugin")) {
            main();
        }
    }

    private void saveDefaultPluginSettings() {
        PluginSettings settings = pluginSettingsFactory.createSettingsForKey(Settings.pluginKey);

//        settings.put("startDate", Settings.dateFormat.format(WorklogExportParams.getStartOfCurrentMonth()));
//        settings.put("endDate", Settings.dateFormat.format(WorklogExportParams.getEndOfCurrentMonth()));
        settings.put("exportPeriod", Settings.exportPeriod.toString());
        settings.put("importPeriod", Settings.exportPeriod.toString());
        settings.put("exportDir", Settings.exportDir);
        settings.put("importDir", Settings.importDir);
        settings.put("projects", "[]");
    }

    public void main() {
        PluginSettings pluginSettings = pluginSettingsFactory.createSettingsForKey(Settings.pluginKey);
        Date startDate = Parser.parseDate(pluginSettings.get("startDate"), new Date());
        Date endDate = Parser.parseDate(pluginSettings.get("endDate"), new Date());
        Collection<String> projectKeys = (Collection<String>) pluginSettings.get("projectKeys");
        //todo параметры
        /*Calendar calendarFrom = Calendar.getInstance();
        calendarFrom.set(Calendar.YEAR, 2016);
        calendarFrom.set(Calendar.MONTH, 6);
        calendarFrom.set(Calendar.DAY_OF_MONTH, 26);
        calendarFrom.set(Calendar.HOUR, 0);
        calendarFrom.set(Calendar.MINUTE, 0);
        calendarFrom.set(Calendar.SECOND, 0);

        Calendar calendarTo = Calendar.getInstance();
        calendarTo.set(Calendar.YEAR, 2016);
        calendarTo.set(Calendar.MONTH, 6);
        calendarTo.set(Calendar.DAY_OF_MONTH, 31);
        calendarTo.set(Calendar.HOUR, 0);
        calendarTo.set(Calendar.MINUTE, 0);
        calendarTo.set(Calendar.SECOND, 0);*/

        try {
            WorklogExportParams exportParams = new WorklogExportParams(startDate, endDate).projects(projectKeys);
            //todo ошибка в названии файла
            WorklogExporter.getInstance().exportWorklog(exportParams, Settings.getExportFileName());
            WorklogImporter.getInstance().importWorklog(Settings.importDir);
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventListener
    public void onIssueEvent(IssueEvent event) {
        if (event.getEventTypeId().equals(EventType.ISSUE_WORKLOGGED_ID)) {//created worklog
            worklogEventListener.onWorklogCreated(event.getWorklog());
        } else if (event.getEventTypeId().equals(EventType.ISSUE_WORKLOG_DELETED_ID)) {
            worklogEventListener.onWorklogDeleted(event.getWorklog().getId());
        } else if (event.getEventTypeId().equals(EventType.ISSUE_WORKLOG_UPDATED_ID)){
            worklogEventListener.onWorklogUpdated(event.getWorklog());
        }
    }
}
