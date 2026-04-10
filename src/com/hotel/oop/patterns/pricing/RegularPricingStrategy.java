package com.hotel.oop.patterns.pricing;

/**
 * <strong>STRATEGY PATTERN — Concrete strategy</strong> “regular / weekday-style” pricing.
 * <p>
 * Simple linear model: base rate × nights (no surcharge). Easy to explain on slides.
 */
public class RegularPricingStrategy implements PricingStrategy {

    @Override
    public double calculateTotal(double baseRatePerNight, int nights) {
        if (nights <= 0) {
            return 0.0;
        }
        return baseRatePerNight * nights;
    }
}
