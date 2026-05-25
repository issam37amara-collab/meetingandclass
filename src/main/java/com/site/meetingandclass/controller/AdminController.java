package com.site.meetingandclass.controller;

import com.site.meetingandclass.dto.UserResponse;
import com.site.meetingandclass.model.User;
import com.site.meetingandclass.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Account management endpoints.
 * Access rules:
 *   - SUPER_ADMIN and DIRECTOR_OF_STUDIES can both list pending users.
 *   - Actual approval hierarchy is enforced inside AdminService.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    /**
     * GET /api/admin/pending-users
     * SUPER_ADMIN sees DIRECTOR_OF_STUDIES pending accounts.
     * DIRECTOR_OF_STUDIES sees all other pending accounts.
     */
    @GetMapping("/pending-users")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTOR_OF_STUDIES')")
    public ResponseEntity<List<UserResponse>> getPendingUsers(@AuthenticationPrincipal User currentUser) {
        // Both SUPER_ADMIN and DIRECTOR_OF_STUDIES can now see all pending users
        return ResponseEntity.ok(adminService.getPendingUsers());
    }

    /**
     * PUT /api/admin/approve/{id}
     * Approves the account. AdminService validates the hierarchy.
     */
    @PutMapping("/approve/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTOR_OF_STUDIES')")
    public ResponseEntity<UserResponse> approveUser(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        UserResponse response = adminService.approveUser(id, currentUser.getRole().name());
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/admin/reject/{id}
     * Rejects the account. AdminService validates the hierarchy.
     */
    @PutMapping("/reject/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTOR_OF_STUDIES')")
    public ResponseEntity<UserResponse> rejectUser(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        UserResponse response = adminService.rejectUser(id, currentUser.getRole().name());
        return ResponseEntity.ok(response);
    }
}
