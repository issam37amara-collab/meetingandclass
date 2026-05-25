package com.site.meetingandclass.repository;

import com.site.meetingandclass.enums.AccountStatus;
import com.site.meetingandclass.enums.Role;
import com.site.meetingandclass.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Fetch users waiting for approval, filtered by role
    List<User> findByStatusAndRole(AccountStatus status, Role role);

    // Fetch all pending users regardless of role
    List<User> findByStatus(AccountStatus status);
}
