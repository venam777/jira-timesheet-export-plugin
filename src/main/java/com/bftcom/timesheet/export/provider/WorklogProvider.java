package com.bftcom.timesheet.export.provider;

import com.bftcom.timesheet.export.WorklogDataDao;
import com.bftcom.timesheet.export.WorklogExportParams;
import com.bftcom.timesheet.export.dto.IssueDTO;
import com.bftcom.timesheet.export.dto.ProjectDTO;
import com.bftcom.timesheet.export.dto.WorklogDTO;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.jdbc.SQLProcessor;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Провайдер таймшитов
 */
public class WorklogProvider {

    private interface Converter<FROM, TO> {

        TO convert(FROM source);

    }

    private WorklogDataDao worklogDataDao;

    public WorklogProvider(WorklogDataDao worklogDataDao) {
        this.worklogDataDao = worklogDataDao;
    }

    public Collection<WorklogDTO> getWorklogs(WorklogExportParams params, Long financeProjectFieldId) {
        Date fromSqlDate = new Date(params.getStartDate().getTime());
        Date toSqlDate = new Date(params.getEndDate().getTime());
        SQLProcessor sqlProcessor = new SQLProcessor("defaultDS");
        List<WorklogDTO> result = new LinkedList<>();
        try {
            String select = "select w.id as worklog_id, w.author as worklog_author, w.worklogbody as worklog_body, w.created as worklog_created, w.timeworked as worklog_timeworked, i.id as issue_id, i.issuenum as issue_num, i.summary issue_summary, p.id as project_id, p.pkey as project_key, cfo.customvalue as finproject_name ";
            String from = " from worklog w join jiraissue i on w.issueid = i.id join project p on i.project = p.id left join customfieldvalue cf on i.id = cf.issue and cf.customfield = ? left join customfieldoption cfo on cf.stringvalue = cfo.id";
            String where = " where (w.startdate BETWEEN ? and ? or w.created BETWEEN ? and ?)";
            if (params.getProjects() != null && params.getProjects().size() > 0) {
                where += " and p.pkey in " + collectionToString(params.getProjects(), source -> "'" + source.getKey() + "'");
            }
            if (params.getUsers() != null && params.getUsers().size() > 0) {
                where += " and w.author in " + collectionToString(params.getUsers(), source -> "'" + source.getName() + "'");
            }
            sqlProcessor.prepareStatement(select + from + where);
            PreparedStatement ps = sqlProcessor.getPreparedStatement();
            int index = 1;
            ps.setLong(index++, financeProjectFieldId);
            ps.setDate(index++, fromSqlDate);
            ps.setDate(index++, toSqlDate);
            ps.setDate(index++, fromSqlDate);
            ps.setDate(index++, toSqlDate);
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()) {
                String issueKey = resultSet.getString("project_key") + "-" + resultSet.getString("issue_num");
                IssueDTO issue = new IssueDTO(resultSet.getInt("issue_id"), issueKey,
                        resultSet.getString("issue_summary"), resultSet.getString("finproject_name"));
                ProjectDTO project = new ProjectDTO(resultSet.getInt("project_id"), resultSet.getString("project_key"));
                WorklogDTO worklog = new WorklogDTO(resultSet.getInt("worklog_id"), resultSet.getString("worklog_author"),
                        resultSet.getString("worklog_body"), resultSet.getDate("worklog_created"), resultSet.getLong("worklog_timeworked"), issue, project);
                if (params.isIncludeAllStatuses() || worklogDataDao.isWorklogExportable(worklog.getId())) {
                    result.add(worklog);
                }
            }
        } catch (GenericEntityException | SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    protected <T> String collectionToString(Collection<T> collection, Converter<T, String> converter) {
        if (collection == null || collection.size() ==0) {
            return "()";
        }
        StringBuilder builder = new StringBuilder("(");
        for (T value : collection) {
            builder.append(converter.convert(value)).append(", ");
        }
        return builder.substring(0, builder.length() - 2) + ")";
    }


}
