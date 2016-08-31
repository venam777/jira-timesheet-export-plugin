package com.bftcom.timesheet.export;

import com.atlassian.jira.bc.issue.worklog.DeletedWorklog;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.worklog.Worklog;
import com.atlassian.jira.issue.worklog.WorklogManager;
import com.atlassian.jira.issue.worklog.WorklogStore;
import com.atlassian.jira.project.Project;
import com.bftcom.timesheet.export.entity.WorklogData;
import com.bftcom.timesheet.export.utils.Parser;
import com.bftcom.timesheet.export.utils.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

public class WorklogExporter {

    private WorklogManager manager;
    private WorklogDataDao dao;
    private CustomField financeProjectField;

    private Long financeProjectFieldId = 12500L;
    private static WorklogExporter instance;
    private static Logger logger = LoggerFactory.getLogger(WorklogExporter.class);

    private WorklogExporter(WorklogDataDao dao) {
        this.dao = dao;
        manager = ComponentAccessor.getWorklogManager();
        financeProjectField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(financeProjectFieldId);
    }

    public void exportWorklog(WorklogExportParams params) throws TransformerException, ParserConfigurationException, IOException {
        exportWorklog(params, Settings.getExportFileName());
    }

    public void exportWorklog(WorklogExportParams params, String fileNameWithPath) throws TransformerException, ParserConfigurationException, IOException {
        logger.debug("Worklog export started");
        Collection<Worklog> updatedWorklogs = getUpdatedWorklogs(params);
        logger.debug("updated worklogs count to xml = " + updatedWorklogs.size());
        Collection<DeletedWorklog> deletedWorklogs = getDeletedWorklogs(params);
        logger.debug("deleted worklogs count to xml = " + deletedWorklogs.size());
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        // root elements
        Document doc = docBuilder.newDocument();

        Element msgElement = doc.createElement("MSG");
        doc.appendChild(msgElement);

        Element bodyElement = doc.createElement("BODY");
        msgElement.appendChild(bodyElement);

        Element rplElement = doc.createElement("RPL");
        bodyElement.appendChild(rplElement);

        //fill issue information
        Collection<Issue> issues = getWorklogIssues(updatedWorklogs);
        Element issueRootElement = doc.createElement("CONTROL");
        rplElement.appendChild(issueRootElement);
        addAttribute(doc, issueRootElement, "RPL_OBJ_NAME", "CONTROL");
        Element updatedIssues = doc.createElement("CHANGED");
        issueRootElement.appendChild(updatedIssues);
        for (Issue issue : issues) {
            Element control = doc.createElement("CONTROL");
            addAttribute(doc, control, "ID", issue.getKey());
            addAttribute(doc, control, "CAPTION", issue.getKey() + " " + issue.getSummary());
            String finProjectId = getFinanceProjectId(issue);
            if (finProjectId != null) {
                addAttribute(doc, control, "FINPROJECTID", finProjectId);
            }
            updatedIssues.appendChild(control);
        }

        Element timesheetRootElement = doc.createElement("TIMESHEET");
        rplElement.appendChild(timesheetRootElement);
        addAttribute(doc, timesheetRootElement, "RPL_OBJ_NAME", "TIMESHEET");

        Element updatedTimesheets = doc.createElement("CHANGED");
        timesheetRootElement.appendChild(updatedTimesheets);

        Element deletedTimesheets = doc.createElement("DELETED");
        timesheetRootElement.appendChild(deletedTimesheets);

        for (Worklog worklog : updatedWorklogs) {
            Element timesheet = doc.createElement("TIMESHEET");
            logger.debug("writing updated worklog in xml");
            addAttribute(doc, timesheet, "ID", worklog.getId().toString());
            addAttribute(doc, timesheet, "ISSUE_ID", worklog.getIssue().getId().toString());
            addAttribute(doc, timesheet, "CONTROLID", worklog.getIssue().getKey());
            Double spentTimeHours = (double) worklog.getTimeSpent() / 60 / 60;
            String amount = String.valueOf((double) Math.round(spentTimeHours * 100) / 100);
            addAttribute(doc, timesheet, "AMOUNT", amount);
            addAttribute(doc, timesheet, "REMARK", Parser.parseWorklogComment(worklog.getComment()));
            addAttribute(doc, timesheet, "WORKDATE", Settings.dateFormat.format(worklog.getStartDate()));
            addAttribute(doc, timesheet, "PROJECTID", worklog.getIssue().getProjectObject().getId().toString());
            addAttribute(doc, timesheet, "LDAPID", "BFT\\" + worklog.getAuthorObject().getUsername());
            logger.debug("worklog params: id = " + worklog.getId() + ", issue.id = " + worklog.getIssue().getId()
                    + ", amount = " + amount + ", comment = " + worklog.getComment() + ", workdate = " + Settings.dateFormat.format(worklog.getStartDate())
                    + ", project.id = " + worklog.getIssue().getProjectObject().getId().toString());
            String financeProjectId = getFinanceProjectId(worklog.getIssue());
            if (financeProjectId != null) {
                addAttribute(doc, timesheet, "FINPROJECTID", financeProjectId);
                logger.debug("worklog finance_project.id = " + financeProjectId);
            }

            WorklogData info = dao.get(worklog.getId(), true);
            addAttribute(doc, timesheet, "REJECT_COMMENT", info.getRejectComment());
            addAttribute(doc, timesheet, "STATUS", info.getStatus());
            logger.debug(" reject comment = " + info.getRejectComment());
            logger.debug(" status = " + info.getStatus());

            updatedTimesheets.appendChild(timesheet);
        }

        for (DeletedWorklog worklog : deletedWorklogs) {
            Element timesheet = doc.createElement("TIMESHEET");
            addAttribute(doc, timesheet, "ID", worklog.getId().toString());
            logger.debug("deleted worklog id = " + worklog.getId());
            deletedTimesheets.appendChild(timesheet);
        }

        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        //formatting
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        DOMSource source = new DOMSource(doc);
        logger.debug("Export file path: " + fileNameWithPath);
        String fileName = fileNameWithPath.substring(fileNameWithPath.lastIndexOf('/') + 1);
        String path = fileNameWithPath.substring(0, fileNameWithPath.lastIndexOf('/'));
        logger.debug("file directory: " + path);
        logger.debug("file name : " + fileName);
        File file = new File(path, fileName);
        file.createNewFile();
        StreamResult result = new StreamResult(file);
        transformer.transform(source, result);

        logger.debug("export finished successfully");
    }

