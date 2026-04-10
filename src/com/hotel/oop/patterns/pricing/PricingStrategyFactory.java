package com.hotel.oop.patterns.pricing;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Chooses which {@link PricingStrategy} to apply based on the guest’s check-in date.
 * <p>
 * This is a small factory helper so your presentation can say:
 * “The Room doesn’t know if it’s Friday or Tuesday — we inject the right Strategy from one place.”
 */
public class PricingStrategyFactory {

    private final RegularPricingStrategy regular = new RegularPricingStrategy();
    private final WeekendPricingStrategy weekend = new WeekendPricingStrategy(1.25);

    /**
     * If check-in falls on Friday or Saturday, we treat the stay as “weekend-priced” for the demo.
     */
    public PricingStrategy strategyForCheckIn(LocalDate checkIn) {
        DayOfWeek d = checkIn.getDayOfWeek();
        if (d == DayOfWeek.FRIDAY || d == DayOfWeek.SATURDAY) {
            return weekend;
        }
        return regular;
    }
}
