package com.bftcom.timesheet.export.entity;

import java.util.HashMap;
import java.util.Map;

@Deprecated
public class WorklogDataStyle {

    public static class Color {
        public final String titleColor;
        public final String borderColor;
        public final String backgroundColor;

        public Color(String titleColor, String borderColor, String backgroundColor) {
            this.titleColor = titleColor;
            this.borderColor = borderColor;
            this.backgroundColor = backgroundColor;
        }
    }

    public static Map<String, Color> style = new HashMap<>();
    static {
        style.put(WorklogData.NOT_VIEWED_STATUS, new Color("#BFBBBB", "#CBE7FB", "#E0DDDD"));
        style.put(WorklogData.APPROVED_STATUS, new Color("#8AC729", "#CBE7FB", "#BADBAD"));
        style.put(WorklogData.REJECTED_STATUS, new Color("#FB2222", "#CBE7FB", "#F36D6D"));
    }
}
