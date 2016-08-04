package com.bftcom.timesheet.export.scheduler;

import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import com.bftcom.timesheet.export.WorklogExportParams;
import com.bftcom.timesheet.export.WorklogExporter;
import com.bftcom.timesheet.export.utils.Callback;
import com.bftcom.timesheet.export.utils.Settings;

import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * Export job
 */
public class ExportPluginJob implements JobRunner {

    private Callback<WorklogExportParams> callback;

    public ExportPluginJob(Callback<WorklogExportParams> callback) {
        this.callback = callback;
    }

    @Nullable
    @Override
    public JobRunnerResponse runJob(JobRunnerRequest request) {
        try {
            //todo ошибка в названии файла
            WorklogExporter.getInstance().exportWorklog(callback.call(), Settings.getExportFileName());
        } catch (TransformerException | ParserConfigurationException e) {
            e.printStackTrace();
            return JobRunnerResponse.failed(e);
        }
        return JobRunnerResponse.success("Worklog export finished successfully!");
    }
}
