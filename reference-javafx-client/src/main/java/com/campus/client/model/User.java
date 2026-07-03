package com.campus.client.model;

/**
 * A registered student account. Passwords are stored as a SHA-256 hash
 * (never plain text) inside the plain-text users.txt file.
 */
public class User {

    private String studentId;
    private String name;
    private String email;
    private String passwordHash;
    private String createdAt;

    public User() {
    }

    public User(String studentId, String name, String email, String passwordHash, String createdAt) {
        this.studentId = studentId;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    /** Format: studentId,name,email,passwordHash,createdAt */
    public String toCsvLine() {
        return String.join(",", studentId, name, email, passwordHash, createdAt);
    }

    public static User fromCsvLine(String line) {
        if (line == null || line.isBlank()) return null;
        String[] p = line.split(",", -1);
        if (p.length < 5) return null;
        return new User(p[0], p[1], p[2], p[3], p[4]);
    }
}