package com.bftcom.timesheet.export.dto;

import java.util.Date;

/**
 *
 */
public class WorklogDTO {

    private int id;
    private String authorName;
    private String body;
    private Date dateCreated;
    private Long timeworked;
    private IssueDTO issue;
    private ProjectDTO project;

    public WorklogDTO(int id, String authorName, String body, Date dateCreated, Long timeworked, IssueDTO issue, ProjectDTO project) {
        this.id = id;
        this.authorName = authorName;
        this.body = body;
        this.dateCreated = dateCreated;
        this.timeworked = timeworked;
        this.issue = issue;
        this.project = project;
    }

    public int getId() {
        return id;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getBody() {
        return body;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public Long getTimeworked() {
        return timeworked;
    }

    public IssueDTO getIssue() {
        return issue;
    }

    public ProjectDTO getProject() {
        return project;
    }
}
