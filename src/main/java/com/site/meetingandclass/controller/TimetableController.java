package com.site.meetingandclass.controller;

import com.site.meetingandclass.model.TimetableEntry;
import com.site.meetingandclass.model.TimetableEntry.DayOfWeek;
import com.site.meetingandclass.service.TimetableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Path: src/main/java/com/site/meetingandclass/controller/TimetableController.java
 *
 * Also enables @Scheduled so the room sync runs automatically every minute.
 * Add @EnableScheduling to your main application class OR keep it here.
 */
@RestController
@RequestMapping("/api/timetable")
public class TimetableController {

    @Autowired
    private TimetableService timetableService;

    // ── GET all entries ────────────────────────────────────────────────────────
    // GET /api/timetable/all
    @GetMapping("/all")
    @PreAuthorize("isAuthenticated()")
    public List<TimetableEntry> getAll() {
        return timetableService.getAll();
    }

    // ── GET entries for today ──────────────────────────────────────────────────
    // GET /api/timetable/today
    @GetMapping("/today")
    @PreAuthorize("isAuthenticated()")
    public List<TimetableEntry> getToday() {
        return timetableService.getTodayEntries();
    }

    // ── GET entries for a specific day ────────────────────────────────────────
    // GET /api/timetable/day/MONDAY
    @GetMapping("/day/{day}")
    @PreAuthorize("isAuthenticated()")
    public List<TimetableEntry> getByDay(@PathVariable String day) {
        return timetableService.getByDay(DayOfWeek.valueOf(day.toUpperCase()));
    }

    // ── GET entries for a specific room ───────────────────────────────────────
    // GET /api/timetable/room/3
    @GetMapping("/room/{roomId}")
    @PreAuthorize("isAuthenticated()")
    public List<TimetableEntry> getByRoom(@PathVariable Long roomId) {
        return timetableService.getByRoom(roomId);
    }

    // ── POST add a timetable entry ────────────────────────────────────────────
    // POST /api/timetable/add
    // Body: {
    //   "roomId": 1,
    //   "dayOfWeek": "MONDAY",
    //   "startTime": "08:00",
    //   "endTime": "09:30",
    //   "courseName": "Advanced Mathematics",
    //   "courseCode": "MATH301",
    //   "teacherName": "Dr. Amara",
    //   "studentGroup": "Group A - 2nd Year"
    // }
    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('DIRECTOR_OF_INSTITUTE','HEAD_OF_DEPARTMENT','DIRECTOR_OF_STUDIES','SUPER_ADMIN')")
    public ResponseEntity<?> addEntry(@RequestBody Map<String, Object> body) {
        try {
            Long   roomId       = Long.valueOf(body.get("roomId").toString());
            String day          = body.get("dayOfWeek").toString();
            String startTime    = body.get("startTime").toString();
            String endTime      = body.get("endTime").toString();
            String courseName   = body.get("courseName").toString();
            String courseCode   = body.getOrDefault("courseCode", "").toString();
            String teacherName  = body.getOrDefault("teacherName", "").toString();
            String studentGroup = body.getOrDefault("studentGroup", "").toString();

            return ResponseEntity.ok(timetableService.addEntry(
                    roomId, day, startTime, endTime, courseName, courseCode, teacherName, studentGroup));
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }

    // ── PUT update a timetable entry ──────────────────────────────────────────
    // PUT /api/timetable/update/5
    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR_OF_INSTITUTE','HEAD_OF_DEPARTMENT','DIRECTOR_OF_STUDIES','SUPER_ADMIN')")
    public ResponseEntity<?> updateEntry(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Long   roomId       = Long.valueOf(body.get("roomId").toString());
            String day          = body.get("dayOfWeek").toString();
            String startTime    = body.get("startTime").toString();
            String endTime      = body.get("endTime").toString();
            String courseName   = body.get("courseName").toString();
            String courseCode   = body.getOrDefault("courseCode", "").toString();
            String teacherName  = body.getOrDefault("teacherName", "").toString();
            String studentGroup = body.getOrDefault("studentGroup", "").toString();

            return ResponseEntity.ok(timetableService.updateEntry(
                    id, roomId, day, startTime, endTime, courseName, courseCode, teacherName, studentGroup));
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }

    // ── DELETE a timetable entry ───────────────────────────────────────────────
    // DELETE /api/timetable/delete/5
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR_OF_INSTITUTE','HEAD_OF_DEPARTMENT','DIRECTOR_OF_STUDIES','SUPER_ADMIN')")
    public ResponseEntity<?> deleteEntry(@PathVariable Long id) {
        try {
            timetableService.deleteEntry(id);
            return ResponseEntity.ok("Entry deleted.");
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }

    // ── Manual sync trigger (for testing) ────────────────────────────────────
    // POST /api/timetable/sync
    @PostMapping("/sync")
    @PreAuthorize("hasAnyRole('DIRECTOR_OF_INSTITUTE','HEAD_OF_DEPARTMENT','DIRECTOR_OF_STUDIES','SUPER_ADMIN')")
    public ResponseEntity<String> syncNow() {
        timetableService.syncRoomStatusesWithTimetable();
        return ResponseEntity.ok("Room statuses synced with timetable.");
    }

    // ── AUTOMATIC SYNC every 1 minute ─────────────────────────────────────────
    // Spring runs this automatically. No frontend trigger needed.
    // Make sure @EnableScheduling is on your main class or this controller.
    @Scheduled(fixedRate = 60000) // every 60 seconds
    public void autoSync() {
        timetableService.syncRoomStatusesWithTimetable();
    }
}