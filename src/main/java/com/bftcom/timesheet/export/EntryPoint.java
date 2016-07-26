package com.bftcom.timesheet.export;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.event.api.*;
import com.atlassian.event.api.EventListener;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.plugin.event.events.PluginEnabledEvent;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.util.*;


@Component
public class EntryPoint {

    @ComponentImport
    protected EventPublisher eventPublisher;
    @ComponentImport
    protected ActiveObjects activeObjects;

    protected WorklogExporter exporter;
    protected WorklogImporter importer;
    protected WorklogEventListener worklogEventListener;

    @Inject
    public EntryPoint(EventPublisher eventPublisher, ActiveObjects activeObjects) {
        this.eventPublisher = eventPublisher;
        WorklogDataDao dao = new WorklogDataDao(activeObjects);
        this.exporter = new WorklogExporter(dao);
        this.importer = new WorklogImporter(dao);
        worklogEventListener = new WorklogEventListener(dao);
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

    public void main() {
        //todo параметры
        Calendar calendarFrom = Calendar.getInstance();
        calendarFrom.set(Calendar.YEAR, 2016);
        calendarFrom.set(Calendar.MONTH, 6);
        calendarFrom.set(Calendar.DAY_OF_MONTH, 25);
        calendarFrom.set(Calendar.HOUR, 0);
        calendarFrom.set(Calendar.MINUTE, 0);
        calendarFrom.set(Calendar.SECOND, 0);

        Calendar calendarTo = Calendar.getInstance();
        calendarTo.set(Calendar.YEAR, 2016);
        calendarTo.set(Calendar.MONTH, 6);
        calendarTo.set(Calendar.DAY_OF_MONTH, 26);
        calendarTo.set(Calendar.HOUR, 0);
        calendarTo.set(Calendar.MINUTE, 0);
        calendarTo.set(Calendar.SECOND, 0);

        try {
            exporter.exportWorklog(new WorklogExportParams(calendarFrom.getTime(), calendarTo.getTime()).projects("TEST"), "msg.xml");
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    @EventListener
    public void onIssueEvent(IssueEvent event) {
        if (event.getEventTypeId().equals(EventType.ISSUE_WORKLOGGED_ID)) {//created worklog
            worklogEventListener.onWorklogCreated(event.getWorklog());
        } else if (event.getEventTypeId().equals(EventType.ISSUE_WORKLOG_DELETED_ID)) {
            worklogEventListener.onWorklogDeleted(event.getWorklog().getId());
        } else if (event.getEventTypeId().equals(EventType.ISSUE_COMMENT_EDITED_ID)){
            worklogEventListener.onWorklogUpdated(event.getWorklog());
        }
    }
}
