package com.bftcom.timesheet.export.provider;

import com.bftcom.timesheet.export.WorklogDataDao;
import com.bftcom.timesheet.export.WorklogExportParams;
import com.bftcom.timesheet.export.dto.IssueDTO;
import com.bftcom.timesheet.export.dto.ProjectDTO;
import com.bftcom.timesheet.export.dto.WorklogDTO;
import com.bftcom.timesheet.export.utils.SQLUtils;
import org.ofbiz.core.entity.GenericDataSourceException;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.jdbc.SQLProcessor;

import java.sql.*;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Провайдер таймшитов
 */
public class WorklogProvider {

    private WorklogDataDao worklogDataDao;

    public WorklogProvider(WorklogDataDao worklogDataDao) {
        this.worklogDataDao = worklogDataDao;
    }

    public Collection<WorklogDTO> getWorklogs(WorklogExportParams params, Long financeProjectFieldId) {
        Timestamp fromSqlDate = new Timestamp(params.getStartDate().getTime());//new Date(params.getStartDate().getTime());
        Timestamp toSqlDate = new Timestamp(params.getEndDate().getTime());
        SQLProcessor sqlProcessor = new SQLProcessor("defaultDS");
        List<WorklogDTO> result = new LinkedList<>();
        try {
            String select = "select w.id as worklog_id, w.author as worklog_author, w.worklogbody as worklog_body, w.startdate as worklog_startdate, w.timeworked as worklog_timeworked, i.id as issue_id, i.issuenum as issue_num, i.summary issue_summary, p.id as project_id, p.pkey as project_key, cfo.customvalue as finproject_name ";
            String from = " from worklog w join jiraissue i on w.issueid = i.id join project p on i.project = p.id left join customfieldvalue cf on i.id = cf.issue and cf.customfield = ? left join customfieldoption cfo on cf.stringvalue = cfo.id";
            String where = " where (w.created BETWEEN ? and ?)";
            /*w.startdate BETWEEN ? and ? or*/
            if (params.getProjects() != null && params.getProjects().size() > 0) {
                where += " and p.pkey in " + SQLUtils.collectionToString(params.getProjects(), source -> "'" + source.getKey() + "'");
            }
            if (params.getUsers() != null && params.getUsers().size() > 0) {
                where += " and w.author in " + SQLUtils.collectionToString(params.getUsers(), source -> "'" + source.getName() + "'");
            }
            sqlProcessor.prepareStatement(select + from + where);
            PreparedStatement ps = sqlProcessor.getPreparedStatement();
            int index = 1;
            ps.setLong(index++, financeProjectFieldId);
//            ps.setDate(index++, new Date(fromSqlDate.getTime()));
//            ps.setDate(index++, new Date(toSqlDate.getTime()));
            ps.setTimestamp(index++, fromSqlDate);
            ps.setTimestamp(index++, toSqlDate);
            //try {
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()) {
                String issueKey = resultSet.getString("project_key") + "-" + resultSet.getString("issue_num");
                IssueDTO issue = new IssueDTO(resultSet.getInt("issue_id"), issueKey,
                        resultSet.getString("issue_summary"), resultSet.getString("finproject_name"));
                ProjectDTO project = new ProjectDTO(resultSet.getInt("project_id"), resultSet.getString("project_key"));
                WorklogDTO worklog = new WorklogDTO(resultSet.getInt("worklog_id"), resultSet.getString("worklog_author"),
                        resultSet.getString("worklog_body"), resultSet.getDate("worklog_startdate"), resultSet.getLong("worklog_timeworked"), issue, project);
                if (params.isIncludeAllStatuses() || worklogDataDao.isWorklogExportable(worklog.getId())) {
                    result.add(worklog);
                }
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


}
