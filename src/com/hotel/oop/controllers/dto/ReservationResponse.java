package com.hotel.oop.controllers.dto;

import com.hotel.oop.models.Reservation;
import com.hotel.oop.models.ReservationStatus;

import java.time.LocalDate;

public class ReservationResponse {

    private long id;
    private long roomId;
    private String roomNumber;
    private String guestName;
    private String guestEmail;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private ReservationStatus status;
    private double totalPrice;

    public static ReservationResponse from(Reservation r) {
        ReservationResponse dto = new ReservationResponse();
        dto.setId(r.getId());
        dto.setRoomId(r.getRoom().getId());
        dto.setRoomNumber(r.getRoom().getRoomNumber());
        dto.setGuestName(r.getGuest().getFullName());
        dto.setGuestEmail(r.getGuest().getEmail());
        dto.setCheckIn(r.getCheckIn());
        dto.setCheckOut(r.getCheckOut());
        dto.setStatus(r.getStatus());
        dto.setTotalPrice(r.getTotalPrice());
        return dto;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
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

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }
}
