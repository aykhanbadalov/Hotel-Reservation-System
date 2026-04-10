package com.hotel.oop.controllers.dto;

import java.time.LocalDate;

/**
 * Authenticated guest booking payload (room + dates only).
 */
public class ReservationRequest {

    private long roomId;
    private LocalDate checkIn;
    private LocalDate checkOut;

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalDate checkIn) {
        this.checkIn = checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalDate checkOut) {
        this.checkOut = checkOut;
    }
}
