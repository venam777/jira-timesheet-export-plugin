package com.bftcom.timesheet.export;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Параметры, по которым выбираются таймшиты из джиры
 */
public class WorklogExportParams {

    private Collection<Project> projects;
    private Date startDate;
    private Date endDate;
    //todo
    //бюджеты
    //пользователи
    //задачи
    //ид-шники worklog

    public WorklogExportParams(Date startDate, Date endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public WorklogExportParams projects(Collection<String> projectKeys) {
        this.projects = transformProjects((String[]) projectKeys.toArray());
        return this;
    }

    public WorklogExportParams projects(String... projectKeys) {
        this.projects = transformProjects(projectKeys);
        return this;
    }

    protected Collection<Project> transformProjects(String... projectKeys) {
        ProjectManager projectManager = ComponentAccessor.getProjectManager();
        List<Project> projectList = new ArrayList<>();
        for (String key : projectKeys) {
            Project project = projectManager.getProjectByCurrentKeyIgnoreCase(key);
            if (project != null) {
                projectList.add(project);
            }
        }
        return projectList;
    }

    public Collection<Project> getProjects() {
        return projects;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }
}
