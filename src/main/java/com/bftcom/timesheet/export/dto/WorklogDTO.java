package com.bftcom.timesheet.export.dto;

import java.util.Date;

/**
 *
 */
public class WorklogDTO {

    private long id;
    private String authorName;
    private String body;
    private Date dateWorked;
    private Long timeworked;
    private IssueDTO issue;
    private ProjectDTO project;

    public WorklogDTO(long id, String authorName, String body, Date dateWorked, Long timeworked, IssueDTO issue, ProjectDTO project) {
        this.id = id;
        this.authorName = authorName;
        this.body = body;
        this.dateWorked = dateWorked;
        this.timeworked = timeworked;
        this.issue = issue;
        this.project = project;
    }

    public Long getId() {
        return id;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getBody() {
        return body;
    }

    public Date getDateWorked() {
        return dateWorked;
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
