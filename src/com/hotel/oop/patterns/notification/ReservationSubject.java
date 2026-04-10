package com.hotel.oop.patterns.notification;

import com.hotel.oop.models.Reservation;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * <strong>OBSERVER PATTERN — Subject</strong> (course requirement).
 */
public class ReservationSubject {

    private final List<ReservationObserver> observers = new CopyOnWriteArrayList<>();

    public void attach(ReservationObserver observer) {
        observers.add(observer);
    }

    public void notifyReservationConfirmed(Reservation reservation) {
        for (ReservationObserver observer : observers) {
            observer.onReservationConfirmed(reservation);
        }
    }

    public void notifyReservationRejected(Reservation reservation) {
        for (ReservationObserver observer : observers) {
            observer.onReservationRejected(reservation);
        }
    }
}
