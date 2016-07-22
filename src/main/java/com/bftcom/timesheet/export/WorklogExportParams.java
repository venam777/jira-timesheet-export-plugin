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

    public WorklogExportParams(Date startDate, Date endDate, Collection<Project> projects) {
        this.projects = projects;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public WorklogExportParams(Date startDate, Date endDate, String... projectKeys) {
        ProjectManager projectManager = ComponentAccessor.getProjectManager();
        List<Project> projectList = new ArrayList<>();
        for (String key : projectKeys) {
            Project project = projectManager.getProjectByCurrentKeyIgnoreCase(key);
            if (project != null) {
                projectList.add(project);
            }
        }
        this.projects = projectList;
        this.startDate = startDate;
        this.endDate = endDate;
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
