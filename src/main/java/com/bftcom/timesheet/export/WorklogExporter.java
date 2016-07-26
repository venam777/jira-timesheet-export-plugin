package com.bftcom.timesheet.export;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.issue.worklog.DeletedWorklog;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.worklog.Worklog;
import com.atlassian.jira.issue.worklog.WorklogManager;
import com.atlassian.jira.project.Project;
import com.bftcom.timesheet.export.entity.WorklogData;
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

    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private Long financeProjectFieldId = 12500L;

    public WorklogExporter(WorklogDataDao dao) {
        this.dao = dao;
        manager = ComponentAccessor.getWorklogManager();
    }

    public void exportWorklog(WorklogExportParams params, String fileNameWithPath) throws TransformerException, ParserConfigurationException {
        Collection<Worklog> updatedWorklogs = getUpdatedWorklogs(params);
        Collection<DeletedWorklog> deletedWorklogs = getDeletedWorklogs(params);
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

            addAttribute(doc, timesheet, "ID", worklog.getId().toString());
            addAttribute(doc, timesheet, "ISSUE_ID", worklog.getIssue().getId().toString());
            Double spentTimeHours = (double) worklog.getTimeSpent() / 60 / 60;
            addAttribute(doc, timesheet, "AMOUNT", String.valueOf(Math.round(spentTimeHours * 100) / 100));
            addAttribute(doc, timesheet, "REMARK", worklog.getComment());
            addAttribute(doc, timesheet, "WORKDATE", dateFormat.format(worklog.getStartDate()));
            addAttribute(doc, timesheet, "PROJECTID", worklog.getIssue().getProjectObject().getId().toString());

            CustomField f = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(financeProjectFieldId);
            if (f != null) {
                String financeProjectName = (String) worklog.getIssue().getCustomFieldValue(f);
                if (financeProjectName != null && !financeProjectName.equals("")) {
                    String financeProjectId = financeProjectName.substring(financeProjectName.lastIndexOf('#') + 1);
                    addAttribute(doc, timesheet, "FINPROJECT_ID", financeProjectId);
                }
            }
            WorklogData info = dao.get(worklog.getId());
            if (info != null) {
                addAttribute(doc, timesheet, "REJECT_COMMENT", info.getRejectComment());
                addAttribute(doc, timesheet, "STATUS", info.getStatus());
            } else {
                addAttribute(doc, timesheet, "REJECT_COMMENT", "");
                addAttribute(doc, timesheet, "STATUS", "");
            }
            updatedTimesheets.appendChild(timesheet);
        }

        for (DeletedWorklog worklog : deletedWorklogs) {
            Element timesheet = doc.createElement("TIMESHEET");
            addAttribute(doc, timesheet, "ID", worklog.getId().toString());

            deletedTimesheets.appendChild(timesheet);
        }

        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(fileNameWithPath));

        transformer.transform(source, result);

        System.out.println("File saved!");
    }

    private Collection<Worklog> getUpdatedWorklogs(WorklogExportParams exportParams) {
        Long dateFrom = new Date().getTime();
        if (exportParams.getStartDate() != null) {
            dateFrom = exportParams.getStartDate().getTime();
        }
        List<Worklog> worklogList = manager.getWorklogsUpdatedSince(dateFrom);
        //статусы
        worklogList.removeIf(w -> dao.isWorklogExportable(w));
        //проекты
        if (exportParams.getProjects() != null && exportParams.getProjects().size() > 0) {
            worklogList.removeIf(w -> {
                for (Project p : exportParams.getProjects()) {
                    if (w.getIssue().getProjectObject().getKey().equals(p.getKey())) {
                        return false;
                    }
                }
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


}
