package com.site.meetingandclass.service;

import com.site.meetingandclass.model.Room;
import com.site.meetingandclass.model.RoomReservation;
import com.site.meetingandclass.repository.RoomRepository;
import com.site.meetingandclass.repository.RoomReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomReservationRepository roomReservationRepository;

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public Room addRoom(Room room) {
        return roomRepository.save(room);
    }

    public Room updateRoom(Long id, Room roomDetails) {
        Room room = roomRepository.findById(id).orElseThrow(() -> new RuntimeException("Room not found"));
        room.setName(roomDetails.getName());
        room.setRoomNumber(roomDetails.getRoomNumber());
        room.setCapacity(roomDetails.getCapacity());
        room.setLocation(roomDetails.getLocation());
        room.setType(roomDetails.getType());
        if (roomDetails.getStatus() != null) {
            room.setStatus(roomDetails.getStatus());
        }
        return roomRepository.save(room);
    }

    public void deleteRoom(Long id) {
        roomRepository.deleteById(id);
    }

    public RoomReservation requestReservation(Long roomId, String teacherName, String teacherEmail, String date,
            String timeSlot, String reason) {
        if (roomId == null || date == null || date.isBlank() || timeSlot == null || timeSlot.isBlank()) {
            throw new IllegalArgumentException("roomId, date and timeSlot are required.");
        }
        // Make sure the room exists.
        roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        // Conflict check: refuse if there's already an APPROVED or PENDING reservation
        // for the same room on the same date/time slot.
        List<RoomReservation> existing =
                roomReservationRepository.findByRoomIdAndDateAndTimeSlot(roomId, date, timeSlot);
        boolean clash = existing.stream()
                .anyMatch(r -> "APPROVED".equalsIgnoreCase(r.getStatus())
                            || "PENDING".equalsIgnoreCase(r.getStatus()));
        if (clash) {
            throw new IllegalArgumentException(
                "This room is already reserved (or pending approval) for " + date + " " + timeSlot + ".");
        }

        RoomReservation res = new RoomReservation();
        res.setRoomId(roomId);
        res.setTeacherName(teacherName);
        res.setTeacherEmail(teacherEmail);
        res.setDate(date);
        res.setTimeSlot(timeSlot);
        res.setReason(reason);
        res.setStatus("PENDING");
        return roomReservationRepository.save(res);
    }

    public List<RoomReservation> getAllReservations() {
        return roomReservationRepository.findAll();
    }

    public RoomReservation approveReservation(Long id) {
        RoomReservation res = roomReservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));
        res.setStatus("APPROVED");
        return roomReservationRepository.save(res);
    }

    public RoomReservation refuseReservation(Long id) {
        RoomReservation res = roomReservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));
        res.setStatus("REFUSED");
        return roomReservationRepository.save(res);
    }
}