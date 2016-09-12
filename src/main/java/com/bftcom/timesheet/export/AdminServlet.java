package com.bftcom.timesheet.export;

import com.atlassian.event.api.*;
import com.atlassian.event.api.EventListener;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.event.events.PluginEnabledEvent;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.scheduler.SchedulerHistoryService;
import com.atlassian.scheduler.SchedulerService;
import com.atlassian.scheduler.config.JobId;
import com.atlassian.scheduler.status.JobDetails;
import com.atlassian.scheduler.status.RunDetails;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.bftcom.timesheet.export.events.AutoExportStartEvent;
import com.bftcom.timesheet.export.events.AutoExportStopEvent;
import com.bftcom.timesheet.export.events.ManualExportStartEvent;
import com.bftcom.timesheet.export.utils.Parser;
import com.bftcom.timesheet.export.utils.Settings;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.*;

@Component
public class AdminServlet extends HttpServlet {

    //    @ComponentImport
    private UserManager userManager;
    //    @ComponentImport
    private LoginUriProvider loginUriProvider;
    private String previousPage;
    private EventPublisher eventPublisher;
    private static AdminServlet previousInstance = null;

    private static Logger logger = LoggerFactory.getLogger(AdminServlet.class);

    //@Inject
    //todo доделать инжект зависимостей, https://bitbucket.org/atlassian/atlassian-spring-scanner
    public AdminServlet(/*UserManager userManager, LoginUriProvider loginUriProvider,
                        TemplateRenderer renderer, PluginSettingsFactory pluginSettingsFactory*/) {
        logger.debug("creating admin servlet");
        this.userManager = ComponentAccessor.getOSGiComponentInstanceOfType(UserManager.class);
        this.loginUriProvider = ComponentAccessor.getOSGiComponentInstanceOfType(LoginUriProvider.class);
        this.eventPublisher = ComponentAccessor.getOSGiComponentInstanceOfType(EventPublisher.class);
        if (previousInstance != null) {
            eventPublisher.unregister(previousInstance);
        }
        previousInstance = this;
        eventPublisher.register(previousInstance);
    }

