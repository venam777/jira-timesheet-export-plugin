package com.bftcom.timesheet.export;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.event.api.*;
import com.atlassian.event.api.EventListener;
import com.atlassian.jira.action.JiraActionSupport;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.web.action.IssueActionSupport;
import com.atlassian.jira.web.action.issue.UpdateWorklog;
import com.atlassian.plugin.event.events.PluginDisabledEvent;
import com.atlassian.plugin.event.events.PluginInstalledEvent;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.scheduler.SchedulerHistoryService;
import com.atlassian.scheduler.SchedulerService;
import com.atlassian.scheduler.SchedulerServiceException;
import com.atlassian.scheduler.config.*;
import com.atlassian.scheduler.status.RunDetails;
import com.bftcom.timesheet.export.events.AutoExportStartEvent;
import com.bftcom.timesheet.export.events.AutoExportStopEvent;
import com.bftcom.timesheet.export.events.ManualExportStartEvent;
import com.bftcom.timesheet.export.scheduler.ExportPluginJob;
import com.bftcom.timesheet.export.scheduler.ImportPluginJob;
import com.bftcom.timesheet.export.utils.Callback;
import com.bftcom.timesheet.export.utils.Parser;
import com.bftcom.timesheet.export.utils.Settings;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.util.*;


@Component
public class EntryPoint {

    protected EventPublisher eventPublisher;
    protected WorklogEventListener worklogEventListener;
    protected PluginSettingsFactory pluginSettingsFactory;
    protected SchedulerService schedulerService;
    protected SchedulerHistoryService historyService;
    protected ActiveObjects activeObjects;
    private static boolean isComponentsInitialized = false;
    private static Logger logger = LoggerFactory.getLogger(EntryPoint.class);

    //если не инжектить ActiveObject через констуктор - будут сыпаться исключения, что AO модуль не готов к использованию
    @Inject
    public EntryPoint(@ComponentImport ActiveObjects activeObjects) {
        logger.debug("Entry point start creating");
        this.eventPublisher = ComponentAccessor.getOSGiComponentInstanceOfType(EventPublisher.class);
        this.pluginSettingsFactory = ComponentAccessor.getOSGiComponentInstanceOfType(PluginSettingsFactory.class);
        this.schedulerService = ComponentAccessor.getOSGiComponentInstanceOfType(SchedulerService.class);
        this.historyService = ComponentAccessor.getOSGiComponentInstanceOfType(SchedulerHistoryService.class);
        this.activeObjects = activeObjects;
        this.worklogEventListener = new WorklogEventListener(new WorklogDataDao(activeObjects));
        WorklogDataDao.createInstance(activeObjects);
        saveDefaultPluginSettings();
        logger.debug("Entry point finish creating");
    }

    @PostConstruct
    private void initComponent() {
        eventPublisher.register(this);
        logger.debug("event publisher registered EntryPoint");
    }

    @PreDestroy
    private void cleanUp() {
        eventPublisher.unregister(this);
        logger.debug("event publisher unregistered EntryPoint");
    }

    @EventListener
    public void pluginInstalled(PluginInstalledEvent event) {
        if (checkPluginByName(event.getPlugin().getName())) {
            logger.debug("plugin was installed");
            saveDefaultPluginSettings();
        }
    }

   /* @EventListener
    public void onPluginEnabled(PluginEnabledEvent event) {
        if (checkPluginByName(event.getPlugin().getName())) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {

                }
            }, startDelayInMinutes * 60 * 1000);
        }
    }*/

    @EventListener
    public void onAutoExportStartEvent(AutoExportStartEvent event) {
        checkComponents();
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
        schedulerService.registerJobRunner(JobRunnerKey.of(Settings.importJobKey), new ImportPluginJob());
        PluginSettings pluginSettings = getSettings();
        try {
            startJob(Parser.parseFloat(pluginSettings.get("exportPeriod"), Settings.exportPeriod), Settings.exportJobKey, Settings.exportJobId);
            startJob(Parser.parseFloat(pluginSettings.get("importPeriod"), Settings.importPeriod), Settings.importJobKey, Settings.importJobId);
        } catch (SchedulerServiceException e) {
            e.printStackTrace();
        }
    }

    @EventListener
    public void onAutoExportStopEvent(AutoExportStopEvent event) {
        schedulerService.unscheduleJob(JobId.of(Settings.exportJobId));
        schedulerService.unscheduleJob(JobId.of(Settings.importJobId));
        schedulerService.unregisterJobRunner(JobRunnerKey.of(Settings.exportJobKey));
        schedulerService.unregisterJobRunner(JobRunnerKey.of(Settings.importJobKey));
    }

