package com.site.meetingandclass.service;

import com.site.meetingandclass.model.Meeting;
import com.site.meetingandclass.repository.MeetingRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class MeetingService {

    private static final Logger log = LoggerFactory.getLogger(MeetingService.class);

    /** Magic bytes that all PDF files start with: "%PDF-". */
    private static final byte[] PDF_MAGIC = new byte[] { 0x25, 0x50, 0x44, 0x46, 0x2D };

    /** Hard cap (10 MB) on the meeting summary PDF size. */
    private static final long MAX_PDF_BYTES = 10L * 1024 * 1024;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.uploads.dir:uploads}")
    private String uploadDir;

    @Value("${app.mail.from:noreply@cuniv-naama.dz}")
    private String mailFrom;

    @Value("${app.mail.admin:admin@cuniv-naama.dz}")
    private String adminEmail;

    @Value("${SENDGRID_API_KEY}")
    private String sendGridApiKey;

    // 1. Create a meeting and email each invited member.
    public Meeting createMeeting(Meeting meeting) {
        Meeting savedMeeting = meetingRepository.save(meeting);
        log.info("Meeting '{}' saved (id={}). Sending invitations to {} member(s)...",
                savedMeeting.getSubject(), savedMeeting.getId(),
                savedMeeting.getMembersEmails() != null ? savedMeeting.getMembersEmails().size() : 0);

        if (savedMeeting.getMembersEmails() != null) {
            for (String email : savedMeeting.getMembersEmails()) {
                if (email == null || email.isBlank()) continue;
                try {
                    sendSimpleEmail(
                        email.trim(),
                        "Meeting Invitation: " + savedMeeting.getSubject(),
                        "Dear colleague,\n\n"
                        + "You have been invited to the following meeting:\n\n"
                        + "  Topic: " + savedMeeting.getSubject() + "\n"
                        + "  Date & Time: " + savedMeeting.getDateTime() + "\n\n"
                        + "Please make sure to attend on time.\n\n"
                        + "Best regards,\n"
                        + "CUNIV Naama - Meeting Management System"
                    );
                    log.info("Email invitation sent successfully to: {}", email.trim());
                } catch (Exception e) {
                    // Log per-recipient failures but don't abort the request.
                    log.error("FAILED to email meeting invite to {}: [{}] {}",
                            email, e.getClass().getSimpleName(), e.getMessage());
                }
            }
        }
        return savedMeeting;
    }

    // 2. Attach a PDF summary to a meeting and forward to admin.
    public Meeting finishMeetingWithPdf(Long id, MultipartFile file) throws IOException {
        validatePdf(file);

        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meeting not found with id: " + id));

        // Resolve the upload directory to an absolute, canonical path so we
        // can reject any path-traversal attempts via the resolved filename.
        Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadRoot);

        // Filename is server-controlled — don't trust client-provided names.
        String safeFileName = "Summary_Meeting_" + id + ".pdf";
        Path target = uploadRoot.resolve(safeFileName).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new SecurityException("Invalid upload destination.");
        }

        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        meeting.setPdfPath(target.toString());
        Meeting updated = meetingRepository.save(meeting);

        try {
            sendEmailWithAttachment(adminEmail,
                "Meeting summary: " + meeting.getSubject(),
                "Please find attached the PDF summary of the meeting.",
                target.toFile());
        } catch (Exception e) {
            log.warn("Failed to email PDF summary to admin: {}", e.getMessage());
        }

        return updated;
    }

    /** Verifies that the multipart file is non-empty, ≤ 10 MB and is a real PDF. */
    private void validatePdf(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty.");
        }
        if (file.getSize() > MAX_PDF_BYTES) {
            throw new IllegalArgumentException("PDF too large (max 10 MB).");
        }

        String ct = file.getContentType();
        if (ct != null && !ct.equalsIgnoreCase("application/pdf")
                       && !ct.equalsIgnoreCase("application/octet-stream")) {
            throw new IllegalArgumentException("Only PDF files are accepted (bad content-type: " + ct + ").");
        }

        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only files with a .pdf extension are accepted.");
        }

        // Magic-byte check — first 5 bytes must be "%PDF-".
        try (InputStream in = file.getInputStream()) {
            byte[] header = in.readNBytes(PDF_MAGIC.length);
            if (header.length < PDF_MAGIC.length) {
                throw new IllegalArgumentException("Uploaded file is not a valid PDF.");
            }
            for (int i = 0; i < PDF_MAGIC.length; i++) {
                if (header[i] != PDF_MAGIC[i]) {
                    throw new IllegalArgumentException("Uploaded file is not a valid PDF.");
                }
            }
        }
    }

private void sendSimpleEmail(String to, String subject, String body) {
    try {
        Email from = new Email(mailFrom);
        Email toEmail = new Email(to);
        Content content = new Content("text/plain", body);
        Mail mail = new Mail(from, subject, toEmail, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());

        Response response = sg.api(request);
        if (response.getStatusCode() >= 400) {
            log.error("SendGrid error {}: {}", response.getStatusCode(), response.getBody());
        } else {
            log.info("Email sent to {} via SendGrid API (status {})", to, response.getStatusCode());
        }
    } catch (Exception e) {
        log.error("Failed to send email to {}: {}", to, e.getMessage());
    }
}

    private void sendEmailWithAttachment(String to, String subject, String body, File file) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            helper.addAttachment(file.getName(), file);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Failed to send email with attachment to {}: {}", to, e.getMessage());
        }
    }
}
