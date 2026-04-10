package com.hotel.oop.models.interfaces;

/**
 * <strong>INTERFACE #1 — {@code Bookable}</strong> (course requirement).
 * <p>
 * <strong>Why an interface?</strong> We promise a booking lifecycle without tying callers to one concrete class.
 * That is <em>polymorphism</em>: any {@code Bookable} can be confirmed or cancelled through the same API.
 * <p>
 * {@link com.hotel.oop.models.Reservation} implements this interface — <strong>composition + interface segregation</strong>
 * keep reservation rules in one place.
 */
public interface Bookable {

    /**
     * Moves a tentative booking to a confirmed state (after payment in our demo flow).
     */
    void confirmBooking();

    /**
     * Guest-side cancellation for a still-pending request maps to rejection in this demo (frees dates).
     */
    void cancelBooking();
}
