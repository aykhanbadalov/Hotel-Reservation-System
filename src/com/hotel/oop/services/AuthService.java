package com.hotel.oop.services;

import com.hotel.oop.models.user.Guest;
import com.hotel.oop.persistence.HotelDataRoot;

import java.util.Locale;
import java.util.Objects;

/**
 * Signup / login against persisted {@link HotelDataRoot} accounts.
 */
public class AuthService {

    private final HotelDataRoot root;
    private final SessionManager sessions;
    private final Runnable persist;

    public AuthService(HotelDataRoot root, SessionManager sessions, Runnable persist) {
        this.root = root;
        this.sessions = sessions;
        this.persist = persist;
    }

    public Guest requireGuest(long guestId) {
        Guest g = root.guestsById.get(guestId);
        if (g == null) {
            throw new IllegalArgumentException("Unknown guest");
        }
        return g;
    }

    public Guest signup(String fullName, String email, String password, String confirmPassword) {
        Objects.requireNonNull(fullName, "fullName");
        Objects.requireNonNull(email, "email");
        if (fullName.isBlank()) {
            throw new IllegalArgumentException("Full name is required");
        }
        if (email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (password == null || password.length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        String key = email.trim().toLowerCase(Locale.ROOT);
        if (root.accountsByEmailKey.containsKey(key)) {
            throw new IllegalStateException("Email already registered");
        }
        long id = root.nextGuestId.getAndIncrement();
        Guest guest = new Guest(id, fullName.trim(), email.trim());
        root.guestsById.put(id, guest);
        String hash = PasswordHasher.hash(email, password);
        root.accountsByEmailKey.put(key, new HotelDataRoot.RegisteredAccount(key, id, hash));
        persist.run();
        return guest;
    }

    public record GuestLoginResult(String token, Guest guest) {
    }

    /**
     * Validates credentials and returns a new session plus the {@link Guest} profile.
     */
    public GuestLoginResult loginWithGuest(String email, String password) {
        if (email == null || email.isBlank() || password == null) {
            throw new IllegalArgumentException("Email and password required");
        }
        String key = email.trim().toLowerCase(Locale.ROOT);
        HotelDataRoot.RegisteredAccount acc = root.accountsByEmailKey.get(key);
        if (acc == null) {
            throw new SecurityException("Invalid email or password");
        }
        if (!PasswordHasher.matches(email, password, acc.getPasswordHashHex())) {
            throw new SecurityException("Invalid email or password");
        }
        Guest guest = requireGuest(acc.getGuestId());
        String token = sessions.createGuestSession(acc.getGuestId());
        return new GuestLoginResult(token, guest);
    }

    public String login(String email, String password) {
        return loginWithGuest(email, password).token();
    }

    public String adminLogin(String password) {
        if (!SessionManager.ADMIN_PASSWORD.equals(password)) {
            throw new SecurityException("Invalid admin password");
        }
        return sessions.createAdminSession();
    }
}
