package com.site.meetingandclass.model;

import jakarta.persistence.*;

import java.util.List;


@Entity
@Table(name = "meetings")
public class Meeting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String subject;

    private String dateTime;

    @ElementCollection
    @CollectionTable(name = "meeting_emails", joinColumns = @JoinColumn(name = "meeting_id"))
    @Column(name = "email")
    private List<String> membersEmails;

// في ملف Meeting.java
private String pdfPath; // بدلاً من decisions و recommendations


    public String getPdfPath() {
        return this.pdfPath;
    }

    public void setPdfPath(String pdfPath) {
        this.pdfPath = pdfPath;
    }

    public Meeting() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubject() {
        return this.subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDateTime() {
        return this.dateTime;
    }


    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public List<String> getMembersEmails() {
        return this.membersEmails;
    }

    public void setMembersEmails(List<String> membersEmails) {
        this.membersEmails = membersEmails;
    }

}