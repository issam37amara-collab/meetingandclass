package com.site.meetingandclass.service;

import com.site.meetingandclass.dto.UserResponse;
import com.site.meetingandclass.enums.AccountStatus;
import com.site.meetingandclass.enums.Role;
import com.site.meetingandclass.model.User;
import com.site.meetingandclass.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages account approval flow:
 * - SUPER_ADMIN approves DIRECTOR_OF_STUDIES accounts.
 * - DIRECTOR_OF_STUDIES approves all other role accounts.
 */
@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    // ── List pending users ───────────────────────────────────────────────────

    /**
     * Returns all PENDING accounts.
     * The caller's role is used by @PreAuthorize — this method just fetches.
     */
    public List<UserResponse> getPendingUsers() {
        return userRepository.findByStatus(AccountStatus.PENDING)
                .stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Returns pending DIRECTOR_OF_STUDIES accounts only (for SUPER_ADMIN view).
     */
    public List<UserResponse> getPendingDirectorOfStudies() {
        return userRepository.findByStatusAndRole(AccountStatus.PENDING, Role.DIRECTOR_OF_STUDIES)
                .stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
    }

    // ── Approve ──────────────────────────────────────────────────────────────

    public UserResponse approveUser(Long userId, String approverRole) {
        User user = findUser(userId);
        validateApprovalPermission(user, approverRole);
        if (user.getStatus() == AccountStatus.APPROVED) {
            throw new IllegalArgumentException("User is already approved.");
        }

        user.setStatus(AccountStatus.APPROVED);
        user.setEnabled(true);  // allow login
        return new UserResponse(userRepository.save(user));
    }

    // ── Reject ───────────────────────────────────────────────────────────────

    public UserResponse rejectUser(Long userId, String approverRole) {
        User user = findUser(userId);
        validateApprovalPermission(user, approverRole);
        if (user.getStatus() == AccountStatus.REJECTED) {
            throw new IllegalArgumentException("User is already rejected.");
        }

        user.setStatus(AccountStatus.REJECTED);
        user.setEnabled(false);
        return new UserResponse(userRepository.save(user));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
    }

    /**
     * Enforces approval hierarchy:
     * - Only SUPER_ADMIN can approve/reject DIRECTOR_OF_STUDIES.
     * - DIRECTOR_OF_STUDIES can approve/reject all other roles.
     * - SUPER_ADMIN cannot be approved via this flow (seeded manually).
     */
    private void validateApprovalPermission(User target, String approverRole) {
        if (target.getRole() == Role.SUPER_ADMIN) {
            throw new IllegalArgumentException("SUPER_ADMIN accounts are managed directly.");
        }

        if (target.getRole() == Role.DIRECTOR_OF_STUDIES) {
            if (!approverRole.equals("SUPER_ADMIN")) {
                throw new org.springframework.security.access.AccessDeniedException(
                    "Only SUPER_ADMIN can approve DIRECTOR_OF_STUDIES accounts."
                );
            }
        } else {
            // All other roles must be approved by DIRECTOR_OF_STUDIES (or SUPER_ADMIN)
            if (!approverRole.equals("DIRECTOR_OF_STUDIES") && !approverRole.equals("SUPER_ADMIN")) {
                throw new org.springframework.security.access.AccessDeniedException(
                    "Only DIRECTOR_OF_STUDIES can approve this account type."
                );
            }
        }
    }
}
