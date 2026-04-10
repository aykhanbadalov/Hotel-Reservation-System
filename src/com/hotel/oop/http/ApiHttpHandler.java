package com.hotel.oop.http;

import com.hotel.oop.controllers.dto.ReservationRequest;
import com.hotel.oop.controllers.dto.ReservationResponse;
import com.hotel.oop.controllers.dto.RoomResponse;
import com.hotel.oop.models.room.RoomType;
import com.hotel.oop.models.user.Guest;
import com.hotel.oop.services.AuthService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST-style routing on JDK {@link com.sun.net.httpserver.HttpServer}.
 */
public class ApiHttpHandler implements HttpHandler {

    public static final String HDR_SESSION = "X-Session-Token";
    public static final String HDR_ADMIN = "X-Admin-Token";

    private final HotelApiFacade api;

    public ApiHttpHandler(HotelApiFacade api) {
        this.api = api;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCors(exchange);
        String method = exchange.getRequestMethod();
        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        try {
            String path = exchange.getRequestURI().getPath();
            String normalized = normalizePath(path);
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());

            if ("GET".equals(method) && ("/api/rooms".equals(normalized) || "/rooms".equals(normalized))) {
                List<RoomResponse> rooms = api.searchRooms(
                        query.get("q"),
                        parseRoomType(query.get("type")),
                        parseIntOrNull(query.get("minGuests")),
                        parseDateOrNull(query.get("checkIn")),
                        parseDateOrNull(query.get("checkOut")));
                sendJson(exchange, 200, JsonUtil.toJsonArrayRooms(rooms));
                return;
            }

            if ("GET".equals(method) && (normalized.startsWith("/api/rooms/") || normalized.startsWith("/rooms/"))) {
                String prefix = normalized.startsWith("/api/rooms/") ? "/api/rooms/" : "/rooms/";
                String idPart = normalized.substring(prefix.length());
                if (idPart.isEmpty() || idPart.contains("/")) {
                    sendJson(exchange, 404, JsonUtil.errorObject("Not found"));
                    return;
                }
                long id = Long.parseLong(idPart);
                RoomResponse room = api.roomById(id,
                        parseDateOrNull(query.get("checkIn")),
                        parseDateOrNull(query.get("checkOut")));
                sendJson(exchange, 200, JsonUtil.toJson(room));
                return;
            }

            if ("POST".equals(method) && ("/api/auth/signup".equals(normalized) || "/auth/signup".equals(normalized))) {
                JsonUtil.SignupBody s = JsonUtil.parseSignupBody(readBody(exchange));
                api.signup(s.fullName(), s.email(), s.password(), s.confirmPassword());
                AuthService.GuestLoginResult lr = api.loginGuestWithProfile(s.email(), s.password());
                sendJson(exchange, 200, JsonUtil.authSuccessGuest(lr.token(), lr.guest()));
                return;
            }

            if ("POST".equals(method) && ("/api/auth/login".equals(normalized) || "/auth/login".equals(normalized))) {
                JsonUtil.LoginBody l = JsonUtil.parseLoginBody(readBody(exchange));
                AuthService.GuestLoginResult lr = api.loginGuestWithProfile(l.email(), l.password());
                sendJson(exchange, 200, JsonUtil.authSuccessGuest(lr.token(), lr.guest()));
                return;
            }

            if ("POST".equals(method) && ("/api/auth/admin-login".equals(normalized) || "/auth/admin-login".equals(normalized))) {
                String pw = JsonUtil.parseAdminPasswordOnly(readBody(exchange));
                String token = api.loginAdmin(pw);
                sendJson(exchange, 200, JsonUtil.authSuccessAdmin(token));
                return;
            }

            if ("POST".equals(method) && ("/api/auth/logout".equals(normalized) || "/auth/logout".equals(normalized))) {
                String guestTok = exchange.getRequestHeaders().getFirst(HDR_SESSION);
                String adminTok = exchange.getRequestHeaders().getFirst(HDR_ADMIN);
                if (adminTok != null && !adminTok.isBlank()) {
                    api.logoutAdmin(adminTok);
                }
                if (guestTok != null && !guestTok.isBlank()) {
                    api.logoutGuest(guestTok);
                }
                sendJson(exchange, 200, "{\"ok\":true}");
                return;
            }

            if ("GET".equals(method) && ("/api/auth/me".equals(normalized) || "/auth/me".equals(normalized))) {
                String tok = exchange.getRequestHeaders().getFirst(HDR_SESSION);
                long gid = api.getSessionManager().requireGuest(tok);
                Guest g = api.me(gid);
                sendJson(exchange, 200, JsonUtil.toJson(g));
                return;
            }

            if ("POST".equals(method) && ("/api/reservations".equals(normalized) || "/reservations".equals(normalized))) {
                String tok = exchange.getRequestHeaders().getFirst(HDR_SESSION);
                JsonUtil.AuthenticatedBookingBody b = JsonUtil.parseAuthenticatedBookingBody(readBody(exchange));
                ReservationRequest req = new ReservationRequest();
                req.setRoomId(b.roomId());
                req.setCheckIn(b.checkIn());
                req.setCheckOut(b.checkOut());
                ReservationResponse created = api.createAuthenticatedReservation(tok, req);
                sendJson(exchange, 200, JsonUtil.toJson(created));
                return;
            }

            if ("GET".equals(method) && ("/api/reservations".equals(normalized) || "/reservations".equals(normalized))) {
                String adminTok = exchange.getRequestHeaders().getFirst(HDR_ADMIN);
                api.getSessionManager().requireAdmin(adminTok);
                List<ReservationResponse> all = api.allReservationsForAdmin();
                sendJson(exchange, 200, JsonUtil.toJsonArrayReservations(all));
                return;
            }

            if ("GET".equals(method) && ("/api/reservations/me".equals(normalized) || "/reservations/me".equals(normalized))) {
                String tok = exchange.getRequestHeaders().getFirst(HDR_SESSION);
                List<ReservationResponse> mine = api.reservationsForGuest(tok);
                sendJson(exchange, 200, JsonUtil.toJsonArrayReservations(mine));
                return;
            }

            if ("POST".equals(method) && (normalized.startsWith("/api/reservations/") || normalized.startsWith("/reservations/"))
                    && normalized.endsWith("/cancel")) {
                String tok = exchange.getRequestHeaders().getFirst(HDR_SESSION);
                String prefix = normalized.startsWith("/api/reservations/") ? "/api/reservations/" : "/reservations/";
                long rid = parseIdAfterPrefix(normalized, prefix, "/cancel");
                ReservationResponse r = api.guestCancelPending(tok, rid);
                sendJson(exchange, 200, JsonUtil.toJson(r));
                return;
            }

            if ("GET".equals(method) && "/api/admin/reservations/pending".equals(path)) {
                String adminTok = exchange.getRequestHeaders().getFirst(HDR_ADMIN);
                api.getSessionManager().requireAdmin(adminTok);
                List<ReservationResponse> pending = api.pendingReservationsForAdmin();
                sendJson(exchange, 200, JsonUtil.toJsonArrayReservations(pending));
                return;
            }

            if ("POST".equals(method) && path.startsWith("/api/admin/reservations/")
                    && path.endsWith("/confirm")) {
                String adminTok = exchange.getRequestHeaders().getFirst(HDR_ADMIN);
                long rid = parseIdAfterPrefix(path, "/api/admin/reservations/", "/confirm");
                ReservationResponse r = api.adminConfirm(adminTok, rid);
                sendJson(exchange, 200, JsonUtil.toJson(r));
                return;
            }

            if ("POST".equals(method) && path.startsWith("/api/admin/reservations/")
                    && path.endsWith("/reject")) {
                String adminTok = exchange.getRequestHeaders().getFirst(HDR_ADMIN);
                long rid = parseIdAfterPrefix(path, "/api/admin/reservations/", "/reject");
                ReservationResponse r = api.adminReject(adminTok, rid);
                sendJson(exchange, 200, JsonUtil.toJson(r));
                return;
            }

            if ("DELETE".equals(method) && path.startsWith("/api/admin/reservations/")) {
                String adminTok = exchange.getRequestHeaders().getFirst(HDR_ADMIN);
                long rid = Long.parseLong(path.substring("/api/admin/reservations/".length()));
                api.adminDeleteConfirmedReservation(adminTok, rid);
                sendJson(exchange, 200, "{\"ok\":true}");
                return;
            }

            if ("GET".equals(method) && "/api/admin/rooms".equals(path)) {
                String adminTok = exchange.getRequestHeaders().getFirst(HDR_ADMIN);
                api.getSessionManager().requireAdmin(adminTok);
                sendJson(exchange, 200, JsonUtil.toJsonArrayRooms(api.listAllRoomsForAdmin()));
                return;
            }

            if ("POST".equals(method) && "/api/admin/rooms".equals(path)) {
                String adminTok = exchange.getRequestHeaders().getFirst(HDR_ADMIN);
                api.getSessionManager().requireAdmin(adminTok);
                JsonUtil.RoomMutateBody rb = JsonUtil.parseRoomMutateBody(readBody(exchange));
                RoomType t = RoomType.valueOf(rb.roomType().trim().toUpperCase());
                RoomResponse created = api.adminCreateRoom(t, rb.roomNumber(), rb.maxGuests(),
                        rb.description(), rb.baseRatePerNight());
                sendJson(exchange, 200, JsonUtil.toJson(created));
                return;
            }

            if ("PUT".equals(method) && path.startsWith("/api/admin/rooms/")) {
                String adminTok = exchange.getRequestHeaders().getFirst(HDR_ADMIN);
                api.getSessionManager().requireAdmin(adminTok);
                long roomId = Long.parseLong(path.substring("/api/admin/rooms/".length()));
                JsonUtil.RoomMutateBody rb = JsonUtil.parseRoomMutateBody(readBody(exchange));
                RoomType t = RoomType.valueOf(rb.roomType().trim().toUpperCase());
                RoomResponse updated = api.adminUpdateRoom(roomId, t, rb.roomNumber(), rb.maxGuests(),
                        rb.description(), rb.baseRatePerNight());
                sendJson(exchange, 200, JsonUtil.toJson(updated));
                return;
            }

            if ("DELETE".equals(method) && path.startsWith("/api/admin/rooms/")) {
                String adminTok = exchange.getRequestHeaders().getFirst(HDR_ADMIN);
                api.getSessionManager().requireAdmin(adminTok);
                long roomId = Long.parseLong(path.substring("/api/admin/rooms/".length()));
                api.adminDeleteRoom(roomId);
                sendJson(exchange, 200, "{\"ok\":true}");
                return;
            }

            sendJson(exchange, 404, JsonUtil.errorObject("Not found"));
        } catch (SecurityException ex) {
            sendJson(exchange, 401, JsonUtil.errorObject(ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, JsonUtil.errorObject(ex.getMessage()));
        } catch (IllegalStateException ex) {
            sendJson(exchange, 409, JsonUtil.errorObject(ex.getMessage()));
        } catch (Exception ex) {
            sendJson(exchange, 500, JsonUtil.errorObject(ex.getMessage() != null ? ex.getMessage() : "Server error"));
        }
    }

