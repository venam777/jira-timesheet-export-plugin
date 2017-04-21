package com.bftcom.timesheet.export.events;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

public class ManualExportStartEvent {

    public final Date startDate;
    public final Date endDate;
    private String[] projectNames;
    private String[] userNames;
    private Date worklogStartDate;
    private Date worklogEndDate;
    private Collection<String> issueKeys;
    private boolean includeAllStatuses;

    public ManualExportStartEvent(Date startDate, Date endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String[] getProjectNames() {
        return projectNames;
    }

    public void setProjectNames(String[] projectNames) {
        this.projectNames = projectNames;
    }

    public String[] getUserNames() {
        return userNames;
    }

    public void setUserNames(String[] userNames) {
        this.userNames = userNames;
    }

    public Date getWorklogStartDate() {
        return worklogStartDate;
    }

    public void setWorklogStartDate(Date worklogStartDate) {
        this.worklogStartDate = worklogStartDate;
    }

    public Date getWorklogEndDate() {
        return worklogEndDate;
    }

    public void setWorklogEndDate(Date worklogEndDate) {
        this.worklogEndDate = worklogEndDate;
    }

    public void setIssueKeys(Collection<String> issueKeys) {
        this.issueKeys = issueKeys;
    }

    public void setIssueKeys(String... issueKeys) {
        if (issueKeys != null) {
            this.issueKeys = Arrays.asList(issueKeys);
        } else {
            this.issueKeys = Collections.emptyList();
        }
    }

    public Collection<String> getIssueKeys() {
        return issueKeys;
    }

    public boolean isIncludeAllStatuses() {
        return includeAllStatuses;
    }

    public void setIncludeAllStatuses(boolean includeAllStatuses) {
        this.includeAllStatuses = includeAllStatuses;
    }
}
