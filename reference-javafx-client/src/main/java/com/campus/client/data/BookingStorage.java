package com.campus.client.data;

import com.campus.client.model.Booking;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reads and writes the client's own local booking history to plain text files.
 * This is separate from the MCP server's records: the server confirms the
 * booking (book_resource), and the client keeps its own history so the app
 * can show "My Bookings" without a dedicated MCP "list bookings" tool.
 *
 * Files (created under ./data/ relative to the working directory):
 *   bookings.txt            - active/upcoming/completed bookings
 *   cancelled_bookings.txt  - cancelled bookings (kept for audit, per design report 7.3)
 *
 * Format (matches the design report, section 7.2 / 7.3):
 *   bookingId,resourceId,facilityName,studentId,bookingDate,startTime,endTime,status,createdAt
 */
public class BookingStorage {

    private final Path bookingsFile;
    private final Path cancelledFile;

    public BookingStorage() {
        this(Paths.get("data", "bookings.txt"), Paths.get("data", "cancelled_bookings.txt"));
    }

    public BookingStorage(Path bookingsFile, Path cancelledFile) {
        this.bookingsFile = bookingsFile;
        this.cancelledFile = cancelledFile;
        ensureFileExists(bookingsFile);
        ensureFileExists(cancelledFile);
    }

    private void ensureFileExists(Path file) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            if (!Files.exists(file)) {
                Files.createFile(file);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not initialise storage file: " + file, e);
        }
    }

    /** Appends a new booking record. */
    public synchronized void save(Booking booking) {
        appendLine(bookingsFile, booking.toCsvLine());
    }

    /** Returns every booking (any student) currently in bookings.txt. */
    public synchronized List<Booking> loadAll() {
        return readAll(bookingsFile);
    }

    /** Returns only the bookings belonging to a given student. */
    public synchronized List<Booking> loadForStudent(String studentId) {
        return loadAll().stream()
                .filter(b -> b.getStudentId() != null && b.getStudentId().equals(studentId))
                .collect(Collectors.toList());
    }

    /** Returns cancelled bookings for a given student. */
    public synchronized List<Booking> loadCancelledForStudent(String studentId) {
        return readAll(cancelledFile).stream()
                .filter(b -> b.getStudentId() != null && b.getStudentId().equals(studentId))
                .collect(Collectors.toList());
    }

    /**
     * Cancels a booking: removes it from bookings.txt, marks it CANCELLED and
     * appends it to cancelled_bookings.txt. Rewrites the whole file since we
     * are working with flat text files (no database, no in-place row update).
     */
    public synchronized void cancel(String bookingId) {
        List<Booking> all = readAll(bookingsFile);
        List<Booking> remaining = new ArrayList<>();
        Booking cancelled = null;

        for (Booking b : all) {
            if (b.getBookingId().equals(bookingId)) {
                b.setStatus(Booking.Status.CANCELLED);
                cancelled = b;
            } else {
                remaining.add(b);
            }
        }

        if (cancelled != null) {
            rewrite(bookingsFile, remaining);
            appendLine(cancelledFile, cancelled.toCsvLine());
        }
    }

    /** Convenience: upcoming = today or later and not cancelled. */
    public List<Booking> upcoming(String studentId) {
        LocalDate today = LocalDate.now();
        return loadForStudent(studentId).stream()
                .filter(b -> b.getStatus() != Booking.Status.CANCELLED)
                .filter(b -> b.getBookingDate() != null && !b.getBookingDate().isBefore(today))
                .collect(Collectors.toList());
    }

    /** Convenience: past = before today and not cancelled. */
    public List<Booking> past(String studentId) {
        LocalDate today = LocalDate.now();
        return loadForStudent(studentId).stream()
                .filter(b -> b.getStatus() != Booking.Status.CANCELLED)
                .filter(b -> b.getBookingDate() != null && b.getBookingDate().isBefore(today))
                .collect(Collectors.toList());
    }

    // --- low-level file helpers ---

    private void appendLine(Path file, String line) {
        try {
            Files.writeString(file, line + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to " + file, e);
        }
    }

    private void rewrite(Path file, List<Booking> bookings) {
        try {
            List<String> lines = bookings.stream().map(Booking::toCsvLine).collect(Collectors.toList());
            Files.write(file, lines, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new RuntimeException("Failed to rewrite " + file, e);
        }
    }

    private List<Booking> readAll(Path file) {
        List<Booking> result = new ArrayList<>();
        try {
            if (!Files.exists(file)) return result;
            for (String line : Files.readAllLines(file)) {
                Booking b = Booking.fromCsvLine(line);
                if (b != null) result.add(b);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + file, e);
        }
        return result;
    }
}