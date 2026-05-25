package com.site.meetingandclass.repository;

import com.site.meetingandclass.model.RoomReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomReservationRepository extends JpaRepository<RoomReservation, Long> {

    /** All reservations made by a specific teacher (by email). */
    List<RoomReservation> findByTeacherEmail(String teacherEmail);

    /** All reservations for a specific room. */
    List<RoomReservation> findByRoomId(Long roomId);

    /** Conflict-detection helper: same room + same date + same time slot. */
    List<RoomReservation> findByRoomIdAndDateAndTimeSlot(Long roomId, String date, String timeSlot);
}
