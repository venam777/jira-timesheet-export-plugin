package com.bftcom.timesheet.export;


import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.issue.worklog.Worklog;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.bftcom.timesheet.export.entity.WorklogData;
import net.java.ao.Query;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class WorklogDataDao {

    @ComponentImport
    protected ActiveObjects activeObjects;

    @Inject
    public WorklogDataDao(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    /**
     * ВОзвращает true, если worklog можно изменять
     * @param worklog
     * @return
     */
    public boolean isWorklogMutable(Worklog worklog) {
        return getWorklogStatus(worklog.getId()).equals(WorklogData.APPROVED_STATUS);
    }

    public boolean isWorklogExportable(Worklog worklog) {
        return getWorklogStatus(worklog.getId()).equals(WorklogData.NOT_VIEWED_STATUS);
    }

    public String getWorklogStatus(Long worklogId) {
        WorklogData data = get(worklogId);
        return data != null ? data.getStatus() : "";
    }

    public void setWorklogStatus(Long worklogId, String status) {
        activeObjects.executeInTransaction(() -> {
            WorklogData data = get(worklogId);
            if (data == null) {
                data = activeObjects.create(WorklogData.class);
                data.setWorklogId(worklogId);
                data.setStatus(status);
                data.setRejectComment("");
            } else {
                data.setStatus(status);
            }
            data.save();
            return data;
        });
    }

    protected WorklogData get(Long worklogId) {
        WorklogData[] mass = activeObjects.find(WorklogData.class, Query.select().where("WORKLOG_ID = ?", worklogId));
        if (mass != null && mass.length == 1) {
            return mass[0];
        }
        return null;
    }

    public void deleteWorklogData(Long worklogId) {
        activeObjects.deleteWithSQL(WorklogData.class, " WORKLOG_ID = ?", worklogId);
    }

}
