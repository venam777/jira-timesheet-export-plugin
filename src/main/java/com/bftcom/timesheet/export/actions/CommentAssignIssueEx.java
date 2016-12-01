package com.bftcom.timesheet.export.actions;

import com.atlassian.jira.bc.issue.comment.CommentService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.web.action.issue.CommentAssignIssue;
import com.atlassian.jira.workflow.IssueWorkflowManager;
import webwork.action.ActionContext;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: a.gshyan
 * Date: 23.12.14
 * Time: 14:01
 * To change this template use File | Settings | File Templates.
 */

public class CommentAssignIssueEx extends CommentAssignIssue {

    private static List<String> errors = new ArrayList<>();

    public CommentAssignIssueEx(/*SubTaskManager subTaskManager, FieldScreenRendererFactory fieldScreenRendererFactory, CommentService commentService, IssueService issueService, UserUtil userUtil, IssueWorkflowManager issueWorkflowManager*/) {
        super(ComponentAccessor.getSubTaskManager(),
                ComponentAccessor.getFieldScreenRendererFactory(),
                ComponentAccessor.getComponent(CommentService.class),
                ComponentAccessor.getIssueService(),
                ComponentAccessor.getUserUtil(),
                ComponentAccessor.getComponent(IssueWorkflowManager.class));    //To change body of overridden methods use File | Settings | File Templates.
    }

    protected Date getParsedStartDate(String date)
    {
        return date != null ? getDateTimeFormatter().parse(date) : null;
    }

    @Override
    protected void doValidation() {
//        log.error("!!!!!!!!!1");
        errors.clear();
        super.doValidation();
        String[] dur = (String[]) ActionContext.getParameters().get("worklog_timeLogged");
        String[] date = (String[]) ActionContext.getParameters().get("worklog_startDate");
        if ((dur != null && dur[0].length() > 0)) {
            CreateWorklogEx.doValidateBudget(getIssueObject(), getLoggedInUser().getName(), getParsedStartDate(date[0]), errors);
            CreateWorklogEx.validatePeriodCloseDate(getIssueObject(), getLoggedInUser().getName(), getParsedStartDate(date[0]), errors);
        }
    }

    @Override
    protected String doExecute() throws Exception {
        return errors.size() > 0 ? "budgetError" : super.doExecute();
    }
}
