package com.bftcom.timesheet.export;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.bftcom.timesheet.export.utils.DateUtils;

import java.util.*;

/**
 * Параметры, по которым выбираются таймшиты из джиры
 */
public class WorklogExportParams {

    private Collection<Project> projects;
    private Collection<ApplicationUser> users;
    private Date startDate;
    private Date endDate;
    private boolean includeAllStatuses;
    //todo
    //бюджеты

    public WorklogExportParams(Date startDate, Date endDate) {
        this.startDate = DateUtils.getStartOfDay(startDate);
        this.endDate = DateUtils.getEndOfDay(endDate);
    }

    public WorklogExportParams projects(Collection<String> projectNames) {
        if (projectNames == null || projectNames.size() == 0) {
            this.projects = Collections.emptyList();
        } else {
            this.projects = transformProjects((String[]) projectNames.toArray());
        }
        return this;
    }

    public WorklogExportParams users(Collection<String> userNames) {
        if (userNames == null || userNames.size() == 0) {
            this.users = Collections.emptyList();
        } else {
            this.users = transformUsers((String[]) userNames.toArray());
        }
        return this;
    }

    public WorklogExportParams includeAllStatuses(boolean includeAllStatuses) {
        this.includeAllStatuses = includeAllStatuses;
        return this;
    }

    protected Collection<Project> transformProjects(String... projectNames) {
        ProjectManager projectManager = ComponentAccessor.getProjectManager();
        List<Project> projectList = new ArrayList<>();
        for (String name : projectNames) {
            Project project = projectManager.getProjectObjByName(name);
            if (project != null) {
                projectList.add(project);
            }
        }
        return projectList;
    }

    protected Collection<ApplicationUser> transformUsers(String... userNames) {
        UserManager userManager = ComponentAccessor.getUserManager();
        List<ApplicationUser> userList = new ArrayList<>();
        for (String name : userNames) {
            ApplicationUser user = userManager.getUserByName(name);
            if (user != null) {
                userList.add(user);
            }
        }
        return userList;
    }

    public Collection<Project> getProjects() {
        return projects;
    }

    public Collection<ApplicationUser> getUsers() {
        return users;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public boolean isIncludeAllStatuses() {
        return includeAllStatuses;
    }
}
