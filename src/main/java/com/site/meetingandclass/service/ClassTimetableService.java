package com.site.meetingandclass.service;

import com.site.meetingandclass.model.ClassTimetable;
import com.site.meetingandclass.model.ClassTimetableEntry;
import com.site.meetingandclass.model.Room;
import com.site.meetingandclass.repository.ClassTimetableEntryRepository;
import com.site.meetingandclass.repository.ClassTimetableRepository;
import com.site.meetingandclass.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ClassTimetableService {

    @Autowired
    private ClassTimetableRepository boardRepo;

    @Autowired
    private ClassTimetableEntryRepository entryRepo;

    @Autowired
    private RoomRepository roomRepo;

    @Autowired
    private com.site.meetingandclass.repository.TimetableRepository classicTimetableRepo;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // ── 1. Get ALL boards with their entries embedded ─────────────────────────
    // Returns a list of maps so the frontend receives exactly:
    // [{ "id":1, "title":"...", "entries":[{...}] }, ...]
    public List<Map<String, Object>> getAllBoards() {
        List<ClassTimetable> boards = boardRepo.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (ClassTimetable board : boards) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", board.getId());
            map.put("title", board.getTitle());
            map.put("entries", buildEntryList(entryRepo.findByTimetableId(board.getId())));
            result.add(map);
        }
        return result;
    }

    // ── 2. Create a new board ─────────────────────────────────────────────────
    public ClassTimetable createBoard(String title) {
        if (title == null || title.isBlank()) {
            throw new RuntimeException("Title cannot be empty.");
        }
        return boardRepo.save(new ClassTimetable(title));
    }

    // ── 3. Delete a board and ALL its entries ─────────────────────────────────
    @Transactional
    public void deleteBoard(Long boardId) {
        if (!boardRepo.existsById(boardId)) {
            throw new RuntimeException("Timetable not found: " + boardId);
        }
        entryRepo.deleteByTimetableId(boardId);
        boardRepo.deleteById(boardId);
    }

    // ── 4. Add an entry to a board (with room conflict check) ─────────────────
    public ClassTimetableEntry addEntry(Long boardId, Map<String, Object> body) {
        ClassTimetable board = boardRepo.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Timetable not found: " + boardId));

        Long roomId     = Long.valueOf(body.get("roomId").toString());
        String day      = body.get("dayOfWeek").toString().toUpperCase();
        String start    = body.get("startTime").toString();
        String end      = body.get("endTime").toString();
        String course   = body.get("courseName").toString();
        String code     = body.getOrDefault("courseCode",    "").toString();
        String teacher  = body.getOrDefault("teacherName",   "").toString();
        String group    = body.getOrDefault("studentGroup",  "").toString();

        Room room = roomRepo.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found: " + roomId));

        ensureNoOverlap(roomId, day, start, end, null, room.getName());

        ClassTimetableEntry entry = new ClassTimetableEntry();
        entry.setTimetable(board);
        entry.setRoom(room);
        entry.setDayOfWeek(day);
        entry.setStartTime(start);
        entry.setEndTime(end);
        entry.setCourseName(course);
        entry.setCourseCode(code);
        entry.setTeacherName(teacher);
        entry.setStudentGroup(group);

        return entryRepo.save(entry);
    }

    // ── 5. Update an existing entry ────────────────────────────────────────────
    public ClassTimetableEntry updateEntry(Long entryId, Long boardId, Map<String, Object> body) {
        ClassTimetableEntry entry = entryRepo.findById(entryId)
                .orElseThrow(() -> new RuntimeException("Entry not found: " + entryId));

        Long roomId     = Long.valueOf(body.get("roomId").toString());
        String day      = body.get("dayOfWeek").toString().toUpperCase();
        String start    = body.get("startTime").toString();
        String end      = body.get("endTime").toString();
        String course   = body.get("courseName").toString();
        String code     = body.getOrDefault("courseCode",   "").toString();
        String teacher  = body.getOrDefault("teacherName",  "").toString();
        String group    = body.getOrDefault("studentGroup", "").toString();

        Room room = roomRepo.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found: " + roomId));

        ensureNoOverlap(roomId, day, start, end, entryId, room.getName());

        entry.setRoom(room);
        entry.setDayOfWeek(day);
        entry.setStartTime(start);
        entry.setEndTime(end);
        entry.setCourseName(course);
        entry.setCourseCode(code);
        entry.setTeacherName(teacher);
        entry.setStudentGroup(group);

        return entryRepo.save(entry);
    }

    // ── 6. Delete a single entry ───────────────────────────────────────────────
    public void deleteEntry(Long entryId) {
        if (!entryRepo.existsById(entryId)) {
            throw new RuntimeException("Entry not found: " + entryId);
        }
        entryRepo.deleteById(entryId);
    }

    /**
     * Reject if any other class-timetable entry OR classic timetable entry
     * for the same room and day overlaps the requested [start, end) window.
     */
    private void ensureNoOverlap(Long roomId, String day, String startStr, String endStr,
                                 Long ignoreEntryId, String roomName) {
        LocalTime newStart, newEnd;
        try {
            newStart = LocalTime.parse(startStr, TIME_FMT);
            newEnd   = LocalTime.parse(endStr,   TIME_FMT);
        } catch (Exception e) {
            throw new IllegalArgumentException("startTime/endTime must be HH:mm.");
        }
        if (!newStart.isBefore(newEnd)) {
            throw new IllegalArgumentException("startTime must be before endTime.");
        }

        // Other class-timetable entries on the same day, any board.
        for (ClassTimetableEntry e : entryRepo.findByDayOfWeek(day)) {
            if (ignoreEntryId != null && ignoreEntryId.equals(e.getId())) continue;
            if (e.getRoom() == null || !roomId.equals(e.getRoom().getId())) continue;
            try {
                LocalTime s = LocalTime.parse(e.getStartTime(), TIME_FMT);
                LocalTime f = LocalTime.parse(e.getEndTime(),   TIME_FMT);
                if (newStart.isBefore(f) && s.isBefore(newEnd)) {
                    throw new IllegalArgumentException(
                        "Room \"" + roomName + "\" is already booked on " + day
                            + " " + e.getStartTime() + "–" + e.getEndTime() + ".");
                }
            } catch (java.time.format.DateTimeParseException ignored) { }
        }

        // Classic timetable entries (the other board).
        com.site.meetingandclass.model.TimetableEntry.DayOfWeek classicDay;
        try {
            classicDay = com.site.meetingandclass.model.TimetableEntry.DayOfWeek.valueOf(day);
        } catch (IllegalArgumentException ex) { return; }

        for (com.site.meetingandclass.model.TimetableEntry t :
                classicTimetableRepo.findByRoomIdAndDayOfWeek(roomId, classicDay)) {
            try {
                LocalTime s = LocalTime.parse(t.getStartTime(), TIME_FMT);
                LocalTime f = LocalTime.parse(t.getEndTime(),   TIME_FMT);
                if (newStart.isBefore(f) && s.isBefore(newEnd)) {
                    throw new IllegalArgumentException(
                        "Room \"" + roomName + "\" conflicts with the classic timetable on "
                            + day + " " + t.getStartTime() + "–" + t.getEndTime() + ".");
                }
            } catch (java.time.format.DateTimeParseException ignored) { }
        }
    }

    // ── Helper: convert entries to frontend-friendly maps ─────────────────────
    private List<Map<String, Object>> buildEntryList(List<ClassTimetableEntry> entries) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ClassTimetableEntry e : entries) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",           e.getId());
            m.put("timetableId",  e.getTimetableId());
            m.put("roomId",       e.getRoomId());
            m.put("roomName",     e.getRoomName());
            m.put("dayOfWeek",    e.getDayOfWeek());
            m.put("startTime",    e.getStartTime());
            m.put("endTime",      e.getEndTime());
            m.put("courseName",   e.getCourseName());
            m.put("courseCode",   e.getCourseCode());
            m.put("teacherName",  e.getTeacherName());
            m.put("studentGroup", e.getStudentGroup());
            list.add(m);
        }
        return list;
    }
}
