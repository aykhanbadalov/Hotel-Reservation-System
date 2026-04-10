package com.hotel.oop.patterns.pricing;

/**
 * <strong>STRATEGY PATTERN — Concrete strategy</strong> “weekend / peak” pricing.
 * <p>
 * Applies a multiplier to simulate higher demand — same interface as {@link RegularPricingStrategy},
 * so callers swap behavior without changing Room code.
 */
public class WeekendPricingStrategy implements PricingStrategy {

    /** Peak multiplier applied on top of the room’s base rate. */
    private final double weekendMultiplier;

    public WeekendPricingStrategy(double weekendMultiplier) {
        this.weekendMultiplier = weekendMultiplier;
    }

    @Override
    public double calculateTotal(double baseRatePerNight, int nights) {
        if (nights <= 0) {
            return 0.0;
        }
        return baseRatePerNight * weekendMultiplier * nights;
    }
}
