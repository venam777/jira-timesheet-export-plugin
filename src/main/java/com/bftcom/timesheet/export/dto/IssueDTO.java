package com.bftcom.timesheet.export.dto;

/**
 *
 */
public class IssueDTO {

    private int id;
    private String key;
    private String summary;
    private String financeProjectName;

    public IssueDTO(int id, String key, String summary, String financeProjectName) {
        this.id = id;
        this.key = key;
        this.summary = summary;
        this.financeProjectName = financeProjectName;
    }

    public int getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getSummary() {
        return summary;
    }

    public String getFinanceProjectName() {
        return financeProjectName;
    }
}
