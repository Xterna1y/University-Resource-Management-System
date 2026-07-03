package com.campus.client.data;

import com.campus.client.model.User;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Local, plain-text-file account store (data/users.txt). No database, per
 * assignment rules. Passwords are hashed with SHA-256 before storage.
 */
public class UserStorage {

    private final Path usersFile;

    public UserStorage() {
        this(Paths.get("data", "users.txt"));
    }

    public UserStorage(Path usersFile) {
        this.usersFile = usersFile;
        try {
            if (usersFile.getParent() != null) Files.createDirectories(usersFile.getParent());
            if (!Files.exists(usersFile)) Files.createFile(usersFile);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialise " + usersFile, e);
        }
    }

    public synchronized boolean exists(String studentId) {
        return findById(studentId).isPresent();
    }

    public synchronized Optional<User> findById(String studentId) {
        return loadAll().stream().filter(u -> u.getStudentId().equalsIgnoreCase(studentId)).findFirst();
    }

    /** Registers a new student. Throws IllegalStateException if the studentId is already taken. */
    public synchronized User register(String studentId, String name, String email, String rawPassword) {
        if (exists(studentId)) {
            throw new IllegalStateException("A student with ID '" + studentId + "' already exists.");
        }
        User user = new User(studentId, name, email, hash(rawPassword), LocalDateTime.now().toString());
        appendLine(user.toCsvLine());
        return user;
    }

    /** Returns the User if studentId/password match, otherwise empty. */
    public synchronized Optional<User> authenticate(String studentId, String rawPassword) {
        return findById(studentId).filter(u -> u.getPasswordHash().equals(hash(rawPassword)));
    }

    private List<User> loadAll() {
        List<User> result = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(usersFile)) {
                User u = User.fromCsvLine(line);
                if (u != null) result.add(u);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + usersFile, e);
        }
        return result;
    }

    private void appendLine(String line) {
        try {
            Files.writeString(usersFile, line + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write " + usersFile, e);
        }
    }

    private static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] out = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // SHA-256 is always available on the JVM
        }
    }
}