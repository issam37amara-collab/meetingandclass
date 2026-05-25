package com.site.meetingandclass.service;

import com.site.meetingandclass.model.ClassTimetableEntry;
import com.site.meetingandclass.model.Room;
import com.site.meetingandclass.model.TimetableEntry;
import com.site.meetingandclass.model.TimetableEntry.DayOfWeek;
import com.site.meetingandclass.repository.ClassTimetableEntryRepository;
import com.site.meetingandclass.repository.RoomRepository;
import com.site.meetingandclass.repository.TimetableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class TimetableService {

    @Autowired
    private TimetableRepository timetableRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private ClassTimetableEntryRepository classEntryRepository;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // ── 1. Get full timetable ─────────────────────────────────────────────────
    public List<TimetableEntry> getAll() {
        return timetableRepository.findAll();
    }

    // ── 2. Get timetable for a specific day ───────────────────────────────────
    public List<TimetableEntry> getByDay(DayOfWeek day) {
        return timetableRepository.findByDayOfWeek(day);
    }

    // ── 3. Get timetable for a specific room ──────────────────────────────────
    public List<TimetableEntry> getByRoom(Long roomId) {
        return timetableRepository.findByRoomId(roomId);
    }

    // ── 4. Add a timetable entry (admin) ──────────────────────────────────────
    public TimetableEntry addEntry(Long roomId, String dayStr, String startTime,
            String endTime, String courseName, String courseCode,
            String teacherName, String studentGroup) {
        DayOfWeek day = DayOfWeek.valueOf(dayStr.toUpperCase());
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found: " + roomId));

        validateTimeRange(startTime, endTime);
        ensureNoConflict(roomId, day, startTime, endTime, null);

        TimetableEntry entry = new TimetableEntry();
        entry.setRoom(room);
        entry.setDayOfWeek(day);
        entry.setStartTime(startTime);
        entry.setEndTime(endTime);
        entry.setCourseName(courseName);
        entry.setCourseCode(courseCode);
        entry.setTeacherName(teacherName);
        entry.setStudentGroup(studentGroup);

        return timetableRepository.save(entry);
    }

    // ── 5. Update a timetable entry (admin) ───────────────────────────────────
    public TimetableEntry updateEntry(Long id, Long roomId, String dayStr, String startTime,
            String endTime, String courseName, String courseCode,
            String teacherName, String studentGroup) {
        TimetableEntry entry = timetableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entry not found: " + id));

        DayOfWeek day = DayOfWeek.valueOf(dayStr.toUpperCase());
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found: " + roomId));

        validateTimeRange(startTime, endTime);
        ensureNoConflict(roomId, day, startTime, endTime, id);

        entry.setRoom(room);
        entry.setDayOfWeek(day);
        entry.setStartTime(startTime);
        entry.setEndTime(endTime);
        entry.setCourseName(courseName);
        entry.setCourseCode(courseCode);
        entry.setTeacherName(teacherName);
        entry.setStudentGroup(studentGroup);

        return timetableRepository.save(entry);
    }

    /**
     * Reject if any other timetable entry (or class-timetable entry) for the
     * same room and same day overlaps the requested [start, end) window.
     */
    private void ensureNoConflict(Long roomId, DayOfWeek day, String startStr,
                                  String endStr, Long ignoreEntryId) {
        LocalTime newStart = LocalTime.parse(startStr, TIME_FMT);
        LocalTime newEnd   = LocalTime.parse(endStr,   TIME_FMT);

        // Classic timetable entries for the same room/day.
        for (TimetableEntry t : timetableRepository.findByRoomIdAndDayOfWeek(roomId, day)) {
            if (ignoreEntryId != null && ignoreEntryId.equals(t.getId())) continue;
            try {
                LocalTime s = LocalTime.parse(t.getStartTime(), TIME_FMT);
                LocalTime e = LocalTime.parse(t.getEndTime(),   TIME_FMT);
                if (overlaps(newStart, newEnd, s, e)) {
                    throw new IllegalArgumentException(
                        "Room is already booked on " + day + " between "
                            + t.getStartTime() + " and " + t.getEndTime() + ".");
                }
            } catch (java.time.format.DateTimeParseException ignored) { /* skip malformed legacy rows */ }
        }

        // Class-timetable boards: same room, same day-of-week.
        for (com.site.meetingandclass.model.ClassTimetableEntry c : classEntryRepository.findByDayOfWeek(day.name())) {
            if (c.getRoomId() == null || !c.getRoomId().equals(roomId)) continue;
            try {
                LocalTime s = LocalTime.parse(c.getStartTime(), TIME_FMT);
                LocalTime e = LocalTime.parse(c.getEndTime(),   TIME_FMT);
                if (overlaps(newStart, newEnd, s, e)) {
                    throw new IllegalArgumentException(
                        "Room conflicts with a class-timetable entry on " + day + " "
                            + c.getStartTime() + "–" + c.getEndTime() + ".");
                }
            } catch (java.time.format.DateTimeParseException ignored) { }
        }
    }

    private boolean overlaps(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    private void validateTimeRange(String startStr, String endStr) {
        try {
            LocalTime s = LocalTime.parse(startStr, TIME_FMT);
            LocalTime e = LocalTime.parse(endStr,   TIME_FMT);
            if (!s.isBefore(e)) {
                throw new IllegalArgumentException("startTime must be before endTime.");
            }
        } catch (java.time.format.DateTimeParseException ex) {
            throw new IllegalArgumentException("startTime/endTime must be HH:mm.");
        }
    }

    // ── 6. Delete a timetable entry (admin) ───────────────────────────────────
    public void deleteEntry(Long id) {
        timetableRepository.deleteById(id);
    }

    // ── 7. KEY METHOD: Sync all room statuses based on current day & time ─────
    //
    // Call this whenever rooms are loaded.
    // Logic:
    // - Get today's day (e.g. MONDAY)
    // - Get current time
    // - For each room: if there's a timetable entry whose window covers NOW →
    // RESERVED
    // - If not covered by timetable → restore to AVAILABLE
    // (but don't touch PENDING rooms — they're waiting for admin approval)
    //
    public void syncRoomStatusesWithTimetable() {
        DayOfWeek today = getCurrentDayOfWeek();
        String todayStr = today.name(); // e.g. "MONDAY" — matches ClassTimetableEntry.dayOfWeek
        LocalTime now = LocalTime.now();

        // Entries from the classic timetable.html
        List<TimetableEntry> todayEntries = timetableRepository.findByDayOfWeek(today);

        // Entries from the new class-timetable.html (all boards)
        List<ClassTimetableEntry> todayClassEntries = classEntryRepository.findByDayOfWeek(todayStr);

        List<Room> allRooms = roomRepository.findAll();

        for (Room room : allRooms) {
            // Never touch a room that's in PENDING (waiting for admin decision)
            if ("PENDING".equals(room.getStatus()))
                continue;

            // Check classic timetable.html entries
            boolean occupiedByOldTt = todayEntries.stream()
                    .filter(e -> e.getRoom().getId().equals(room.getId()))
                    .anyMatch(e -> isCurrentlyOccupied(e, now));

            // Check class-timetable.html entries (from all boards)
            boolean occupiedByClassTt = todayClassEntries.stream()
                    .filter(e -> e.getRoomId() != null && e.getRoomId().equals(room.getId()))
                    .anyMatch(e -> isClassEntryOccupied(e, now));

            boolean occupiedNow = occupiedByOldTt || occupiedByClassTt;

            if (occupiedNow) {
                room.setStatus("RESERVED");
            } else {
                if ("RESERVED".equals(room.getStatus())) {
                    room.setStatus("AVAILABLE");
                }
            }
            roomRepository.save(room);
        }
    }

    // ── 8a. Check if a classic TimetableEntry covers NOW ─────────────────────
    private boolean isCurrentlyOccupied(TimetableEntry entry, LocalTime now) {
        try {
            LocalTime start = LocalTime.parse(entry.getStartTime(), TIME_FMT);
            LocalTime end = LocalTime.parse(entry.getEndTime(), TIME_FMT);
            return !now.isBefore(start) && now.isBefore(end);
        } catch (Exception e) {
            return false;
        }
    }

    // ── 8b. Check if a ClassTimetableEntry (from class-timetable.html) covers NOW
    private boolean isClassEntryOccupied(ClassTimetableEntry entry, LocalTime now) {
        try {
            LocalTime start = LocalTime.parse(entry.getStartTime(), TIME_FMT);
            LocalTime end = LocalTime.parse(entry.getEndTime(), TIME_FMT);
            return !now.isBefore(start) && now.isBefore(end);
        } catch (Exception e) {
            return false;
        }
    }

    // ── 9. Convert Java's DayOfWeek to our enum ────────────────────────────────
    private DayOfWeek getCurrentDayOfWeek() {
        java.time.DayOfWeek javaDow = java.time.LocalDate.now().getDayOfWeek();
        return DayOfWeek.valueOf(javaDow.name()); // Both use same names: MONDAY, TUESDAY...
    }

    // ── 10. Get today's timetable entries (used by frontend) ──────────────────
    public List<TimetableEntry> getTodayEntries() {
        return timetableRepository.findByDayOfWeek(getCurrentDayOfWeek());
    }
}