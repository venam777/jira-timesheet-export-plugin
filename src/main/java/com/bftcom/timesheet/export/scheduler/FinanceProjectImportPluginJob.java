package com.bftcom.timesheet.export.scheduler;

import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import com.bftcom.timesheet.export.FinanceProjectImporter;
import com.bftcom.timesheet.export.utils.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

/**
 *
 */
public class FinanceProjectImportPluginJob implements JobRunner {

    private static Logger logger = LoggerFactory.getLogger(FinanceProjectImportPluginJob.class);

    @Nullable
    @Override
    public JobRunnerResponse runJob(JobRunnerRequest request) {
        logger.debug("finance project import job started, start time = " + request.getStartTime()
                + ", parameters = " + request.getJobConfig().getParameters());
        try {
            FinanceProjectImporter.getInstance().startImport(Settings.get("financeProjectImportDir"));
        } catch (ParserConfigurationException | IOException | SAXException e) {
            e.printStackTrace();
            return JobRunnerResponse.failed(e);
        }
        return JobRunnerResponse.success("finance project import job run successfully");
    }
}
