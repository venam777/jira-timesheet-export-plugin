package com.bftcom.timesheet.export.provider;

import com.bftcom.timesheet.export.WorklogExportParams;
import com.bftcom.timesheet.export.dto.IssueDTO;
import com.bftcom.timesheet.export.dto.ProjectDTO;
import com.bftcom.timesheet.export.dto.WorklogDTO;
import com.bftcom.timesheet.export.utils.SQLUtils;
import org.ofbiz.core.entity.GenericDataSourceException;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.jdbc.SQLProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Провайдер таймшитов
 */
public class WorklogProvider {

    private static final Logger logger = LoggerFactory.getLogger(WorklogProvider.class);

    public Collection<WorklogDTO> getWorklogs(WorklogExportParams params, Long financeProjectFieldId) {
        logger.debug("WorklogProvider#getWorklogs started, params : " + params.toString() + ", financeProjectFieldId = " + financeProjectFieldId);
        SQLProcessor sqlProcessor = new SQLProcessor("defaultDS");
        List<WorklogDTO> result = new LinkedList<>();
        try {
            String select = "select w.id as worklog_id, w.author as worklog_author, w.worklogbody as worklog_body, w.startdate as worklog_startdate, w.timeworked as worklog_timeworked, i.id as issue_id, i.issuenum as issue_num, i.summary issue_summary, p.id as project_id, p.pkey as project_key, cfo.customvalue as finproject_name, concat(concat(p.pkey, '-'), i.issuenum) as issuekey ";
            String from = " from worklog w join jiraissue i on w.issueid = i.id join project p on i.project = p.id left join customfieldvalue cf on i.id = cf.issue and cf.customfield = ? left join customfieldoption cfo on cf.stringvalue = cfo.id";
            String where = " where 1=1 ";
            //(w.created BETWEEN ? and ?)
            if (params.getStartDate() != null) {
                where += " and w.created >= ? ";
            }
            if (params.getEndDate() != null) {
                where += " and w.created <= ? ";
            }
            /*w.startdate BETWEEN ? and ? or*/
            if (params.getProjects() != null && params.getProjects().size() > 0) {
                where += " and p.pkey in " + SQLUtils.collectionToString(params.getProjects(), source -> "'" + source.getKey() + "'");
            }
            if (params.getUsers() != null && params.getUsers().size() > 0) {
                where += " and w.author in " + SQLUtils.collectionToString(params.getUsers(), source -> "'" + source.getName() + "'");
            }
            if (params.getWorklogStartDate() != null) {
                where += " and w.startdate >= ? ";
            }
            if (params.getWorklogEndDate() != null) {
                where += " and w.startdate <= ? ";
            }
            String outerQuery = "select worklog_id, worklog_author, worklog_body, worklog_startdate, worklog_timeworked, issue_id, issue_num, issue_summary, project_id, project_key, finproject_name, issuekey from ( "
                    + select + from + where + ") t where 1=1 ";
            if (params.getIssueKeys() != null && params.getIssueKeys().size() > 0) {
                outerQuery += " and issuekey in " + collectionToString(params.getIssueKeys());
            }
            logger.debug("Query = " + outerQuery);
            sqlProcessor.prepareStatement(outerQuery);
            PreparedStatement ps = sqlProcessor.getPreparedStatement();
            int index = 1;
            logger.debug("parameter " + index + " : " + financeProjectFieldId);
            ps.setLong(index++, financeProjectFieldId);
            if (params.getStartDate() != null) {
                ps.setTimestamp(index++, new Timestamp(params.getStartDate().getTime()));
            }
            if (params.getEndDate() != null) {
                ps.setTimestamp(index++, new Timestamp(params.getEndDate().getTime()));
            }
            if (params.getWorklogStartDate() != null) {
                ps.setTimestamp(index++, new Timestamp(params.getWorklogStartDate().getTime()));
            }
            if (params.getWorklogEndDate() != null) {
                ps.setTimestamp(index++, new Timestamp(params.getWorklogEndDate().getTime()));
            }
            //try {
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()) {
                IssueDTO issue = new IssueDTO(resultSet.getInt("issue_id"), resultSet.getString("issuekey"),
                        resultSet.getString("issue_summary"), resultSet.getString("finproject_name"));
                ProjectDTO project = new ProjectDTO(resultSet.getInt("project_id"), resultSet.getString("project_key"));
                WorklogDTO worklog = new WorklogDTO(resultSet.getInt("worklog_id"), resultSet.getString("worklog_author"),
                        resultSet.getString("worklog_body"), resultSet.getDate("worklog_startdate"), resultSet.getLong("worklog_timeworked"), issue, project);
                result.add(worklog);
            }
            //}
        } catch (GenericEntityException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                sqlProcessor.close();
            } catch (GenericDataSourceException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    protected String collectionToString(Collection<String> collection) {
        if (collection == null || collection.size() == 0) {
            return "('')";
        }
        Iterator<String> iterator = collection.iterator();
        StringBuilder builder = new StringBuilder("(");
        while(iterator.hasNext()) {
            String element = iterator.next();
            builder.append("'").append(element.toUpperCase().trim()).append("'");
            if (iterator.hasNext()) {
                builder.append(",");
            }
        }
        builder.append(")");
        return builder.toString();
    }

}
