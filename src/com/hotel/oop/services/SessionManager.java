package com.hotel.oop.services;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory session tokens (not persisted — users re-login after server restart).
 */
public class SessionManager {

    public static final String ADMIN_PASSWORD = "admin123";

    private final Map<String, Long> guestTokenToGuestId = new ConcurrentHashMap<>();
    private final Map<String, Boolean> adminTokens = new ConcurrentHashMap<>();

    public String createGuestSession(long guestId) {
        String token = UUID.randomUUID().toString();
        guestTokenToGuestId.put(token, guestId);
        return token;
    }

    public String createAdminSession() {
        String token = UUID.randomUUID().toString();
        adminTokens.put(token, Boolean.TRUE);
        return token;
    }

    public long requireGuest(String token) {
        if (token == null || token.isBlank()) {
            throw new SecurityException("Missing session");
        }
        Long id = guestTokenToGuestId.get(token.trim());
        if (id == null) {
            throw new SecurityException("Invalid or expired session");
        }
        return id;
    }

    public void requireAdmin(String token) {
        if (token == null || token.isBlank() || !Boolean.TRUE.equals(adminTokens.get(token.trim()))) {
            throw new SecurityException("Admin session required");
        }
    }

    public void logoutGuest(String token) {
        if (token != null) {
            guestTokenToGuestId.remove(token.trim());
        }
    }

    public void logoutAdmin(String token) {
        if (token != null) {
            adminTokens.remove(token.trim());
        }
    }
}
