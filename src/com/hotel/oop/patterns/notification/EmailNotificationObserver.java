package com.hotel.oop.patterns.notification;

import com.hotel.oop.models.Reservation;

import java.util.logging.Logger;

/**
 * Simulates email delivery when an admin confirms or rejects a booking.
 */
public class EmailNotificationObserver implements ReservationObserver {

    private static final Logger LOG = Logger.getLogger(EmailNotificationObserver.class.getName());

    public EmailNotificationObserver(ReservationSubject subject) {
        subject.attach(this);
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        LOG.info(String.format(
                "[EMAIL] CONFIRMED → %s | reservation #%d | room %s | %s → %s",
                reservation.getGuest().getEmail(),
                reservation.getId(),
                reservation.getRoom().getRoomNumber(),
                reservation.getCheckIn(),
                reservation.getCheckOut()));
    }

    @Override
    public void onReservationRejected(Reservation reservation) {
        LOG.info(String.format(
                "[EMAIL] REJECTED → %s | reservation #%d | room %s",
                reservation.getGuest().getEmail(),
                reservation.getId(),
                reservation.getRoom().getRoomNumber()));
    }
}
