package com.bftcom.timesheet.export;

import com.atlassian.jira.bc.issue.worklog.DeletedWorklog;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.worklog.Worklog;
import com.atlassian.jira.issue.worklog.WorklogManager;
import com.atlassian.jira.project.Project;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


@Component
public class EntryPoint implements InitializingBean {

    private WorklogManager worklogManager;
    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private Long financeProjectFieldId = 12500L;

    @Override
    public void afterPropertiesSet() throws Exception {
        worklogManager = ComponentAccessor.getWorklogManager();
        main();
    }

    public void main() {
        //todo параметры
        Calendar calendarFrom = Calendar.getInstance();
        calendarFrom.set(Calendar.YEAR, 2016);
        calendarFrom.set(Calendar.MONTH, 6);
        calendarFrom.set(Calendar.DAY_OF_MONTH, 22);
        calendarFrom.set(Calendar.HOUR, 0);
        calendarFrom.set(Calendar.MINUTE, 0);
        calendarFrom.set(Calendar.SECOND, 0);

        Calendar calendarTo = Calendar.getInstance();
        calendarTo.set(Calendar.YEAR, 2016);
        calendarTo.set(Calendar.MONTH, 6);
        calendarTo.set(Calendar.DAY_OF_MONTH, 23);
        calendarTo.set(Calendar.HOUR, 0);
        calendarTo.set(Calendar.MINUTE, 0);
        calendarTo.set(Calendar.SECOND, 0);

        Collection<Worklog> worklogs = getUpdatedWorklogs(new WorklogExportParams(calendarFrom.getTime(), calendarTo.getTime(), Collections.<Project>emptyList()));
        Collection<DeletedWorklog> deletedWorklogs = getDeletedWorklogs(new WorklogExportParams(calendarFrom.getTime(), calendarTo.getTime(), Collections.<Project>emptyList()));

        //exportWorklogsToXML(worklogs, deletedWorklogs, "msg.xml");
    }

    private Collection<Worklog> getUpdatedWorklogs(WorklogExportParams exportParams) {
        Long dateFrom = new Date().getTime();
        if (exportParams.getStartDate() != null) {
            dateFrom = exportParams.getStartDate().getTime();
        }
        List<Worklog> worklogList = worklogManager.getWorklogsUpdatedSince(dateFrom);
        //проекты
        if (exportParams.getProjects() != null && exportParams.getProjects().size() > 0) {
            projectFilter(worklogList, exportParams.getProjects());
        }
        if (exportParams.getEndDate() != null) {
            endDateFilter(worklogList, exportParams.getEndDate());
        }
        return worklogList;
    }

    private Collection<DeletedWorklog> getDeletedWorklogs(WorklogExportParams exportParams) {
        Date dateFrom = new Date();
        Date dateTo = new Date();
        if (exportParams.getStartDate() != null) {
            dateFrom = exportParams.getStartDate();
        }
        if (exportParams.getEndDate() != null) {
            dateTo = exportParams.getEndDate();
        }
        List<DeletedWorklog> worklogList = worklogManager.getWorklogsDeletedSince(dateFrom.getTime());
        for (Iterator<DeletedWorklog> iterator = worklogList.iterator(); iterator.hasNext();) {
            DeletedWorklog w = iterator.next();
            if (w.getDeletionTime().after(dateTo)) {
                iterator.remove();
                break;
            }
        }
        return worklogList;
    }

    private void projectFilter(Collection<Worklog> worklogs, Collection<Project> projects) {
        for (Iterator<Worklog> iterator = worklogs.iterator(); iterator.hasNext(); ) {
            Worklog w = iterator.next();
            boolean f = false;
            for (Project p : projects) {
                if (w.getIssue().getProjectObject().getKey().equals(p.getKey())) {
                    f = true;
                    break;
                }
            }
            if (!f) {
                iterator.remove();
            }
        }
    }

    private void endDateFilter(Collection<Worklog> worklogs, Date endDate) {
        for (Iterator<Worklog> iterator = worklogs.iterator(); iterator.hasNext(); ) {
            if (iterator.next().getCreated().after(endDate)) {
                iterator.remove();
                break;
            }
        }
    }

    private void exportWorklogsToXML(Collection<Worklog> updatedWorklogs, Collection<DeletedWorklog> deletedWorklogs, String fileName) throws ParserConfigurationException, TransformerException {
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
            updatedTimesheets.appendChild(timesheet);
            //REJECT_COMMENT
            //STATUS
        }

        for (DeletedWorklog worklog : deletedWorklogs) {
            Element timesheet = doc.createElement("TIMESHEET");
            addAttribute(doc, timesheet, "ID", worklog.getId().toString());

            deletedTimesheets.appendChild(timesheet);
        }

        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(fileName));

        // Output to console for testing
        // StreamResult result = new StreamResult(System.out);

        transformer.transform(source, result);

        System.out.println("File saved!");

    }

    private void addAttribute(Document doc, Element element, String name, String value) {
        Attr attr = doc.createAttribute(name);
        attr.setValue(value);
        element.setAttributeNode(attr);
    }
}
