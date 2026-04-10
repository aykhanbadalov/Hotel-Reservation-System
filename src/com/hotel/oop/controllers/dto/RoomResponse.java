package com.hotel.oop.controllers.dto;

import com.hotel.oop.models.room.RoomType;

/**
 * JSON-friendly view of a room for the guest UI (avoids serializing abstract {@link com.hotel.oop.models.room.Room} subtypes over HTTP).
 */
public class RoomResponse {

    private long id;
    private String roomNumber;
    private int maxGuests;
    private String description;
    private RoomType roomType;
    private double baseRatePerNight;
    /** Populated when the client passes check-in/out on search. */
    private Double estimatedStayTotal;

    public RoomResponse() {
    }

    public RoomResponse(long id, String roomNumber, int maxGuests, String description,
                        RoomType roomType, double baseRatePerNight, Double estimatedStayTotal) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.maxGuests = maxGuests;
        this.description = description;
        this.roomType = roomType;
        this.baseRatePerNight = baseRatePerNight;
        this.estimatedStayTotal = estimatedStayTotal;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public int getMaxGuests() {
        return maxGuests;
    }

    public void setMaxGuests(int maxGuests) {
        this.maxGuests = maxGuests;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public double getBaseRatePerNight() {
        return baseRatePerNight;
    }

    public void setBaseRatePerNight(double baseRatePerNight) {
        this.baseRatePerNight = baseRatePerNight;
    }

    public Double getEstimatedStayTotal() {
        return estimatedStayTotal;
    }

    public void setEstimatedStayTotal(Double estimatedStayTotal) {
        this.estimatedStayTotal = estimatedStayTotal;
    }
}
