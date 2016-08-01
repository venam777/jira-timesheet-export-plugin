package com.bftcom.timesheet.export;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;

import java.util.*;

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
        if (projectKeys == null) {
            this.projects = Collections.emptyList();
        } else {
            this.projects = transformProjects((String[]) projectKeys.toArray());
        }
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

    public static Date getStartOfCurrentMonth() {
        Calendar start = Calendar.getInstance();
        start.setTime(new Date());
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        return start.getTime();
    }

    public static Date getEndOfCurrentMonth() {
        Calendar end = Calendar.getInstance();
        end.setTime(new Date());
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        return end.getTime();
    }

    public static WorklogExportParams getDefaultParams() {
        WorklogExportParams result = new WorklogExportParams(getStartOfCurrentMonth(), getEndOfCurrentMonth());
        return result;

    }
}
