package com.bftcom.timesheet.export;

import com.atlassian.jira.bc.issue.comment.CommentService;
import com.atlassian.jira.bc.issue.worklog.WorklogService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.issue.RendererManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.web.FieldVisibilityManager;
import com.atlassian.jira.web.action.issue.UpdateWorklog;
import com.bftcom.timesheet.export.entity.WorklogData;
import com.bftcom.timesheet.export.utils.Parser;

/**
 * Кастомный обработчик редактирования ворклога
 */
public class CustomUpdateWorklog extends UpdateWorklog {

    public CustomUpdateWorklog() {
        super(ComponentAccessor.getOSGiComponentInstanceOfType(WorklogService.class),
                ComponentAccessor.getOSGiComponentInstanceOfType(CommentService.class),
                ComponentAccessor.getOSGiComponentInstanceOfType(ProjectRoleManager.class),
                ComponentAccessor.getJiraDurationUtils(),
                ComponentAccessor.getComponent(DateTimeFormatterFactory.class),
                ComponentAccessor.getComponent(FieldVisibilityManager.class),
                ComponentAccessor.getComponent(FieldLayoutManager.class),
                ComponentAccessor.getComponent(RendererManager.class),
                ComponentAccessor.getUserUtil(),
                ComponentAccessor.getComponent(FeatureManager.class));
    }

    /**
     * Показываем пользователю чистый коммент,без стилей и цветов
     *
     * @return
     */
    @Override
    public String getComment() {
        return Parser.parseWorklogComment(super.getComment());
    }

    /**
     * После редактирования ворклога автоматически ставим ему статус "Не просмотрено"
     *
     * @return
     * @throws Exception
     */
    @Override
    protected String doExecute() throws Exception {
        String result = super.doExecute();
        WorklogDataDao.getInstance().setWorklogStatus(getWorklogId(), WorklogData.NOT_VIEWED_STATUS);
        return result;
    }
}