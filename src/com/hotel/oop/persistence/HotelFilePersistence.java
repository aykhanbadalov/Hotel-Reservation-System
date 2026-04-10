package com.hotel.oop.persistence;

import com.hotel.oop.models.Reservation;
import com.hotel.oop.models.ReservationStatus;
import com.hotel.oop.models.room.RoomType;
import com.hotel.oop.models.room.StandardRoom;
import com.hotel.oop.models.room.SuiteRoom;
import com.hotel.oop.models.user.Guest;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;

/**
 * <strong>File-based persistence (Java SE only)</strong>: one binary {@code .dat} file via standard object serialization.
 * <p>
 * On each meaningful state change the server rewrites the snapshot atomically (temp file + replace).
 */
public final class HotelFilePersistence {

    private static final String FILE_NAME = "hotel-state.dat";

    private HotelFilePersistence() {
    }

    public static Path dataFile(Path dataDirectory) {
        return dataDirectory.resolve(FILE_NAME);
    }

    public static void save(Path dataDirectory, HotelDataRoot root) throws IOException {
        Files.createDirectories(dataDirectory);
        HotelDataStore snap = snapshotFrom(root);
        Path target = dataFile(dataDirectory);
        Path temp = dataDirectory.resolve(FILE_NAME + ".tmp");
        try (OutputStream os = Files.newOutputStream(temp);
             ObjectOutputStream oos = new ObjectOutputStream(os)) {
            oos.writeObject(snap);
            oos.flush();
        }
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.copy(temp, target, StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(temp);
        }
    }

    public static void loadOrSeed(Path dataDirectory, HotelDataRoot root) throws IOException, ClassNotFoundException {
        Path file = dataFile(dataDirectory);
        if (!Files.isRegularFile(file)) {
            seedDefaultRooms(root);
            save(dataDirectory, root);
            return;
        }
        try (InputStream is = Files.newInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(is)) {
            HotelDataStore snap = (HotelDataStore) ois.readObject();
            applySnapshot(root, snap);
        }
    }

    private static void seedDefaultRooms(HotelDataRoot root) {
        root.roomsById.put(1L, new StandardRoom(1, "101", 2,
                "Cozy standard room, city view, queen bed.", 120.0));
        root.roomsById.put(2L, new StandardRoom(2, "102", 2,
                "Quiet standard room, courtyard view.", 110.0));
        root.roomsById.put(3L, new StandardRoom(3, "205", 3,
                "Family standard with sofa bed.", 145.0));
        root.roomsById.put(4L, new SuiteRoom(4, "301", 4,
                "Junior suite, separate living area, skyline view.", 240.0));
        root.roomsById.put(5L, new SuiteRoom(5, "302", 4,
                "Executive suite, workspace and lounge.", 280.0));
        root.nextRoomId.set(6);
    }

    private static HotelDataStore snapshotFrom(HotelDataRoot root) {
        HotelDataStore s = new HotelDataStore();
        for (var e : root.roomsById.entrySet()) {
            var row = new HotelDataStore.RoomRow();
            row.id = e.getKey();
            row.roomType = e.getValue().getRoomType().name();
            row.roomNumber = e.getValue().getRoomNumber();
            row.maxGuests = e.getValue().getMaxGuests();
            row.description = e.getValue().getDescription();
            row.baseRatePerNight = e.getValue().getBaseRatePerNight();
            s.getRooms().add(row);
        }
        for (var g : root.guestsById.values()) {
            var row = new HotelDataStore.GuestRow();
            row.id = g.getId();
            row.fullName = g.getFullName();
            row.email = g.getEmail();
            s.getGuests().add(row);
        }
        for (var a : root.accountsByEmailKey.values()) {
            var row = new HotelDataStore.AccountRow();
            row.emailKey = a.getEmailKey();
            row.guestId = a.getGuestId();
            row.passwordHashHex = a.getPasswordHashHex();
            s.getAccounts().add(row);
        }
        for (var r : root.reservationsById.values()) {
            var row = new HotelDataStore.ReservationRow();
            row.id = r.getId();
            row.roomId = r.getRoom().getId();
            row.guestId = r.getGuest().getId();
            row.checkIn = r.getCheckIn().toString();
            row.checkOut = r.getCheckOut().toString();
            row.status = r.getStatus().name();
            row.totalPrice = r.getTotalPrice();
            s.getReservations().add(row);
        }
        s.setNextRoomId(root.nextRoomId.get());
        s.setNextGuestId(root.nextGuestId.get());
        s.setNextReservationId(root.nextReservationId.get());
        return s;
    }

    private static void applySnapshot(HotelDataRoot root, HotelDataStore s) {
        root.roomsById.clear();
        root.guestsById.clear();
        root.accountsByEmailKey.clear();
        root.reservationsById.clear();

        for (HotelDataStore.RoomRow row : s.getRooms()) {
            RoomType type = RoomType.valueOf(row.roomType);
            if (type == RoomType.STANDARD) {
                root.roomsById.put(row.id, new StandardRoom(row.id, row.roomNumber, row.maxGuests,
                        row.description, row.baseRatePerNight));
            } else {
                root.roomsById.put(row.id, new SuiteRoom(row.id, row.roomNumber, row.maxGuests,
                        row.description, row.baseRatePerNight));
            }
        }
        for (HotelDataStore.GuestRow row : s.getGuests()) {
            root.guestsById.put(row.id, new Guest(row.id, row.fullName, row.email));
        }
        for (HotelDataStore.AccountRow row : s.getAccounts()) {
            root.accountsByEmailKey.put(row.emailKey,
                    new HotelDataRoot.RegisteredAccount(row.emailKey, row.guestId, row.passwordHashHex));
        }
        for (HotelDataStore.ReservationRow row : s.getReservations()) {
            var room = root.roomsById.get(row.roomId);
            var guest = root.guestsById.get(row.guestId);
            if (room == null || guest == null) {
                continue;
            }
            Reservation res = new Reservation(row.id, room, guest,
                    LocalDate.parse(row.checkIn), LocalDate.parse(row.checkOut),
                    ReservationStatus.valueOf(row.status), row.totalPrice);
            root.reservationsById.put(row.id, res);
        }
        root.nextRoomId.set(s.getNextRoomId());
        root.nextGuestId.set(s.getNextGuestId());
        root.nextReservationId.set(s.getNextReservationId());
    }
}
