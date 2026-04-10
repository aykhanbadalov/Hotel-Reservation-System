package com.hotel.oop.persistence;

import com.hotel.oop.models.Reservation;
import com.hotel.oop.models.room.Room;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Single in-memory aggregate for users, rooms, and reservations — shared by services and snapshotted to disk.
 */
public class HotelDataRoot {

    public final Map<Long, Room> roomsById = new ConcurrentHashMap<>();
    public final AtomicLong nextRoomId = new AtomicLong(1);

    public final Map<Long, com.hotel.oop.models.user.Guest> guestsById = new ConcurrentHashMap<>();
    public final AtomicLong nextGuestId = new AtomicLong(1);

    /** Key: email trimmed and lower-case. */
    public final Map<String, RegisteredAccount> accountsByEmailKey = new ConcurrentHashMap<>();

    public final Map<Long, Reservation> reservationsById = new ConcurrentHashMap<>();
    public final AtomicLong nextReservationId = new AtomicLong(1);

    /**
     * Credential row stored next to guest directory (both persisted).
     */
    public static final class RegisteredAccount {
        private final String emailKey;
        private final long guestId;
        private final String passwordHashHex;

        public RegisteredAccount(String emailKey, long guestId, String passwordHashHex) {
            this.emailKey = emailKey;
            this.guestId = guestId;
            this.passwordHashHex = passwordHashHex;
        }

        public String getEmailKey() {
            return emailKey;
        }

        public long getGuestId() {
            return guestId;
        }

        public String getPasswordHashHex() {
            return passwordHashHex;
        }
    }
}
