package com.hotel.oop.patterns.pricing;

/**
 * <strong>STRATEGY PATTERN — Strategy interface</strong> (course requirement).
 * <p>
 * <strong>Intent:</strong> Encapsulate a family of pricing algorithms and make them interchangeable at runtime.
 * <p>
 * <strong>Where used:</strong> {@link com.hotel.oop.models.room.Room#calculatePrice(int, PricingStrategy)}
 * delegates “how to price” to a concrete strategy ({@link RegularPricingStrategy} vs {@link WeekendPricingStrategy}).
 * <p>
 * <strong>Why it matters for OOP:</strong> We avoid giant {@code if (weekend)} chains inside the Room hierarchy;
 * new pricing rules = new class, not edits to existing room code (Open/Closed Principle).
 */
public interface PricingStrategy {

    /**
     * @param baseRatePerNight room’s own base rate (Standard vs Suite differs in subclasses)
     * @param nights           length of stay
     * @return total price for the stay under this strategy
     */
    double calculateTotal(double baseRatePerNight, int nights);
}
