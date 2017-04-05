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
import com.atlassian.jira.user.ApplicationUser;
import com.bftcom.timesheet.export.dto.IssueDTO;
import com.bftcom.timesheet.export.dto.WorklogDTO;
import com.bftcom.timesheet.export.entity.WorklogData;
import com.bftcom.timesheet.export.provider.WorklogProvider;
import com.bftcom.timesheet.export.utils.Constants;
import com.bftcom.timesheet.export.utils.Parser;
import com.bftcom.timesheet.export.utils.SQLUtils;
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
    private WorklogProvider worklogProvider;

    //private Long financeProjectFieldId = 12500L;
    private static WorklogExporter instance;
    private static Logger logger = LoggerFactory.getLogger(WorklogExporter.class);

    private WorklogExporter(WorklogDataDao dao) {
        this.dao = dao;
        manager = ComponentAccessor.getWorklogManager();
        financeProjectField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(Constants.financeProjectFieldName);
        worklogProvider = new WorklogProvider(dao);
    }

    public void exportWorklog(WorklogExportParams params) throws TransformerException, ParserConfigurationException, IOException {
        exportWorklog(params, Settings.getExportFileName());
    }

    public void exportWorklog(WorklogExportParams params, String fileNameWithPath) throws TransformerException, ParserConfigurationException, IOException {
        logger.debug("Worklog export started");
        Collection<WorklogDTO> updatedWorklogs = getWorklogs(params);
        logger.debug("updated worklogs count (without status condition) to xml = " + updatedWorklogs.size());
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
        //Collection<Issue> issues = worklogProvider.getWorklogIssues(updatedWorklogs);
        Element issueRootElement = doc.createElement("CONTROL");
        rplElement.appendChild(issueRootElement);
        addAttribute(doc, issueRootElement, "RPL_OBJ_NAME", "CONTROL");
        Element updatedIssues = doc.createElement("CHANGED");
        issueRootElement.appendChild(updatedIssues);
        for (IssueDTO issue : getIssues(updatedWorklogs)) {
            Element control = doc.createElement("CONTROL");
            addAttribute(doc, control, "ID", issue.getKey());
            addAttribute(doc, control, "CAPTION", issue.getKey() + " " + issue.getSummary());
            addAttribute(doc, control, "URL", getShortIssueUrl(issue));
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
        Collection<WorklogData> updatedTimesheetsData = new LinkedList<>();
//        Map<Long, WorklogData> worklogDataMap = dao.getWorklogData(updatedWorklogs);
        for (WorklogDTO worklog : updatedWorklogs) {
            WorklogData info = dao.get(worklog.getId(), true);
            if (info != null) {
                if (!params.isIncludeAllStatuses() && info.getStatus().equalsIgnoreCase(WorklogData.APPROVED_STATUS)) {
                    logger.debug("Skipping worklog id = " + info.getWorklogId() + " because not includeAllStatuses and worklog status = APPROVED");
                    continue;
                }
                if (!params.isIgnoreExportedFlag() && info.isExported()) {
                    logger.debug("Skipping worklog id = " + info.getWorklogId() + " because not ignoreExportedFlag and exported = true");
                    continue;
                }
            }
            logger.debug("All is ok, we must export worklog with id = " + worklog.getId());
            Element timesheet = doc.createElement("TIMESHEET");
            logger.debug("writing updated worklog in xml");
            addAttribute(doc, timesheet, "ID", String.valueOf(worklog.getId()));
            addAttribute(doc, timesheet, "ISSUE_ID", String.valueOf(worklog.getIssue().getId()));
            addAttribute(doc, timesheet, "CONTROLID", worklog.getIssue().getKey());
            Double spentTimeHours = (double) worklog.getTimeworked() / 60 / 60;
            String amount = String.valueOf((double) Math.round(spentTimeHours * 100) / 100);
            addAttribute(doc, timesheet, "AMOUNT", amount);
            addAttribute(doc, timesheet, "REMARK", Parser.parseWorklogComment(worklog.getBody()));
            addAttribute(doc, timesheet, "WORKDATE", Settings.dateFormat.format(worklog.getDateWorked()));
            addAttribute(doc, timesheet, "PROJECTID", String.valueOf(worklog.getProject().getId()));
            addAttribute(doc, timesheet, "LDAPID", "BFT\\" + worklog.getAuthorName());
            logger.debug("worklog params: id = " + worklog.getId() + ", issue.id = " + worklog.getIssue().getId()
                    + ", amount = " + amount + ", comment = " + worklog.getBody() + ", workdate = " + Settings.dateFormat.format(worklog.getDateWorked())
                    + ", project.id = " + String.valueOf(worklog.getProject().getId()));
            String financeProjectId = getFinanceProjectId(worklog.getIssue());
            if (financeProjectId != null) {
                addAttribute(doc, timesheet, "FINPROJECTID", financeProjectId);
                logger.debug("worklog finance_project.id = " + financeProjectId);
            }
            addAttribute(doc, timesheet, "REJECT_COMMENT", info.getRejectComment());
            addAttribute(doc, timesheet, "STATUS", info.getStatus());
            logger.debug(" reject comment = " + info.getRejectComment());
            logger.debug(" status = " + info.getStatus());
            updatedTimesheets.appendChild(timesheet);
            updatedTimesheetsData.add(info);
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
        String fileName = fileNameWithPath.substring(fileNameWithPath.lastIndexOf(File.separator) + 1);
        String path = fileNameWithPath.substring(0, fileNameWithPath.lastIndexOf(File.separator));
        logger.debug("file directory: " + path);
        logger.debug("file name : " + fileName);
        File file = new File(path, fileName);
        file.createNewFile();
        StreamResult result = new StreamResult(file);
        transformer.transform(source, result);
        if (!params.isIgnoreExportedFlag()) {
            for (WorklogData worklogData : updatedTimesheetsData) {
                dao.setExported(worklogData, true);
            }
        }

        logger.debug("export finished successfully");
    }

    private String getFinanceProjectId(IssueDTO issue) {
        return issue.getFinanceProjectName() != null ? issue.getFinanceProjectName().substring(issue.getFinanceProjectName().lastIndexOf("#") + 1) : null;
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

    private Collection<WorklogDTO> getWorklogs(WorklogExportParams params) {
        return worklogProvider.getWorklogs(params, financeProjectField.getIdAsLong());
    }

    private Collection<IssueDTO> getIssues(Collection<WorklogDTO> worklogs) {
        Map<Integer, IssueDTO> result = new HashMap<>();
        for (WorklogDTO worklog : worklogs) {
            result.put(worklog.getIssue().getId(), worklog.getIssue());
        }
        return result.values();
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

    private String getShortIssueUrl(IssueDTO issue) {
        return ComponentAccessor.getApplicationProperties().getString("jira.baseurl") + "/browse/" + issue.getKey();
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
