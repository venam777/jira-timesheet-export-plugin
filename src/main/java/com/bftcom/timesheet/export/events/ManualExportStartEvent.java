package com.bftcom.timesheet.export.events;

import java.util.Date;

public class ManualExportStartEvent {

    public final Date startDate;
    public final Date endDate;
    private String[] projectNames;
    private String[] userNames;
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

    public boolean isIncludeAllStatuses() {
        return includeAllStatuses;
    }

    public void setIncludeAllStatuses(boolean includeAllStatuses) {
        this.includeAllStatuses = includeAllStatuses;
    }
}
