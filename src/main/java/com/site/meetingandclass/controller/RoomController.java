package com.site.meetingandclass.controller;

import com.site.meetingandclass.model.Room;
import com.site.meetingandclass.model.RoomReservation;
import com.site.meetingandclass.model.User;
import com.site.meetingandclass.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RoomController {

    @Autowired
    private RoomService roomService;

    // ── ROOMS ─────────────────────────────────────────────────────────────────

    /** GET /api/rooms/all — Any authenticated user can view rooms. */
    @GetMapping("/rooms/all")
    @PreAuthorize("isAuthenticated()")
    public List<Room> getAllRooms() {
        return roomService.getAllRooms();
    }

    /** POST /api/rooms/add — Only Directors can add rooms. */
    @PostMapping("/rooms/add")
    @PreAuthorize("hasAnyRole('DIRECTOR_OF_INSTITUTE','HEAD_OF_DEPARTMENT','DIRECTOR_OF_STUDIES','SUPER_ADMIN')")
    public ResponseEntity<?> addRoom(@RequestBody Room room) {
        try {
            return ResponseEntity.ok(roomService.addRoom(room));
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }

    /** PUT /api/rooms/update/{id} — Only Directors can update rooms. */
    @PutMapping("/rooms/update/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR_OF_INSTITUTE','HEAD_OF_DEPARTMENT','DIRECTOR_OF_STUDIES','SUPER_ADMIN')")
    public ResponseEntity<?> updateRoom(@PathVariable Long id, @RequestBody Room room) {
        try {
            return ResponseEntity.ok(roomService.updateRoom(id, room));
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }

    /** DELETE /api/rooms/delete/{id} — Only Directors can delete rooms. */
    @DeleteMapping("/rooms/delete/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR_OF_INSTITUTE','HEAD_OF_DEPARTMENT','DIRECTOR_OF_STUDIES','SUPER_ADMIN')")
    public ResponseEntity<?> deleteRoom(@PathVariable Long id) {
        try {
            roomService.deleteRoom(id);
            return ResponseEntity.ok("Room deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }

    // ── RESERVATIONS ──────────────────────────────────────────────────────────

    /**
     * POST /api/reservations/request
     * Any authenticated user can reserve a room.
     * Teacher name/email auto-filled from their JWT-authenticated account.
     */
    @PostMapping("/reservations/request")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> requestReservation(@RequestBody Map<String, Object> body,
                                                @AuthenticationPrincipal User currentUser) {
        try {
            Long roomId = Long.valueOf(body.get("roomId").toString());
            String date = body.get("date").toString();
            String timeSlot = body.get("timeSlot").toString();
            String reason = body.get("reason").toString();

            // Use authenticated user's info instead of trusting the request body
            return ResponseEntity.ok(
                roomService.requestReservation(roomId, currentUser.getFullName(),
                    currentUser.getEmail(), date, timeSlot, reason)
            );
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }

    /** GET /api/reservations/all — Directors and HoD see all reservations. */
    @GetMapping("/reservations/all")
    @PreAuthorize("hasAnyRole('HEAD_OF_DEPARTMENT','DIRECTOR_OF_INSTITUTE','DIRECTOR_OF_STUDIES','SUPER_ADMIN')")
    public List<RoomReservation> getAllReservations() {
        return roomService.getAllReservations();
    }

    /** PUT /api/reservations/{id}/approved — Only HoD can approve reservations. */
    @PutMapping("/reservations/{id}/approved")
    @PreAuthorize("hasAnyRole('HEAD_OF_DEPARTMENT','SUPER_ADMIN')")
    public ResponseEntity<?> approveReservation(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(roomService.approveReservation(id));
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }

    /** PUT /api/reservations/{id}/refused — Only HoD can refuse reservations. */
    @PutMapping("/reservations/{id}/refused")
    @PreAuthorize("hasAnyRole('HEAD_OF_DEPARTMENT','SUPER_ADMIN')")
    public ResponseEntity<?> refuseReservation(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(roomService.refuseReservation(id));
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }

    // ── New endpoints matching the spec ──────────────────────────────────────

    /** PUT /api/reservations/approve/{id} — spec-style approve endpoint */
    @PutMapping("/reservations/approve/{id}")
    @PreAuthorize("hasAnyRole('HEAD_OF_DEPARTMENT','SUPER_ADMIN')")
    public ResponseEntity<?> approveReservationSpec(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(roomService.approveReservation(id));
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }

    /** PUT /api/reservations/reject/{id} — spec-style reject endpoint */
    @PutMapping("/reservations/reject/{id}")
    @PreAuthorize("hasAnyRole('HEAD_OF_DEPARTMENT','SUPER_ADMIN')")
    public ResponseEntity<?> rejectReservationSpec(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(roomService.refuseReservation(id));
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }
}