    @EventListener
    public void onManualExportStartEvent(ManualExportStartEvent event) {
        checkComponents();
        PluginSettings settings = getSettings();
        Date startDate = Parser.parseDate(settings.get("startDate"), WorklogExportParams.getStartOfCurrentMonth());
        Date endDate = Parser.parseDate(settings.get("endDate"), WorklogExportParams.getEndOfCurrentMonth());
        String[] projects = Parser.parseArray((String) settings.get("projects"));
        WorklogExportParams exportParams = new WorklogExportParams(startDate, endDate).projects(projects);
        try {
            WorklogExporter.getInstance().exportWorklog(exportParams);
        } catch (TransformerException | ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    private void checkComponents() {
        if (isComponentsInitialized) return;
        //ActiveObjects activeObjects = ComponentAccessor.getOSGiComponentInstanceOfType(ActiveObjects.class);
        if (activeObjects == null) {
            throw new NullPointerException("Active objects is null!");
        }
        WorklogDataDao dao = new WorklogDataDao(activeObjects);
        WorklogExporter.createInstance(dao);
        WorklogImporter.createInstance(dao);
        worklogEventListener = new WorklogEventListener(dao);
    }

    @EventListener
    public void onPluginDisabled(PluginDisabledEvent event) {
        if (checkPluginByName(event.getPlugin().getName())) {
            schedulerService.unregisterJobRunner(JobRunnerKey.of(Settings.exportJobKey));
            schedulerService.unregisterJobRunner(JobRunnerKey.of(Settings.importJobKey));
        }
    }

    private void saveDefaultPluginSettings() {
        logger.debug("saving default settings");
        PluginSettings settings = getSettings();
        if (settings == null) {
            logger.debug("There was an error while creating plugin settings, settings are null");
            return;
        }
//        settings.put("startDate", Settings.dateFormat.format(WorklogExportParams.getStartOfCurrentMonth()));
//        settings.put("endDate", Settings.dateFormat.format(WorklogExportParams.getEndOfCurrentMonth()));
        settings.put("exportPeriod", Settings.exportPeriod.toString());
        settings.put("importPeriod", Settings.exportPeriod.toString());
        settings.put("exportDir", Settings.exportDir);
        settings.put("importDir", Settings.importDir);
        settings.put("projects", "[]");
    }

    private void startJob(Float periodInHours, String jobRunnerKey, String jobId) throws SchedulerServiceException {
        logger.debug("start job, period = " + periodInHours + ", jobRunnerKey = " + jobRunnerKey + ", jobId = " + jobId);
        JobConfig config = JobConfig.forJobRunnerKey(JobRunnerKey.of(jobRunnerKey))
                .withSchedule(Schedule.forInterval((long) (periodInHours * 60 * 60 * 1000), new Date()))
                .withParameters(ImmutableMap.of())
                .withRunMode(RunMode.RUN_LOCALLY);
        logger.debug("Job config created, config = " + config.toString() + ", params = " + config.getParameters());
        schedulerService.scheduleJob(JobId.of(jobId), config);
        logger.debug("job scheduled");
    }

    private boolean checkPluginByName(String pluginName) {
        return pluginName.equals("jira-timesheet-export-plugin");
    }

    private PluginSettings getSettings() {
        return pluginSettingsFactory.createSettingsForKey(Settings.pluginKey);
    }

    @EventListener
    public void onIssueEvent(IssueEvent event) {
        if (worklogEventListener == null) {
            return;
        }
        logger.debug("onIssueEvent started");
        if (event.getEventTypeId().equals(EventType.ISSUE_WORKLOGGED_ID)) {//created worklog
            logger.debug("Issue has new worklog");
            worklogEventListener.onWorklogCreated(event.getWorklog());
        } else if (event.getEventTypeId().equals(EventType.ISSUE_WORKLOG_DELETED_ID)) {
            logger.debug("Issue's worklog was deleted");
            worklogEventListener.onWorklogDeleted(event.getWorklog().getId());
        } else if (event.getEventTypeId().equals(EventType.ISSUE_WORKLOG_UPDATED_ID)) {
            logger.debug("Issue's worklog was updated");
            worklogEventListener.onWorklogUpdated(event.getWorklog());
        }
    }
}
