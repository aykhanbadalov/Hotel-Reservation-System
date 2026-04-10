package com.hotel.oop.patterns.notification;

import com.hotel.oop.models.Reservation;

/**
 * <strong>OBSERVER PATTERN — Observer interface</strong> (course requirement).
 * <p>
 * Notifications fire only after <em>admin</em> decisions (confirm / reject), not when a guest submits a PENDING request.
 */
public interface ReservationObserver {

    void onReservationConfirmed(Reservation reservation);

    void onReservationRejected(Reservation reservation);
}
