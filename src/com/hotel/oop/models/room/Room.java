package com.hotel.oop.models.room;

import com.hotel.oop.patterns.pricing.PricingStrategy;

import java.util.Objects;

/**
 * <strong>ABSTRACT CLASS #1 — {@code Room}</strong> (course requirement).
 * <p>
 * <strong>Why abstract?</strong> Every room shares identity and description, but <em>pricing base</em> and
 * amenities differ per subtype. {@link #getBaseRatePerNight()} forces subclasses to supply their rate —
 * the compiler enforces completeness (Template Method style hook).
 * <p>
 * <strong>Encapsulation:</strong> Fields are private; mutations go through controlled setters where needed.
 * <p>
 * <strong>Polymorphism:</strong> The rest of the app works with {@code Room} references; at runtime we may have
 * {@link StandardRoom} or {@link SuiteRoom} instances.
 */
public abstract class Room {

    private final long id;
    private final String roomNumber;
    private final int maxGuests;
    private String description;

    protected Room(long id, String roomNumber, int maxGuests, String description) {
        this.id = id;
        this.roomNumber = Objects.requireNonNull(roomNumber);
        this.maxGuests = maxGuests;
        this.description = Objects.requireNonNull(description);
    }

    public long getId() {
        return id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public int getMaxGuests() {
        return maxGuests;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = Objects.requireNonNull(description);
    }

    /**
     * Subclasses define the nightly base rate before strategy multipliers are applied.
     */
    public abstract double getBaseRatePerNight();

    public abstract RoomType getRoomType();

    /**
     * <strong>STRATEGY PATTERN in action:</strong> total price depends on the injected {@link PricingStrategy}.
     * The Room supplies the base rate; the strategy supplies the algorithm.
     */
    public double calculatePrice(int nights, PricingStrategy pricingStrategy) {
        return pricingStrategy.calculateTotal(getBaseRatePerNight(), nights);
    }

    /**
     * <strong>equals/hashCode contract:</strong> Two rooms are the same if their stable {@code id} matches.
     * This matches how we identify inventory in maps and prevents duplicate bookings for the “same” entity.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Room room = (Room) o;
        return id == room.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + ", roomNumber='" + roomNumber + "'}";
    }
}
