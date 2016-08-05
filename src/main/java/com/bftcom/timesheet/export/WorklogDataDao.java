package com.bftcom.timesheet.export;


import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.worklog.Worklog;
import com.atlassian.jira.issue.worklog.WorklogManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.bftcom.timesheet.export.entity.WorklogData;
import net.java.ao.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

//@Component
public class WorklogDataDao {

    //    @ComponentImport
    protected ActiveObjects activeObjects;
    private static Logger logger = LoggerFactory.getLogger(WorklogDataDao.class);

    //    @Inject
    public WorklogDataDao(ActiveObjects activeObjects) {
        if (activeObjects == null) {
            throw new NullPointerException("Active objects can't be null!");
        }
        this.activeObjects = activeObjects;
    }

    /**
     * ВОзвращает true, если worklog можно изменять
     *
     * @param worklog
     * @return
     */
    public boolean isWorklogMutable(Worklog worklog) {
        return getWorklogStatus(worklog.getId()).equals(WorklogData.APPROVED_STATUS);
    }

    public boolean isWorklogExportable(Worklog worklog) {
        String worklogStatus = getWorklogStatus(worklog.getId());
        boolean result = worklogStatus.equals("") || worklogStatus.equals(WorklogData.NOT_VIEWED_STATUS);
        logger.debug("worklog status = " + worklogStatus + ", worklog exportable = " + result);
        return result;
    }

    public String getWorklogStatus(Long worklogId) {
        WorklogData data = get(worklogId, false);
        return data != null ? data.getStatus() : "";
    }

    public void setWorklogStatus(Long worklogId, String status) {
        logger.debug("set worklog status, status = " + status + ", worklogId = " + worklogId);
        activeObjects.executeInTransaction(() -> {
            WorklogData data = get(worklogId, true);
            data.setStatus(status);
            data.save();
            return data;
        });
    }

    public void delete(Long worklogId) {
        logger.debug("deleting worklog data with worklog.id = " + worklogId);
        activeObjects.deleteWithSQL(WorklogData.class, " WORKLOG_ID = ?", worklogId);
    }

    public void update(Long worklogId, String status, String rejectComment) {
        logger.debug("updating worklog data, status = " + status + ", rejectComment = " + rejectComment);
        activeObjects.executeInTransaction(() -> {
            WorklogData data = get(worklogId, true);
            data.setStatus(status);
            data.setRejectComment(rejectComment);
            data.save();
            logger.debug("updating was finished");
            return data;
        });
    }

    protected WorklogData create(Long worklogId) {
        logger.debug("creating new worklog data for worklog with id = " + worklogId);
        return activeObjects.executeInTransaction(() -> {
            WorklogData data = activeObjects.create(WorklogData.class);
            data.setWorklogId(worklogId);
            data.setStatus(WorklogData.NOT_VIEWED_STATUS);
            data.setRejectComment("");
            data.save();
            return data;
        });
    }

    protected WorklogData get(Long worklogId, boolean createIfNotExists) {
        logger.debug("get worklog data for worklog with id = " + worklogId);
        WorklogData[] mass = activeObjects.find(WorklogData.class, Query.select().where("WORKLOG_ID = ?", worklogId));
        if (mass != null && mass.length == 1) {
            logger.debug("worklog data was found, worklogdata.status = " + mass[0].getStatus() + ", worklogdata.rejectcomment = " + mass[0].getRejectComment());
            return mass[0];
        }
        logger.debug("worklog data was not found");
        if (createIfNotExists) {
            return create(worklogId);
        }
        return null;
    }
}
