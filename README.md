# Hotel Reservation System (Pure Java SE + Tailwind CLI)

Full-stack **university OOP portfolio** project: a **Java SE** backend using only the JDK (`com.sun.net.httpserver.HttpServer`), plus a static frontend built with the **Tailwind CLI**.

The domain model highlights **abstract classes**, **interfaces**, **inheritance**, **composition**, **equals/hashCode**, **Strategy** (pricing), and **Observer** (admin decision notifications). There is **no Spring Boot, Hibernate, JDBC, or external DB driver**.

---

## Features (stateful app)

| Area | Behaviour |
|------|-----------|
| **Guest auth** | Sign up / log in on `auth.html`. Passwords stored as **SHA-256** hashes (see `PasswordHasher`). Sessions use random tokens in **`localStorage`** (`hotelOopGuest`). |
| **Booking** | Logged-in guests submit **only dates**; the server binds the booking to their account. New reservations are **`PENDING`** until an admin acts. |
| **Admin** | Separate flow: password **`admin123`** on `auth.html` → `admin.html`. Token in `hotelOopAdmin`, sent as **`X-Admin-Token`**. |
| **Room CRUD** | Admin can list / create / update / delete rooms. Delete is blocked if **any** reservation references that room. |
| **Approval workflow** | Admin **Confirm** runs **`Payable`** then sets **`CONFIRMED`** and triggers **Observer** notifications. **Reject** sets **`REJECTED`** and notifies observers. Guests do **not** trigger observers. |
| **Persistence** | All rooms, registered guests, accounts, and reservations are saved under **`data/hotel-state.dat`** using **`ObjectOutputStream` / `ObjectInputStream`** (see below). |

---

## File-based persistence (how it works)

1. **Startup:** `HotelServerApp` creates a `HotelDataRoot` (in-memory maps + id counters). `HotelFilePersistence.loadOrSeed(...)` loads `data/hotel-state.dat` if it exists; otherwise it **seeds** the default five demo rooms and writes the first file.
2. **Snapshot model:** `HotelDataStore` is a **`Serializable`** DTO with simple rows (`RoomRow`, `GuestRow`, `AccountRow`, `ReservationRow`) plus `nextRoomId`, `nextGuestId`, `nextReservationId`.
3. **Save:** On each meaningful change (signup, booking, admin confirm/reject, room CRUD), services call a shared **`Runnable persist`** that rebuilds a `HotelDataStore` from `HotelDataRoot` and writes it to disk.
4. **Atomic write:** The writer streams to **`hotel-state.dat.tmp`**, then moves it over the real file (with a fallback copy if atomic move is unsupported).
5. **Sessions** (guest/admin tokens) are **not** persisted — after a JVM restart, users sign in again.

The `data/` directory is listed in **`.gitignore`** so local state is not committed by default.

---

## Project layout

```
Hotel OOP Project/
├── package.json
├── tailwind.config.js
├── frontend/public/
│   ├── index.html, auth.html, admin.html
│   ├── api-client.js, app.js, style.css
├── src/com/hotel/oop/
│   ├── HotelServerApp.java
│   ├── persistence/          # HotelDataRoot, HotelDataStore, HotelFilePersistence
│   ├── http/
│   ├── controllers/dto/
│   ├── models/
│   ├── patterns/
│   └── services/             # Auth, sessions, rooms, reservations, payment
├── scripts/compile.sh
├── docs/class-diagram.puml
└── README.md
```

---

## Prerequisites

| Tool | Notes |
|------|--------|
| **JDK 17+** | `record`, `HexFormat`, `HttpServer`, etc. |
| **Node.js + npm** | Tailwind CLI |

---

## Build CSS

```bash
npm install
npm run build:css
# or: npm run watch:css
```

---

## Compile & run Java

From the **project root**:

```bash
./scripts/compile.sh
java -cp out com.hotel.oop.HotelServerApp
```

- Guest site: [http://localhost:8080/](http://localhost:8080/) — use **Sign in** before booking.
- Auth / admin entry: [http://localhost:8080/auth.html](http://localhost:8080/auth.html) (use **Admin login** for staff).
- Dashboard (requires admin token): [http://localhost:8080/admin.html](http://localhost:8080/admin.html) — redirects to auth if not logged in as admin.

Watch the terminal for **Observer** log lines when you confirm or reject bookings.

---

## REST API (summary)

**Public**

| Method | Path | Notes |
|--------|------|--------|
| `GET` | `/api/rooms` | Search / availability (optional `checkIn`/`checkOut`) |
| `GET` | `/api/rooms/{id}` | Detail + optional quote |

**Auth (no prior session)**

| Method | Path | Body (JSON) |
|--------|------|-------------|
| `POST` | `/api/auth/signup` | `fullName`, `email`, `password`, `confirmPassword` |
| `POST` | `/api/auth/login` | `email`, `password` |
| `POST` | `/api/auth/admin-login` | `password` only (demo: `admin123`) |
| `POST` | `/api/auth/logout` | Headers: guest `X-Session-Token` and/or admin `X-Admin-Token` |

**Guest (header `X-Session-Token`)**

| Method | Path | Body |
|--------|------|------|
| `GET` | `/api/auth/me` | — |
| `POST` | `/api/reservations` | `roomId`, `checkIn`, `checkOut` |

**Admin (header `X-Admin-Token`)**

| Method | Path | Notes |
|--------|------|--------|
| `GET` | `/api/reservations` | All reservations |
| `GET` | `/api/admin/reservations/pending` | Pending only |
| `POST` | `/api/admin/reservations/{id}/confirm` | Payment + observers |
| `POST` | `/api/admin/reservations/{id}/reject` | Observers |
| `GET` | `/api/admin/rooms` | List rooms |
| `POST` | `/api/admin/rooms` | Create (`roomType`, `roomNumber`, `maxGuests`, `description`, `baseRatePerNight`) |
| `PUT` | `/api/admin/rooms/{id}` | Update |
| `DELETE` | `/api/admin/rooms/{id}` | Delete |

CORS allows headers: `Content-Type`, `X-Session-Token`, `X-Admin-Token`.

---

## OOP & patterns

- **Abstract classes:** `Room`, `User`
- **Interfaces:** `Bookable`, `Payable`, `Searchable`
- **Inheritance:** `StandardRoom` / `SuiteRoom`, `Guest` / `Admin`
- **Composition:** `Reservation` has `Room` + `Guest`
- **Strategy:** pricing strategies + `PricingStrategyFactory`
- **Observer:** `ReservationObserver` with **`onReservationConfirmed`** and **`onReservationRejected`** — invoked only from admin confirm/reject paths

UML: [`docs/class-diagram.puml`](docs/class-diagram.puml)

---

## License

Educational / portfolio use.
