package com.site.meetingandclass.model;

import jakarta.persistence.*;

/**
 * Represents a named timetable board (e.g. "1st Year Informatics – Semester 5").
 * Each board holds multiple ClassTimetableEntry records.
 */
@Entity
@Table(name = "class_timetables")
public class ClassTimetable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    public ClassTimetable() {}

    public ClassTimetable(String title) {
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
