package com.campus.client.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Domain model for a facility booking.
 * Mirrors the bookings.txt format from the design report:
 * bookingId,resourceId,facilityName,studentId,bookingDate,startTime,endTime,status,createdAt
 */
public class Booking {

    public enum Status {
        UPCOMING, COMPLETED, CANCELLED
    }

    private String bookingId;
    private String resourceId;
    private String facilityName;
    private String studentId;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Status status;
    private String createdAt; // stored as ISO string for simplicity in text files

    public Booking() {
    }

    public Booking(String bookingId, String resourceId, String facilityName, String studentId,
                   LocalDate bookingDate, LocalTime startTime, LocalTime endTime,
                   Status status, String createdAt) {
        this.bookingId = bookingId;
        this.resourceId = resourceId;
        this.facilityName = facilityName;
        this.studentId = studentId;
        this.bookingDate = bookingDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.createdAt = createdAt;
    }

    // --- Getters / Setters ---

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public String getFacilityName() { return facilityName; }
    public void setFacilityName(String facilityName) { this.facilityName = facilityName; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public LocalDate getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDate bookingDate) { this.bookingDate = bookingDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    /** Serialises this booking to one CSV line for bookings.txt / cancelled_bookings.txt. */
    public String toCsvLine() {
        return String.join(",",
                nullSafe(bookingId), nullSafe(resourceId), nullSafe(facilityName), nullSafe(studentId),
                bookingDate == null ? "" : bookingDate.toString(),
                startTime == null ? "" : startTime.toString(),
                endTime == null ? "" : endTime.toString(),
                status == null ? "" : status.name(),
                nullSafe(createdAt));
    }

    /** Parses one CSV line (as written by toCsvLine) back into a Booking. Returns null on malformed lines. */
    public static Booking fromCsvLine(String line) {
        if (line == null || line.isBlank()) return null;
        String[] p = line.split(",", -1);
        if (p.length < 9) return null;
        try {
            return new Booking(
                    p[0], p[1], p[2], p[3],
                    LocalDate.parse(p[4]),
                    LocalTime.parse(p[5]),
                    LocalTime.parse(p[6]),
                    Status.valueOf(p[7]),
                    p[8]
            );
        } catch (Exception e) {
            return null; // skip malformed/legacy rows rather than crashing the app
        }
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    @Override
    public String toString() {
        return facilityName + " | " + bookingDate + " " + startTime + "-" + endTime + " | " + status;
    }
}