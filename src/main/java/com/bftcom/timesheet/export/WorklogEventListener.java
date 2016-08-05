package com.bftcom.timesheet.export;

import com.atlassian.jira.issue.worklog.Worklog;
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
        dao.setWorklogStatus(worklog.getId(), WorklogData.NOT_VIEWED_STATUS);
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
