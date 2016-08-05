package com.bftcom.timesheet.export.scheduler;

import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import com.bftcom.timesheet.export.WorklogExportParams;
import com.bftcom.timesheet.export.WorklogExporter;
import com.bftcom.timesheet.export.utils.Callback;
import com.bftcom.timesheet.export.utils.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Export job
 */
public class ExportPluginJob implements JobRunner {

    private Callback<WorklogExportParams> callback;
    private static Logger logger = LoggerFactory.getLogger(ExportPluginJob.class);

    public ExportPluginJob(Callback<WorklogExportParams> callback) {
        this.callback = callback;
    }

    @Nullable
    @Override
    public JobRunnerResponse runJob(JobRunnerRequest request) {
        try {
            logger.debug("export job started, start time = " + request.getStartTime()
                    + ", parameters = " + request.getJobConfig().getParameters());
            //todo ошибка в названии файла
            WorklogExporter.getInstance().exportWorklog(callback.call(), Settings.getExportFileName());
        } catch (TransformerException | ParserConfigurationException e) {
            e.printStackTrace();
            e.printStackTrace();
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return JobRunnerResponse.failed(sw.toString());
        }
        return JobRunnerResponse.success("Worklog export finished successfully!");
    }
}
