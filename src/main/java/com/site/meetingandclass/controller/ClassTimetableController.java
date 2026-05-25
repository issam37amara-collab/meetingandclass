package com.site.meetingandclass.controller;

import com.site.meetingandclass.model.ClassTimetable;
import com.site.meetingandclass.model.ClassTimetableEntry;
import com.site.meetingandclass.service.ClassTimetableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for class timetable boards and their entries.
 *
 * Endpoints used by class-timetable.html:
 *   GET    /api/class-timetables/all
 *   POST   /api/class-timetables/add
 *   DELETE /api/class-timetables/delete/{id}
 *   POST   /api/class-timetables/{id}/entries/add
 *   PUT    /api/class-timetables/{id}/entries/update/{entryId}
 *   DELETE /api/class-timetables/{id}/entries/delete/{entryId}
 */
@RestController
@RequestMapping("/api/class-timetables")
public class ClassTimetableController {

    @Autowired
    private ClassTimetableService service;

    // ── GET all boards (with entries embedded) ────────────────────────────────
    @GetMapping("/all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAllBoards() {
        try {
            List<Map<String, Object>> boards = service.getAllBoards();
            return ResponseEntity.ok(boards);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ── POST create a new board ───────────────────────────────────────────────
    // Body: { "title": "1st Year Informatics – Semester 5" }
    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('DIRECTOR_OF_INSTITUTE','HEAD_OF_DEPARTMENT','DIRECTOR_OF_STUDIES','SUPER_ADMIN')")
    public ResponseEntity<?> createBoard(@RequestBody Map<String, Object> body) {
        try {
            String title = body.get("title").toString();
            ClassTimetable board = service.createBoard(title);
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("id",    board.getId());
            resp.put("title", board.getTitle());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }

    // ── DELETE a board and all its entries ────────────────────────────────────
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR_OF_INSTITUTE','HEAD_OF_DEPARTMENT','DIRECTOR_OF_STUDIES','SUPER_ADMIN')")
    public ResponseEntity<?> deleteBoard(@PathVariable Long id) {
        try {
            service.deleteBoard(id);
            return ResponseEntity.ok("Timetable deleted.");
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }

    // ── POST add a class entry to a board ─────────────────────────────────────
    // Body: { "roomId":1, "dayOfWeek":"MONDAY", "startTime":"08:30",
    //         "endTime":"10:00", "courseName":"Math", "courseCode":"MATH301",
    //         "teacherName":"Dr. X", "studentGroup":"Group A" }
    @PostMapping("/{id}/entries/add")
    @PreAuthorize("hasAnyRole('DIRECTOR_OF_INSTITUTE','HEAD_OF_DEPARTMENT','DIRECTOR_OF_STUDIES','SUPER_ADMIN')")
    public ResponseEntity<?> addEntry(@PathVariable Long id,
                                      @RequestBody Map<String, Object> body) {
        try {
            ClassTimetableEntry entry = service.addEntry(id, body);
            return ResponseEntity.ok(toMap(entry));
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }

    // ── PUT update an existing entry ──────────────────────────────────────────
    @PutMapping("/{id}/entries/update/{entryId}")
    @PreAuthorize("hasAnyRole('DIRECTOR_OF_INSTITUTE','HEAD_OF_DEPARTMENT','DIRECTOR_OF_STUDIES','SUPER_ADMIN')")
    public ResponseEntity<?> updateEntry(@PathVariable Long id,
                                         @PathVariable Long entryId,
                                         @RequestBody Map<String, Object> body) {
        try {
            ClassTimetableEntry entry = service.updateEntry(entryId, id, body);
            return ResponseEntity.ok(toMap(entry));
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }

    // ── DELETE a single entry ─────────────────────────────────────────────────
    @DeleteMapping("/{id}/entries/delete/{entryId}")
    @PreAuthorize("hasAnyRole('DIRECTOR_OF_INSTITUTE','HEAD_OF_DEPARTMENT','DIRECTOR_OF_STUDIES','SUPER_ADMIN')")
    public ResponseEntity<?> deleteEntry(@PathVariable Long id,
                                          @PathVariable Long entryId) {
        try {
            service.deleteEntry(entryId);
            return ResponseEntity.ok("Entry deleted.");
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }

    // ── Helper: entry → map (avoids Map.of() 10-entry limit) ─────────────────
    private Map<String, Object> toMap(ClassTimetableEntry e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",           e.getId());
        m.put("timetableId",  e.getTimetableId());
        m.put("roomId",       e.getRoomId());
        m.put("roomName",     e.getRoomName()     != null ? e.getRoomName()     : "");
        m.put("dayOfWeek",    e.getDayOfWeek());
        m.put("startTime",    e.getStartTime());
        m.put("endTime",      e.getEndTime());
        m.put("courseName",   e.getCourseName());
        m.put("courseCode",   e.getCourseCode()   != null ? e.getCourseCode()   : "");
        m.put("teacherName",  e.getTeacherName()  != null ? e.getTeacherName()  : "");
        m.put("studentGroup", e.getStudentGroup() != null ? e.getStudentGroup() : "");
        return m;
    }
}