    @PreDestroy
    public void cleanUp() {
        eventPublisher.unregister(previousInstance);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        logger.debug("requesting config page");
        String user = userManager.getRemoteUsername(request);
        if (user == null || !userManager.isSystemAdmin(user)) {
            redirectToLogin(request, response);
            return;
        }
        previousPage = request.getHeader("Referer");
        logger.debug("previous page : " + previousPage);

        Map<String, Object> params = new HashMap<>();
        params.put("startDate", ""/*pluginSettings.get("startDate")*/);
        params.put("endDate", ""/*pluginSettings.get("endDate")*/);
        params.put("exportPeriod", Settings.get("exportPeriod"));
        params.put("importPeriod", Settings.get("importPeriod"));
        params.put("exportDir", Settings.get("exportDir"));
        params.put("importDir", Settings.get("importDir"));
        params.put("includeAllProjects", Parser.parseBoolean(Settings.get("includeAllProjects"), false));
        params.put("includeAllUsers", Parser.parseBoolean(Settings.get("includeAllUsers"), false));

        RunDetails lastRunExportDetails = ComponentAccessor.getOSGiComponentInstanceOfType(SchedulerHistoryService.class).getLastRunForJob(JobId.of(Settings.exportJobId));
        params.put("lastRunDateExport", lastRunExportDetails != null ? Settings.dateTimeFormat.format(lastRunExportDetails.getStartTime()) : "");
        params.put("lastRunMessageExport", lastRunExportDetails != null ? lastRunExportDetails.getMessage() : "");

        RunDetails lastRunImportDetails = ComponentAccessor.getOSGiComponentInstanceOfType(SchedulerHistoryService.class).getLastRunForJob(JobId.of(Settings.importJobId));
        params.put("lastRunDateImport", lastRunImportDetails != null ? Settings.dateTimeFormat.format(lastRunImportDetails.getStartTime()) : "");
        params.put("lastRunMessageImport", lastRunImportDetails != null ? lastRunImportDetails.getMessage() : "");

        params.put("projects", getProjectNames());
        String[] selectedProjects = Parser.parseArray(Settings.get("projects"));
        params.put("selectedProjects", Arrays.asList(selectedProjects));

        params.put("users", getUserNames());
        String[] selectedUsers = Parser.parseArray(Settings.get("users"));
        params.put("selectedUsers", Arrays.asList(selectedUsers));

        JobDetails exportDetails = ComponentAccessor.getOSGiComponentInstanceOfType(SchedulerService.class).getJobDetails(JobId.of(Settings.exportJobId));
        JobDetails importDetails = ComponentAccessor.getOSGiComponentInstanceOfType(SchedulerService.class).getJobDetails(JobId.of(Settings.importJobId));
        params.put("runningInAutoMode", exportDetails != null && exportDetails.getNextRunTime() != null && importDetails != null && importDetails.getNextRunTime() != null);
        //todo default param?
        //params.put("exportType", Settings.get("exportType"));
        logger.debug("form parametrs : " + params);
        VelocityContext context = new VelocityContext(params);
        VelocityEngine ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        try {
            ve.init();
            Template t = ve.getTemplate("admin.vm", "UTF-8");
//            t.setEncoding("UTF-8");
            response.setContentType("text/html;charset=utf-8");
            t.merge(context, response.getWriter());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        logger.debug("saving configure parameters, parameters = " + req.getParameterMap());
        String action = req.getParameter("submitButton");
        switch (action) {
            //todo i18n
            case "Сохранить":
                saveMainSettings(req);
                break;
            case "Запустить":
                logger.debug("export type = start auto");
                saveMainSettings(req);
                eventPublisher.publish(new AutoExportStartEvent());
                break;
            case "Остановить":
                logger.debug("export type = stop auto");
                saveMainSettings(req);
                eventPublisher.publish(new AutoExportStopEvent());
                break;
            case "Выполнить в ручном режиме":
                logger.debug("export type = manual");

                ManualExportStartEvent event = new ManualExportStartEvent(Parser.parseDate(req.getParameter("startDate"), Settings.getStartOfCurrentMonth()),
                        Parser.parseDate(req.getParameter("endDate"), Settings.getEndOfCurrentMonth()));

                boolean includeAllProjects = req.getParameterMap().containsKey("includeAllProjects") && req.getParameter("includeAllProjects").equalsIgnoreCase("on");
                event.setProjectNames(includeAllProjects ? new String[0] : req.getParameterMap().get("projects"));

                boolean includeAllUsers = req.getParameterMap().containsKey("includeAllUsers") && req.getParameter("includeAllUsers").equalsIgnoreCase("on");
                event.setUserNames(includeAllUsers ? new String[0] : req.getParameterMap().get("users"));
                eventPublisher.publish(event);
                break;
        }
        resp.sendRedirect(previousPage);
    }

    private void saveMainSettings(HttpServletRequest req) {
        Settings.put("exportDir", req.getParameter("exportDir"));
        Settings.put("importDir", req.getParameter("importDir"));
        Settings.put("exportPeriod", req.getParameter("exportPeriod"));
        Settings.put("importPeriod", req.getParameter("importPeriod"));
        Settings.put("includeAllProjects", req.getParameterMap().containsKey("includeAllProjects") && req.getParameter("includeAllProjects").equalsIgnoreCase("on"));
        Settings.put("projects", Arrays.toString(req.getParameterMap().get("projects")));
        Settings.put("includeAllUsers", req.getParameterMap().containsKey("includeAllUsers") && req.getParameter("includeAllUsers").equalsIgnoreCase("on"));
        Settings.put("users", Arrays.toString(req.getParameterMap().get("users")));
    }

    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(loginUriProvider.getLoginUri(getUri(request)).toASCIIString());
    }

    private URI getUri(HttpServletRequest request) {
        StringBuffer builder = request.getRequestURL();
        if (request.getQueryString() != null) {
            builder.append("?");
            builder.append(request.getQueryString());
        }
        return URI.create(builder.toString());
    }

    //todo нужно переделать проекты по аналогии с пользователями.в качестве ключа-ключ проекта, значение - название проекта
    private Collection<String> getProjectNames() {
        ProjectManager projectManager = ComponentAccessor.getProjectManager();
        List<String> projectNames = new ArrayList<>();
        projectManager.getProjects().forEach(p -> {
            projectNames.add(p.getName());
        });
        Collections.sort(projectNames);
        return projectNames;
    }

    private Map<String, String> getUserNames() {
        com.atlassian.jira.user.util.UserManager userManager = ComponentAccessor.getUserManager();
        Collection<ApplicationUser> users = userManager.getAllApplicationUsers();
        Map<String, String> result = new HashMap<>();
        for(ApplicationUser user : users) {
            result.put(user.getName(), user.getDisplayName());
        }
        List<Map.Entry<String,String>> list = new LinkedList<>(result.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, String>>() {
            @Override
            public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
        Map<String, String> sortedResult = new LinkedHashMap<>();
        for (Map.Entry<String, String> user : list) {
            sortedResult.put(user.getKey(), user.getValue());
        }
        return sortedResult;
    }

    @EventListener
    public void onPluginEnabledEvent(PluginEnabledEvent event) {
        //заглушка, чтобы spring не ругался
    }
}