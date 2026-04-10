package com.hotel.oop.patterns.notification;

import com.hotel.oop.models.Reservation;

import java.util.logging.Logger;

/**
 * Simulates SMS delivery for admin confirm / reject events.
 */
public class SmsNotificationObserver implements ReservationObserver {

    private static final Logger LOG = Logger.getLogger(SmsNotificationObserver.class.getName());

    public SmsNotificationObserver(ReservationSubject subject) {
        subject.attach(this);
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        LOG.info(String.format(
                "[SMS] CONFIRMED | guest %s | #%d OK",
                reservation.getGuest().getFullName(),
                reservation.getId()));
    }

    @Override
    public void onReservationRejected(Reservation reservation) {
        LOG.info(String.format(
                "[SMS] REJECTED | guest %s | #%d declined",
                reservation.getGuest().getFullName(),
                reservation.getId()));
    }
}
