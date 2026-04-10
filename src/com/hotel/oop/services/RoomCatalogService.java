package com.hotel.oop.services;

import com.hotel.oop.models.Reservation;
import com.hotel.oop.models.ReservationStatus;
import com.hotel.oop.models.interfaces.Searchable;
import com.hotel.oop.models.room.Room;
import com.hotel.oop.models.room.RoomType;
import com.hotel.oop.models.room.StandardRoom;
import com.hotel.oop.models.room.SuiteRoom;
import com.hotel.oop.persistence.HotelDataRoot;
import com.hotel.oop.services.search.SearchCriteria;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Room inventory backed by {@link HotelDataRoot}.
 */
public class RoomCatalogService implements Searchable {

    private final HotelDataRoot root;
    private final Runnable persist;

    public RoomCatalogService(HotelDataRoot root, Runnable persist) {
        this.root = root;
        this.persist = persist;
    }

    public void registerRoom(Room room) {
        root.roomsById.put(room.getId(), room);
        persist.run();
    }

    /**
     * Admin: create a new room with the next available id.
     */
    public Room createRoom(RoomType type, String roomNumber, int maxGuests, String description, double baseRatePerNight) {
        if (roomNumber == null || roomNumber.isBlank()) {
            throw new IllegalArgumentException("roomNumber is required");
        }
        String num = roomNumber.trim();
        boolean taken = root.roomsById.values().stream()
                .anyMatch(r -> r.getRoomNumber().equalsIgnoreCase(num));
        if (taken) {
            throw new IllegalStateException("Room number already exists");
        }
        long id = root.nextRoomId.getAndIncrement();
        Room room = type == RoomType.SUITE
                ? new SuiteRoom(id, num, maxGuests, description != null ? description : "", baseRatePerNight)
                : new StandardRoom(id, num, maxGuests, description != null ? description : "", baseRatePerNight);
        root.roomsById.put(id, room);
        persist.run();
        return room;
    }

    public Room updateRoom(long id, RoomType type, String roomNumber, int maxGuests, String description, double baseRatePerNight) {
        Room existing = root.roomsById.get(id);
        if (existing == null) {
            throw new IllegalArgumentException("Unknown room id: " + id);
        }
        String num = roomNumber != null ? roomNumber.trim() : existing.getRoomNumber();
        boolean taken = root.roomsById.values().stream()
                .anyMatch(r -> r.getId() != id && r.getRoomNumber().equalsIgnoreCase(num));
        if (taken) {
            throw new IllegalStateException("Room number already in use");
        }
        String desc = description != null ? description : existing.getDescription();
        Room replacement = type == RoomType.SUITE
                ? new SuiteRoom(id, num, maxGuests, desc, baseRatePerNight)
                : new StandardRoom(id, num, maxGuests, desc, baseRatePerNight);
        root.roomsById.put(id, replacement);
        persist.run();
        return replacement;
    }

    public void deleteRoom(long id) {
        if (!root.roomsById.containsKey(id)) {
            throw new IllegalArgumentException("Unknown room id: " + id);
        }
        root.roomsById.remove(id);
        persist.run();
    }

    public Collection<Room> allRooms() {
        return root.roomsById.values();
    }

    public Optional<Room> findById(long id) {
        return Optional.ofNullable(root.roomsById.get(id));
    }

    public boolean isAvailable(Room room, LocalDate checkIn, LocalDate checkOut, List<Reservation> reservations) {
        return reservations.stream()
                .filter(r -> r.getRoom().getId() == room.getId())
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED || r.getStatus() == ReservationStatus.PENDING)
                .noneMatch(r -> overlaps(r.getCheckIn(), r.getCheckOut(), checkIn, checkOut));
    }

    private boolean overlaps(LocalDate in1, LocalDate out1, LocalDate in2, LocalDate out2) {
        return in2.isBefore(out1) && out2.isAfter(in1);
    }

    @Override
    public List<Room> searchByCriteria(SearchCriteria criteria) {
        List<Room> base = new ArrayList<>(root.roomsById.values());

        if (criteria.getRoomType() != null) {
            base = base.stream()
                    .filter(r -> r.getRoomType() == criteria.getRoomType())
                    .collect(Collectors.toList());
        }
        if (criteria.getMinGuests() != null) {
            int min = criteria.getMinGuests();
            base = base.stream().filter(r -> r.getMaxGuests() >= min).collect(Collectors.toList());
        }
        if (criteria.getKeyword() != null && !criteria.getKeyword().isBlank()) {
            String q = criteria.getKeyword().trim().toLowerCase(Locale.ROOT);
            base = base.stream()
                    .filter(r -> r.getRoomNumber().toLowerCase(Locale.ROOT).contains(q)
                            || r.getDescription().toLowerCase(Locale.ROOT).contains(q))
                    .collect(Collectors.toList());
        }
        return base;
    }
}
