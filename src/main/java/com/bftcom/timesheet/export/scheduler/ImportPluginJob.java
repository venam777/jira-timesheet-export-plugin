package com.bftcom.timesheet.export.scheduler;

import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import com.bftcom.timesheet.export.WorklogImporter;
import com.bftcom.timesheet.export.utils.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Import job
 */
public class ImportPluginJob implements JobRunner {

    private static Logger logger = LoggerFactory.getLogger(ImportPluginJob.class);

    @Nullable
    @Override
    public JobRunnerResponse runJob(JobRunnerRequest request) {
        try {
            logger.debug("import job started, start date = " + request.getStartTime() +
                    ", parameters = " + request.getJobConfig().getParameters());
            WorklogImporter.getInstance().importWorklog(Settings.get("importDir"));
        } catch (ParserConfigurationException | IOException | SAXException e) {
            e.printStackTrace();
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return JobRunnerResponse.failed(sw.toString());
        }
        return JobRunnerResponse.success("Worklog import finished successfully!");
    }
}
