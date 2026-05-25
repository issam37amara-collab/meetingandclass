package com.site.meetingandclass.model;

import jakarta.persistence.*;

/**
 * Represents one scheduled class inside a ClassTimetable board.
 * Identified by: timetable + dayOfWeek + startTime + endTime.
 */
@Entity
@Table(name = "class_timetable_entries")
public class ClassTimetableEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "timetable_id", nullable = false)
    private ClassTimetable timetable;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    private String dayOfWeek;   // e.g. "MONDAY"
    private String startTime;   // e.g. "08:30"
    private String endTime;     // e.g. "10:00"

    private String courseName;
    private String courseCode;
    private String teacherName;
    private String studentGroup;

    public ClassTimetableEntry() {}

    // ── Transient helpers sent to the frontend ──────────────────────────────────

    @Transient
    public Long getRoomId() {
        return room != null ? room.getId() : null;
    }

    @Transient
    public String getRoomName() {
        return room != null ? room.getName() : null;
    }

    @Transient
    public Long getTimetableId() {
        return timetable != null ? timetable.getId() : null;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ClassTimetable getTimetable() { return timetable; }
    public void setTimetable(ClassTimetable timetable) { this.timetable = timetable; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public String getStudentGroup() { return studentGroup; }
    public void setStudentGroup(String studentGroup) { this.studentGroup = studentGroup; }
}
