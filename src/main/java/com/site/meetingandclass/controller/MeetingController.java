package com.site.meetingandclass.controller;

import com.site.meetingandclass.model.Meeting;
import com.site.meetingandclass.model.User;
import com.site.meetingandclass.repository.MeetingRepository;
import com.site.meetingandclass.service.MeetingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private MeetingRepository meetingRepository;

    /** POST /api/meetings/create — Directors and HoD can create meetings. */
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('DIRECTOR_OF_INSTITUTE','HEAD_OF_DEPARTMENT','DIRECTOR_OF_STUDIES','SUPER_ADMIN')")
    public Meeting create(@RequestBody Meeting meeting) {
        return meetingService.createMeeting(meeting);
    }

    /** GET /api/meetings/all — Teachers only see meetings where their email is invited. */
    @GetMapping("/all")
    @PreAuthorize("isAuthenticated()")
    public List<Meeting> getAllMeetings(@AuthenticationPrincipal User currentUser) {
        List<Meeting> all = meetingRepository.findAll();
        if (currentUser.getRole().name().equals("TEACHER")) {
            String myEmail = currentUser.getEmail();
            return all.stream()
                .filter(m -> m.getMembersEmails() != null
                          && myEmail != null
                          && m.getMembersEmails().stream().anyMatch(myEmail::equalsIgnoreCase))
                .toList();
        }
        return all;
    }

    /** PUT /api/meetings/{id}/upload-summary — Any authenticated user can upload. */
    @PutMapping("/{id}/upload-summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadSummary(@PathVariable Long id,
                                           @RequestParam("file") MultipartFile file,
                                           @AuthenticationPrincipal User currentUser) {
        // Teachers can only upload summaries for meetings they were invited to.
        if (currentUser != null && currentUser.getRole().name().equals("TEACHER")) {
            Meeting m = meetingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found."));
            String myEmail = currentUser.getEmail();
            boolean invited = m.getMembersEmails() != null
                    && myEmail != null
                    && m.getMembersEmails().stream().anyMatch(myEmail::equalsIgnoreCase);
            if (!invited) {
                return ResponseEntity.status(403)
                    .body("You may only upload summaries for meetings you were invited to.");
            }
        }
        try {
            meetingService.finishMeetingWithPdf(id, file);
            return ResponseEntity.ok("Summary uploaded successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Upload failed.");
        }
    }

    /**
     * GET /api/meetings/download/{id}
     * Streams the stored PDF if it exists. Teachers only get access if they
     * were invited to the meeting.
     */
    @GetMapping("/download/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadPdf(@PathVariable Long id,
                                                @AuthenticationPrincipal User currentUser) {
        Meeting meeting = meetingRepository.findById(id).orElse(null);
        if (meeting == null || meeting.getPdfPath() == null || meeting.getPdfPath().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        if (currentUser != null && currentUser.getRole().name().equals("TEACHER")) {
            String myEmail = currentUser.getEmail();
            boolean invited = meeting.getMembersEmails() != null
                    && myEmail != null
                    && meeting.getMembersEmails().stream().anyMatch(myEmail::equalsIgnoreCase);
            if (!invited) return ResponseEntity.status(403).build();
        }

        try {
            Path path = Paths.get(meeting.getPdfPath()).toAbsolutePath().normalize();
            if (!java.nio.file.Files.exists(path)) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(path.toUri());
            String filename = path.getFileName().toString().replaceAll("[\\r\\n\"]", "_");
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }
}
