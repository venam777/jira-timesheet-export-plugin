package com.bftcom.timesheet.export.events;

import java.util.Date;

public class ManualExportStartEvent {

    public final Date startDate;
    public final Date endDate;

    public ManualExportStartEvent(Date startDate, Date endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
