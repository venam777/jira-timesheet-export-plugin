package com.bftcom.timesheet.export.dto;

/**
 *
 */
public class ProjectDTO {

    private int id;
    private String key;

    public ProjectDTO(int id, String key) {
        this.id = id;
        this.key = key;
    }

    public int getId() {
        return id;
    }

    public String getKey() {
        return key;
    }
}
