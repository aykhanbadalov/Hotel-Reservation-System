package com.hotel.oop.models;

/**
 * Reservation lifecycle for the admin approval workflow.
 * <p>
 * {@link #PENDING}: guest requested a stay; blocks the calendar until approved or rejected.<br>
 * {@link #CONFIRMED}: admin approved; payment is taken at approval time in our demo.<br>
 * {@link #REJECTED}: admin declined; dates become available again.
 */
public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    REJECTED
}
