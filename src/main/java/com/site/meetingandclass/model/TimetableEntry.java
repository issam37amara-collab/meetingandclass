package com.site.meetingandclass.model;

import jakarta.persistence.*;

/**
 * Represents one scheduled class in the university timetable.
 * e.g.: "Mathematics" in "Lecture Hall 101" every MONDAY from 08:00 to 09:30
 *
 * Path: src/main/java/com/site/meetingandclass/model/TimetableEntry.java
 */
@Entity
@Table(name = "timetable_entries")
public class TimetableEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The room this class takes place in
    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    // Day of week: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    // Times stored as "HH:mm" strings for simplicity, e.g. "08:00"
    private String startTime; // e.g. "08:00"
    private String endTime; // e.g. "09:30"

    // Course / module info
    private String courseName; // e.g. "Advanced Mathematics"
    private String courseCode; // e.g. "MATH301"
    private String teacherName; // e.g. "Dr. Amara"
    private String studentGroup; // e.g. "Group A - 2nd Year"

    public enum DayOfWeek {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }

    // ── Constructors ──────────────────────────────────────────────────────────
    public TimetableEntry() {
    }

    // ── Transient helpers for the frontend ────────────────────────────────────
    @Transient
    public Long getRoomId() {
        return room != null ? room.getId() : null;
    }

    @Transient
    public String getRoomName() {
        return room != null ? room.getName() : null;
    }

    @Transient
    public String getRoomNumber() {
        return room != null ? room.getRoomNumber() : null;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getStudentGroup() {
        return studentGroup;
    }

    public void setStudentGroup(String studentGroup) {
        this.studentGroup = studentGroup;
    }
}