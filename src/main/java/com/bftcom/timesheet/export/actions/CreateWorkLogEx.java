package com.bftcom.timesheet.export.actions;

import com.atlassian.jira.bc.issue.comment.CommentService;
import com.atlassian.jira.bc.issue.worklog.WorklogService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.web.FieldVisibilityManager;
import com.atlassian.jira.web.action.issue.AbstractIssueSelectAction;
import com.atlassian.jira.web.action.issue.CreateWorklog;
import com.bftcom.timesheet.export.utils.Constants;
import com.bftcom.timesheet.export.utils.DateUtils;
import com.bftcom.timesheet.export.utils.Parser;
import com.bftcom.timesheet.export.utils.XMLUtil;
import org.apache.log4j.Logger;
import org.apache.xpath.XPathAPI;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: a.gshyan
 * Date: 19.12.14
 * Time: 18:30
 * To change this template use File | Settings | File Templates.
 */
//todo сделать инжект зависимостей с помощью спринга. При использовании @Component, @Inject, @ComponentImport появляется следующая ошибка:
//java.io.FileNotFoundException: class path resource [com/atlassian/jira/web/action/IssueActionSupport.class] cannot be opened because it does not exist
//@Component
public class CreateWorklogEx extends CreateWorklog {

    //    @Inject
    public CreateWorklogEx(/*@ComponentImport WorklogService worklogService,
                           @ComponentImport CommentService commentService,
                           @ComponentImport ProjectRoleManager projectRoleManager,
                           @ComponentImport JiraDurationUtils jiraDurationUtils,
                           @ComponentImport DateTimeFormatterFactory dateTimeFormatterFactory,
                           @ComponentImport FieldVisibilityManager fieldVisibilityManager,
                           @ComponentImport FieldLayoutManager fieldLayoutManager,
                           @ComponentImport RendererManager rendererManager,
                           @ComponentImport UserUtil userUtil,
                           @ComponentImport FeatureManager featureManager*/) {
//        super(worklogService, commentService, projectRoleManager, jiraDurationUtils, dateTimeFormatterFactory, fieldVisibilityManager, fieldLayoutManager, rendererManager, userUtil, featureManager);
        super(ComponentAccessor.getComponent(WorklogService.class),
                ComponentAccessor.getComponent(CommentService.class),
                ComponentAccessor.getComponent(ProjectRoleManager.class),
                ComponentAccessor.getJiraDurationUtils(),
                ComponentAccessor.getComponent(DateTimeFormatterFactory.class),
                ComponentAccessor.getComponent(FieldVisibilityManager.class),
                ComponentAccessor.getFieldLayoutManager(),
                ComponentAccessor.getRendererManager(),
                ComponentAccessor.getUserUtil(),
                ComponentAccessor.getComponent(FeatureManager.class));
    }

    @Override
    public String doDefault() throws Exception {
        return super.doDefault();
    }

    private static Object getCustomFieldValue(Issue issue, String customFieldName) {
        CustomField customField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(customFieldName);
        return (customField != null ? issue.getCustomFieldValue(customField) : null);
    }

