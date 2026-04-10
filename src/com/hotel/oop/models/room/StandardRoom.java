package com.hotel.oop.models.room;

/**
 * <strong>INHERITANCE:</strong> Concrete room type extending {@link Room}.
 * <p>
 * Standard rooms have a lower base rate than suites — polymorphic {@link #getBaseRatePerNight()}.
 */
public class StandardRoom extends Room {

    private final double baseRatePerNight;

    public StandardRoom(long id, String roomNumber, int maxGuests, String description, double baseRatePerNight) {
        super(id, roomNumber, maxGuests, description);
        this.baseRatePerNight = baseRatePerNight;
    }

    @Override
    public double getBaseRatePerNight() {
        return baseRatePerNight;
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.STANDARD;
    }
}
