package com.bftcom.timesheet.export;

import com.atlassian.event.api.*;
import com.atlassian.event.api.EventListener;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.plugin.event.events.PluginEnabledEvent;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.bftcom.timesheet.export.events.AutoExportStartEvent;
import com.bftcom.timesheet.export.events.AutoExportStopEvent;
import com.bftcom.timesheet.export.events.ManualExportStartEvent;
import com.bftcom.timesheet.export.utils.Settings;
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
    //    @ComponentImport
    private TemplateRenderer renderer;
    //    @ComponentImport
    private PluginSettingsFactory pluginSettingsFactory;
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
        this.renderer = ComponentAccessor.getOSGiComponentInstanceOfType(TemplateRenderer.class);
        this.pluginSettingsFactory = ComponentAccessor.getOSGiComponentInstanceOfType(PluginSettingsFactory.class);
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
        PluginSettings pluginSettings = pluginSettingsFactory.createSettingsForKey(Settings.pluginKey);
        Map<String, Object> params = new HashMap<>();
        params.put("startDate", ""/*pluginSettings.get("startDate")*/);
        params.put("endDate", ""/*pluginSettings.get("endDate")*/);
        params.put("exportPeriod", pluginSettings.get("exportPeriod"));
        params.put("importPeriod", pluginSettings.get("importPeriod"));
        params.put("exportDir", pluginSettings.get("exportDir"));
        params.put("importDir", pluginSettings.get("importDir"));
        ProjectManager projectManager = ComponentAccessor.getProjectManager();
        List<String> projectNames = new ArrayList<>();
        projectManager.getProjects().forEach(p -> {
            projectNames.add(p.getName());
        });
        Collections.sort(projectNames);
        params.put("projects", projectNames);
        params.put("exportType", pluginSettings.get("exportType"));
        logger.debug("form parametrs : " + params);
        response.setContentType("text/html;charset=utf-8");
        renderer.render("admin.vm", params, response.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        logger.debug("saving configure parameters, parameters = " + req.getParameterMap());
        PluginSettings settings = pluginSettingsFactory.createSettingsForKey(Settings.pluginKey);
        String action = req.getParameter("submitButton");
        switch (action) {
            case "Запустить":
                logger.debug("export type = start auto");
                //            settings.put("exportPeriod", req.getParameter("exportPeriod"));
//            settings.put("importPeriod", req.getParameter("importPeriod"));
                //settings.put("exportType", req.getParameter("exportType"));

                settings.put("projects", Arrays.toString(req.getParameterMap().get("projects")));
                eventPublisher.publish(new AutoExportStartEvent());
                //resp.sendRedirect(previousPage);
                break;
            case "Остановить":
                logger.debug("export type = stop auto");
                eventPublisher.publish(new AutoExportStopEvent());
                break;
            case "Выполнить в ручном режиме":
                logger.debug("export type = manual");
                settings.put("startDate", req.getParameter("startDate"));
                settings.put("endDateDate", req.getParameter("endDateDate"));
                settings.put("projects", Arrays.toString(req.getParameterMap().get("projects")));
                eventPublisher.publish(new ManualExportStartEvent());
                  /*Date startDate = Parser.parseDate(req.getParameter("startDate"), WorklogExportParams.getStartOfCurrentMonth());
            Date endDate = Parser.parseDate(req.getParameter("endDate"), WorklogExportParams.getEndOfCurrentMonth());
            logger.debug("start date = " + startDate + ", end date = " + endDate);
            try {
                WorklogExportParams params = new WorklogExportParams(startDate, endDate);
                params.projects(req.getParameterMap().get("projects"));
                logger.debug("projects = " + Arrays.toString(req.getParameterMap().get("projects")));
                WorklogExporter.getInstance().exportWorklog(params, Settings.getExportFileName());
            } catch (TransformerException | ParserConfigurationException e) {
                e.printStackTrace();
                //todo error popup
            }*/
                break;
        }
        resp.sendRedirect(previousPage);
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

    @EventListener
    public void onPluginEnabledEvent(PluginEnabledEvent event) {
        int i = 5;
    }
}