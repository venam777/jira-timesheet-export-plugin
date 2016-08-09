package com.bftcom.timesheet.export;

import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.issue.worklog.WorklogInputParameters;
import com.atlassian.jira.bc.issue.worklog.WorklogInputParametersImpl;
import com.atlassian.jira.bc.issue.worklog.WorklogResult;
import com.atlassian.jira.bc.issue.worklog.WorklogService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.worklog.Worklog;
import com.atlassian.jira.issue.worklog.WorklogManager;
import com.atlassian.jira.util.ErrorCollections;
import com.bftcom.timesheet.export.entity.WorklogData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorklogEventListener {

    private WorklogDataDao dao;
    private static Logger logger = LoggerFactory.getLogger(WorklogEventListener.class);


    public WorklogEventListener(WorklogDataDao dao) {
        this.dao = dao;
    }

    public void onWorklogCreated(Worklog worklog) {
        dao.create(worklog.getId());
        //dao.setWorklogStatus(worklog.getId(), WorklogData.NOT_VIEWED_STATUS);
    }

    public void onWorklogUpdated(Worklog worklog) {
        String status = dao.getWorklogStatus(worklog.getId());
        if (status.equals(WorklogData.REJECTED_STATUS)) {
            dao.setWorklogStatus(worklog.getId(), WorklogData.NOT_VIEWED_STATUS);
        }
    }

    public void onWorklogDeleted(Long worklogId) {
        dao.delete(worklogId);
    }
}
