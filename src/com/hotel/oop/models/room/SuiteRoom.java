package com.hotel.oop.models.room;

/**
 * <strong>INHERITANCE:</strong> Premium subtype of {@link Room}.
 * <p>
 * Suites carry a higher {@link #getBaseRatePerNight()} — same operations as {@link StandardRoom}, different data.
 */
public class SuiteRoom extends Room {

    private final double baseRatePerNight;

    public SuiteRoom(long id, String roomNumber, int maxGuests, String description, double baseRatePerNight) {
        super(id, roomNumber, maxGuests, description);
        this.baseRatePerNight = baseRatePerNight;
    }

    @Override
    public double getBaseRatePerNight() {
        return baseRatePerNight;
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SUITE;
    }
}
