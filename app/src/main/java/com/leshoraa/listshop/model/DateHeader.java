package com.leshoraa.listshop.model;

public class DateHeader {
    private String uiDate; // e.g., "27 Juni"
    private String dbDate; // e.g., "2025-06-27"

    public DateHeader(String uiDate, String dbDate) {
        this.uiDate = uiDate;
        this.dbDate = dbDate;
    }

    public String getUiDate() {
        return uiDate;
    }

    public String getDbDate() {
        return dbDate;
    }
}