    private String getFinanceProjectId(Issue issue) {
        if (financeProjectField != null) {
            Option option = (Option) issue.getCustomFieldValue(financeProjectField);
            if (option != null) {
                String financeProjectName = option.getValue();
                if (financeProjectName == null) {
                    financeProjectName = option.toString();
                }
                if (financeProjectName != null && !financeProjectName.equals("")) {
                    return financeProjectName.substring(financeProjectName.lastIndexOf('#') + 1);
                }
            }
        }
        return null;
    }

    private Collection<Worklog> getUpdatedWorklogs(WorklogExportParams exportParams) {
        logger.debug("getUpdatedWorklogs started");
        Date dateFrom = new Date();
        if (exportParams.getStartDate() != null) {
            dateFrom = exportParams.getStartDate();
        }
        logger.debug("start date = " + dateFrom);
        WorklogStore worklogStore = getWorklogStore();
        if (worklogStore == null) {
            logger.error("worklog store is null!");
            return Collections.emptyList();
        }
        List<Worklog> worklogList = worklogStore.getWorklogsUpdateSince(dateFrom.getTime(), Integer.MAX_VALUE);
        logger.debug("since " + dateFrom + " was updated " + worklogList.size() + " worklogs");
        for (Worklog w : worklogList) {
            logger.debug("id : " + w.getId() + ", issue = " + w.getIssue().getKey());
        }
        //статусы
        worklogList.removeIf(w -> !dao.isWorklogExportable(w));
        //проекты
        if (exportParams.getProjects() != null && exportParams.getProjects().size() > 0) {
            logger.debug("Filter worklogs by projects");
            logger.debug("Projects: " + exportParams.getProjects());
            worklogList.removeIf(w -> {
                for (Project p : exportParams.getProjects()) {
                    if (w.getIssue().getProjectId().equals(p.getId())) {
                        logger.debug("worklog with id = " + w.getId() + " is correct for project with id = " + p.getId());
                        return false;
                    }
                }
                logger.debug("worklog with id = " + w.getId() + " doesn't match for selected projects");
                return true;
            });
        }
        //дата ДО
        if (exportParams.getEndDate() != null) {
            worklogList.removeIf(w -> w.getCreated().after(exportParams.getEndDate()));
        }
        //бюджеты
        //пользователи
        //задачи
        //ид-шники worklog
        return worklogList;
    }

    private Collection<Issue> getWorklogIssues(Collection<Worklog> worklogs) {
        Set<String> issueKeys = new HashSet<>();
        for (Worklog w : worklogs) {
            Issue issue = w.getIssue();
            if (issue != null) {
                issueKeys.add(issue.getKey());
            }
        }
        Collection<Issue> result = new ArrayList<>();
        IssueManager issueManager = ComponentAccessor.getIssueManager();
        for (String key : issueKeys) {
            result.add(issueManager.getIssueByCurrentKey(key));
        }
        return result;
    }

    private Collection<DeletedWorklog> getDeletedWorklogs(WorklogExportParams exportParams) {
        Date dateFrom = new Date();
        if (exportParams.getStartDate() != null) {
            dateFrom = exportParams.getStartDate();
        }
        final Date dateTo = exportParams.getEndDate() != null ? exportParams.getEndDate() : new Date();
        WorklogStore store = getWorklogStore();
        if (store == null) {
            logger.error("worklog store is null!");
            return Collections.emptyList();
        }
        List<DeletedWorklog> worklogList = store.getWorklogsDeletedSince(dateFrom.getTime(), Integer.MAX_VALUE);
        worklogList.removeIf(w -> w.getDeletionTime().after(dateTo));
        return worklogList;
    }

    private void addAttribute(Document doc, Element element, String name, String value) {
        Attr attr = doc.createAttribute(name);
        attr.setValue(value);
        element.setAttributeNode(attr);
    }

    private WorklogStore getWorklogStore() {
        if (manager == null) return null;
        Field field;
        try {
            field = manager.getClass().getDeclaredField("worklogStore");
            field.setAccessible(true);
            return (WorklogStore) field.get(manager);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public synchronized static void createInstance(WorklogDataDao dao) {
        instance = new WorklogExporter(dao);
    }

    public synchronized static WorklogExporter getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Instance must be created by method createInstance(dao) before using!");
        }
        return instance;
    }

}
