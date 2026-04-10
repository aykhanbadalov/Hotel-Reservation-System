package com.hotel.oop.http;

import com.hotel.oop.controllers.dto.ReservationRequest;
import com.hotel.oop.controllers.dto.ReservationResponse;
import com.hotel.oop.controllers.dto.RoomResponse;
import com.hotel.oop.models.Reservation;
import com.hotel.oop.models.room.Room;
import com.hotel.oop.models.room.RoomType;
import com.hotel.oop.models.user.Guest;
import com.hotel.oop.services.AuthService;
import com.hotel.oop.services.ReservationService;
import com.hotel.oop.services.RoomCatalogService;
import com.hotel.oop.services.SessionManager;
import com.hotel.oop.services.search.SearchCriteria;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Aggregates HTTP-facing operations for guests, auth, and admin.
 */
public class HotelApiFacade {

    private final RoomCatalogService roomCatalogService;
    private final ReservationService reservationService;
    private final AuthService authService;
    private final SessionManager sessionManager;

    public HotelApiFacade(RoomCatalogService roomCatalogService,
                          ReservationService reservationService,
                          AuthService authService,
                          SessionManager sessionManager) {
        this.roomCatalogService = roomCatalogService;
        this.reservationService = reservationService;
        this.authService = authService;
        this.sessionManager = sessionManager;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public List<RoomResponse> searchRooms(String q, RoomType type, Integer minGuests,
                                          LocalDate checkIn, LocalDate checkOut) {
        SearchCriteria criteria = new SearchCriteria(q, type, minGuests, checkIn, checkOut);
        List<Room> rooms = reservationService.searchAvailableRooms(criteria);
        return rooms.stream()
                .map(room -> toRoomResponse(room, checkIn, checkOut))
                .collect(Collectors.toList());
    }

    public RoomResponse roomById(long id, LocalDate checkIn, LocalDate checkOut) {
        Room room = reservationService.getRoom(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown room id: " + id));
        return toRoomResponse(room, checkIn, checkOut);
    }

    public List<RoomResponse> listAllRoomsForAdmin() {
        return roomCatalogService.allRooms().stream()
                .map(r -> toRoomResponse(r, null, null))
                .sorted(Comparator.comparingLong(RoomResponse::getId))
                .collect(Collectors.toList());
    }

    public RoomResponse adminCreateRoom(RoomType type, String roomNumber, int maxGuests,
                                        String description, double baseRate) {
        Room r = roomCatalogService.createRoom(type, roomNumber, maxGuests, description, baseRate);
        return toRoomResponse(r, null, null);
    }

    public RoomResponse adminUpdateRoom(long id, RoomType type, String roomNumber, int maxGuests,
                                        String description, double baseRate) {
        Room r = roomCatalogService.updateRoom(id, type, roomNumber, maxGuests, description, baseRate);
        reservationService.rebuildReservationsForRoomId(id);
        return toRoomResponse(r, null, null);
    }

    public void adminDeleteRoom(long id) {
        if (reservationService.roomReferencedByAnyReservation(id)) {
            throw new IllegalStateException("Cannot delete room: it has reservation history");
        }
        roomCatalogService.deleteRoom(id);
    }

    public Guest signup(String fullName, String email, String password, String confirmPassword) {
        return authService.signup(fullName, email, password, confirmPassword);
    }

    public AuthService.GuestLoginResult loginGuestWithProfile(String email, String password) {
        return authService.loginWithGuest(email, password);
    }

    public String loginAdmin(String password) {
        return authService.adminLogin(password);
    }

    public Guest me(long guestId) {
        return authService.requireGuest(guestId);
    }

    public ReservationResponse createAuthenticatedReservation(String sessionToken, ReservationRequest request) {
        long guestId = sessionManager.requireGuest(sessionToken);
        validateBooking(request);
        Reservation r = reservationService.createReservationForGuest(
                guestId, request.getRoomId(), request.getCheckIn(), request.getCheckOut());
        return ReservationResponse.from(r);
    }

    public List<ReservationResponse> allReservationsForAdmin() {
        return reservationService.allReservations().stream()
                .map(ReservationResponse::from)
                .sorted(Comparator.comparingLong(ReservationResponse::getId).reversed())
                .collect(Collectors.toList());
    }

    public List<ReservationResponse> reservationsForGuest(String sessionToken) {
        long guestId = sessionManager.requireGuest(sessionToken);
        return reservationService.reservationsForGuest(guestId).stream()
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }

    public List<ReservationResponse> pendingReservationsForAdmin() {
        return reservationService.pendingReservations().stream()
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }

    public ReservationResponse adminConfirm(String adminToken, long reservationId) {
        sessionManager.requireAdmin(adminToken);
        return ReservationResponse.from(reservationService.adminConfirmReservation(reservationId));
    }

    public ReservationResponse adminReject(String adminToken, long reservationId) {
        sessionManager.requireAdmin(adminToken);
        return ReservationResponse.from(reservationService.adminRejectReservation(reservationId));
    }

    public ReservationResponse guestCancelPending(String sessionToken, long reservationId) {
        long guestId = sessionManager.requireGuest(sessionToken);
        return ReservationResponse.from(reservationService.guestCancelPendingReservation(guestId, reservationId));
    }

    public void adminDeleteConfirmedReservation(String adminToken, long reservationId) {
        sessionManager.requireAdmin(adminToken);
        reservationService.adminDeleteConfirmedReservation(reservationId);
    }

    public void logoutGuest(String token) {
        sessionManager.logoutGuest(token);
    }

    public void logoutAdmin(String token) {
        sessionManager.logoutAdmin(token);
    }

    private void validateBooking(ReservationRequest request) {
        if (request.getCheckIn() == null || request.getCheckOut() == null) {
            throw new IllegalArgumentException("checkIn and checkOut are required");
        }
        LocalDate today = LocalDate.now();
        if (request.getCheckIn().isBefore(today)) {
            throw new IllegalArgumentException("checkIn cannot be before today");
        }
        if (!request.getCheckOut().isAfter(request.getCheckIn())) {
            throw new IllegalArgumentException("checkOut must be after checkIn");
        }
        if (request.getCheckIn().getYear() > 9999 || request.getCheckOut().getYear() > 9999
                || request.getCheckIn().getYear() < 1000 || request.getCheckOut().getYear() < 1000) {
            throw new IllegalArgumentException("Dates must use a valid 4-digit year");
        }
    }

    private RoomResponse toRoomResponse(Room room, LocalDate checkIn, LocalDate checkOut) {
        Double estimate = null;
        if (checkIn != null && checkOut != null && checkOut.isAfter(checkIn)) {
            estimate = reservationService.quoteStay(room, checkIn, checkOut);
        }
        return new RoomResponse(
                room.getId(),
                room.getRoomNumber(),
                room.getMaxGuests(),
                room.getDescription(),
                room.getRoomType(),
                room.getBaseRatePerNight(),
                estimate);
    }
}
