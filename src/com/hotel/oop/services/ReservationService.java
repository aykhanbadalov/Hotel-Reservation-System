package com.hotel.oop.services;

import com.hotel.oop.models.Reservation;
import com.hotel.oop.models.ReservationStatus;
import com.hotel.oop.models.interfaces.Payable;
import com.hotel.oop.models.room.Room;
import com.hotel.oop.models.user.Guest;
import com.hotel.oop.persistence.HotelDataRoot;
import com.hotel.oop.patterns.notification.ReservationSubject;
import com.hotel.oop.patterns.pricing.PricingStrategy;
import com.hotel.oop.patterns.pricing.PricingStrategyFactory;
import com.hotel.oop.services.search.SearchCriteria;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Bookings + admin approval. Guest creates {@link ReservationStatus#PENDING}; admin confirm runs {@link Payable} and observers.
 */
public class ReservationService {

    private final HotelDataRoot root;
    private final RoomCatalogService roomCatalog;
    private final Payable paymentService;
    private final PricingStrategyFactory pricingStrategyFactory;
    private final ReservationSubject reservationSubject;
    private final AuthService authService;
    private final Runnable persist;

    public ReservationService(HotelDataRoot root,
                              RoomCatalogService roomCatalog,
                              Payable paymentService,
                              PricingStrategyFactory pricingStrategyFactory,
                              ReservationSubject reservationSubject,
                              AuthService authService,
                              Runnable persist) {
        this.root = root;
        this.roomCatalog = roomCatalog;
        this.paymentService = paymentService;
        this.pricingStrategyFactory = pricingStrategyFactory;
        this.reservationSubject = reservationSubject;
        this.authService = authService;
        this.persist = persist;
    }

    public Collection<Reservation> allReservations() {
        return root.reservationsById.values();
    }

    public List<Reservation> pendingReservations() {
        return root.reservationsById.values().stream()
                .filter(r -> r.getStatus() == ReservationStatus.PENDING)
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .toList();
    }

    public List<Reservation> reservationsForGuest(long guestId) {
        return root.reservationsById.values().stream()
                .filter(r -> r.getGuest().getId() == guestId)
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .toList();
    }

    public boolean roomHasActiveBookings(long roomId) {
        return root.reservationsById.values().stream()
                .filter(r -> r.getRoom().getId() == roomId)
                .anyMatch(r -> r.getStatus() == ReservationStatus.PENDING
                        || r.getStatus() == ReservationStatus.CONFIRMED);
    }

    /** Admin cannot delete a room that appears on any reservation history row (keeps referential integrity). */
    public boolean roomReferencedByAnyReservation(long roomId) {
        return root.reservationsById.values().stream()
                .anyMatch(r -> r.getRoom().getId() == roomId);
    }

    /**
     * After a room row is replaced in the catalog, re-point live {@link Reservation} objects so they reference the new instance.
     */
    public void rebuildReservationsForRoomId(long roomId) {
        Room canonical = roomCatalog.findById(roomId).orElseThrow();
        for (long rid : new java.util.ArrayList<>(root.reservationsById.keySet())) {
            Reservation r = root.reservationsById.get(rid);
            if (r.getRoom().getId() != roomId) {
                continue;
            }
            Reservation copy = new Reservation(r.getId(), canonical, r.getGuest(), r.getCheckIn(), r.getCheckOut(),
                    r.getStatus(), r.getTotalPrice());
            root.reservationsById.put(rid, copy);
        }
        persist.run();
    }

    public List<Room> searchAvailableRooms(SearchCriteria criteria) {
        List<Room> candidates = roomCatalog.searchByCriteria(criteria);
        if (!criteria.hasAvailabilityWindow()) {
            return candidates;
        }
        LocalDate in = criteria.getAvailableFrom();
        LocalDate out = criteria.getAvailableTo();
        List<Reservation> blocking = new ArrayList<>(root.reservationsById.values());
        return candidates.stream()
                .filter(room -> roomCatalog.isAvailable(room, in, out, blocking))
                .toList();
    }

    public Optional<Room> getRoom(long id) {
        return roomCatalog.findById(id);
    }

    public double quoteStay(Room room, LocalDate checkIn, LocalDate checkOut) {
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights <= 0) {
            throw new IllegalArgumentException("Nights must be positive");
        }
        PricingStrategy strategy = pricingStrategyFactory.strategyForCheckIn(checkIn);
        return room.calculatePrice(nights, strategy);
    }

    /**
     * Authenticated guest: creates {@link ReservationStatus#PENDING} only (no payment, no observers).
     */
    public Reservation createReservationForGuest(long guestId, long roomId, LocalDate checkIn, LocalDate checkOut) {
        Guest guest = authService.requireGuest(guestId);
        Room room = roomCatalog.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown room id: " + roomId));

        List<Reservation> blocking = new ArrayList<>(root.reservationsById.values());
        if (!roomCatalog.isAvailable(room, checkIn, checkOut, blocking)) {
            throw new IllegalStateException("Room is not available for the selected dates");
        }

        double total = quoteStay(room, checkIn, checkOut);
        long id = root.nextReservationId.getAndIncrement();
        Reservation reservation = new Reservation(id, room, guest, checkIn, checkOut,
                ReservationStatus.PENDING, total);
        root.reservationsById.put(id, reservation);
        persist.run();
        return reservation;
    }

    public Reservation adminConfirmReservation(long reservationId) {
        Reservation r = root.reservationsById.get(reservationId);
        if (r == null) {
            throw new IllegalArgumentException("Unknown reservation id: " + reservationId);
        }
        boolean paid = paymentService.processPayment(r.getTotalPrice());
        if (!paid) {
            throw new IllegalStateException("Payment failed");
        }
        r.confirmBooking();
        persist.run();
        reservationSubject.notifyReservationConfirmed(r);
        return r;
    }

    public Reservation adminRejectReservation(long reservationId) {
        Reservation r = root.reservationsById.get(reservationId);
        if (r == null) {
            throw new IllegalArgumentException("Unknown reservation id: " + reservationId);
        }
        r.rejectBooking();
        persist.run();
        reservationSubject.notifyReservationRejected(r);
        return r;
    }

    public Reservation guestCancelPendingReservation(long guestId, long reservationId) {
        Reservation r = root.reservationsById.get(reservationId);
        if (r == null) {
            throw new IllegalArgumentException("Unknown reservation id: " + reservationId);
        }
        if (r.getGuest().getId() != guestId) {
            throw new SecurityException("You can only cancel your own reservations");
        }
        r.cancelBooking();
        persist.run();
        reservationSubject.notifyReservationRejected(r);
        return r;
    }

    public void adminDeleteConfirmedReservation(long reservationId) {
        Reservation r = root.reservationsById.get(reservationId);
        if (r == null) {
            throw new IllegalArgumentException("Unknown reservation id: " + reservationId);
        }
        if (r.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Only CONFIRMED reservations can be removed");
        }
        root.reservationsById.remove(reservationId);
        persist.run();
    }
}
