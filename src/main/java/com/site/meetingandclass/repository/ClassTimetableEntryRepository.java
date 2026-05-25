package com.site.meetingandclass.repository;

import com.site.meetingandclass.model.ClassTimetableEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassTimetableEntryRepository extends JpaRepository<ClassTimetableEntry, Long> {

    /** All entries belonging to a specific board */
    @org.springframework.data.jpa.repository.Query("SELECT c FROM ClassTimetableEntry c WHERE c.timetable.id = :timetableId")
    List<ClassTimetableEntry> findByTimetableId(@org.springframework.data.repository.query.Param("timetableId") Long timetableId);

    /** Delete all entries of a board (used when deleting the whole board) */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("DELETE FROM ClassTimetableEntry c WHERE c.timetable.id = :timetableId")
    void deleteByTimetableId(@org.springframework.data.repository.query.Param("timetableId") Long timetableId);

    /**
     * Find all entries at an exact day + time slot across ALL boards.
     * Used to detect room conflicts before saving a new entry.
     */
    List<ClassTimetableEntry> findByDayOfWeekAndStartTimeAndEndTime(
            String dayOfWeek, String startTime, String endTime);

    /**
     * Find ALL entries for a specific day across ALL boards.
     * Used by the room-status sync to check what is happening today.
     */
    List<ClassTimetableEntry> findByDayOfWeek(String dayOfWeek);
}
