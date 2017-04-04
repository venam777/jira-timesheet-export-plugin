package com.bftcom.timesheet.export;


import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.issue.worklog.WorklogInputParameters;
import com.atlassian.jira.bc.issue.worklog.WorklogInputParametersImpl;
import com.atlassian.jira.bc.issue.worklog.WorklogResult;
import com.atlassian.jira.bc.issue.worklog.WorklogService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.worklog.Worklog;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.JiraDurationUtils;
import com.bftcom.timesheet.export.dto.WorklogDTO;
import com.bftcom.timesheet.export.entity.WorklogData;
import com.bftcom.timesheet.export.utils.Converter;
import com.bftcom.timesheet.export.utils.Parser;
import com.bftcom.timesheet.export.utils.SQLUtils;
import net.java.ao.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

//@Component
public class WorklogDataDao {

    //    @ComponentImport
    protected ActiveObjects activeObjects;
    private static Logger logger = LoggerFactory.getLogger(WorklogDataDao.class);
    private static WorklogDataDao instance;

    //    @Inject
    public WorklogDataDao(ActiveObjects activeObjects) {
        if (activeObjects == null) {
            throw new NullPointerException("Active objects can't be null!");
        }
        this.activeObjects = activeObjects;
    }

    public static void createInstance(ActiveObjects activeObjects) {
        instance = new WorklogDataDao(activeObjects);
    }

    public synchronized static WorklogDataDao getInstance() {
        return instance;
    }

    @Deprecated
    public synchronized boolean isWorklogExportable(Long worklogId) {
        logger.debug("Is worklog id = " + worklogId + " exportable?");
        WorklogData data = get(worklogId, false);
        String worklogStatus = data != null ? data.getStatus() : "";
        boolean result = data == null || !data.isExported() && (worklogStatus == null || worklogStatus.equals("") || worklogStatus.equals(WorklogData.NOT_VIEWED_STATUS));
        logger.debug("data = " + (data != null ? data : "null") + "; data.isExported = " + (data != null ? data.isExported() : "false" + "; worklogStatus = " + worklogStatus));
        logger.debug("Result = " + result);
        return result;
        /*String worklogStatus = getWorklogStatus(worklogId);
        return worklogStatus.equals("") || worklogStatus.equals(WorklogData.NOT_VIEWED_STATUS);*/
        /*
         WorklogData data = get(worklogId, false);
        String worklogStatus = data != null ? data.getStatus() : "";
        return data == null || !data.isExported() && (worklogStatus == null || worklogStatus.equals("") || worklogStatus.equals(WorklogData.NOT_VIEWED_STATUS));
         */
    }

    public String getWorklogStatus(Long worklogId) {
        WorklogData data = get(worklogId, false);
        return data != null ? data.getStatus() : "";
    }

    public synchronized void setWorklogStatus(Long worklogId, String status, String rejectComment) {
        logger.debug("set worklog status, status = " + status + ", worklogId = " + worklogId);
        activeObjects.executeInTransaction(() -> {
            WorklogData data = get(worklogId, true);
            data.setStatus(status);
            if (rejectComment != null) {
                data.setRejectComment(rejectComment);
            }
            if (status.equalsIgnoreCase(WorklogData.REJECTED_STATUS) || status.equalsIgnoreCase(WorklogData.NOT_VIEWED_STATUS)) {
                logger.debug("Set exported = false");
                data.setExported(false);
            }
            data.save();
            onWorklogStatusChanged(worklogId);
            return data;
        });
    }

    public synchronized void delete(Long worklogId) {
        logger.debug("deleting worklog data with worklog.id = " + worklogId);
        activeObjects.deleteWithSQL(WorklogData.class, " WORKLOG_ID = ?", worklogId);
    }

    public synchronized void update(Long worklogId, String status, String rejectComment) {
        logger.debug("WorklogDataDao#update started");
        if (ComponentAccessor.getWorklogManager().getById(worklogId) == null) {
            logger.debug("no worklog with id = " + worklogId + " was found, nothing to do");
        }
        logger.debug("updating worklog data, status = " + status + ", rejectComment = " + rejectComment);
        setWorklogStatus(worklogId, status, rejectComment);
    }

    public void setExported(WorklogData data, boolean exported) {
        if (data != null) {
            activeObjects.executeInTransaction(() -> {
                data.setExported(exported);
                data.save();
                return data;
            });
        }
    }

    protected synchronized WorklogData create(Long worklogId) {
        logger.debug("creating new worklog data for worklog with id = " + worklogId);
        return activeObjects.executeInTransaction(() -> {
            WorklogData data = activeObjects.create(WorklogData.class);
            data.setWorklogId(worklogId);
            data.setStatus(WorklogData.NOT_VIEWED_STATUS);
            data.setRejectComment("");
            data.save();
            onWorklogStatusChanged(worklogId);
            return data;
        });
    }

    protected synchronized WorklogData get(Long worklogId, boolean createIfNotExists) {
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

    protected Map<Long, WorklogData> getWorklogData(Collection<WorklogDTO> worklogs) {
        WorklogData[] mass = activeObjects.find(WorklogData.class, Query.select().where("WORKLOG_ID in ?", SQLUtils.collectionToString(worklogs, source -> source.getId().toString())));
        Map<Long, WorklogData> result = new HashMap<>();
        for (WorklogData data : mass) {
            result.put(data.getWorklogId(), data);
        }
        return result;
    }

    protected void onWorklogStatusChanged(Long worklogId) {
        logger.debug("WorklogDataDao#onWorklogStatusChanged started");
        Worklog worklog = ComponentAccessor.getWorklogManager().getById(worklogId);
        if (worklog == null) {
            logger.error("Worklog with id = " + worklogId + " was not found!");
            return;
        }
        WorklogData worklogData = get(worklogId, false);
        if (worklogData == null) {
            logger.error("WorklogData for worklog with id = " + worklogId + " was not found!");
            return;
        }
        String title = " | Статус: " + worklogData.getStatus();
        if (title.equals(WorklogData.REJECTED_STATUS) && worklogData.getRejectComment() != null && !worklogData.getRejectComment().equals("")) {
            title += ", причина: " + worklogData.getRejectComment();
        }
        logger.debug("new title = " + title);
        String comment = Parser.parseWorklogComment(worklog.getComment());
        logger.debug("clear worklog comment = " + comment);
        comment = comment + title;
        logger.debug("new worklog comment = " + comment);

        WorklogService worklogService = ComponentAccessor.getComponent(WorklogService.class);
        ApplicationUser user = ComponentAccessor.getUserManager().getUserByKey("admin");
        if (user == null) {
            logger.error("Admin user was not found!");
            return;
        }

        JiraServiceContext serviceContext = new JiraServiceContextImpl(user);
        JiraDurationUtils durationUtils = ComponentAccessor.getJiraDurationUtils();
        WorklogInputParameters parameters = WorklogInputParametersImpl.builder()
                .worklogId(worklog.getId()).comment(comment).issue(worklog.getIssue()).startDate(worklog.getStartDate())
                .timeSpent(durationUtils.getShortFormattedDuration(worklog.getTimeSpent()))
                .build();
        WorklogResult result = worklogService.validateUpdate(serviceContext, parameters);
        if (result != null) {
            logger.debug("start updating worklog");
            worklogService.updateAndRetainRemainingEstimate(serviceContext, result, false);
            logger.debug("finished updating worklog");
        } else {
            logger.error("updating is not allowed!");
        }
    }
}
