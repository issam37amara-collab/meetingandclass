package com.site.meetingandclass.dto;

public class AuthResponse {
    private String token;
    private String role;
    private String fullName;
    private String email;
    private String status;

    public AuthResponse(String token, String role, String fullName, String email, String status) {
        this.token = token;
        this.role = role;
        this.fullName = fullName;
        this.email = email;
        this.status = status;
    }

    public String getToken() { return token; }
    public String getRole() { return role; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getStatus() { return status; }
}
