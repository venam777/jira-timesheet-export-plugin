package com.bftcom.timesheet.export;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.event.api.*;
import com.atlassian.event.api.EventListener;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.event.events.PluginDisabledEvent;
import com.atlassian.plugin.event.events.PluginEnabledEvent;
import com.atlassian.plugin.event.events.PluginInstalledEvent;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
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
import com.bftcom.timesheet.export.scheduler.FinanceProjectImportPluginJob;
import com.bftcom.timesheet.export.scheduler.ImportPluginJob;
import com.bftcom.timesheet.export.utils.Callback;
import com.bftcom.timesheet.export.utils.DateUtils;
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
import java.io.IOException;
import java.util.*;


@Component
public class EntryPoint {

    protected EventPublisher eventPublisher;
    protected WorklogEventListener worklogEventListener;
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
        this.schedulerService = ComponentAccessor.getOSGiComponentInstanceOfType(SchedulerService.class);
        this.historyService = ComponentAccessor.getOSGiComponentInstanceOfType(SchedulerHistoryService.class);
        this.activeObjects = activeObjects;
        this.worklogEventListener = new WorklogEventListener(new WorklogDataDao(activeObjects));
        WorklogDataDao.createInstance(activeObjects);
        Settings.init(ComponentAccessor.getOSGiComponentInstanceOfType(PluginSettingsFactory.class));
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
        if (checkPluginByName(event.getPlugin().getKey())) {
            logger.debug("plugin was installed");
            Settings.saveDefaultSettings();
        }
    }

    @EventListener
    public void onPluginEnabled(PluginEnabledEvent event) {
        if (checkPluginByName(event.getPlugin().getKey())) {
            logger.debug("plugin was enabled");
            eventPublisher.publish(new AutoExportStartEvent());
        }
    }

    @EventListener
    public void onAutoExportStartEvent(AutoExportStartEvent event) {
        checkComponents();
        schedulerService.registerJobRunner(JobRunnerKey.of(Settings.exportJobKey), new ExportPluginJob(new Callback<WorklogExportParams>() {
            @Override
            public WorklogExportParams call() {
                RunDetails details = historyService.getLastSuccessfulRunForJob(JobId.of(Settings.exportJobId));
                if (details == null) {
                    return new WorklogExportParams(DateUtils.getStartOfCurrentMonth(), DateUtils.getEndOfCurrentMonth()).projects(getProjectsFromSettings()).users(getUsersFromSettings());
                }
                return new WorklogExportParams(details.getStartTime(), DateUtils.getEndOfCurrentMonth()).projects(getProjectsFromSettings()).users(getUsersFromSettings());
            }
        }));
        schedulerService.registerJobRunner(JobRunnerKey.of(Settings.importJobKey), new ImportPluginJob());
        schedulerService.registerJobRunner(JobRunnerKey.of(Settings.financeProjectImportJobKey), new FinanceProjectImportPluginJob());
        try {
            startJob(Parser.parseFloat(Settings.get("exportPeriod"), Settings.getDefault("exportPeriod")), Settings.exportJobKey, Settings.exportJobId);
            startJob(Parser.parseFloat(Settings.get("importPeriod"), Settings.getDefault("importPeriod")), Settings.importJobKey, Settings.importJobId);
            startJob(Parser.parseFloat(Settings.get("importPeriod"), Settings.getDefault("importPeriod")), Settings.financeProjectImportJobKey, Settings.financeProjectImportJobId);
        } catch (SchedulerServiceException e) {
            e.printStackTrace();
        }
    }

    @EventListener
    public void onAutoExportStopEvent(AutoExportStopEvent event) {
        logger.debug("Stopping auto import and export jobs");
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        if (user != null) {
            logger.debug("current user name = " + user.getName());
        }
        schedulerService.unscheduleJob(JobId.of(Settings.exportJobId));
        schedulerService.unscheduleJob(JobId.of(Settings.importJobId));
        schedulerService.unscheduleJob(JobId.of(Settings.financeProjectImportJobId));
        schedulerService.unregisterJobRunner(JobRunnerKey.of(Settings.exportJobKey));
        schedulerService.unregisterJobRunner(JobRunnerKey.of(Settings.importJobKey));
        schedulerService.unregisterJobRunner(JobRunnerKey.of(Settings.financeProjectImportJobKey));
    }

    @EventListener
    public void onManualExportStartEvent(ManualExportStartEvent event) {
        logger.debug("onManualExportStartEvent started");
        checkComponents();
        Collection<String> projects = Arrays.asList(event.getProjectNames());
        logger.debug("projects = " + projects);
        Collection<String> users = Arrays.asList(event.getUserNames());
        logger.debug("users = " + users);
        WorklogExportParams exportParams = new WorklogExportParams(event.startDate, event.endDate).projects(projects)
                .users(users).includeAllStatuses(event.isIncludeAllStatuses()).ignoreExportedFlag(true);
        try {
            WorklogExporter.getInstance().exportWorklog(exportParams);
        } catch (TransformerException | ParserConfigurationException | IOException e) {
            e.printStackTrace();
        }
    }

    private void checkComponents() {
        if (isComponentsInitialized) return;
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
        if (checkPluginByName(event.getPlugin().getKey())) {
            logger.debug("Plugin disabled event fired, stopping auto import and export jobs");
            ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
            if (user != null) {
                logger.debug("current user name = " + user.getName());
            }
            schedulerService.unregisterJobRunner(JobRunnerKey.of(Settings.exportJobKey));
            schedulerService.unregisterJobRunner(JobRunnerKey.of(Settings.importJobKey));
            schedulerService.unregisterJobRunner(JobRunnerKey.of(Settings.financeProjectImportJobKey));
        }
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

    private boolean checkPluginByName(String pluginKey) {
        return pluginKey.equals("com.bftcom.jira-timesheet-export-plugin");
    }

    private Collection<String> getProjectsFromSettings() {
        boolean includeAllProjects = Parser.parseBoolean(Settings.get("includeAllProjects"), false);
        logger.debug("includeAllProjects = " + includeAllProjects);
        return includeAllProjects ? Collections.emptyList() : Arrays.asList(Parser.parseArray(Settings.get("projects")));
    }

    private Collection<String> getUsersFromSettings() {
        boolean includeAllUsers = Parser.parseBoolean(Settings.get("includeAllUsers"), false);
        logger.debug("includeAllUsers = " +includeAllUsers);
        return includeAllUsers ? Collections.emptyList() : Arrays.asList(Parser.parseArray(Settings.get("users")));
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
