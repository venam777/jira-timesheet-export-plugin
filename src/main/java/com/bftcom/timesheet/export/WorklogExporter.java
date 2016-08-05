package com.bftcom.timesheet.export;

import com.atlassian.jira.bc.issue.worklog.DeletedWorklog;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.worklog.Worklog;
import com.atlassian.jira.issue.worklog.WorklogManager;
import com.atlassian.jira.project.Project;
import com.bftcom.timesheet.export.entity.WorklogData;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class WorklogExporter {

    private WorklogManager manager;
    private WorklogDataDao dao;

    private Long financeProjectFieldId = 12500L;
    private static WorklogExporter instance;
    private static Logger logger = LoggerFactory.getLogger(WorklogExporter.class);

    private WorklogExporter(WorklogDataDao dao) {
        this.dao = dao;
        manager = ComponentAccessor.getWorklogManager();
    }

    public void exportWorklog(WorklogExportParams params) throws TransformerException, ParserConfigurationException {
        exportWorklog(params, Settings.getExportFileName());
    }

    public void exportWorklog(WorklogExportParams params, String fileNameWithPath) throws TransformerException, ParserConfigurationException {
        logger.debug("Worklog export started");
        Collection<Worklog> updatedWorklogs = getUpdatedWorklogs(params);
        logger.debug("updated worklogs count to xml = " + updatedWorklogs.size());
        Collection<DeletedWorklog> deletedWorklogs = getDeletedWorklogs(params);
        logger.debug("deleted worklogs count to xml = " + deletedWorklogs.size());
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        // root elements
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("BODY");
        doc.appendChild(rootElement);
        Element timesheetRootElement = doc.createElement("TIMESHEET");
        rootElement.appendChild(timesheetRootElement);

        Element updatedTimesheets = doc.createElement("CHANGED");
        timesheetRootElement.appendChild(updatedTimesheets);

        Element deletedTimesheets = doc.createElement("DELETED");
        timesheetRootElement.appendChild(deletedTimesheets);

        for (Worklog worklog : updatedWorklogs) {
            Element timesheet = doc.createElement("TIMESHEET");
            logger.debug("writing updated worklog in xml");
            addAttribute(doc, timesheet, "ID", worklog.getId().toString());
            addAttribute(doc, timesheet, "ISSUE_ID", worklog.getIssue().getId().toString());
            Double spentTimeHours = (double) worklog.getTimeSpent() / 60 / 60;
            String amount = String.valueOf((double) Math.round(spentTimeHours * 100) / 100);
            addAttribute(doc, timesheet, "AMOUNT", amount);
            addAttribute(doc, timesheet, "REMARK", worklog.getComment());
            addAttribute(doc, timesheet, "WORKDATE", Settings.dateFormat.format(worklog.getStartDate()));
            addAttribute(doc, timesheet, "PROJECTID", worklog.getIssue().getProjectObject().getId().toString());
            logger.debug("worklog params: id = " + worklog.getId() + ", issue.id = " + worklog.getIssue().getId()
                    + ", amount = " + amount + ", comment = " + worklog.getComment() + ", workdate = " + Settings.dateFormat.format(worklog.getStartDate())
                    + ", project.id = " + worklog.getIssue().getProjectObject().getId().toString());
            CustomField f = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(financeProjectFieldId);
            if (f != null) {
                String financeProjectName = (String) worklog.getIssue().getCustomFieldValue(f);
                if (financeProjectName != null && !financeProjectName.equals("")) {
                    String financeProjectId = financeProjectName.substring(financeProjectName.lastIndexOf('#') + 1);
                    addAttribute(doc, timesheet, "FINPROJECT_ID", financeProjectId);
                    logger.debug("worklog finance_project.id = " + financeProjectId);
                }
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
        StreamResult result = new StreamResult(new File(fileNameWithPath));

        transformer.transform(source, result);

        logger.debug("export finished successfully");
    }

    private Collection<Worklog> getUpdatedWorklogs(WorklogExportParams exportParams) {
        Date dateFrom = new Date();
        if (exportParams.getStartDate() != null) {
            dateFrom = exportParams.getStartDate();
        }
        logger.debug("start date = " + dateFrom);
        List<Worklog> worklogList = manager.getWorklogsUpdatedSince(dateFrom.getTime());
        logger.debug("since " + dateFrom + " was updated " + worklogList.size() + " worklogs");
        //статусы
        worklogList.removeIf(w -> !dao.isWorklogExportable(w));
        //проекты
        if (exportParams.getProjects() != null && exportParams.getProjects().size() > 0) {
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

    private Collection<DeletedWorklog> getDeletedWorklogs(WorklogExportParams exportParams) {
        Date dateFrom = new Date();
        if (exportParams.getStartDate() != null) {
            dateFrom = exportParams.getStartDate();
        }
        final Date dateTo = exportParams.getEndDate() != null ? exportParams.getEndDate() : new Date();
        List<DeletedWorklog> worklogList = manager.getWorklogsDeletedSince(dateFrom.getTime());
        worklogList.removeIf(w -> w.getDeletionTime().after(dateTo));
        return worklogList;
    }

    private void addAttribute(Document doc, Element element, String name, String value) {
        Attr attr = doc.createAttribute(name);
        attr.setValue(value);
        element.setAttributeNode(attr);
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
