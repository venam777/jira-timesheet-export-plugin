package com.bftcom.timesheet.export.entity;

import net.java.ao.Entity;

/**
 * Дополнительная информация к worklog'ам
 */
public interface WorklogData extends Entity {

    String REJECTED_STATUS = "Отклонено";
    String APPROVED_STATUS = "Утверждено";
    String NOT_VIEWED_STATUS = "Не просмотрено";

    Long getWorklogId();
    void setWorklogId(Long worklogId);

    String getStatus();
    void setStatus(String status);

    String getRejectComment();
    void setRejectComment(String rejectComment);

}
