package com.bftcom.timesheet.export;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.bftcom.timesheet.export.utils.Parser;
import com.bftcom.timesheet.export.utils.Settings;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
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

    //@Inject
    //todo доделать инжект зависимостей, https://bitbucket.org/atlassian/atlassian-spring-scanner
    public AdminServlet(/*UserManager userManager, LoginUriProvider loginUriProvider,
                        TemplateRenderer renderer, PluginSettingsFactory pluginSettingsFactory*/) {
        this.userManager = ComponentAccessor.getOSGiComponentInstanceOfType(UserManager.class);
        this.loginUriProvider = ComponentAccessor.getOSGiComponentInstanceOfType(LoginUriProvider.class);
        this.renderer = ComponentAccessor.getOSGiComponentInstanceOfType(TemplateRenderer.class);
        this.pluginSettingsFactory = ComponentAccessor.getOSGiComponentInstanceOfType(PluginSettingsFactory.class);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        UserProfile user = userManager.getRemoteUser(request);
        if (user == null || !userManager.isSystemAdmin(user.getUserKey())) {
            redirectToLogin(request, response);
            return;
        }
        previousPage = request.getHeader("Referer");
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

        response.setContentType("text/html;charset=utf-8");
        renderer.render("admin.vm", params, response.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PluginSettings settings = pluginSettingsFactory.createSettingsForKey(Settings.pluginKey);
        if (req.getParameter("exportType").equals("manual")) {
            Date startDate = Parser.parseDate(req.getParameter("startDate"), WorklogExportParams.getStartOfCurrentMonth());
            Date endDate = Parser.parseDate(req.getParameter("endDate"), WorklogExportParams.getEndOfCurrentMonth());
            try {
                WorklogExportParams params = new WorklogExportParams(startDate, endDate);
                params.projects(req.getParameterMap().get("projects"));
                WorklogExporter.getInstance().exportWorklog(params, Settings.getExportFileName());
            } catch (TransformerException | ParserConfigurationException e) {
                e.printStackTrace();
                //todo error popup
            }
        } else {
            settings.put("exportPeriod", req.getParameter("exportPeriod"));
            settings.put("importPeriod", req.getParameter("importPeriod"));
            settings.put("exportType", req.getParameter("exportType"));
            settings.put("projects", Arrays.toString(req.getParameterMap().get("projects")));
            resp.sendRedirect(previousPage);
        }
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


}