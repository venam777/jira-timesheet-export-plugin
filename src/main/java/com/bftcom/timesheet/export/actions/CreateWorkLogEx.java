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
import com.bftcom.timesheet.export.utils.Parser;
import com.bftcom.timesheet.export.utils.Settings;
import com.bftcom.timesheet.export.utils.XMLUtil;
import org.apache.xpath.XPathAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

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

    private static String userGroup = "jira-users";
    private static Logger log = LoggerFactory.getLogger(CreateWorklogEx.class);
    private static List<String> errors = new ArrayList<>();

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

    public static void doValidateBudget(Issue issue, String user, Date worklog_StartDate, Collection<String> errorMessages) {
        log.debug("Validating budget started");
        log.debug("User = " + user);
        log.debug("Issue key = " + issue.getKey());
        log.debug("start date = " + Parser.formatDate(worklog_StartDate));
        if (ComponentAccessor.getUserUtil().getGroupNamesForUser(user).contains(userGroup)) {
            log.debug("User groups contains group with name = " + userGroup);
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
                    File f = getFinanceProjectFile();
                    try {
                        Document doc = XMLUtil.createDocument(f, "windows-1251");
                        Element docElem = doc.getDocumentElement();
                        NodeList list = XPathAPI.selectNodeList(docElem, "/MSG/BODY/RPL/PERIOD/CHANGED/PERIOD[@FINPROJECT_ID=" + budgetId + "]");
                        if ((list == null) || (list.getLength() == 0)) {
                            errorMessages.add("Списание времени невозможно. Бюджет или этап [" + budgetName + "], ID[" + budgetId + "] не найден");
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
                                errorMessages.add("Списание времени невозможно. Указан бюджет проекта не действующий на выбранную дату.");
                            }
                        }
                    } catch (FileNotFoundException e) {
                        errorMessages.add("Списание времени невозможно. Не найден файл с выгрузкой бюджетов (msg_000000_000001_.xml)");
                        log.error("Ошибка в методе проверки списания времени: " + e.getMessage());
                    } catch (TransformerException e) {
                        errorMessages.add("Списание времени невозможно. TransformerException");
                        log.error("Ошибка в методе проверки списания времени: " + e.getMessage());
                    }
                } else {
                    errorMessages.add("Списание времени невозможно. Не найден ID в значении бюджета проекта");
                }
            } else {
                errorMessages.add("Списание времени невозможно. Не указан бюджет проекта");
            }
        }
    }

    public static void validatePeriodCloseDate(Issue issue, String user, Date worklogStartDate, Collection<String> errorMessages) {
        log.debug("Validating period closed date started");
        log.debug("User = " + user);
        log.debug("Issue key = " + issue.getKey());
        log.debug("start date = " + Parser.formatDate(worklogStartDate));
        if (ComponentAccessor.getUserUtil().getGroupNamesForUser(user).contains(userGroup)) {
            log.debug("User groups contains group with name = " + userGroup);
            File f = getFinanceProjectFile();
            try {
                Document doc = XMLUtil.createDocument(f, "windows-1251");
                Element docElem = doc.getDocumentElement();
                //[@NAME=period.close.date]
                NodeList list = XPathAPI.selectNodeList(docElem, "/MSG/BODY/RPL/SYSPARAM/CHANGED/SYSPARAM");
                if ((list != null) && (list.getLength() != 0)) {
                    for (int i = 0; i < list.getLength(); i++) {
                        Element elem = (Element) list.item(i);
                        if (!elem.getAttribute("NAME").equalsIgnoreCase("period.close.date")) {
                            continue;
                        }
                        log.debug("period.close.date = " + elem.getAttribute("PARAM_VALUE"));
                        Date startDate = Parser.parseDate(elem.getAttribute("PARAM_VALUE"), null);
                        if (startDate == null) {
                            log.error("start date is null!");
                            continue;
                        }
                        log.debug("date after parsing : " + Parser.formatDateTime(startDate));
                        if (worklogStartDate.before(startDate)) {
                            log.debug("");
                            errorMessages.add("Списание времени невозможно. Указан бюджет проекта не действующий на выбранную дату.");
                        }
                    }
                } else {
                    log.error("There is no param /MSG/BODY/RPL/SYSPARAM/CHANGED/SYSPARAM in xml file!");
                }
            } catch (FileNotFoundException e) {
                errorMessages.add("Списание времени невозможно. Не найден файл с выгрузкой бюджетов (msg_000000_000001_.xml)");
                log.error("Ошибка в методе проверки списания времени: ", e);
            } catch (TransformerException e) {
                errorMessages.add("Списание времени невозможно. TransformerException");
                log.error("Ошибка в методе проверки списания времени: ", e);
            }
        }
    }

    private static File getFinanceProjectFile() {
        String filePath = Settings.get("financeProjectImportDir");
        log.debug("XML file path = " + filePath);
        if (!filePath.endsWith(File.separator)) {
            filePath = filePath + File.separator;
        }
        return new File(filePath + "msg_000000_000001_.xml");
    }

    @Override
    protected String doExecute() throws Exception {
        return errors.size() > 0 ? "budgetError" : super.doExecute();
    }


    @Override
    protected void doValidation() {
        errors.clear();
        super.doValidation();
        if (getWorklog() != null) {
            doValidateBudget(getIssueObject(), getWorklog().getAuthorKey(), getWorklog().getStartDate(), errors);
            validatePeriodCloseDate(getIssueObject(), getWorklog().getAuthorKey(), getWorklog().getStartDate(), errors);
        }
    }

    public Collection<String> getWorklogErrors() {
        return errors;
    }
}
