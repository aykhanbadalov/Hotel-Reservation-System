package com.hotel.oop.persistence;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Serializable snapshot of all persisted hotel data (written with {@link java.io.ObjectOutputStream}).
 */
public class HotelDataStore implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<RoomRow> rooms = new ArrayList<>();
    private final List<GuestRow> guests = new ArrayList<>();
    private final List<AccountRow> accounts = new ArrayList<>();
    private final List<ReservationRow> reservations = new ArrayList<>();
    private long nextRoomId = 1;
    private long nextGuestId = 1;
    private long nextReservationId = 1;

    public List<RoomRow> getRooms() {
        return rooms;
    }

    public List<GuestRow> getGuests() {
        return guests;
    }

    public List<AccountRow> getAccounts() {
        return accounts;
    }

    public List<ReservationRow> getReservations() {
        return reservations;
    }

    public long getNextRoomId() {
        return nextRoomId;
    }

    public void setNextRoomId(long nextRoomId) {
        this.nextRoomId = nextRoomId;
    }

    public long getNextGuestId() {
        return nextGuestId;
    }

    public void setNextGuestId(long nextGuestId) {
        this.nextGuestId = nextGuestId;
    }

    public long getNextReservationId() {
        return nextReservationId;
    }

    public void setNextReservationId(long nextReservationId) {
        this.nextReservationId = nextReservationId;
    }

    public static class RoomRow implements Serializable {
        private static final long serialVersionUID = 1L;
        public long id;
        public String roomType;
        public String roomNumber;
        public int maxGuests;
        public String description;
        public double baseRatePerNight;
    }

    public static class GuestRow implements Serializable {
        private static final long serialVersionUID = 1L;
        public long id;
        public String fullName;
        public String email;
    }

    public static class AccountRow implements Serializable {
        private static final long serialVersionUID = 1L;
        public String emailKey;
        public long guestId;
        public String passwordHashHex;
    }

    public static class ReservationRow implements Serializable {
        private static final long serialVersionUID = 1L;
        public long id;
        public long roomId;
        public long guestId;
        public String checkIn;
        public String checkOut;
        public String status;
        public double totalPrice;
    }
}
