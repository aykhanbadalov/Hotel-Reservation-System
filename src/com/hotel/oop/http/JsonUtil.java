package com.hotel.oop.http;

import com.hotel.oop.controllers.dto.ReservationResponse;
import com.hotel.oop.controllers.dto.RoomResponse;
import com.hotel.oop.models.user.Guest;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal JSON read/write using only Java SE.
 */
public final class JsonUtil {

    private JsonUtil() {
    }

    private static final Pattern STR = Pattern.compile("\"(\\w+)\"\\s*:\\s*\"([^\"]*)\"");

    public static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static String toJson(RoomResponse r) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"id\":").append(r.getId()).append(',');
        sb.append("\"roomNumber\":\"").append(escape(r.getRoomNumber())).append("\",");
        sb.append("\"maxGuests\":").append(r.getMaxGuests()).append(',');
        sb.append("\"description\":\"").append(escape(r.getDescription())).append("\",");
        sb.append("\"roomType\":\"").append(r.getRoomType().name()).append("\",");
        sb.append("\"baseRatePerNight\":").append(r.getBaseRatePerNight());
        if (r.getEstimatedStayTotal() != null) {
            sb.append(",\"estimatedStayTotal\":").append(r.getEstimatedStayTotal());
        } else {
            sb.append(",\"estimatedStayTotal\":null");
        }
        sb.append('}');
        return sb.toString();
    }

    public static String toJsonArrayRooms(List<RoomResponse> list) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(toJson(list.get(i)));
        }
        sb.append(']');
        return sb.toString();
    }

    public static String toJson(ReservationResponse r) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"id\":").append(r.getId()).append(',');
        sb.append("\"roomId\":").append(r.getRoomId()).append(',');
        sb.append("\"roomNumber\":\"").append(escape(r.getRoomNumber())).append("\",");
        sb.append("\"guestName\":\"").append(escape(r.getGuestName())).append("\",");
        sb.append("\"guestEmail\":\"").append(escape(r.getGuestEmail())).append("\",");
        sb.append("\"checkIn\":\"").append(r.getCheckIn()).append("\",");
        sb.append("\"checkOut\":\"").append(r.getCheckOut()).append("\",");
        sb.append("\"status\":\"").append(r.getStatus().name()).append("\",");
        sb.append("\"totalPrice\":").append(r.getTotalPrice());
        sb.append('}');
        return sb.toString();
    }

    public static String toJsonArrayReservations(List<ReservationResponse> list) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(toJson(list.get(i)));
        }
        sb.append(']');
        return sb.toString();
    }

    public static String toJson(Guest g) {
        return "{\"id\":" + g.getId()
                + ",\"fullName\":\"" + escape(g.getFullName()) + "\""
                + ",\"email\":\"" + escape(g.getEmail()) + "\"}";
    }

    public static String authSuccessGuest(String token, Guest g) {
        return "{\"token\":\"" + escape(token) + "\",\"guest\":" + toJson(g) + "}";
    }

    public static String authSuccessAdmin(String token) {
        return "{\"token\":\"" + escape(token) + "\",\"role\":\"ADMIN\"}";
    }

    public static String errorObject(String message) {
        return "{\"message\":\"" + escape(message) + "\"}";
    }

    public static AuthenticatedBookingBody parseAuthenticatedBookingBody(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Empty body");
        }
        String c = json.replaceAll("\\s+", " ");
        long roomId = extractLongNamed(c, "roomId");
        LocalDate checkIn = LocalDate.parse(extractString(c, "checkIn"));
        LocalDate checkOut = LocalDate.parse(extractString(c, "checkOut"));
        return new AuthenticatedBookingBody(roomId, checkIn, checkOut);
    }

    public record AuthenticatedBookingBody(long roomId, LocalDate checkIn, LocalDate checkOut) {
    }

    public static SignupBody parseSignupBody(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Empty body");
        }
        String c = json.replaceAll("\\s+", " ");
        return new SignupBody(
                extractString(c, "fullName"),
                extractString(c, "email"),
                extractString(c, "password"),
                extractString(c, "confirmPassword"));
    }

    public record SignupBody(String fullName, String email, String password, String confirmPassword) {
    }

    public static LoginBody parseLoginBody(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Empty body");
        }
        String c = json.replaceAll("\\s+", " ");
        return new LoginBody(extractString(c, "email"), extractString(c, "password"));
    }

    public record LoginBody(String email, String password) {
    }

    public static String parseAdminPasswordOnly(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Empty body");
        }
        String c = json.replaceAll("\\s+", " ");
        return extractString(c, "password");
    }

    public static RoomMutateBody parseRoomMutateBody(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Empty body");
        }
        String c = json.replaceAll("\\s+", " ");
        String typeStr = extractString(c, "roomType");
        String roomNumber = extractString(c, "roomNumber");
        int maxGuests = (int) extractLongNamed(c, "maxGuests");
        String description = extractOptionalString(c, "description");
        double base = extractDoubleNamed(c, "baseRatePerNight");
        return new RoomMutateBody(typeStr, roomNumber, maxGuests, description, base);
    }

    public record RoomMutateBody(String roomType, String roomNumber, int maxGuests,
                                 String description, double baseRatePerNight) {
    }

    private static String extractString(String json, String key) {
        Matcher m = STR.matcher(json);
        while (m.find()) {
            if (key.equals(m.group(1))) {
                return m.group(2);
            }
        }
        throw new IllegalArgumentException("Missing or invalid field: " + key);
    }

    private static String extractOptionalString(String json, String key) {
        Matcher m = STR.matcher(json);
        while (m.find()) {
            if (key.equals(m.group(1))) {
                return m.group(2);
            }
        }
        return "";
    }

    private static final Pattern LONG_ANY = Pattern.compile("\"(\\w+)\"\\s*:\\s*(-?\\d+)");

    private static long extractLongNamed(String json, String key) {
        Matcher m = LONG_ANY.matcher(json);
        while (m.find()) {
            if (key.equals(m.group(1))) {
                return Long.parseLong(m.group(2));
            }
        }
        throw new IllegalArgumentException("Missing or invalid field: " + key);
    }

    private static final Pattern DOUBLE_ANY = Pattern.compile("\"(\\w+)\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");

    private static double extractDoubleNamed(String json, String key) {
        Matcher m = DOUBLE_ANY.matcher(json);
        while (m.find()) {
            if (key.equals(m.group(1))) {
                return Double.parseDouble(m.group(2));
            }
        }
        throw new IllegalArgumentException("Missing or invalid field: " + key);
    }
}
