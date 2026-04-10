package com.hotel.oop.models;

import com.hotel.oop.models.interfaces.Bookable;
import com.hotel.oop.models.room.Room;
import com.hotel.oop.models.user.Guest;

import java.time.LocalDate;
import java.util.Objects;

/**
 * <strong>COMPOSITION (CRITICAL REQUIREMENT):</strong> A reservation <em>has-a</em> {@link Room} and <em>has-a</em> {@link Guest}.
 * <p>
 * Status machine: {@link ReservationStatus#PENDING} → {@link ReservationStatus#CONFIRMED} or {@link ReservationStatus#REJECTED}
 * (driven by admin actions in {@link com.hotel.oop.services.ReservationService}).
 */
public class Reservation implements Bookable {

    private final long id;
    private final Room room;
    private final Guest guest;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private ReservationStatus status;
    private double totalPrice;

    public Reservation(long id, Room room, Guest guest, LocalDate checkIn, LocalDate checkOut,
                       ReservationStatus status, double totalPrice) {
        this.id = id;
        this.room = Objects.requireNonNull(room);
        this.guest = Objects.requireNonNull(guest);
        this.checkIn = Objects.requireNonNull(checkIn);
        this.checkOut = Objects.requireNonNull(checkOut);
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("checkOut must be after checkIn");
        }
        this.status = Objects.requireNonNull(status);
        this.totalPrice = totalPrice;
    }

    public long getId() {
        return id;
    }

    public Room getRoom() {
        return room;
    }

    public Guest getGuest() {
        return guest;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public int getNights() {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /**
     * Admin approval path: {@code PENDING} → {@code CONFIRMED}.
     */
    @Override
    public void confirmBooking() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only PENDING reservations can be confirmed");
        }
        this.status = ReservationStatus.CONFIRMED;
    }

    /**
     * Admin rejection path: {@code PENDING} → {@link ReservationStatus#REJECTED}.
     */
    public void rejectBooking() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only PENDING reservations can be rejected");
        }
        this.status = ReservationStatus.REJECTED;
    }

    /**
     * {@link Bookable} hook — for this project, maps to rejecting a still-pending request (frees dates).
     */
    @Override
    public void cancelBooking() {
        if (status == ReservationStatus.PENDING) {
            this.status = ReservationStatus.REJECTED;
            return;
        }
        throw new IllegalStateException("Only PENDING reservations can be cancelled by the guest in this demo");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Reservation that = (Reservation) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}