    private static long parseIdAfterPrefix(String path, String prefix, String suffix) {
        if (!path.startsWith(prefix) || !path.endsWith(suffix)) {
            throw new IllegalArgumentException("Bad path");
        }
        String mid = path.substring(prefix.length(), path.length() - suffix.length());
        return Long.parseLong(mid);
    }

    private static void addCors(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, " + HDR_SESSION + ", " + HDR_ADMIN);
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        byte[] raw = is.readAllBytes();
        return new String(raw, StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null || raw.isEmpty()) {
            return map;
        }
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                String key = urlDecode(pair.substring(0, eq));
                String val = urlDecode(pair.substring(eq + 1));
                map.put(key, val);
            } else if (!pair.isEmpty()) {
                map.put(urlDecode(pair), "");
            }
        }
        return map;
    }

    private static String urlDecode(String s) {
        return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private static RoomType parseRoomType(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return RoomType.valueOf(s.trim().toUpperCase());
    }

    private static Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return Integer.parseInt(s.trim());
    }

    private static LocalDate parseDateOrNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return LocalDate.parse(s.trim());
    }

    private static String normalizePath(String raw) {
        if (raw == null || raw.isBlank()) {
            return "/";
        }
        String p = raw.startsWith("/") ? raw : "/" + raw;
        p = p.replaceAll("/{2,}", "/");
        if (p.length() > 1 && p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }
}
