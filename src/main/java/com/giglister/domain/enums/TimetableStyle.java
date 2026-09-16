package com.giglister.domain.enums;

/** How an EventSeries' Timetable should be rendered - see EventSeries.timetableStyle.
 * LIST suits a series with few, easy-to-scan locations; GRID (locations as columns,
 * time as rows) suits a series with many simultaneous locations, where comparing
 * venues at a glance matters more than a flat chronological read. */
public enum TimetableStyle {
    LIST,
    GRID
}
