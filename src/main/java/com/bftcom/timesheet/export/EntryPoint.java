package com.bftcom.timesheet.export;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.event.api.*;
import com.atlassian.event.api.EventListener;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.plugin.event.events.PluginDisabledEvent;
import com.atlassian.plugin.event.events.PluginEnabledEvent;
import com.atlassian.plugin.event.events.PluginInstalledEvent;
import com.atlassian.plugin.event.events.PluginModuleEnabledEvent;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.scheduler.SchedulerHistoryService;
import com.atlassian.scheduler.SchedulerService;
import com.atlassian.scheduler.SchedulerServiceException;
import com.atlassian.scheduler.config.*;
import com.atlassian.scheduler.status.RunDetails;
import com.bftcom.timesheet.export.scheduler.ExportPluginJob;
import com.bftcom.timesheet.export.scheduler.ImportJobRunner;
import com.bftcom.timesheet.export.utils.Callback;
import com.bftcom.timesheet.export.utils.Parser;
import com.bftcom.timesheet.export.utils.Settings;
import com.google.common.collect.ImmutableMap;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.*;


@Component
public class EntryPoint {

    protected EventPublisher eventPublisher;
    protected WorklogEventListener worklogEventListener;
    protected PluginSettingsFactory pluginSettingsFactory;
    protected SchedulerService schedulerService;
    protected SchedulerHistoryService historyService;

    protected final int startDelayInMinutes = 2;

    public EntryPoint() {
        this.eventPublisher = ComponentAccessor.getOSGiComponentInstanceOfType(EventPublisher.class);
        WorklogDataDao dao = new WorklogDataDao(ComponentAccessor.getOSGiComponentInstanceOfType(ActiveObjects.class));
        WorklogExporter.createInstance(dao);
        WorklogImporter.createInstance(dao);
        this.pluginSettingsFactory = ComponentAccessor.getOSGiComponentInstanceOfType(PluginSettingsFactory.class);
        this.schedulerService = ComponentAccessor.getOSGiComponentInstanceOfType(SchedulerService.class);
        this.historyService = ComponentAccessor.getOSGiComponentInstanceOfType(SchedulerHistoryService.class);
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
    public void pluginInstalled(PluginInstalledEvent event) {
        if (checkPluginByName(event.getPlugin().getName())) {
            saveDefaultPluginSettings();
        }
    }

    @EventListener
    public void onPluginEnabled(PluginEnabledEvent event) {
        if (checkPluginByName(event.getPlugin().getName())) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    schedulerService.registerJobRunner(JobRunnerKey.of(Settings.exportJobKey), new ExportPluginJob(new Callback<WorklogExportParams>() {
                        @Override
                        public WorklogExportParams call() {
                            RunDetails details = historyService.getLastSuccessfulRunForJob(JobId.of(Settings.exportJobId));
                            if (details == null) {
                                return new WorklogExportParams(WorklogExportParams.getStartOfCurrentMonth(), WorklogExportParams.getEndOfCurrentMonth());
                            }
                            return new WorklogExportParams(details.getStartTime(), WorklogExportParams.getEndOfCurrentMonth());
                        }
                    }));
                    schedulerService.registerJobRunner(JobRunnerKey.of(Settings.importJobKey), new ImportJobRunner());
                    PluginSettings pluginSettings = pluginSettingsFactory.createSettingsForKey(Settings.pluginKey);
                    try {
                        startJob(Parser.parseFloat(pluginSettings.get("exportPeriod"), Settings.exportPeriod), Settings.exportJobKey, Settings.exportJobId);
                        startJob(Parser.parseFloat(pluginSettings.get("importPeriod"), Settings.importPeriod), Settings.importJobKey, Settings.importJobId);
                    } catch (SchedulerServiceException e) {
                        e.printStackTrace();
                    }
                }
            }, startDelayInMinutes * 60 * 1000);
        }
    }

    @EventListener
    public void onPluginDisabled(PluginDisabledEvent event) {
        if (checkPluginByName(event.getPlugin().getName())) {
            schedulerService.unregisterJobRunner(JobRunnerKey.of(Settings.exportJobKey));
            schedulerService.unregisterJobRunner(JobRunnerKey.of(Settings.importJobKey));
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

    private void startJob(Float periodInHours, String jobRunnerKey, String jobId) throws SchedulerServiceException {
        JobConfig config = JobConfig.forJobRunnerKey(JobRunnerKey.of(jobRunnerKey))
                .withSchedule(Schedule.forInterval((long) (periodInHours * 60 * 60 * 1000), new Date()))
                .withParameters(ImmutableMap.of())
                .withRunMode(RunMode.RUN_LOCALLY);
        schedulerService.scheduleJob(JobId.of(jobId), config);
    }

    private boolean checkPluginByName(String pluginName) {
        return pluginName.equals("jira-timesheet-export-plugin");
    }

    @EventListener
    public void onIssueEvent(IssueEvent event) {
        if (event.getEventTypeId().equals(EventType.ISSUE_WORKLOGGED_ID)) {//created worklog
            worklogEventListener.onWorklogCreated(event.getWorklog());
        } else if (event.getEventTypeId().equals(EventType.ISSUE_WORKLOG_DELETED_ID)) {
            worklogEventListener.onWorklogDeleted(event.getWorklog().getId());
        } else if (event.getEventTypeId().equals(EventType.ISSUE_WORKLOG_UPDATED_ID)) {
            worklogEventListener.onWorklogUpdated(event.getWorklog());
        }
    }
}
