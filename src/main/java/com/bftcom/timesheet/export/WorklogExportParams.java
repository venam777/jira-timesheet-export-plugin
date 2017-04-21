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
    private Date worklogStartDate;
    private Date worklogEndDate;
    private Collection<String> issueKeys;
    private boolean includeAllStatuses;
    private boolean ignoreExportedFlag;
    //todo
    //бюджеты

    public WorklogExportParams(Date startDate, Date endDate) {
        this.startDate = startDate != null ? DateUtils.getStartOfDay(startDate) : null;
        this.endDate = endDate != null ? DateUtils.getEndOfDay(endDate) : null;
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

    public WorklogExportParams worklogDates(Date worklogStartDate, Date worklogEndDate) {
        this.worklogStartDate = worklogStartDate;
        this.worklogEndDate = worklogEndDate;
        return this;
    }

    public WorklogExportParams issueKeys(String... issueKeys) {
        this.issueKeys = Arrays.asList(issueKeys);
        return this;
    }

    public WorklogExportParams issueKeys(Collection<String> issueKeys) {
        this.issueKeys = issueKeys;
        return this;
    }

    public WorklogExportParams includeAllStatuses(boolean includeAllStatuses) {
        this.includeAllStatuses = includeAllStatuses;
        return this;
    }

    public WorklogExportParams ignoreExportedFlag(boolean ignoreExportedFlag) {
        this.ignoreExportedFlag = ignoreExportedFlag;
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

    public Date getWorklogStartDate() {
        return worklogStartDate;
    }

    public Date getWorklogEndDate() {
        return worklogEndDate;
    }

    public Collection<String> getIssueKeys() {
        return issueKeys;
    }

    public boolean isIncludeAllStatuses() {
        return includeAllStatuses;
    }

    public boolean isIgnoreExportedFlag() {
        return ignoreExportedFlag;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (projects != null) {
            builder.append("projects : ").append(Arrays.toString(projects.toArray())).append(", ");
        }
        if (users != null) {
            builder.append("users : ").append(Arrays.toString(users.toArray())).append(", ");
        }
        if (issueKeys != null) {
            builder.append("issueKeys : ").append(Arrays.toString(issueKeys.toArray())).append(", ");
        }
        if (startDate != null) {
            builder.append("startDate : ").append(startDate).append(", ");
        }
        if (endDate != null) {
            builder.append("endDate : ").append(endDate).append(", ");
        }
        if (worklogStartDate != null) {
            builder.append("worklogStartDate : ").append(worklogStartDate).append(", ");
        }
        if (worklogEndDate != null) {
            builder.append("worklogEndDate : ").append(worklogEndDate).append(", ");
        }
        builder.append("includeAllStatues : ").append(includeAllStatuses).append(", ");
        builder.append("ignoreExportedFlag : ").append(ignoreExportedFlag).append(", ");
        return builder.toString();
    }
}
