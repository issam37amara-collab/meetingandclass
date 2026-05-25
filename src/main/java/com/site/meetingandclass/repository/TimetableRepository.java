package com.site.meetingandclass.repository;

import com.site.meetingandclass.model.TimetableEntry;
import com.site.meetingandclass.model.TimetableEntry.DayOfWeek;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimetableRepository extends JpaRepository<TimetableEntry, Long> {

    // Get all entries for a specific day
    List<TimetableEntry> findByDayOfWeek(DayOfWeek day);

    // Get all entries for a specific room
    @org.springframework.data.jpa.repository.Query("SELECT t FROM TimetableEntry t WHERE t.room.id = :roomId")
    List<TimetableEntry> findByRoomId(@org.springframework.data.repository.query.Param("roomId") Long roomId);

    // Get all entries for a specific room on a specific day
    @org.springframework.data.jpa.repository.Query("SELECT t FROM TimetableEntry t WHERE t.room.id = :roomId AND t.dayOfWeek = :day")
    List<TimetableEntry> findByRoomIdAndDayOfWeek(@org.springframework.data.repository.query.Param("roomId") Long roomId, @org.springframework.data.repository.query.Param("day") DayOfWeek day);
}