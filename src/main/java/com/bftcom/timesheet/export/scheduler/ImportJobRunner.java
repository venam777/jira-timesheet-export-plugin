package com.bftcom.timesheet.export.scheduler;

import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import com.bftcom.timesheet.export.WorklogImporter;
import com.bftcom.timesheet.export.utils.Settings;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

/**
 * Import job
 */
public class ImportJobRunner implements JobRunner {

    @Nullable
    @Override
    public JobRunnerResponse runJob(JobRunnerRequest request) {
        try {
            WorklogImporter.getInstance().importWorklog(Settings.importDir);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            e.printStackTrace();
            return JobRunnerResponse.failed(e);
        }
        return JobRunnerResponse.success("Worklog import finished successfully!");
    }
}
