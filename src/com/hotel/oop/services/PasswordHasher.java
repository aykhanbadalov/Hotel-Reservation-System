package com.hotel.oop.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 password hashing (Core Java only — no BCrypt dependency).
 */
public final class PasswordHasher {

    private static final String PEPPER = "hotel-oop-course-pepper";

    private PasswordHasher() {
    }

    public static String hash(String email, String plainPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String payload = PEPPER + "|" + email.trim().toLowerCase() + "|" + plainPassword;
            byte[] digest = md.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean matches(String email, String plainPassword, String storedHashHex) {
        return hash(email, plainPassword).equalsIgnoreCase(storedHashHex);
    }
}