    public static void doValidateBudget(AbstractIssueSelectAction obj, Issue issue, String user, Date worklog_StartDate, Logger log) {
        log.debug("Validating budget started");
        log.debug("User = " + user);
        log.debug("Issue key = " + issue.getKey());
        log.debug("start date = " + Parser.formatDate(worklog_StartDate));
        if (ComponentAccessor.getUserUtil().getGroupNamesForUser(user).contains("check_worklog")) {
            log.debug("User groups contains group with name = check_worklog");
            Object budgetNameObj = getCustomFieldValue(issue, Constants.financeProjectFieldName);
            String budgetName = (budgetNameObj == null ? null : budgetNameObj.toString());
            log.debug("budget name = " + budgetName);
            if (issue.isSubTask()) {
                if (budgetName == null || budgetName.length() == 0) {
                    budgetNameObj = getCustomFieldValue(issue.getParentObject(), Constants.financeProjectFieldName);
                    budgetName = (budgetNameObj == null ? null : budgetNameObj.toString());
                }
            }
            if (budgetName != null && budgetName.length() > 0) {

                int pos = budgetName.lastIndexOf("#");
                if (pos >= 0 && pos != budgetName.length() - 1) {

                    String budgetId = budgetName.substring(pos + 1);

//                    File f = new File("D:\\timesheets\\msg_000000_000001_.xml");
                    File f = new File("/mnt/pm/export/msg_000000_000001_.xml");

                    try {
                        Document doc = XMLUtil.createDocument(f, "windows-1251");
                        Element docElem = doc.getDocumentElement();
                        NodeList list = XPathAPI.selectNodeList(docElem, "/MSG/BODY/RPL/PERIOD/CHANGED/PERIOD[@FINPROJECT_ID=" + budgetId + "]");
                        if ((list == null) || (list.getLength() == 0)) {
                            obj.addErrorMessage("Списание времени невозможно. Бюджет или этап [" + budgetName + "], ID[" + budgetId + "] не найден");
                        } else {
                            boolean checkPeriod = false;
                            for (int i = 0; i < list.getLength(); i++) {
                                Element elem = (Element) list.item(i);
                                Date start_date = Timestamp.valueOf(elem.getAttribute("BEGIN") + " 00:00:00");
                                Date end_date = Timestamp.valueOf(elem.getAttribute("END") + " 23:59:59");
                                if ((!worklog_StartDate.before(start_date)) && (!worklog_StartDate.after(end_date))) {
                                    checkPeriod = true;
                                    break;
                                }
                            }
                            if (!checkPeriod) {
                                obj.addErrorMessage("Списание времени невозможно. Указан бюджет проекта не действующий на выбранную дату.");
                            }
                        }
                    } catch (FileNotFoundException e) {
                        obj.addErrorMessage("Списание времени невозможно. FileNotFoundException");
                        log.error("Ошибка в методе проверки списания времени: " + e.getMessage());
                    } catch (TransformerException e) {
                        obj.addErrorMessage("Списание времени невозможно. TransformerException");
                        log.error("Ошибка в методе проверки списания времени: " + e.getMessage());
                    }
                } else {
                    obj.addErrorMessage("Списание времени невозмножно. Не найден ID в значении бюджета проекта");
                }
            } else {
                obj.addErrorMessage("Списание времени невозмножно. Не указан бюджет проекта");
            }
        }
    }

    public static void validatePeriodCloseDate(AbstractIssueSelectAction obj, Issue issue, String user, Date worklogStartDate, Logger log) {
        log.debug("Validating period closed date started");
        log.debug("User = " + user);
        log.debug("Issue key = " + issue.getKey());
        log.debug("start date = " + Parser.formatDate(worklogStartDate));
        if (ComponentAccessor.getUserUtil().getGroupNamesForUser(user).contains("check_worklog")) {
            log.debug("User groups contains group with name = check_worklog");
            File f = new File("/mnt/pm/export/msg_000000_000001_.xml");
            try {
                Document doc = XMLUtil.createDocument(f, "windows-1251");
                Element docElem = doc.getDocumentElement();
                NodeList list = XPathAPI.selectNodeList(docElem, "/MSG/BODY/RPL/SYSPARAM/CHANGED/SYSPARAM[@NAME=period.close.date]");
                if ((list != null) && (list.getLength() != 0)) {
                    for (int i = 0; i < list.getLength(); i++) {
                        Element elem = (Element) list.item(i);
                        log.debug("period.close.date = " + elem.getAttribute("PARAM_VALUE"));
                        Date startDate = Parser.parseDate(elem.getAttribute("PARAM_VALUE"), null);
                        if (startDate == null) {
                            log.error("start date is null!");
                            continue;
                        }
                        log.debug("date after parsing : " + Parser.formatDateTime(startDate));
                        if (worklogStartDate.before(startDate)) {
                            log.debug("");
                            obj.addErrorMessage("Списание времени невозможно. Указан бюджет проекта не действующий на выбранную дату.");
                        }
                    }
                } else {
                    log.error("There is no param /MSG/BODY/RPL/SYSPARAM/CHANGED/SYSPARAM in xml file!");
                }
            } catch (FileNotFoundException e) {
                obj.addErrorMessage("Списание времени невозможно. FileNotFoundException");
                log.error("Ошибка в методе проверки списания времени: " + e.getMessage());
            } catch (TransformerException e) {
                obj.addErrorMessage("Списание времени невозможно. TransformerException");
                log.error("Ошибка в методе проверки списания времени: " + e.getMessage());
            }
        }
    }


    @Override
    protected void doValidation() {
        super.doValidation();
        if (getWorklog() != null) {
            doValidateBudget(this, getIssueObject(), getWorklog().getAuthorKey(), getWorklog().getStartDate(), log);
            validatePeriodCloseDate(this, getIssueObject(), getWorklog().getAuthorKey(), getWorklog().getStartDate(), log);
        }
    }
}
