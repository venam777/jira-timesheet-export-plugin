package com.bftcom.timesheet.export.actions;

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
import com.bftcom.timesheet.export.WorklogDataDao;
import com.bftcom.timesheet.export.entity.WorklogData;
import com.bftcom.timesheet.export.utils.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Кастомный обработчик редактирования ворклога
 */
public class CustomUpdateWorklog extends UpdateWorklog {

    private static Logger logger = LoggerFactory.getLogger(CustomUpdateWorklog.class);

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
        logger.debug("CustomUpdateWorklog created");
    }

    /**
     * Показываем пользователю чистый коммент,без стилей и цветов
     *
     * @return
     */
    @Override
    public String getComment() {
        logger.debug("get comment started");
        String result = super.getComment();
        logger.debug("parent getComment() returned " + result);
        if (result != null) {
            result = Parser.parseWorklogComment(super.getComment());
        }
        logger.debug("comment after parsing : " + result);
        return result;
    }

    /**
     * После редактирования ворклога автоматически ставим ему статус "Не просмотрено"
     *
     * @return
     * @throws Exception
     */
    @Override
    protected String doExecute() throws Exception {
        logger.debug("doExecute started");
        String result = super.doExecute();
        logger.debug("parent doExecute() returned " + result);
        WorklogDataDao.getInstance().setWorklogStatus(getWorklogId(), WorklogData.NOT_VIEWED_STATUS, "");
        return result;
    }
}
